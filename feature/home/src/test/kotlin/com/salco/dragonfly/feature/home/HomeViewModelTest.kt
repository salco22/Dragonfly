package com.salco.dragonfly.feature.home

import app.cash.turbine.test
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.Bill
import com.salco.dragonfly.core.model.BillStatus
import com.salco.dragonfly.core.model.Budget
import com.salco.dragonfly.core.model.RecurrenceFrequency
import com.salco.dragonfly.core.model.RecurrenceRule
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.testing.FakeAccountsRepository
import com.salco.dragonfly.core.testing.FakeBillsRepository
import com.salco.dragonfly.core.testing.FakeBudgetsRepository
import com.salco.dragonfly.core.testing.FakeCategoriesRepository
import com.salco.dragonfly.core.testing.FakeInsightsRepository
import com.salco.dragonfly.core.testing.FakePiggyBanksRepository
import com.salco.dragonfly.core.testing.FakeTagsRepository
import com.salco.dragonfly.core.testing.FakeTransactionsRepository
import com.salco.dragonfly.core.testing.FakeUserDataRepository
import com.salco.dragonfly.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accounts = FakeAccountsRepository()
    private val budgets = FakeBudgetsRepository()
    private val bills = FakeBillsRepository()
    private val transactions = FakeTransactionsRepository(accounts, FakeCategoriesRepository(), FakeTagsRepository(), budgets, bills)
    private val clock = Clock.fixed(LocalDate.of(2026, 10, 18).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private fun viewModel() = HomeViewModel(
        FakeUserDataRepository(currency = "EUR"), accounts, FakeInsightsRepository(accounts, transactions), budgets, bills,
        FakePiggyBanksRepository(accounts), clock,
    )

    private suspend fun add(type: TransactionType, date: LocalDate, amount: Long, other: String, category: String? = null) =
        transactions.saveTransaction(
            TransactionDraft(
                type = type, description = other, date = date, amount = amount, currencyCode = "EUR",
                source = if (type == TransactionType.DEPOSIT) AccountRef.New(other, AccountType.REVENUE, "EUR") else AccountRef.Existing(1),
                destination = if (type == TransactionType.DEPOSIT) AccountRef.Existing(1) else AccountRef.New(other, AccountType.EXPENSE, "EUR"),
                categoryName = category,
            ),
        )

    @Test
    fun withoutAccounts_showsWelcome() = runTest {
        viewModel().uiState.test {
            assertFalse((awaitItem() as HomeUiState.Success).hasAccounts)
        }
    }

    @Test
    fun dashboardAggregatesTheMonth() = runTest {
        accounts.saveAccount(Account(name = "Conto", type = AccountType.ASSET, currencyCode = "EUR", openingBalance = 100_000))
        add(TransactionType.DEPOSIT, LocalDate.of(2026, 9, 27), 200_000, "Azienda")
        add(TransactionType.WITHDRAWAL, LocalDate.of(2026, 10, 3), 30_000, "Supermercato", "Spesa")
        add(TransactionType.WITHDRAWAL, LocalDate.of(2026, 10, 9), 5_000, "Bar", "Svago")
        budgets.saveBudget(Budget(name = "Svago", amount = 4_000, currencyCode = "EUR"))
        bills.saveBill(
            Bill(name = "Luce", amountMin = 6_000, amountMax = 6_000, currencyCode = "EUR",
                rule = RecurrenceRule(RecurrenceFrequency.MONTHLY, startDate = LocalDate.of(2026, 1, 25))),
        )
        bills.saveBill(
            Bill(name = "Assicurazione", amountMin = 30_000, amountMax = 30_000, currencyCode = "EUR",
                rule = RecurrenceRule(RecurrenceFrequency.YEARLY, startDate = LocalDate.of(2027, 3, 1))),
        )

        viewModel().uiState.test {
            val state = expectMostRecentItem() as HomeUiState.Success
            assertTrue(state.hasAccounts)
            // The fake account balance ignores transactions; the history includes them.
            assertEquals(YearMonth.of(2026, 10), state.netWorthHistory.last().month)
            assertEquals(100_000L + 200_000 - 35_000, state.netWorthHistory.last().amount)
            assertEquals(12, state.netWorthHistory.size)
            assertEquals(6, state.cashFlow.size)
            assertEquals(35_000L, state.thisMonth.expenses)
            assertEquals(200_000L, state.cashFlow[4].income)
            assertEquals(listOf("Spesa", "Svago"), state.categorySpending.map { it.categoryName })
            // The insurance starts next year, so only the electricity bill is due within two weeks.
            assertEquals(listOf("Luce"), state.bills.map { it.first.name })
            assertEquals(BillStatus.Due(LocalDate.of(2026, 10, 25)), state.bills.single().second)
        }
    }
}
