package com.salco.dragonfly.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CsvImportTest {

    @Test
    fun parsesQuotedFieldsAndDetectsDelimiter() {
        val text = "Data;Descrizione;Importo\r\n01/10/2026;\"PAGAMENTO; \"\"POS\"\"\";-42,50\r\n\r\n02/10/2026;\"riga\nnuova\";1.200,00\n"
        assertEquals(';', detectDelimiter(text))
        assertEquals(
            listOf(
                listOf("Data", "Descrizione", "Importo"),
                listOf("01/10/2026", "PAGAMENTO; \"POS\"", "-42,50"),
                listOf("02/10/2026", "riga\nnuova", "1.200,00"),
            ),
            parseCsv(text),
        )
        assertEquals(',', detectDelimiter("date,description,amount\n2026-10-01,\"a; b\",3.5"))
    }

    @Test
    fun decodesUtf8WithBomAndWindows1252() {
        assertEquals("Caffè €", decodeCsvBytes("﻿Caffè €".toByteArray(Charsets.UTF_8)))
        assertEquals("Caffè", decodeCsvBytes("Caffè".toByteArray(charset("windows-1252"))))
    }

    @Test
    fun guessesItalianBankColumns() {
        assertEquals(
            listOf(ColumnRole.DATE, ColumnRole.IGNORE, ColumnRole.DESCRIPTION, ColumnRole.EXPENSE, ColumnRole.INCOME),
            guessColumnRoles(listOf("Data contabile", "Data valuta", "Descrizione operazione", "Dare", "Avere")),
        )
        assertEquals(
            listOf(
                ColumnRole.DATE, ColumnRole.TYPE, ColumnRole.DESCRIPTION, ColumnRole.AMOUNT, ColumnRole.IGNORE, ColumnRole.IGNORE,
                ColumnRole.IGNORE, ColumnRole.COUNTERPARTY, ColumnRole.CATEGORY, ColumnRole.TAGS, ColumnRole.IGNORE, ColumnRole.IGNORE, ColumnRole.NOTES,
            ),
            guessColumnRoles(
                listOf("date", "type", "description", "amount", "currency", "foreign_amount", "source", "destination", "category", "tags", "budget", "bill", "notes"),
            ),
        )
    }

    @Test
    fun detectsDateFormats() {
        assertEquals("dd/MM/yyyy", detectDatePattern(listOf("01/10/2026", "31/10/2026")))
        assertEquals("yyyy-MM-dd", detectDatePattern(listOf("2026-10-01")))
        assertEquals("MM/dd/yyyy", detectDatePattern(listOf("10/31/2026")))
        assertNull(detectDatePattern(listOf("ieri")))
    }

    @Test
    fun mapsRowsWithSignedOrSeparateAmounts() {
        val signed = mapImportRows(
            rows = listOf(listOf("01/10/2026", "POS ESSELUNGA", "-42,50"), listOf("02/10/2026", "STIPENDIO", "2.000,00"), listOf("xx", "?", "1"), listOf("03/10/2026", "?", "0")),
            roles = listOf(ColumnRole.DATE, ColumnRole.DESCRIPTION, ColumnRole.AMOUNT),
            datePattern = "dd/MM/yyyy",
            currencyCode = "EUR",
            firstLine = 2,
        )
        assertEquals(
            ImportRow.Valid(2, LocalDate.of(2026, 10, 1), TransactionType.WITHDRAWAL, 4_250, "POS ESSELUNGA", null, null, emptyList(), null),
            signed[0],
        )
        assertEquals(TransactionType.DEPOSIT, (signed[1] as ImportRow.Valid).type)
        assertEquals(200_000L, (signed[1] as ImportRow.Valid).amount)
        assertEquals(ImportRow.Invalid(4, InvalidReason.DATE), signed[2])
        assertEquals(ImportRow.Invalid(5, InvalidReason.ZERO_AMOUNT), signed[3])

        val separate = mapImportRows(
            rows = listOf(listOf("2026-10-01", "Affitto", "800,00", ""), listOf("2026-10-02", "Rimborso", "", "15")),
            roles = listOf(ColumnRole.DATE, ColumnRole.DESCRIPTION, ColumnRole.EXPENSE, ColumnRole.INCOME),
            datePattern = "yyyy-MM-dd",
            currencyCode = "EUR",
        )
        assertEquals(listOf(TransactionType.WITHDRAWAL to 80_000L, TransactionType.DEPOSIT to 1_500L), separate.map { (it as ImportRow.Valid).type to it.amount })
    }

    @Test
    fun readsDragonflyExport() {
        val rows = mapImportRows(
            rows = listOf(
                listOf("2026-10-03", "withdrawal", "Spesa", "42.50", "Supermercato", "Cibo", "Casa Settimana"),
                listOf("2026-10-04", "transfer", "Giroconto", "10.00", "", "", ""),
            ),
            roles = listOf(ColumnRole.DATE, ColumnRole.TYPE, ColumnRole.DESCRIPTION, ColumnRole.AMOUNT, ColumnRole.COUNTERPARTY, ColumnRole.CATEGORY, ColumnRole.TAGS),
            datePattern = "yyyy-MM-dd",
            currencyCode = "EUR",
        )
        with(rows[0] as ImportRow.Valid) {
            assertEquals(TransactionType.WITHDRAWAL, type)
            assertEquals(4_250L, amount)
            assertEquals("Supermercato", counterparty)
            assertEquals(listOf("Casa", "Settimana"), tags)
        }
        assertEquals(ImportRow.Invalid(2, InvalidReason.TRANSFER), rows[1])
    }
}
