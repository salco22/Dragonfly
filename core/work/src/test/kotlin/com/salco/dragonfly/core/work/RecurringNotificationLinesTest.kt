package com.salco.dragonfly.core.work

import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.formatMoney
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecurringNotificationLinesTest {

    private fun draft(description: String, type: TransactionType, amount: Long, day: Int) = TransactionDraft(
        type = type, description = description, date = LocalDate.of(2026, 10, day), amount = amount, currencyCode = "EUR",
        source = AccountRef.Existing(1), destination = AccountRef.Existing(2),
    )

    @Test
    fun linesAreSignedAndInDateOrder() {
        val lines = recurringNotificationLines(
            listOf(
                draft("Stipendio", TransactionType.DEPOSIT, 200_000, 27),
                draft("Affitto", TransactionType.WITHDRAWAL, 80_000, 1),
                draft("Risparmio", TransactionType.TRANSFER, 10_000, 5),
            ),
        )
        assertEquals(
            listOf(
                "Affitto · −${formatMoney(80_000, "EUR")}",
                "Risparmio · ${formatMoney(10_000, "EUR")}",
                "Stipendio · +${formatMoney(200_000, "EUR")}",
            ),
            lines,
        )
    }
}
