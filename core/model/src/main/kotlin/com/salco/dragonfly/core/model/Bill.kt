package com.salco.dragonfly.core.model

import java.time.LocalDate

/**
 * A bill or subscription expected on the dates of [rule] (like Firefly III's bills/subscriptions),
 * costing between [amountMin] and [amountMax] minor units of [currencyCode]. Withdrawals are
 * linked to it explicitly when paid.
 */
data class Bill(
    val id: Long = 0,
    val name: String,
    val amountMin: Long,
    val amountMax: Long,
    val currencyCode: String,
    val rule: RecurrenceRule,
    val active: Boolean = true,
    val notes: String? = null,
)

data class BillRef(val id: Long, val name: String)

/** A withdrawal linked to a bill. */
data class BillPayment(val billId: Long, val transactionId: Long, val date: LocalDate, val amount: Long)

sealed interface BillStatus {
    /** Paid in the current period, on [date]. */
    data class Paid(val date: LocalDate, val amount: Long, val nextDue: LocalDate?) : BillStatus

    /** Not paid yet and due on [dueDate] (today or later). */
    data class Due(val dueDate: LocalDate) : BillStatus

    /** [dueDate] passed without a linked payment. */
    data class Overdue(val dueDate: LocalDate) : BillStatus

    /** The first expected date is still ahead. */
    data class NotStarted(val firstDate: LocalDate) : BillStatus

    /** The schedule has ended or the bill is inactive. */
    data object Inactive : BillStatus
}

/** A payment made up to this many days before the due date still counts for that due date. */
const val BILL_EARLY_PAYMENT_DAYS = 7L

/**
 * Status of the bill on [today]. Each due date owns the period from [BILL_EARLY_PAYMENT_DAYS]
 * before it until the same distance before the next due date; the bill is paid for a due date
 * when one of [payments] falls in its period.
 */
fun Bill.statusOn(today: LocalDate, payments: List<BillPayment>): BillStatus {
    if (!active) return BillStatus.Inactive
    val own = payments.filter { it.billId == id }
    var index = 0
    // Find the current period: the last due date whose window has already opened.
    if (rule.occurrence(0).minusDays(BILL_EARLY_PAYMENT_DAYS).isAfter(today)) {
        return BillStatus.NotStarted(rule.occurrence(0))
    }
    while (true) {
        val next = rule.nextAfter(index + 1) ?: break
        if (next.minusDays(BILL_EARLY_PAYMENT_DAYS).isAfter(today)) break
        index++
    }
    val due = rule.nextAfter(index) ?: return BillStatus.Inactive
    val windowStart = due.minusDays(BILL_EARLY_PAYMENT_DAYS)
    val nextDue = rule.nextAfter(index + 1)
    val windowEnd = nextDue?.minusDays(BILL_EARLY_PAYMENT_DAYS)
    val payment = own.filter { !it.date.isBefore(windowStart) && (windowEnd == null || it.date.isBefore(windowEnd)) }
        .maxByOrNull { it.date }
    return when {
        payment != null -> BillStatus.Paid(payment.date, payment.amount, nextDue)
        due.isBefore(today) -> {
            // Once the schedule has ended, an unpaid last date is no longer worth flagging.
            if (nextDue == null && rule.endDate != null && today.isAfter(rule.endDate)) BillStatus.Inactive else BillStatus.Overdue(due)
        }
        else -> BillStatus.Due(due)
    }
}

/** Due dates of the bill between [from] and [to] inclusive. */
fun Bill.dueDatesBetween(from: LocalDate, to: LocalDate): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    var index = 0
    while (true) {
        val date = rule.nextAfter(index) ?: break
        if (date.isAfter(to)) break
        if (!date.isBefore(from)) dates += date
        index++
    }
    return dates
}
