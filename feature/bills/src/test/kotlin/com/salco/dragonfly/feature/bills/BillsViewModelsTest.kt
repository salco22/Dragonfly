package com.salco.dragonfly.feature.bills

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.Bill
import com.salco.dragonfly.core.model.BillStatus
import com.salco.dragonfly.core.model.RecurrenceFrequency
import com.salco.dragonfly.core.model.RecurrenceRule
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.testing.FakeAccountsRepository
import com.salco.dragonfly.core.testing.FakeBillsRepository
import com.salco.dragonfly.core.testing.FakeBudgetsRepository
import com.salco.dragonfly.core.testing.FakeCategoriesRepository
import com.salco.dragonfly.core.testing.FakeTagsRepository
import com.salco.dragonfly.core.testing.FakeTransactionsRepository
import com.salco.dragonfly.core.testing.FakeUserDataRepository
import com.salco.dragonfly.core.testing.MainDispatcherRule
import com.salco.dragonfly.feature.bills.detail.BillDetailUiState
import com.salco.dragonfly.feature.bills.detail.BillDetailViewModel
import com.salco.dragonfly.feature.bills.edit.AmountError
import com.salco.dragonfly.feature.bills.edit.BillEditViewModel
import com.salco.dragonfly.feature.bills.list.BillsUiState
import com.salco.dragonfly.feature.bills.list.BillsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

class BillsViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accounts = FakeAccountsRepository()
    private val bills = FakeBillsRepository()
    private val transactions = FakeTransactionsRepository(accounts, FakeCategoriesRepository(), FakeTagsRepository(), FakeBudgetsRepository(), bills)
    private val clock = Clock.fixed(LocalDate.of(2026, 10, 18).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private var rent = 0L
    private var power = 0L

    @Before
    fun setUp() = runTest {
        bills.attach(transactions)
        accounts.saveAccount(Account(name = "Conto", type = AccountType.ASSET, currencyCode = "EUR"))
        val monthly = { day: Int -> RecurrenceRule(RecurrenceFrequency.MONTHLY, startDate = LocalDate.of(2026, 1, day)) }
        rent = bills.saveBill(Bill(name = "Affitto", amountMin = 80_000, amountMax = 80_000, currencyCode = "EUR", rule = monthly(5)))
        power = bills.saveBill(Bill(name = "Luce", amountMin = 4_000, amountMax = 7_000, currencyCode = "EUR", rule = monthly(25)))
    }

    private suspend fun pay(billId: Long, date: LocalDate, amount: Long) = transactions.saveTransaction(
        TransactionDraft(
            type = TransactionType.WITHDRAWAL, description = "Pagamento", date = date, amount = amount, currencyCode = "EUR",
            source = AccountRef.Existing(1), destination = AccountRef.New("Fornitore", AccountType.EXPENSE, "EUR"), billId = billId,
        ),
    )

    @Test
    fun list_ordersByUrgencyAndSumsTheMonth() = runTest {
        BillsViewModel(bills, clock).uiState.test {
            with(awaitItem() as BillsUiState.Success) {
                assertEquals(listOf("Affitto", "Luce"), items.map { it.bill.name })
                assertEquals(BillStatus.Overdue(LocalDate.of(2026, 10, 5)), items[0].status)
                assertEquals(BillStatus.Due(LocalDate.of(2026, 10, 25)), items[1].status)
                assertEquals(mapOf("EUR" to 87_000L), expectedThisMonth)
            }
            pay(rent, LocalDate.of(2026, 10, 10), 80_000)
            with(awaitItem() as BillsUiState.Success) {
                assertEquals(listOf("Luce", "Affitto"), items.map { it.bill.name })
                assertTrue(items[1].status is BillStatus.Paid)
                assertEquals(mapOf("EUR" to 80_000L), paidThisMonth)
            }
        }
    }

    @Test
    fun detail_showsUpcomingDatesAndPayments() = runTest {
        pay(power, LocalDate.of(2026, 9, 25), 5_500)
        BillDetailViewModel(SavedStateHandle(mapOf("billId" to power)), bills, transactions, clock).uiState.test {
            with(awaitItem() as BillDetailUiState.Success) {
                assertEquals(
                    listOf(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 11, 25), LocalDate.of(2026, 12, 25)),
                    upcoming,
                )
                assertEquals(listOf(5_500L), payments.map { it.amount })
            }
        }
    }

    @Test
    fun edit_validatesRangeAndSaves() = runTest {
        val vm = BillEditViewModel(SavedStateHandle(mapOf("billId" to 0L)), bills, FakeUserDataRepository(), clock)
        vm.onNameChange("Internet")
        vm.onAmountMinChange("30")
        vm.onAmountMaxChange("20")
        vm.save()
        assertEquals(AmountError.RANGE, vm.uiState.value.amountError)

        vm.onAmountMaxChange("")
        vm.onFrequencyChange(RecurrenceFrequency.YEARLY)
        vm.save()
        assertTrue(vm.uiState.value.isFinished)
        with(bills.bills.value.single { it.name == "Internet" }) {
            assertEquals(3_000L, amountMin)
            assertEquals(3_000L, amountMax)
            assertEquals(RecurrenceFrequency.YEARLY, rule.frequency)
            assertEquals(LocalDate.of(2026, 10, 18), rule.startDate)
        }
    }
}
