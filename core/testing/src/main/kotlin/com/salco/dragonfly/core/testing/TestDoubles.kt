package com.salco.dragonfly.core.testing

import com.salco.dragonfly.core.data.repository.AccountsRepository
import com.salco.dragonfly.core.data.repository.TransactionsRepository
import com.salco.dragonfly.core.data.repository.UserDataRepository
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.AccountSummary
import com.salco.dragonfly.core.model.Transaction
import com.salco.dragonfly.core.model.TransactionDraft
import java.time.LocalDate
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
}

/** In-memory transactions that resolve accounts against [accounts], mirroring the Room implementation. */
class FakeTransactionsRepository(
    private val accounts: FakeAccountsRepository = FakeAccountsRepository(),
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
