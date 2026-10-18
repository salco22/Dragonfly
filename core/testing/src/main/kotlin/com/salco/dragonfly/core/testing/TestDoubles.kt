package com.salco.dragonfly.core.testing

import com.salco.dragonfly.core.data.repository.AccountsRepository
import com.salco.dragonfly.core.data.repository.BillsRepository
import com.salco.dragonfly.core.data.repository.BudgetsRepository
import com.salco.dragonfly.core.data.repository.CategoriesRepository
import com.salco.dragonfly.core.data.repository.ImportRepository
import com.salco.dragonfly.core.data.repository.ImportResult
import com.salco.dragonfly.core.data.repository.InsightsRepository
import com.salco.dragonfly.core.data.repository.InsufficientFundsException
import com.salco.dragonfly.core.data.repository.PiggyBanksRepository
import com.salco.dragonfly.core.data.repository.RecurringTransactionsRepository
import com.salco.dragonfly.core.data.repository.RulesRepository
import com.salco.dragonfly.core.data.repository.DuplicateNameException
import com.salco.dragonfly.core.data.repository.TagsRepository
import com.salco.dragonfly.core.data.repository.TransactionsRepository
import com.salco.dragonfly.core.model.Bill
import com.salco.dragonfly.core.model.BillPayment
import com.salco.dragonfly.core.model.BillRef
import com.salco.dragonfly.core.model.Budget
import com.salco.dragonfly.core.model.BudgetRef
import com.salco.dragonfly.core.model.BudgetSpending
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.Category
import com.salco.dragonfly.core.model.ImportRow
import com.salco.dragonfly.core.model.CategorySpending
import com.salco.dragonfly.core.model.MonthlySummary
import com.salco.dragonfly.core.model.NetWorthPoint
import com.salco.dragonfly.core.model.PiggyBank
import com.salco.dragonfly.core.model.PiggyBankEvent
import com.salco.dragonfly.core.model.PiggyBankSummary
import com.salco.dragonfly.core.model.RecurringTransaction
import com.salco.dragonfly.core.model.Rule
import com.salco.dragonfly.core.model.RuleSubject
import com.salco.dragonfly.core.model.applyRules
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.CategorySummary
import com.salco.dragonfly.core.model.Tag
import com.salco.dragonfly.core.model.TagSummary
import com.salco.dragonfly.core.data.repository.UserDataRepository
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.AccountSummary
import com.salco.dragonfly.core.model.Transaction
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionFilter
import java.time.LocalDate
import java.time.YearMonth
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

class FakeAccountsRepository : AccountsRepository {
    val accounts = MutableStateFlow<List<Account>>(emptyList())
    private var nextId = 1L

    override fun observeAccounts(): Flow<List<Account>> = accounts
    override fun observeAccount(id: Long): Flow<Account?> = accounts.map { list -> list.find { it.id == id } }

    override suspend fun saveAccount(account: Account): Long {
        val saved = if (account.id == 0L) account.copy(id = nextId++) else account
        accounts.update { list -> list.filterNot { it.id == saved.id } + saved }
        return saved.id
    }

    override suspend fun deleteAccount(id: Long) = accounts.update { list -> list.filterNot { it.id == id } }
}

class FakeUserDataRepository(currency: String = "EUR") : UserDataRepository {
    private val prefs = MutableStateFlow(UserPreferences(appLockEnabled = false, defaultCurrencyCode = currency))
    override val userPreferences: Flow<UserPreferences> = prefs
    override suspend fun setAppLockEnabled(enabled: Boolean) = prefs.update { it.copy(appLockEnabled = enabled) }
    override suspend fun setDefaultCurrencyCode(currencyCode: String) =
        prefs.update { it.copy(defaultCurrencyCode = currencyCode) }
    override suspend fun setAutoBackupFolder(uri: String?) = prefs.update { it.copy(autoBackupFolder = uri) }
    override suspend fun setLastBackupAt(epochMillis: Long) = prefs.update { it.copy(lastBackupAt = epochMillis) }
}

/** In-memory transactions that resolve accounts against [accounts], mirroring the Room implementation. */
class FakeTransactionsRepository(
    private val accounts: FakeAccountsRepository = FakeAccountsRepository(),
    private val categories: FakeCategoriesRepository = FakeCategoriesRepository(),
    private val tags: FakeTagsRepository = FakeTagsRepository(),
    private val budgets: FakeBudgetsRepository = FakeBudgetsRepository(),
    private val bills: FakeBillsRepository = FakeBillsRepository(),
) : TransactionsRepository {
    val transactions = MutableStateFlow<List<Transaction>>(emptyList())
    private var nextId = 1L

    override fun observeTransactions(from: LocalDate, to: LocalDate): Flow<List<Transaction>> =
        transactions.map { list -> list.filter { it.date in from..to }.sortedWith(newestFirst) }

    override fun observeAccountTransactions(accountId: Long): Flow<List<Transaction>> =
        transactions.map { list ->
            list.filter { it.source.id == accountId || it.destination.id == accountId }.sortedWith(newestFirst)
        }

    override fun observeTransaction(id: Long): Flow<Transaction?> = transactions.map { list -> list.find { it.id == id } }

    override fun searchTransactions(filter: TransactionFilter): Flow<List<Transaction>> =
        transactions.map { list -> list.filter { it.matches(filter) }.sortedWith(newestFirst) }

    private fun Transaction.matches(filter: TransactionFilter): Boolean {
        val query = filter.query?.trim()?.takeIf { it.isNotEmpty() }
        val texts = listOfNotNull(description, notes, source.name, destination.name, category?.name) + tags.map { it.name }
        return (query == null || texts.any { it.contains(query, ignoreCase = true) }) &&
            (filter.type == null || type == filter.type) &&
            (filter.categoryId == null || category?.id == filter.categoryId) &&
            (filter.tagId == null || tags.any { it.id == filter.tagId }) &&
            (filter.budgetId == null || budget?.id == filter.budgetId) &&
            (filter.billId == null || bill?.id == filter.billId) &&
            (filter.accountId == null || source.id == filter.accountId || destination.id == filter.accountId) &&
            (filter.from == null || date >= filter.from) &&
            (filter.to == null || date <= filter.to)
    }

    override suspend fun saveTransaction(draft: TransactionDraft): Long {
        val id = if (draft.id == 0L) nextId++ else draft.id
        val transaction = Transaction(
            id = id,
            type = draft.type,
            description = draft.description,
            date = draft.date,
            amount = draft.amount,
            currencyCode = draft.currencyCode,
            foreignAmount = draft.foreignAmount,
            source = resolve(draft.source),
            destination = resolve(draft.destination),
            notes = draft.notes,
            category = draft.categoryName?.trim()?.takeIf { it.isNotEmpty() }?.let { categories.resolve(it) },
            tags = draft.tagNames.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
                .map { tags.resolve(it) },
            budget = draft.budgetId.takeIf { draft.type == TransactionType.WITHDRAWAL }
                ?.let { id -> budgets.budgets.value.firstOrNull { it.id == id } }
                ?.let { BudgetRef(it.id, it.name) },
            bill = draft.billId.takeIf { draft.type == TransactionType.WITHDRAWAL }
                ?.let { id -> bills.bills.value.firstOrNull { it.id == id } }
                ?.let { BillRef(it.id, it.name) },
        )
        transactions.update { list -> list.filterNot { it.id == id } + transaction }
        return id
    }

    override suspend fun deleteTransaction(id: Long) = transactions.update { list -> list.filterNot { it.id == id } }

    private suspend fun resolve(ref: AccountRef): AccountSummary {
        val account = when (ref) {
            is AccountRef.Existing -> accounts.accounts.value.first { it.id == ref.id }
            is AccountRef.New -> accounts.accounts.value.firstOrNull { it.type == ref.type && it.name.equals(ref.name, true) }
                ?: Account(name = ref.name, type = ref.type, currencyCode = ref.currencyCode)
                    .let { it.copy(id = accounts.saveAccount(it)) }
        }
        return AccountSummary(account.id, account.name, account.type, account.currencyCode)
    }

    private companion object {
        val newestFirst = compareByDescending<Transaction> { it.date }.thenByDescending { it.id }
    }
}

class FakeCategoriesRepository : CategoriesRepository {
    val categories = MutableStateFlow<List<Category>>(emptyList())
    private var nextId = 1L

    override fun observeCategories(): Flow<List<Category>> = categories.map { list -> list.sortedBy { it.name.lowercase() } }

    // Totals need transactions, which this fake does not know about.
    override fun observeCategorySummaries(from: LocalDate, to: LocalDate): Flow<List<CategorySummary>> =
        observeCategories().map { list -> list.map { CategorySummary(it, emptyMap(), emptyMap()) } }

    override suspend fun saveCategory(category: Category): Long {
        val name = category.name.trim()
        if (categories.value.any { it.name.equals(name, true) && it.id != category.id }) throw DuplicateNameException(name)
        val saved = category.copy(id = if (category.id == 0L) nextId++ else category.id, name = name)
        categories.update { list -> list.filterNot { it.id == saved.id } + saved }
        return saved.id
    }

    override suspend fun deleteCategory(id: Long) = categories.update { list -> list.filterNot { it.id == id } }

    suspend fun resolve(name: String): Category =
        categories.value.firstOrNull { it.name.equals(name, true) } ?: Category(name = name).let { it.copy(id = saveCategory(it)) }
}

class FakeTagsRepository : TagsRepository {
    val tags = MutableStateFlow<List<Tag>>(emptyList())
    private var nextId = 1L

    override fun observeTags(): Flow<List<Tag>> = tags.map { list -> list.sortedBy { it.name.lowercase() } }

    override fun observeTagSummaries(): Flow<List<TagSummary>> = observeTags().map { list -> list.map { TagSummary(it, 0) } }

    override suspend fun saveTag(tag: Tag): Long {
        val name = tag.name.trim()
        if (tags.value.any { it.name.equals(name, true) && it.id != tag.id }) throw DuplicateNameException(name)
        val saved = tag.copy(id = if (tag.id == 0L) nextId++ else tag.id, name = name)
        tags.update { list -> list.filterNot { it.id == saved.id } + saved }
        return saved.id
    }

    override suspend fun deleteTag(id: Long) = tags.update { list -> list.filterNot { it.id == id } }

    suspend fun resolve(name: String): Tag =
        tags.value.firstOrNull { it.name.equals(name, true) } ?: Tag(name = name).let { it.copy(id = saveTag(it)) }
}

/** Budgets whose spending is computed from [transactions] once [attach] is called. */
class FakeBudgetsRepository : BudgetsRepository {
    val budgets = MutableStateFlow<List<Budget>>(emptyList())
    private var transactions: MutableStateFlow<List<Transaction>> = MutableStateFlow(emptyList())
    private var nextId = 1L

    fun attach(repository: FakeTransactionsRepository) {
        transactions = repository.transactions
    }

    override fun observeBudgets(): Flow<List<Budget>> = budgets

    override fun observeBudget(id: Long): Flow<Budget?> = budgets.map { list -> list.find { it.id == id } }

    override fun observeBudgetSpending(from: LocalDate, to: LocalDate): Flow<List<BudgetSpending>> =
        combine(budgets, transactions) { budgets, transactions ->
            budgets.map { budget ->
                BudgetSpending(
                    budget,
                    transactions.filter {
                        it.budget?.id == budget.id && it.type == TransactionType.WITHDRAWAL &&
                            it.currencyCode == budget.currencyCode && it.date in from..to
                    }.sumOf { it.amount },
                )
            }
        }

    override fun observeUnbudgetedSpending(from: LocalDate, to: LocalDate): Flow<Map<String, Long>> =
        transactions.map { list ->
            list.filter { it.type == TransactionType.WITHDRAWAL && it.budget == null && it.date in from..to }
                .groupBy { it.currencyCode }.mapValues { (_, l) -> l.sumOf { it.amount } }
        }

    override suspend fun saveBudget(budget: Budget): Long {
        val name = budget.name.trim()
        if (budgets.value.any { it.name.equals(name, true) && it.id != budget.id }) throw DuplicateNameException(name)
        val saved = budget.copy(id = if (budget.id == 0L) nextId++ else budget.id, name = name)
        budgets.update { list -> list.filterNot { it.id == saved.id } + saved }
        return saved.id
    }

    override suspend fun deleteBudget(id: Long) {
        budgets.update { list -> list.filterNot { it.id == id } }
        transactions.update { list -> list.map { if (it.budget?.id == id) it.copy(budget = null) else it } }
    }
}

/** Keeps schedules in memory and creates their transactions through [transactions]. */
class FakeRecurringTransactionsRepository(
    private val transactions: FakeTransactionsRepository,
    private val accounts: FakeAccountsRepository,
) : RecurringTransactionsRepository {
    val recurring = MutableStateFlow<List<RecurringTransaction>>(emptyList())
    private var nextId = 1L

    override fun observeRecurringTransactions(): Flow<List<RecurringTransaction>> = recurring

    override fun observeRecurringTransaction(id: Long): Flow<RecurringTransaction?> = recurring.map { list -> list.find { it.id == id } }

    override suspend fun saveRecurringTransaction(recurring: RecurringTransaction): Long {
        val previous = this.recurring.value.find { it.id == recurring.id }
        val created = previous?.occurrencesCreated ?: 0
        val saved = recurring.copy(
            id = if (recurring.id == 0L) nextId++ else recurring.id,
            occurrencesCreated = created,
            nextDate = recurring.rule.nextAfter(created),
            sourceName = recurring.template.source.displayName(),
            destinationName = recurring.template.destination.displayName(),
        )
        this.recurring.update { list -> list.filterNot { it.id == saved.id } + saved }
        return saved.id
    }

    override suspend fun deleteRecurringTransaction(id: Long) = recurring.update { list -> list.filterNot { it.id == id } }

    override suspend fun generateDueTransactions(today: LocalDate): Int {
        var created = 0
        recurring.value = recurring.value.map { item ->
            var current = item
            while (current.active && current.nextDate != null && !current.nextDate!!.isAfter(today)) {
                transactions.saveTransaction(current.template.copy(id = 0, date = current.nextDate!!))
                val count = current.occurrencesCreated + 1
                current = current.copy(occurrencesCreated = count, nextDate = current.rule.nextAfter(count))
                created++
            }
            current
        }
        return created
    }

    private fun AccountRef.displayName(): String = when (this) {
        is AccountRef.Existing -> accounts.accounts.value.firstOrNull { it.id == id }?.name.orEmpty()
        is AccountRef.New -> name
    }
}

/** Bills whose payments come from [transactions] once [attach] is called. */
class FakeBillsRepository : BillsRepository {
    val bills = MutableStateFlow<List<Bill>>(emptyList())
    private var transactions: MutableStateFlow<List<Transaction>> = MutableStateFlow(emptyList())
    private var nextId = 1L

    fun attach(repository: FakeTransactionsRepository) {
        transactions = repository.transactions
    }

    override fun observeBills(): Flow<List<Bill>> = bills

    override fun observeBill(id: Long): Flow<Bill?> = bills.map { list -> list.find { it.id == id } }

    override fun observePayments(): Flow<List<BillPayment>> = transactions.map { list ->
        list.mapNotNull { t -> t.bill?.let { BillPayment(it.id, t.id, t.date, t.amount) } }.sortedByDescending { it.date }
    }

    override suspend fun saveBill(bill: Bill): Long {
        val name = bill.name.trim()
        if (bills.value.any { it.name.equals(name, true) && it.id != bill.id }) throw DuplicateNameException(name)
        val saved = bill.copy(id = if (bill.id == 0L) nextId++ else bill.id, name = name)
        bills.update { list -> list.filterNot { it.id == saved.id } + saved }
        return saved.id
    }

    override suspend fun deleteBill(id: Long) {
        bills.update { list -> list.filterNot { it.id == id } }
        transactions.update { list -> list.map { if (it.bill?.id == id) it.copy(bill = null) else it } }
    }
}

/** Piggy banks kept in memory; available money comes from [accounts] balances. */
class FakePiggyBanksRepository(private val accounts: FakeAccountsRepository) : PiggyBanksRepository {
    val piggyBanks = MutableStateFlow<List<PiggyBank>>(emptyList())
    val events = MutableStateFlow<List<PiggyBankEvent>>(emptyList())
    private var nextId = 1L

    private val summaries: Flow<List<PiggyBankSummary>> = combine(piggyBanks, events, accounts.accounts) { banks, events, accounts ->
        banks.mapNotNull { bank ->
            val account = accounts.firstOrNull { it.id == bank.accountId } ?: return@mapNotNull null
            PiggyBankSummary(bank, account.name, account.currencyCode, events.filter { it.piggyBankId == bank.id }.sumOf { it.amount })
        }.sortedBy { it.piggyBank.name.lowercase() }
    }

    override fun observePiggyBanks(): Flow<List<PiggyBankSummary>> = summaries
    override fun observePiggyBank(id: Long): Flow<PiggyBankSummary?> = summaries.map { list -> list.find { it.piggyBank.id == id } }
    override fun observeAccountPiggyBanks(accountId: Long): Flow<List<PiggyBankSummary>> =
        summaries.map { list -> list.filter { it.piggyBank.accountId == accountId } }
    override fun observeEvents(piggyBankId: Long): Flow<List<PiggyBankEvent>> =
        events.map { list -> list.filter { it.piggyBankId == piggyBankId }.sortedByDescending { it.date } }

    override suspend fun savePiggyBank(piggyBank: PiggyBank): Long {
        val name = piggyBank.name.trim()
        if (piggyBanks.value.any { it.name.equals(name, true) && it.id != piggyBank.id }) throw DuplicateNameException(name)
        val saved = piggyBank.copy(id = if (piggyBank.id == 0L) nextId++ else piggyBank.id, name = name)
        piggyBanks.update { list -> list.filterNot { it.id == saved.id } + saved }
        return saved.id
    }

    override suspend fun deletePiggyBank(id: Long) {
        piggyBanks.update { list -> list.filterNot { it.id == id } }
        events.update { list -> list.filterNot { it.piggyBankId == id } }
    }

    override suspend fun moveMoney(piggyBankId: Long, amount: Long, date: LocalDate) {
        val bank = piggyBanks.value.first { it.id == piggyBankId }
        if (amount > 0) {
            val balance = accounts.accounts.value.first { it.id == bank.accountId }.balance
            val savedInAccount = events.value.filter { e -> piggyBanks.value.any { it.id == e.piggyBankId && it.accountId == bank.accountId } }
                .sumOf { it.amount }
            val available = (balance - savedInAccount).coerceAtLeast(0)
            if (amount > available) throw InsufficientFundsException(available)
        } else {
            val saved = events.value.filter { it.piggyBankId == piggyBankId }.sumOf { it.amount }
            if (-amount > saved) throw InsufficientFundsException(saved)
        }
        events.update { it + PiggyBankEvent(id = nextId++, piggyBankId = piggyBankId, date = date, amount = amount) }
    }
}

/** Insights computed in memory from [transactions] and [accounts], mirroring the SQL queries. */
class FakeInsightsRepository(
    private val accounts: FakeAccountsRepository,
    private val transactions: FakeTransactionsRepository,
) : InsightsRepository {

    override fun observeMonthlySummaries(from: YearMonth, to: YearMonth): Flow<List<MonthlySummary>> =
        transactions.transactions.map { list ->
            list.filter { it.type != TransactionType.TRANSFER && YearMonth.from(it.date) in from..to }
                .groupBy { YearMonth.from(it.date) to it.currencyCode }
                .map { (key, l) ->
                    MonthlySummary(
                        key.first, key.second,
                        income = l.filter { it.type == TransactionType.DEPOSIT }.sumOf { it.amount },
                        expenses = l.filter { it.type == TransactionType.WITHDRAWAL }.sumOf { it.amount },
                    )
                }.sortedBy { it.month }
        }

    override fun observeCategorySpending(month: YearMonth): Flow<List<CategorySpending>> =
        transactions.transactions.map { list ->
            list.filter { it.type == TransactionType.WITHDRAWAL && YearMonth.from(it.date) == month }
                .groupBy { it.category?.id to it.currencyCode }
                .map { (key, l) -> CategorySpending(key.first, l.first().category?.name, key.second, l.sumOf { it.amount }) }
                .sortedByDescending { it.amount }
        }

    override fun observeNetWorthHistory(from: YearMonth, to: YearMonth): Flow<List<NetWorthPoint>> =
        combine(accounts.accounts, transactions.transactions) { accounts, transactions ->
            val included = accounts.filter {
                (it.type == AccountType.ASSET || it.type == AccountType.LIABILITY) && it.active && it.includeInNetWorth
            }
            included.map { it.currencyCode }.distinct().flatMap { currency ->
                val ids = included.filter { it.currencyCode == currency }.map { it.id }.toSet()
                val opening = included.filter { it.currencyCode == currency }.sumOf { it.openingBalance }
                generateSequence(from) { it.plusMonths(1) }.takeWhile { it <= to }.map { month ->
                    val upTo = transactions.filter { !YearMonth.from(it.date).isAfter(month) }
                    val delta = upTo.sumOf { t ->
                        (if (t.destination.id in ids) t.foreignAmount ?: t.amount else 0) - (if (t.source.id in ids) t.amount else 0)
                    }
                    NetWorthPoint(month, currency, opening + delta)
                }.toList()
            }
        }
}

/** Rules kept in memory and applied to [transactions] with the real rule engine. */
class FakeRulesRepository(
    private val transactions: FakeTransactionsRepository,
    private val categories: FakeCategoriesRepository,
    private val tags: FakeTagsRepository,
) : RulesRepository {
    val rules = MutableStateFlow<List<Rule>>(emptyList())
    private var nextId = 1L

    override fun observeRules(): Flow<List<Rule>> = rules.map { list -> list.sortedWith(compareBy({ it.position }, { it.id })) }
    override fun observeRule(id: Long): Flow<Rule?> = rules.map { list -> list.find { it.id == id } }

    override suspend fun saveRule(rule: Rule): Long {
        val saved = if (rule.id == 0L) rule.copy(id = nextId++, position = rules.value.size) else rule
        rules.update { list -> list.filterNot { it.id == saved.id } + saved }
        return saved.id
    }

    override suspend fun setRuleActive(id: Long, active: Boolean) =
        rules.update { list -> list.map { if (it.id == id) it.copy(active = active) else it } }

    override suspend fun deleteRule(id: Long) = rules.update { list -> list.filterNot { it.id == id } }

    override suspend fun countAffected(rule: Rule): Int = changes(listOf(rule.copy(active = true))).size

    override suspend fun applyToExisting(rule: Rule?): Int {
        val changed = changes(rule?.let { listOf(it.copy(active = true)) } ?: rules.value)
        changed.forEach { (transaction, subject) ->
            transactions.transactions.update { list ->
                list.map { t ->
                    if (t.id != transaction.id) t else t.copy(
                        description = subject.description,
                        notes = subject.notes,
                        category = subject.categoryName?.let { categories.resolve(it) },
                        tags = subject.tagNames.map { tags.resolve(it) },
                    )
                }
            }
        }
        return changed.size
    }

    private fun changes(rules: List<Rule>) = transactions.transactions.value.mapNotNull { t ->
        val subject = RuleSubject(
            type = t.type, description = t.description, amount = t.amount, notes = t.notes,
            accountName = if (t.type == TransactionType.DEPOSIT) t.destination.name else t.source.name,
            counterpartyName = if (t.type == TransactionType.DEPOSIT) t.source.name else t.destination.name,
            categoryName = t.category?.name, tagNames = t.tags.map { it.name }, budgetId = t.budget?.id, billId = t.bill?.id,
        )
        val result = applyRules(rules, subject)
        if (result == subject) null else t to result
    }
}

/** Imports rows into [transactions], spotting duplicates like the Room implementation. */
class FakeImportRepository(
    private val transactions: FakeTransactionsRepository,
    private val accounts: FakeAccountsRepository,
) : ImportRepository {

    private fun ImportRow.Valid.isDuplicateOn(accountId: Long) = transactions.transactions.value.any {
        (it.source.id == accountId || it.destination.id == accountId) && it.date == date && it.amount == amount &&
            it.description.equals(description, ignoreCase = true)
    }

    override suspend fun findDuplicates(accountId: Long, rows: List<ImportRow.Valid>): Set<Int> =
        rows.filter { it.isDuplicateOn(accountId) }.map { it.line }.toSet()

    override suspend fun importRows(accountId: Long, rows: List<ImportRow.Valid>, skipDuplicates: Boolean, unnamedCounterparty: String): ImportResult {
        val currency = accounts.accounts.value.first { it.id == accountId }.currencyCode
        var imported = 0
        var duplicates = 0
        rows.forEach { row ->
            if (skipDuplicates && row.isDuplicateOn(accountId)) {
                duplicates++
                return@forEach
            }
            val own = AccountRef.Existing(accountId)
            val withdrawal = row.type == TransactionType.WITHDRAWAL
            val other = AccountRef.New(row.counterparty ?: unnamedCounterparty, if (withdrawal) AccountType.EXPENSE else AccountType.REVENUE, currency)
            transactions.saveTransaction(
                TransactionDraft(
                    type = row.type, description = row.description, date = row.date, amount = row.amount, currencyCode = currency,
                    source = if (withdrawal) own else other, destination = if (withdrawal) other else own,
                    notes = row.notes, categoryName = row.category, tagNames = row.tags,
                ),
            )
            imported++
        }
        return ImportResult(imported, duplicates)
    }
}
