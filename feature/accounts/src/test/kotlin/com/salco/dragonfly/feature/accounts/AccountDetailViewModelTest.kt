package com.salco.dragonfly.feature.accounts

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.testing.FakeAccountsRepository
import com.salco.dragonfly.core.testing.FakePiggyBanksRepository
import com.salco.dragonfly.core.testing.FakeTransactionsRepository
import com.salco.dragonfly.core.testing.MainDispatcherRule
import com.salco.dragonfly.feature.accounts.detail.AccountDetailUiState
import com.salco.dragonfly.feature.accounts.detail.AccountDetailViewModel
import com.salco.dragonfly.feature.accounts.detail.FlowTotals
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class AccountDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accounts = FakeAccountsRepository()
    private val transactions = FakeTransactionsRepository(accounts)

    private suspend fun transfer(description: String, from: Long, to: Long, amount: Long, foreign: Long? = null) =
        transactions.saveTransaction(
            TransactionDraft(
                type = TransactionType.TRANSFER, description = description, date = LocalDate.of(2026, 10, 1),
                amount = amount, currencyCode = "EUR", foreignAmount = foreign,
                source = AccountRef.Existing(from), destination = AccountRef.Existing(to),
            ),
        )

    @Test
    fun showsAccountTransactionsAndTotals_andFiltersByQuery() = runTest {
        val checking = accounts.saveAccount(Account(name = "Conto", type = AccountType.ASSET, currencyCode = "EUR"))
        val savings = accounts.saveAccount(Account(name = "Risparmi", type = AccountType.ASSET, currencyCode = "EUR"))
        val other = accounts.saveAccount(Account(name = "Altro", type = AccountType.ASSET, currencyCode = "EUR"))
        transfer("Accantonamento", checking, savings, 10_000)
        transfer("Rimborso", savings, checking, 2_500)
        transfer("Non mio", checking, other, 999)

        val vm = AccountDetailViewModel(SavedStateHandle(mapOf("accountId" to savings)), accounts, transactions, FakePiggyBanksRepository(accounts))
        vm.uiState.test {
            with(awaitItem() as AccountDetailUiState.Success) {
                assertEquals("Risparmi", account.name)
                assertEquals(2, days.flatMap { it.second }.size)
                assertEquals(FlowTotals(incoming = 10_000, outgoing = 2_500), totals)
            }
            vm.onQueryChange("rimb")
            assertEquals(listOf("Rimborso"), (awaitItem() as AccountDetailUiState.Success).days.flatMap { it.second }.map { it.description })
        }
    }

    @Test
    fun deletedAccount_reportsNotFound() = runTest {
        val id = accounts.saveAccount(Account(name = "Conto", type = AccountType.ASSET, currencyCode = "EUR"))
        val vm = AccountDetailViewModel(SavedStateHandle(mapOf("accountId" to id)), accounts, transactions, FakePiggyBanksRepository(accounts))
        vm.uiState.test {
            awaitItem()
            accounts.deleteAccount(id)
            assertEquals(AccountDetailUiState.NotFound, awaitItem())
        }
    }
}
