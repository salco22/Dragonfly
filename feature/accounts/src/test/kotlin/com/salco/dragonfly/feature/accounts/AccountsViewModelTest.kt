package com.salco.dragonfly.feature.accounts

import app.cash.turbine.test
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.feature.accounts.list.AccountsUiState
import com.salco.dragonfly.feature.accounts.list.AccountsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import com.salco.dragonfly.core.testing.FakeAccountsRepository
import com.salco.dragonfly.core.testing.FakeUserDataRepository
import com.salco.dragonfly.core.testing.MainDispatcherRule
import org.junit.Rule
import org.junit.Test

class AccountsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAccountsRepository()

    @Test
    fun emptyRepository_showsEmptyState() = runTest {
        AccountsViewModel(repository).uiState.test {
            assertEquals(AccountsUiState.Empty, awaitItem())
        }
    }

    @Test
    fun netWorth_sumsActiveAssetsAndLiabilitiesPerCurrency() = runTest {
        repository.accounts.value = listOf(
            Account(id = 1, name = "Conto", type = AccountType.ASSET, currencyCode = "EUR", openingBalance = 100_000),
            Account(id = 2, name = "Mutuo", type = AccountType.LIABILITY, currencyCode = "EUR", openingBalance = -30_000),
            Account(id = 3, name = "Chiuso", type = AccountType.ASSET, currencyCode = "EUR", openingBalance = 5_000, active = false),
            Account(id = 4, name = "Supermercato", type = AccountType.EXPENSE, currencyCode = "EUR"),
            Account(id = 5, name = "USD", type = AccountType.ASSET, currencyCode = "USD", openingBalance = 2_500),
        )

        AccountsViewModel(repository).uiState.test {
            val state = awaitItem() as AccountsUiState.Success
            assertEquals(mapOf("EUR" to 70_000L, "USD" to 2_500L), state.netWorthByCurrency)
            // Payees (expense accounts) are not listed.
            assertEquals(listOf(AccountType.ASSET, AccountType.LIABILITY), state.sections.keys.toList())
        }
    }
}
