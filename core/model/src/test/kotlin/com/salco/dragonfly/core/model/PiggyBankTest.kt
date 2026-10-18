package com.salco.dragonfly.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PiggyBankTest {

    private fun summary(target: Long?, saved: Long, targetDate: LocalDate? = null) = PiggyBankSummary(
        PiggyBank(1, "Vacanze", accountId = 1, targetAmount = target, startDate = LocalDate.of(2026, 1, 1), targetDate = targetDate),
        accountName = "Risparmi",
        currencyCode = "EUR",
        saved = saved,
    )

    @Test
    fun progressAndRemaining() {
        val jar = summary(target = 100_000, saved = 25_000)
        assertEquals(0.25f, jar.progress!!, 0.0001f)
        assertEquals(75_000L, jar.remaining)
        assertTrue(summary(target = 100_000, saved = 120_000).isComplete)
        assertNull(summary(target = null, saved = 10).progress)
    }

    @Test
    fun monthlyNeeded_spreadsTheRemainderUntilTheTargetMonth() {
        val today = LocalDate.of(2026, 10, 18)
        // October to March: 6 months for €750, rounded up to the cent.
        assertEquals(12_500L, summary(100_000, 25_000, LocalDate.of(2027, 3, 31)).monthlyNeeded(today))
        assertEquals(75_000L, summary(100_000, 25_000, LocalDate.of(2026, 9, 1)).monthlyNeeded(today))
        assertEquals(0L, summary(100_000, 100_000, LocalDate.of(2027, 3, 31)).monthlyNeeded(today))
        assertNull(summary(100_000, 0).monthlyNeeded(today))
    }
}
