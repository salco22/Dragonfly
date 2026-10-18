package com.salco.dragonfly.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BillStatusTest {

    private val rent = Bill(
        id = 1,
        name = "Affitto",
        amountMin = 80_000,
        amountMax = 80_000,
        currencyCode = "EUR",
        rule = RecurrenceRule(RecurrenceFrequency.MONTHLY, startDate = LocalDate.of(2026, 1, 5)),
    )

    private fun pay(date: LocalDate, billId: Long = 1) = BillPayment(billId, 1, date, 80_000)

    @Test
    fun dueThenOverdueWhenNotPaid() {
        assertEquals(BillStatus.Due(LocalDate.of(2026, 10, 5)), rent.statusOn(LocalDate.of(2026, 10, 1), emptyList()))
        assertEquals(BillStatus.Overdue(LocalDate.of(2026, 10, 5)), rent.statusOn(LocalDate.of(2026, 10, 18), emptyList()))
    }

    @Test
    fun earlyPaymentCountsForTheComingDueDate() {
        val status = rent.statusOn(LocalDate.of(2026, 10, 18), listOf(pay(LocalDate.of(2026, 10, 1))))
        assertEquals(BillStatus.Paid(LocalDate.of(2026, 10, 1), 80_000, LocalDate.of(2026, 11, 5)), status)
    }

    @Test
    fun paymentsOfOtherPeriodsOrBillsDoNotCount() {
        val payments = listOf(pay(LocalDate.of(2026, 9, 5)), pay(LocalDate.of(2026, 10, 6), billId = 2))
        assertEquals(BillStatus.Overdue(LocalDate.of(2026, 10, 5)), rent.statusOn(LocalDate.of(2026, 10, 18), payments))
    }

    @Test
    fun notStartedInactiveAndEnded() {
        assertEquals(BillStatus.NotStarted(LocalDate.of(2026, 1, 5)), rent.statusOn(LocalDate.of(2025, 12, 1), emptyList()))
        assertEquals(BillStatus.Inactive, rent.copy(active = false).statusOn(LocalDate.of(2026, 10, 18), emptyList()))
        val ended = rent.copy(rule = rent.rule.copy(endDate = LocalDate.of(2026, 3, 31)))
        assertEquals(BillStatus.Inactive, ended.statusOn(LocalDate.of(2026, 10, 18), emptyList()))
    }

    @Test
    fun dueDatesInMonth() {
        val weekly = rent.copy(rule = RecurrenceRule(RecurrenceFrequency.WEEKLY, startDate = LocalDate.of(2026, 10, 2)))
        assertEquals(5, weekly.dueDatesBetween(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)).size)
        assertEquals(listOf(LocalDate.of(2026, 10, 5)), rent.dueDatesBetween(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
    }
}
