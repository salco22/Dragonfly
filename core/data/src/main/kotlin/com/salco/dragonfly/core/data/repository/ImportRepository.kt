package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.database.DatabaseTransactionRunner
import com.salco.dragonfly.core.database.dao.AccountDao
import com.salco.dragonfly.core.database.dao.BillDao
import com.salco.dragonfly.core.database.dao.BudgetDao
import com.salco.dragonfly.core.database.model.AccountEntity
import com.salco.dragonfly.core.database.model.BudgetEntity
import com.salco.dragonfly.core.database.dao.TransactionDao
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.FireflyAccount
import com.salco.dragonfly.core.model.FireflyAccountKind
import com.salco.dragonfly.core.model.FireflyExport
import com.salco.dragonfly.core.model.FireflyTransaction
import com.salco.dragonfly.core.model.FireflyType
import com.salco.dragonfly.core.model.LiabilityType
import com.salco.dragonfly.core.model.TransactionGroupDraft
import com.salco.dragonfly.core.model.ImportRow
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionType
import kotlinx.coroutines.flow.first
import java.time.ZoneOffset
import javax.inject.Inject

data class ImportResult(val imported: Int, val duplicates: Int, val accountsCreated: Int = 0)

/** What importing a Firefly III export would do with the data already here. */
data class FireflyImportPreview(
    /** Own accounts of the export not found here by name: they will be created. */
    val newAccounts: List<String>,
    val existingAccounts: List<String>,
    /** Movements already present (same account, day, amount and description). */
    val duplicates: Int,
)

interface ImportRepository {
    /** True for the rows already present on [accountId] (same day, amount and description). */
    suspend fun findDuplicates(accountId: Long, rows: List<ImportRow.Valid>): Set<Int>

    /**
     * Creates the [rows] as transactions on [accountId], atomically. Rows without a payee/payer go
     * to [unnamedCounterparty]; the user's rules run on each new transaction as usual.
     */
    suspend fun importRows(
        accountId: Long,
        rows: List<ImportRow.Valid>,
        skipDuplicates: Boolean,
        unnamedCounterparty: String,
    ): ImportResult

    suspend fun previewFirefly(export: FireflyExport): FireflyImportPreview

    /**
     * Imports a whole Firefly III export, atomically: creates the missing own accounts with their
     * opening balances, budgets (inactive, with no amount: the export has none), and every
     * movement with its category, budget, bill (when one with that name exists) and tags.
     * Payments with no named counterparty go to [unnamedCounterparty], reconciliations to
     * [reconciliationName]. Rules do not run: the data is already sorted.
     */
    suspend fun importFirefly(
        export: FireflyExport,
        skipDuplicates: Boolean,
        unnamedCounterparty: String,
        reconciliationName: String,
    ): ImportResult
}

internal class OfflineImportRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val budgetDao: BudgetDao,
    private val billDao: BillDao,
    private val transactionsRepository: TransactionsRepository,
    private val runInTransaction: DatabaseTransactionRunner,
) : ImportRepository {

    override suspend fun findDuplicates(accountId: Long, rows: List<ImportRow.Valid>): Set<Int> =
        rows.filter { it.isDuplicateOn(accountId) }.map { it.line }.toSet()

    override suspend fun importRows(
        accountId: Long,
        rows: List<ImportRow.Valid>,
        skipDuplicates: Boolean,
        unnamedCounterparty: String,
    ): ImportResult = runInTransaction {
        val account = accountDao.getById(accountId) ?: error("Account $accountId not found")
        var imported = 0
        var duplicates = 0
        rows.forEach { row ->
            // Checked before saving each row, so repeated lines in the same file count once.
            if (skipDuplicates && row.isDuplicateOn(accountId)) {
                duplicates++
                return@forEach
            }
            val own = AccountRef.Existing(accountId)
            val counterpartyName = row.counterparty ?: unnamedCounterparty
            val withdrawal = row.type == TransactionType.WITHDRAWAL
            val counterparty = AccountRef.New(
                counterpartyName,
                if (withdrawal) AccountType.EXPENSE else AccountType.REVENUE,
                account.currencyCode,
            )
            transactionsRepository.saveTransaction(
                TransactionDraft(
                    type = row.type,
                    description = row.description.ifBlank { counterpartyName },
                    date = row.date,
                    amount = row.amount,
                    currencyCode = account.currencyCode,
                    source = if (withdrawal) own else counterparty,
                    destination = if (withdrawal) counterparty else own,
                    notes = row.notes,
                    categoryName = row.category,
                    tagNames = row.tags,
                ),
            )
            imported++
        }
        ImportResult(imported, duplicates)
    }

    override suspend fun previewFirefly(export: FireflyExport): FireflyImportPreview {
        val existing = ownAccountsByName()
        val (found, missing) = export.ownAccounts.keys.partition { it.lowercase() in existing }
        val duplicates = export.movements.count { t -> ownAccountOf(t)?.let { existing[it.lowercase()] }?.let { t.isDuplicateOn(it.id) } == true }
        return FireflyImportPreview(newAccounts = missing.sorted(), existingAccounts = found.sorted(), duplicates = duplicates)
    }

    override suspend fun importFirefly(
        export: FireflyExport,
        skipDuplicates: Boolean,
        unnamedCounterparty: String,
        reconciliationName: String,
    ): ImportResult = runInTransaction {
        val accounts = ownAccountsByName().mapValues { it.value.id }.toMutableMap()
        // Decided before writing anything: two identical rows in the file (say, two equal lunches on the
        // same day) are both real, only rows matching data that was already here are duplicates.
        val alreadyHere = if (!skipDuplicates) emptySet() else export.movements.filter { t ->
            ownAccountOf(t)?.let { accounts[it.lowercase()] }?.let { t.isDuplicateOn(it) } == true
        }.map { it.journalId }.toSet()
        var created = 0
        export.ownAccounts.forEach { (name, kindAndCurrency) ->
            if (name.lowercase() in accounts) return@forEach
            val (kind, currency) = kindAndCurrency
            val firstDate = export.transactions.filter { it.source.name == name || it.destination.name == name }.minOf { it.date }
            accounts[name.lowercase()] = accountDao.upsert(
                AccountEntity(
                    name = name,
                    type = if (kind == FireflyAccountKind.LIABILITY) AccountType.LIABILITY else AccountType.ASSET,
                    currencyCode = currency,
                    openingBalance = export.openingBalances[name] ?: 0,
                    iban = null,
                    notes = null,
                    active = true,
                    includeInNetWorth = true,
                    assetRole = null,
                    liabilityType = if (kind == FireflyAccountKind.LIABILITY) LiabilityType.DEBT else null,
                    createdAt = firstDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                ),
            )
            created++
        }
        val currencies = export.ownAccounts.mapKeys { it.key.lowercase() }.mapValues { it.value.second }
        val budgets = export.budgets.associateWith { name ->
            budgetDao.findByName(name)?.id ?: budgetDao.upsert(
                BudgetEntity(name = name, amount = 0, currencyCode = currencies.values.firstOrNull() ?: "EUR", active = false),
            )
        }
        val bills = export.transactions.mapNotNull { it.bill }.toSet().associateWith { billDao.findByName(it)?.id }

        var imported = 0
        var duplicates = 0
        export.movements.groupBy { it.groupId }.values.forEach { parts ->
            val drafts = parts.mapNotNull { t ->
                val draft = t.asDraft(accounts, currencies, budgets, bills, unnamedCounterparty, reconciliationName) ?: return@mapNotNull null
                if (t.journalId in alreadyHere) {
                    duplicates++
                    return@mapNotNull null
                }
                draft
            }
            // Parts of one split transaction share type and own account; anything else is saved apart.
            val splittable = drafts.size > 1 && drafts.map { it.type }.distinct().size == 1 &&
                drafts.map { if (it.type == TransactionType.DEPOSIT) it.destination else it.source }.distinct().size == 1
            if (splittable) {
                transactionsRepository.saveSplitTransaction(
                    TransactionGroupDraft(title = parts.first().groupTitle ?: parts.first().description, splits = drafts),
                    applyRules = false,
                )
            } else {
                drafts.forEach { transactionsRepository.saveTransaction(it, applyRules = false) }
            }
            imported += drafts.size
        }
        ImportResult(imported = imported, duplicates = duplicates, accountsCreated = created)
    }

    private suspend fun ownAccountsByName() = accountDao.observeAll().first().map { it.account }
        .filter { it.type == AccountType.ASSET || it.type == AccountType.LIABILITY }
        .associateBy { it.name.lowercase() }

    /** The user's own account a movement is recorded on. */
    private fun ownAccountOf(t: FireflyTransaction): String? = when {
        t.type == FireflyType.DEPOSIT -> t.destination.name.takeIf { t.destination.isOwn }
        t.source.isOwn -> t.source.name
        t.destination.isOwn -> t.destination.name
        else -> null
    }

    private fun FireflyTransaction.asDraft(
        accounts: Map<String, Long>,
        currencies: Map<String, String>,
        budgets: Map<String, Long>,
        bills: Map<String, Long?>,
        unnamed: String,
        reconciliationName: String,
    ): TransactionDraft? {
        fun own(account: FireflyAccount) = accounts[account.name.lowercase()]?.let(AccountRef::Existing)
        fun named(account: FireflyAccount) = if (account.kind == FireflyAccountKind.CASH) unnamed else account.name
        val (kind, from, to) = when (type) {
            FireflyType.WITHDRAWAL -> Triple(
                TransactionType.WITHDRAWAL,
                own(source) ?: return null,
                own(destination) ?: AccountRef.New(named(destination), AccountType.EXPENSE, currencyCode),
            )
            FireflyType.DEPOSIT -> Triple(
                TransactionType.DEPOSIT,
                own(source) ?: AccountRef.New(named(source), AccountType.REVENUE, currencyCode),
                own(destination) ?: return null,
            )
            FireflyType.TRANSFER -> Triple(TransactionType.TRANSFER, own(source) ?: return null, own(destination) ?: return null)
            // Money appearing on (or disappearing from) an account to match the bank.
            FireflyType.RECONCILIATION -> if (destination.isOwn) {
                Triple(TransactionType.DEPOSIT, AccountRef.New(reconciliationName, AccountType.REVENUE, currencyCode), own(destination) ?: return null)
            } else {
                Triple(TransactionType.WITHDRAWAL, own(source) ?: return null, AccountRef.New(reconciliationName, AccountType.EXPENSE, currencyCode))
            }
            FireflyType.OPENING_BALANCE -> return null
        }
        val destinationCurrency = currencies[destination.name.lowercase()]
        return TransactionDraft(
            type = kind,
            description = description.ifBlank { named(if (kind == TransactionType.DEPOSIT) source else destination) },
            date = date,
            amount = amount,
            currencyCode = currencyCode,
            foreignAmount = foreignAmount.takeIf { kind == TransactionType.TRANSFER && destinationCurrency != null && destinationCurrency != currencyCode },
            source = from,
            destination = to,
            notes = notes,
            categoryName = category,
            tagNames = tags,
            budgetId = budget?.let { budgets[it] }.takeIf { kind == TransactionType.WITHDRAWAL },
            billId = bill?.let { bills[it] }.takeIf { kind == TransactionType.WITHDRAWAL },
        )
    }

    private suspend fun FireflyTransaction.isDuplicateOn(accountId: Long): Boolean =
        transactionDao.countSimilar(accountId, date.toEpochDay(), amount, description) > 0

    private suspend fun ImportRow.Valid.isDuplicateOn(accountId: Long): Boolean =
        transactionDao.countSimilar(accountId, date.toEpochDay(), amount, description.ifBlank { counterparty.orEmpty() }) > 0
}
