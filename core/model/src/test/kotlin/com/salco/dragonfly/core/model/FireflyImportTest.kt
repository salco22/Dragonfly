package com.salco.dragonfly.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FireflyImportTest {

    // The same columns as a real export, trimmed to the ones used; the last column is often missing.
    private val csv = """
        user_id,group_id,journal_id,group_title,type,amount,foreign_amount,currency_code,foreign_currency_code,description,date,source_name,source_type,destination_name,destination_type,category,budget,bill,tags,notes,recurrence_date
        1,10,100,,Withdrawal,-42.500000000000,,EUR,,"pranzo - La Farcia, Cuneo",2026-09-25T12:05:00+02:00,ISP,Asset account,Cash account,Cash account,Spese Variabili,,,"Ristoranti,Lavoro",
        1,11,101,,Deposit,-2027.000000000000,,EUR,,Stipendio Settembre,2026-09-27T09:00:00+02:00,Cash account,Cash account,ISP,Asset account,Entrate,,,Stipendio,"riga 1
        riga 2"
        1,12,102,,Transfer,-300.000000000000,,EUR,,invio soldi a Revolut,2026-09-28T10:00:00+02:00,ISP,Asset account,Revolut,Asset account,,,,,
        1,13,103,,Opening balance,-27120.840000000000,,EUR,,Saldo iniziale ISP,2024-02-03T00:00:00+01:00,ISP initial balance,Initial balance account,ISP,Asset account,,,,,
        1,14,104,,Reconciliation,-1.000000000000,,EUR,,Riconciliazione,2025-05-01T00:00:00+02:00,ISY,Asset account,ISY riconciliazione (EUR),Reconciliation account,,,,,
        1,15,105,Spesa grande,Withdrawal,-30.000000000000,,EUR,,Pane,2026-09-29T10:00:00+02:00,ISP,Asset account,Forno,Expense account,,Spese essenziali,,,
        1,15,106,Spesa grande,Withdrawal,-12.345000000000,,EUR,,Latte,2026-09-29T10:00:00+02:00,ISP,Asset account,Latteria,Expense account,,,,,
        1,16,107,,Withdrawal,abc,,EUR,,Rotta,2026-09-29T10:00:00+02:00,ISP,Asset account,Cash account,Cash account,,,,,
    """.trimIndent()

    private val export = parseFireflyExport(parseCsv(csv))

    @Test
    fun header_isRecognised() {
        assertTrue(isFireflyExport(parseCsv(csv).first()))
        assertFalse(isFireflyExport(listOf("Data", "Descrizione", "Importo")))
    }

    @Test
    fun amountsUseTheType_notTheSign() {
        val (withdrawal, deposit) = export.transactions
        assertEquals(FireflyType.WITHDRAWAL, withdrawal.type)
        assertEquals(4_250L, withdrawal.amount)
        assertEquals(FireflyType.DEPOSIT, deposit.type)
        assertEquals(202_700L, deposit.amount)
        // 12 decimals are rounded to the currency's two.
        assertEquals(1_235L, export.transactions.single { it.journalId == "106" }.amount)
        assertEquals(LocalDate.of(2026, 9, 25), withdrawal.date)
        assertEquals(listOf("Ristoranti", "Lavoro"), withdrawal.tags)
        assertEquals("riga 1\nriga 2", deposit.notes)
        assertEquals(FireflyAccountKind.CASH, withdrawal.destination.kind)
    }

    @Test
    fun accountsOpeningBalancesAndLists() {
        assertEquals(setOf("ISP", "Revolut", "ISY"), export.ownAccounts.keys)
        assertEquals(mapOf("ISP" to 2_712_084L), export.openingBalances)
        assertEquals(setOf("Entrate", "Spese Variabili"), export.categories)
        assertEquals(setOf("Spese essenziali"), export.budgets)
        assertEquals(1, export.count(FireflyType.TRANSFER))
        assertEquals(1, export.count(FireflyType.RECONCILIATION))
        assertEquals(6, export.movements.size)
        assertEquals(listOf(9), export.invalidLines)
        assertNull(export.transactions.first().groupTitle)
        assertEquals(2, export.transactions.count { it.groupId == "15" })
    }
}
