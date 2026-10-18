package com.salco.dragonfly.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class MoneyTest {

    @Test
    fun parsesDecimalSeparators() {
        assertEquals(123_450L, parseAmountToMinorUnits("1234,5", "EUR"))
        assertEquals(123_456L, parseAmountToMinorUnits("1.234,56", "EUR"))
        assertEquals(123_456L, parseAmountToMinorUnits("1,234.56", "USD"))
        assertEquals(-1_230L, parseAmountToMinorUnits("-12.30", "EUR"))
        assertEquals(50L, parseAmountToMinorUnits(",5", "EUR"))
    }

    @Test
    fun treatsThreeTrailingDigitsAsGrouping() {
        assertEquals(123_400L, parseAmountToMinorUnits("1.234", "EUR"))
        assertEquals(1_500L, parseAmountToMinorUnits("1.500", "JPY"))
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(parseAmountToMinorUnits("", "EUR"))
        assertNull(parseAmountToMinorUnits("abc", "EUR"))
        assertNull(parseAmountToMinorUnits("-", "EUR"))
        assertNull(parseAmountToMinorUnits("12.3.4a", "EUR"))
    }

    @Test
    fun roundTripsInputText() {
        assertEquals("1234.56", minorUnitsToInput(123_456L, "EUR"))
        assertEquals(123_456L, parseAmountToMinorUnits(minorUnitsToInput(123_456L, "EUR"), "EUR"))
    }

    @Test
    fun formatsWithCurrency() {
        assertEquals("€1,234.56", formatMoney(123_456L, "EUR", Locale.US))
        assertEquals("¥1,500", formatMoney(1_500L, "JPY", Locale.US))
    }
}
