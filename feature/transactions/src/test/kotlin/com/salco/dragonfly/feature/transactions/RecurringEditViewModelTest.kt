package com.salco.dragonfly.feature.transactions

import androidx.lifecycle.SavedStateHandle
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.RecurrenceFrequency
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.testing.FakeAccountsRepository
import com.salco.dragonfly.core.testing.FakeBillsRepository
import com.salco.dragonfly.core.testing.FakeBudgetsRepository
import com.salco.dragonfly.core.testing.FakeCategoriesRepository
import com.salco.dragonfly.core.testing.FakeRecurringTransactionsRepository
import com.salco.dragonfly.core.testing.FakeTagsRepository
import com.salco.dragonfly.core.testing.FakeTransactionsRepository
import com.salco.dragonfly.core.testing.MainDispatcherRule
import com.salco.dragonfly.feature.transactions.recurring.EndMode
import com.salco.dragonfly.feature.transactions.recurring.RecurringEditViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

class RecurringEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accounts = FakeAccountsRepository()
    private val categories = FakeCategoriesRepository()
    private val tags = FakeTagsRepository()
    private val budgets = FakeBudgetsRepository()
    private val bills = FakeBillsRepository()
    private val transactions = FakeTransactionsRepository(accounts, categories, tags, budgets, bills)
    private val recurring = FakeRecurringTransactionsRepository(transactions, accounts)
    private val clock = Clock.fixed(LocalDate.of(2026, 10, 18).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    @Before
    fun setUp() = runTest {
        accounts.saveAccount(Account(name = "Conto", type = AccountType.ASSET, currencyCode = "EUR"))
    }

    private fun viewModel(id: Long = 0L) = RecurringEditViewModel(
        SavedStateHandle(mapOf("recurringId" to id)), recurring, accounts, categories, tags, budgets, bills, clock,
    )

    @Test
    fun salaryStartingInThePast_createsDueTransactionsOnSave() = runTest {
        val vm = viewModel()
        vm.onTypeChange(TransactionType.DEPOSIT)
        vm.onAmountChange("2000")
        vm.onDescriptionChange("Stipendio")
        vm.onCounterpartyChange("Azienda")
        vm.onDateChange(LocalDate.of(2026, 8, 27))
        vm.save()

        assertTrue(vm.uiState.value.isFinished)
        assertEquals(
            listOf(LocalDate.of(2026, 8, 27), LocalDate.of(2026, 9, 27)),
            transactions.transactions.value.map { it.date }.sorted(),
        )
        val saved = recurring.recurring.value.single()
        assertEquals(RecurrenceFrequency.MONTHLY, saved.rule.frequency)
        assertEquals(LocalDate.of(2026, 10, 27), saved.nextDate)
    }

    @Test
    fun invalidSchedule_isRejected() = runTest {
        val vm = viewModel()
        vm.onAmountChange("10")
        vm.onDescriptionChange("Palestra")
        vm.onCounterpartyChange("Palestra")
        vm.onIntervalChange("0")
        vm.onEndModeChange(EndMode.TIMES)
        vm.save()

        assertTrue(vm.schedule.value.intervalError)
        assertTrue(vm.schedule.value.endError)
        assertFalse(vm.uiState.value.isFinished)
        assertTrue(recurring.recurring.value.isEmpty())
    }

    @Test
    fun existingSchedule_isLoadedForEditing() = runTest {
        val first = viewModel()
        first.onAmountChange("9,99")
        first.onDescriptionChange("Streaming")
        first.onCounterpartyChange("Netflix")
        first.onCategoryChange("Svago")
        first.onDateChange(LocalDate.of(2026, 11, 5))
        first.onEndModeChange(EndMode.TIMES)
        first.onMaxOccurrencesChange("12")
        first.save()
        assertTrue(transactions.transactions.value.isEmpty())

        val vm = viewModel(recurring.recurring.value.single().id)
        with(vm.uiState.value) {
            assertEquals("9.99", amount)
            assertEquals("Netflix", counterparty)
            assertEquals("Svago", category)
            assertEquals(LocalDate.of(2026, 11, 5), date)
        }
        assertEquals(EndMode.TIMES, vm.schedule.value.end)
        assertEquals("12", vm.schedule.value.maxOccurrences)
    }
}
