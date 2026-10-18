package com.salco.dragonfly.feature.reports

import app.cash.turbine.test
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.Budget
import com.salco.dragonfly.core.model.PeriodLength
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.testing.FakeAccountsRepository
import com.salco.dragonfly.core.testing.FakeBudgetsRepository
import com.salco.dragonfly.core.testing.FakeCategoriesRepository
import com.salco.dragonfly.core.testing.FakeInsightsRepository
import com.salco.dragonfly.core.testing.FakeTagsRepository
import com.salco.dragonfly.core.testing.FakeTransactionsRepository
import com.salco.dragonfly.core.testing.FakeUserDataRepository
import com.salco.dragonfly.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

class ReportsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accounts = FakeAccountsRepository()
    private val budgets = FakeBudgetsRepository()
    private val transactions = FakeTransactionsRepository(accounts, FakeCategoriesRepository(), FakeTagsRepository(), budgets)
    private val clock = Clock.fixed(LocalDate.of(2026, 10, 18).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private suspend fun add(type: TransactionType, date: LocalDate, amount: Long, other: String, category: String? = null, tags: List<String> = emptyList()) =
        transactions.saveTransaction(
            TransactionDraft(
                type = type, description = other, date = date, amount = amount, currencyCode = "EUR",
                source = if (type == TransactionType.DEPOSIT) AccountRef.New(other, AccountType.REVENUE, "EUR") else AccountRef.Existing(1),
                destination = if (type == TransactionType.DEPOSIT) AccountRef.Existing(1) else AccountRef.New(other, AccountType.EXPENSE, "EUR"),
                categoryName = category, tagNames = tags,
                budgetId = if (category == "Spesa") 1 else null,
            ),
        )

    @Before
    fun setUp() = runTest {
        budgets.attach(transactions)
        accounts.saveAccount(Account(name = "Conto", type = AccountType.ASSET, currencyCode = "EUR"))
        budgets.saveBudget(Budget(name = "Spesa", amount = 40_000, currencyCode = "EUR"))
        add(TransactionType.DEPOSIT, LocalDate.of(2026, 9, 27), 200_000, "Azienda")
        add(TransactionType.WITHDRAWAL, LocalDate.of(2026, 9, 5), 20_000, "Esselunga", "Spesa")
        add(TransactionType.DEPOSIT, LocalDate.of(2026, 10, 27), 210_000, "Azienda")
        add(TransactionType.WITHDRAWAL, LocalDate.of(2026, 10, 3), 30_000, "Esselunga", "Spesa", listOf("Casa"))
        add(TransactionType.WITHDRAWAL, LocalDate.of(2026, 10, 9), 10_000, "Bar", "Svago")
    }

    private fun viewModel() = ReportsViewModel(FakeInsightsRepository(accounts, transactions), budgets, FakeUserDataRepository(), clock)

    @Test
    fun monthReport_comparesWithThePreviousMonth() = runTest {
        viewModel().uiState.test {
            val state = expectMostRecentItem() as ReportsUiState.Success
            assertEquals(210_000L, state.income)
            assertEquals(40_000L, state.expenses)
            assertEquals(20_000L, state.previousExpenses)
            assertEquals(81, state.savingsRate)
            assertEquals(listOf("Spesa", "Svago"), state.expenseCategories.map { it.total.name })
            assertEquals(0.75f, state.expenseCategories[0].share, 0.001f)
            // Spesa went from €200 to €300; Svago had nothing last month.
            assertEquals(listOf(50, null), state.expenseCategories.map { it.change })
            assertEquals(listOf("Esselunga", "Bar"), state.payees.map { it.name })
            assertEquals(listOf("Casa"), state.tags.map { it.name })
            assertEquals(listOf(BudgetRow("Spesa", budgeted = 40_000, spent = 30_000)), state.budgets)
        }
    }

    @Test
    fun quarterReport_coversThreeMonthsAndMultipliesBudgets() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            expectMostRecentItem()
            vm.setLength(PeriodLength.QUARTER)
            // 18 October falls in the fourth quarter: October to December.
            val state = expectMostRecentItem() as ReportsUiState.Success
            assertEquals(3, state.monthly.size)
            assertEquals(210_000L, state.income)
            assertEquals(40_000L, state.expenses)
            assertEquals(13_333L, state.averageMonthlyExpenses)
            assertEquals(120_000L, state.budgets.single().budgeted)
            assertEquals(30_000L, state.budgets.single().spent)

            vm.previous()
            with(expectMostRecentItem() as ReportsUiState.Success) {
                assertEquals(LocalDate.of(2026, 7, 1), period.start)
                assertEquals(200_000L, income)
                assertEquals(20_000L, expenses)
            }
        }
    }
}
