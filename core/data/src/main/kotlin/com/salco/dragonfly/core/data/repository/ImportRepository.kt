package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.database.DatabaseTransactionRunner
import com.salco.dragonfly.core.database.dao.AccountDao
import com.salco.dragonfly.core.database.dao.TransactionDao
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.ImportRow
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionType
import javax.inject.Inject

data class ImportResult(val imported: Int, val duplicates: Int)

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
}

internal class OfflineImportRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
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

    private suspend fun ImportRow.Valid.isDuplicateOn(accountId: Long): Boolean =
        transactionDao.countSimilar(accountId, date.toEpochDay(), amount, description.ifBlank { counterparty.orEmpty() }) > 0
}
