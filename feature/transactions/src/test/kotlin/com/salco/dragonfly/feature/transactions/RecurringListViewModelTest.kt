package com.salco.dragonfly.feature.transactions

import com.salco.dragonfly.core.testing.FakeAccountsRepository
import com.salco.dragonfly.core.testing.FakeRecurringTransactionsRepository
import com.salco.dragonfly.core.testing.FakeTransactionsRepository
import com.salco.dragonfly.core.testing.FakeUserDataRepository
import com.salco.dragonfly.core.testing.MainDispatcherRule
import com.salco.dragonfly.feature.transactions.recurring.RecurringListUiState
import com.salco.dragonfly.feature.transactions.recurring.RecurringListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accounts = FakeAccountsRepository()
    private val recurring = FakeRecurringTransactionsRepository(FakeTransactionsRepository(accounts), accounts)
    private val userData = FakeUserDataRepository()

    @Test
    fun notificationsAreWantedUntilDeclined() = runTest {
        val viewModel = RecurringListViewModel(recurring, userData)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertTrue((viewModel.uiState.value as RecurringListUiState.Success).notificationsWanted)

        viewModel.declineNotifications()
        assertFalse((viewModel.uiState.value as RecurringListUiState.Success).notificationsWanted)
        assertFalse(userData.userPreferences.first().recurringNotificationsEnabled)
    }
}
