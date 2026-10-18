package com.salco.dragonfly.feature.budgets

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.Budget
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.testing.FakeAccountsRepository
import com.salco.dragonfly.core.testing.FakeBudgetsRepository
import com.salco.dragonfly.core.testing.FakeCategoriesRepository
import com.salco.dragonfly.core.testing.FakeTagsRepository
import com.salco.dragonfly.core.testing.FakeTransactionsRepository
import com.salco.dragonfly.core.testing.FakeUserDataRepository
import com.salco.dragonfly.core.testing.MainDispatcherRule
import com.salco.dragonfly.feature.budgets.detail.BudgetDetailUiState
import com.salco.dragonfly.feature.budgets.detail.BudgetDetailViewModel
import com.salco.dragonfly.feature.budgets.edit.BudgetEditViewModel
import com.salco.dragonfly.feature.budgets.edit.NameError
import com.salco.dragonfly.feature.budgets.list.BudgetTotals
import com.salco.dragonfly.feature.budgets.list.BudgetsUiState
import com.salco.dragonfly.feature.budgets.list.BudgetsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class BudgetsViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accounts = FakeAccountsRepository()
    private val budgets = FakeBudgetsRepository()
    private val transactions = FakeTransactionsRepository(accounts, FakeCategoriesRepository(), FakeTagsRepository(), budgets)
    private val clock = Clock.fixed(LocalDate.of(2026, 10, 22).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private var groceries = 0L

    @Before
    fun setUp() = runTest {
        budgets.attach(transactions)
        accounts.saveAccount(Account(name = "Conto", type = AccountType.ASSET, currencyCode = "EUR"))
        groceries = budgets.saveBudget(Budget(name = "Spesa", amount = 40_000, currencyCode = "EUR"))
    }

    private suspend fun withdraw(amount: Long, date: LocalDate, budgetId: Long?) = transactions.saveTransaction(
        TransactionDraft(
            type = TransactionType.WITHDRAWAL, description = "Uscita", date = date, amount = amount, currencyCode = "EUR",
            source = AccountRef.Existing(1), destination = AccountRef.New("Negozio", AccountType.EXPENSE, "EUR"),
            budgetId = budgetId,
        ),
    )

    @Test
    fun list_showsSpendingRemainingAndUnbudgeted() = runTest {
        withdraw(25_000, LocalDate.of(2026, 10, 5), groceries)
        withdraw(30_000, LocalDate.of(2026, 9, 5), groceries)
        withdraw(4_000, LocalDate.of(2026, 10, 6), null)

        BudgetsViewModel(budgets, clock).uiState.test {
            with(awaitItem() as BudgetsUiState.Success) {
                assertEquals(25_000L, budgets.single().spent)
                assertEquals(15_000L, budgets.single().remaining)
                assertEquals(mapOf("EUR" to BudgetTotals(budgeted = 40_000, spent = 25_000)), totals)
                assertEquals(mapOf("EUR" to 4_000L), unbudgeted)
                // 22 to 31 October, today included.
                assertEquals(10, daysLeft)
            }
        }
    }

    @Test
    fun detail_listsBudgetTransactionsOfTheMonth() = runTest {
        withdraw(25_000, LocalDate.of(2026, 10, 5), groceries)
        withdraw(50_000, LocalDate.of(2026, 10, 7), groceries)
        withdraw(4_000, LocalDate.of(2026, 10, 6), null)
        val vm = BudgetDetailViewModel(
            SavedStateHandle(mapOf("budgetId" to groceries, "month" to "2026-10")), budgets, transactions, clock,
        )
        vm.uiState.test {
            with(awaitItem() as BudgetDetailUiState.Success) {
                assertEquals(2, days.flatMap { it.second }.size)
                assertTrue(spending.isOverspent)
                assertEquals(-35_000L, spending.remaining)
            }
            vm.previousMonth()
            with(awaitItem() as BudgetDetailUiState.Success) {
                assertEquals(YearMonth.of(2026, 9), month)
                assertEquals(0L, spending.spent)
            }
        }
    }

    @Test
    fun edit_validatesAndSaves() = runTest {
        val vm = BudgetEditViewModel(SavedStateHandle(mapOf("budgetId" to 0L)), budgets, FakeUserDataRepository(currency = "CHF"))
        assertEquals("CHF", vm.uiState.value.currencyCode)
        vm.save()
        assertEquals(NameError.BLANK, vm.uiState.value.nameError)
        assertTrue(vm.uiState.value.amountError)

        vm.onNameChange("spesa")
        vm.onAmountChange("100")
        vm.save()
        assertEquals(NameError.DUPLICATE, vm.uiState.value.nameError)

        vm.onNameChange("Svago")
        vm.save()
        assertTrue(vm.uiState.value.isFinished)
        assertEquals(10_000L, budgets.budgets.value.single { it.name == "Svago" }.amount)
    }

    @Test
    fun deletingBudget_keepsTransactionsWithoutBudget() = runTest {
        withdraw(1_000, LocalDate.of(2026, 10, 5), groceries)
        val vm = BudgetEditViewModel(SavedStateHandle(mapOf("budgetId" to groceries)), budgets, FakeUserDataRepository())
        assertEquals("400.00", vm.uiState.value.amount)
        vm.delete()
        assertTrue(budgets.budgets.value.isEmpty())
        assertEquals(null, transactions.transactions.value.single().budget)
    }
}
