package com.salco.dragonfly.feature.accounts

import com.salco.dragonfly.core.data.repository.AccountsRepository
import com.salco.dragonfly.core.data.repository.UserDataRepository
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
