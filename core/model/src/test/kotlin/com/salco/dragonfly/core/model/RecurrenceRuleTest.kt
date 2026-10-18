package com.salco.dragonfly.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RecurrenceRuleTest {

    @Test
    fun monthlyOn31st_clampsToShortMonthsWithoutDrifting() {
        val rule = RecurrenceRule(RecurrenceFrequency.MONTHLY, startDate = LocalDate.of(2026, 1, 31))
        assertEquals(LocalDate.of(2026, 2, 28), rule.occurrence(1))
        assertEquals(LocalDate.of(2026, 3, 31), rule.occurrence(2))
        assertEquals(LocalDate.of(2026, 4, 30), rule.occurrence(3))
    }

    @Test
    fun intervalsForEveryFrequency() {
        val start = LocalDate.of(2026, 10, 1)
        assertEquals(LocalDate.of(2026, 10, 7), RecurrenceRule(RecurrenceFrequency.DAILY, 3, start).occurrence(2))
        assertEquals(LocalDate.of(2026, 10, 29), RecurrenceRule(RecurrenceFrequency.WEEKLY, 2, start).occurrence(2))
        assertEquals(LocalDate.of(2027, 4, 1), RecurrenceRule(RecurrenceFrequency.MONTHLY, 3, start).occurrence(2))
        assertEquals(LocalDate.of(2028, 10, 1), RecurrenceRule(RecurrenceFrequency.YEARLY, 1, start).occurrence(2))
    }

    @Test
    fun endDateAndMaxOccurrences_stopTheRule() {
        val start = LocalDate.of(2026, 10, 1)
        val untilDate = RecurrenceRule(RecurrenceFrequency.MONTHLY, startDate = start, endDate = LocalDate.of(2026, 12, 1))
        assertEquals(LocalDate.of(2026, 12, 1), untilDate.nextAfter(2))
        assertNull(untilDate.nextAfter(3))

        val times = RecurrenceRule(RecurrenceFrequency.WEEKLY, startDate = start, maxOccurrences = 2)
        assertEquals(LocalDate.of(2026, 10, 8), times.nextAfter(1))
        assertNull(times.nextAfter(2))
    }
}
