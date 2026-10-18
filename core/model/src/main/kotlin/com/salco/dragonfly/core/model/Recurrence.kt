package com.salco.dragonfly.core.model

import java.time.LocalDate

enum class RecurrenceFrequency { DAILY, WEEKLY, MONTHLY, YEARLY }

/**
 * When a recurring transaction repeats: every [interval] [frequency] units from [startDate],
 * until [endDate] and/or for at most [maxOccurrences] times (both optional).
 *
 * Occurrences are always computed from [startDate], so a monthly rule starting on the 31st
 * falls on the last day of shorter months and returns to the 31st afterwards.
 */
data class RecurrenceRule(
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val maxOccurrences: Int? = null,
) {
    init {
        require(interval >= 1) { "interval must be at least 1" }
    }

    /** Date of the occurrence at [index] (0 is [startDate]), ignoring the end limits. */
    fun occurrence(index: Int): LocalDate {
        val steps = index.toLong() * interval
        return when (frequency) {
            RecurrenceFrequency.DAILY -> startDate.plusDays(steps)
            RecurrenceFrequency.WEEKLY -> startDate.plusWeeks(steps)
            RecurrenceFrequency.MONTHLY -> startDate.plusMonths(steps)
            RecurrenceFrequency.YEARLY -> startDate.plusYears(steps)
        }
    }

    /** The occurrence after [created] have been generated, or null when the rule has ended. */
    fun nextAfter(created: Int): LocalDate? {
        if (maxOccurrences != null && created >= maxOccurrences) return null
        val next = occurrence(created)
        return if (endDate != null && next.isAfter(endDate)) null else next
    }
}

/** A transaction template repeated by [rule]; [template]'s date is ignored. */
data class RecurringTransaction(
    val id: Long = 0,
    val template: TransactionDraft,
    val rule: RecurrenceRule,
    val occurrencesCreated: Int = 0,
    /** Next date a transaction will be created, or null when the rule has ended. */
    val nextDate: LocalDate? = rule.nextAfter(occurrencesCreated),
    val active: Boolean = true,
    /** Display names of both sides, resolved from accounts or the names typed in the template. */
    val sourceName: String = "",
    val destinationName: String = "",
)
