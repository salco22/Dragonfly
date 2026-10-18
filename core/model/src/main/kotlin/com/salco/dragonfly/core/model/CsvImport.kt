package com.salco.dragonfly.core.model

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/** Meaning of a CSV column when importing transactions. */
enum class ColumnRole {
    IGNORE,
    DATE,
    DESCRIPTION,

    /** One signed amount: negative for money going out. */
    AMOUNT,

    /** Separate columns for money coming in and going out ("Avere"/"Dare"), sign ignored. */
    INCOME,
    EXPENSE,

    /** Dragonfly's own export: "withdrawal" or "deposit", with a positive amount. */
    TYPE,
    COUNTERPARTY,
    CATEGORY,
    TAGS,
    NOTES,
}

/** Decodes a file as UTF-8 (dropping a byte order mark), falling back to Windows-1252 used by many banks. */
fun decodeCsvBytes(bytes: ByteArray): String {
    val utf8 = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
    val text = try {
        utf8.decode(ByteBuffer.wrap(bytes)).toString()
    } catch (_: CharacterCodingException) {
        String(bytes, Charset.forName("windows-1252"))
    }
    return text.removePrefix("\uFEFF")
}

/** The most frequent of `;`, `,` and tab in the first lines (outside quotes). */
fun detectDelimiter(text: String): Char {
    val sample = text.lineSequence().take(10).joinToString("\n")
    return listOf(';', ',', '\t').maxBy { delimiter ->
        var inQuotes = false
        sample.count { c ->
            if (c == '"') inQuotes = !inQuotes
            !inQuotes && c == delimiter
        }
    }
}

/** Parses CSV text (RFC 4180: quoted fields, doubled quotes, line breaks inside quotes); blank lines are dropped. */
fun parseCsv(text: String, delimiter: Char = detectDelimiter(text)): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    val field = StringBuilder()
    var inQuotes = false
    var i = 0
    fun endField() {
        row.add(field.toString().trim())
        field.clear()
    }
    fun endRow() {
        endField()
        if (row.any { it.isNotEmpty() }) rows.add(row)
        row = mutableListOf()
    }
    while (i < text.length) {
        val c = text[i]
        when {
            inQuotes && c == '"' && text.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
            c == '"' -> inQuotes = !inQuotes
            !inQuotes && c == delimiter -> endField()
            !inQuotes && (c == '\n' || c == '\r') -> {
                endRow()
                if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
            }
            else -> field.append(c)
        }
        i++
    }
    if (field.isNotEmpty() || row.isNotEmpty()) endRow()
    return rows
}

private val ROLE_HINTS = listOf(
    ColumnRole.DATE to listOf("data contabile", "data operazione", "data valuta", "data", "date", "datum"),
    ColumnRole.INCOME to listOf("avere", "entrate", "accrediti", "accredito", "income", "credit"),
    ColumnRole.EXPENSE to listOf("dare", "uscite", "addebiti", "addebito", "expense", "debit"),
    ColumnRole.AMOUNT to listOf("importo", "amount", "ammontare", "valore", "betrag"),
    ColumnRole.TYPE to listOf("type", "tipo"),
    ColumnRole.COUNTERPARTY to listOf("beneficiario", "controparte", "esercente", "payee", "destination", "ordinante"),
    ColumnRole.CATEGORY to listOf("categoria", "category"),
    ColumnRole.TAGS to listOf("tags", "tag"),
    ColumnRole.NOTES to listOf("note", "notes", "memo"),
    ColumnRole.DESCRIPTION to listOf("descrizione", "causale", "description", "dettagli", "operazione", "testo"),
)

/**
 * Guesses each column's role from the header row; each role is used once, and the first
 * matching column wins (e.g. "Data contabile" before "Data valuta").
 */
fun guessColumnRoles(header: List<String>): List<ColumnRole> {
    val roles = MutableList(header.size) { ColumnRole.IGNORE }
    val used = mutableSetOf<ColumnRole>()
    ROLE_HINTS.forEach { (role, hints) ->
        if (role in used) return@forEach
        // Exact names first, then names containing a hint.
        val index = header.indexOfFirst { h -> roles[header.indexOf(h)] == ColumnRole.IGNORE && hints.any { h.trim().equals(it, true) } }
            .takeIf { it >= 0 }
            ?: header.indices.firstOrNull { i -> roles[i] == ColumnRole.IGNORE && hints.any { header[i].contains(it, ignoreCase = true) } }
        if (index != null) {
            roles[index] = role
            used += role
        }
    }
    // A signed amount and separate in/out columns are alternatives.
    if (ColumnRole.AMOUNT in used && (ColumnRole.INCOME in used || ColumnRole.EXPENSE in used)) {
        roles.replaceAll { if (it == ColumnRole.INCOME || it == ColumnRole.EXPENSE) ColumnRole.IGNORE else it }
    }
    return roles
}

/** Date formats tried when reading bank exports. */
val SupportedDatePatterns = listOf("dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy", "dd.MM.yyyy", "dd/MM/yy", "MM/dd/yyyy", "yyyyMMdd")

private fun formatter(pattern: String) = DateTimeFormatter.ofPattern(pattern.replace("yyyy", "uuuu").replace("yy", "uu"))
    .withResolverStyle(ResolverStyle.STRICT)

fun parseDate(value: String, pattern: String): LocalDate? =
    runCatching { LocalDate.parse(value.trim().take(10).trim(), formatter(pattern)) }.getOrNull()

/** The first supported pattern that reads every sample, or null. */
fun detectDatePattern(samples: List<String>): String? {
    val values = samples.map { it.trim() }.filter { it.isNotEmpty() }.take(50)
    if (values.isEmpty()) return null
    return SupportedDatePatterns.firstOrNull { pattern -> values.all { parseDate(it, pattern) != null } }
}

/** A CSV row turned into a transaction to import, or the reason it could not be. */
sealed interface ImportRow {
    val line: Int

    data class Valid(
        override val line: Int,
        val date: LocalDate,
        val type: TransactionType,
        /** Positive, in minor units of the target account's currency. */
        val amount: Long,
        val description: String,
        val counterparty: String?,
        val category: String?,
        val tags: List<String>,
        val notes: String?,
    ) : ImportRow

    data class Invalid(override val line: Int, val reason: InvalidReason) : ImportRow
}

enum class InvalidReason { DATE, AMOUNT, ZERO_AMOUNT, TRANSFER }

/**
 * Turns data rows into transactions using [roles]; [firstLine] is the 1-based line number of
 * the first data row, used in error reports.
 */
fun mapImportRows(
    rows: List<List<String>>,
    roles: List<ColumnRole>,
    datePattern: String,
    currencyCode: String,
    firstLine: Int = 1,
): List<ImportRow> = rows.mapIndexed { index, row ->
    val line = firstLine + index
    fun value(role: ColumnRole): String? = roles.indexOf(role).takeIf { it >= 0 }?.let { row.getOrNull(it)?.trim() }?.takeIf { it.isNotEmpty() }
    fun amount(role: ColumnRole): Long? = value(role)?.let { parseAmountToMinorUnits(it.replace("€", "").replace("EUR", "").trim(), currencyCode) }

    val date = value(ColumnRole.DATE)?.let { parseDate(it, datePattern) } ?: return@mapIndexed ImportRow.Invalid(line, InvalidReason.DATE)
    val type = value(ColumnRole.TYPE)?.lowercase()
    if (type == "transfer") return@mapIndexed ImportRow.Invalid(line, InvalidReason.TRANSFER)

    val signed: Long = when {
        ColumnRole.AMOUNT in roles -> {
            val raw = amount(ColumnRole.AMOUNT) ?: return@mapIndexed ImportRow.Invalid(line, InvalidReason.AMOUNT)
            // An explicit type (Dragonfly's export) gives the direction of a positive amount.
            when (type) {
                "withdrawal" -> -kotlin.math.abs(raw)
                "deposit" -> kotlin.math.abs(raw)
                else -> raw
            }
        }
        else -> {
            val income = amount(ColumnRole.INCOME)?.let { kotlin.math.abs(it) } ?: 0
            val expense = amount(ColumnRole.EXPENSE)?.let { kotlin.math.abs(it) } ?: 0
            if (value(ColumnRole.INCOME) == null && value(ColumnRole.EXPENSE) == null) {
                return@mapIndexed ImportRow.Invalid(line, InvalidReason.AMOUNT)
            }
            income - expense
        }
    }
    if (signed == 0L) return@mapIndexed ImportRow.Invalid(line, InvalidReason.ZERO_AMOUNT)

    ImportRow.Valid(
        line = line,
        date = date,
        type = if (signed < 0) TransactionType.WITHDRAWAL else TransactionType.DEPOSIT,
        amount = kotlin.math.abs(signed),
        description = value(ColumnRole.DESCRIPTION) ?: value(ColumnRole.COUNTERPARTY) ?: "",
        counterparty = value(ColumnRole.COUNTERPARTY),
        category = value(ColumnRole.CATEGORY),
        tags = value(ColumnRole.TAGS)?.split(' ', ',', ';')?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty(),
        notes = value(ColumnRole.NOTES),
    )
}
