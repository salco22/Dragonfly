package com.salco.dragonfly.core.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A savings goal inside an asset account, like Firefly III's piggy banks: money put aside stays
 * in the account but is no longer available to spend.
 */
data class PiggyBank(
    val id: Long = 0,
    val name: String,
    val accountId: Long,
    /** Goal in minor units of the account's currency; null for an open-ended jar. */
    val targetAmount: Long? = null,
    val startDate: LocalDate,
    val targetDate: LocalDate? = null,
    val notes: String? = null,
)

/** Money added to (positive) or removed from (negative) a piggy bank. */
data class PiggyBankEvent(
    val id: Long = 0,
    val piggyBankId: Long,
    val date: LocalDate,
    val amount: Long,
)

data class PiggyBankSummary(
    val piggyBank: PiggyBank,
    val accountName: String,
    val currencyCode: String,
    /** Money currently saved, in minor units. */
    val saved: Long,
) {
    val remaining: Long? get() = piggyBank.targetAmount?.let { (it - saved).coerceAtLeast(0) }
    val isComplete: Boolean get() = piggyBank.targetAmount != null && saved >= piggyBank.targetAmount

    /** Share of the target saved, between 0 and 1; null without a target. */
    val progress: Float? get() = piggyBank.targetAmount?.takeIf { it > 0 }?.let { (saved.toFloat() / it).coerceIn(0f, 1f) }

    /**
     * How much to save each month, counting the current one, to reach the target by its date;
     * null without a target or a date, 0 when already reached, the whole remainder when late.
     */
    fun monthlyNeeded(today: LocalDate): Long? {
        val remaining = remaining ?: return null
        val targetDate = piggyBank.targetDate ?: return null
        if (remaining == 0L) return 0
        val months = ChronoUnit.MONTHS.between(today.withDayOfMonth(1), targetDate.withDayOfMonth(1)) + 1
        return if (months <= 0) remaining else (remaining + months - 1) / months
    }
}
