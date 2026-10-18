package com.salco.dragonfly.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SplitTransactionTest {

    private val own = AccountSummary(1, "Conto", AccountType.ASSET, "EUR")
    private val shop = AccountSummary(2, "Supermercato", AccountType.EXPENSE, "EUR")
    private val pharmacy = AccountSummary(3, "Farmacia", AccountType.EXPENSE, "EUR")
    private val date = LocalDate.of(2026, 10, 18)

    private fun part(id: Long, amount: Long, destination: AccountSummary, category: String?, groupId: Long? = 7) = Transaction(
        id = id, type = TransactionType.WITHDRAWAL, description = "parte $id", date = date, amount = amount,
        currencyCode = "EUR", source = own, destination = destination,
        category = category?.let { Category(id, it) }, groupId = groupId, groupTitle = groupId?.let { "Centro commerciale" },
    )

    @Test
    fun parts_areMergedIntoOneEntryWithTheTotal() {
        val single = part(1, 500, shop, "Alimentari", groupId = null)
        val merged = listOf(part(4, 300, shop, "Alimentari"), single, part(3, 1_200, pharmacy, "Salute")).mergingSplits()

        assertEquals(2, merged.size)
        with(merged.first()) {
            // Parts come newest first in lists; the entry follows the order they were entered.
            assertEquals(3L, id)
            assertEquals("Centro commerciale", description)
            assertEquals(1_500L, amount)
            assertEquals("Farmacia, Supermercato", destination.name)
            assertEquals(own, source)
            // The parts have different categories, so the entry shows none.
            assertNull(category)
        }
        assertEquals(single, merged[1])
    }

    @Test
    fun aGroupWithOnePartInTheList_staysThatPart() {
        val alone = part(4, 300, shop, "Alimentari")
        assertEquals(listOf(alone), listOf(alone).mergingSplits())
    }
}
