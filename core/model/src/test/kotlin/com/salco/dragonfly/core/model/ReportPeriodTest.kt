package com.salco.dragonfly.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ReportPeriodTest {

    @Test
    fun boundsAndNavigation() {
        val day = LocalDate.of(2026, 11, 18)
        with(ReportPeriod(PeriodLength.QUARTER, day)) {
            assertEquals(LocalDate.of(2026, 10, 1), start)
            assertEquals(LocalDate.of(2026, 12, 31), end)
            assertEquals(listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 11), YearMonth.of(2026, 12)), months)
            assertEquals(LocalDate.of(2026, 7, 1), previous().start)
            assertEquals(LocalDate.of(2027, 1, 1), next().start)
        }
        assertEquals(LocalDate.of(2026, 11, 30), ReportPeriod(PeriodLength.MONTH, day).end)
        assertEquals(12, ReportPeriod(PeriodLength.YEAR, day).months.size)
        assertEquals(LocalDate.of(2025, 1, 1), ReportPeriod(PeriodLength.YEAR, day).previous().start)
    }

    @Test
    fun percentChanges() {
        assertEquals(50, percentChange(150, 100))
        assertEquals(-25, percentChange(75, 100))
        assertNull(percentChange(10, 0))
    }
}
