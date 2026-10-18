package com.salco.dragonfly.core.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Currencies offered in the app; the first one is the default. */
val SupportedCurrencyCodes = listOf("EUR", "USD", "GBP", "CHF", "JPY")

fun fractionDigits(currencyCode: String): Int =
    runCatching { Currency.getInstance(currencyCode).defaultFractionDigits }
        .getOrDefault(2)
        .coerceAtLeast(0)

/** Formats an amount expressed in minor units, e.g. 123456 EUR -> "€1,234.56". */
fun formatMoney(minorUnits: Long, currencyCode: String, locale: Locale = Locale.getDefault()): String {
    val digits = fractionDigits(currencyCode)
    val format = NumberFormat.getCurrencyInstance(locale).apply {
        runCatching { currency = Currency.getInstance(currencyCode) }
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }
    return format.format(BigDecimal.valueOf(minorUnits, digits))
}

/** Renders minor units as an editable decimal string, e.g. 123456 -> "1234.56". */
fun minorUnitsToInput(minorUnits: Long, currencyCode: String): String =
    BigDecimal.valueOf(minorUnits, fractionDigits(currencyCode)).toPlainString()

/**
 * Parses user input such as "1234,5", "1.234,56" or "-12.30" into minor units.
 * The last '.' or ',' is the decimal separator when it is followed by 1 up to the
 * currency's fraction digits; every other separator is grouping ("1.234" is 1234 EUR).
 * Returns null when the text is not a valid amount.
 */
fun parseAmountToMinorUnits(text: String, currencyCode: String): Long? {
    val digits = fractionDigits(currencyCode)
    val cleaned = text.trim().replace(" ", "")
    if (cleaned.isEmpty()) return null
    val negative = cleaned.startsWith("-")
    val body = cleaned.removePrefix("-").removePrefix("+")
    if (body.isEmpty() || body.any { !it.isDigit() && it != '.' && it != ',' }) return null

    val lastSeparator = body.indexOfLast { it == '.' || it == ',' }
    val decimalIndex = lastSeparator.takeIf { it >= 0 && body.length - it - 1 in 1..digits }
    val integerPart = (if (decimalIndex != null) body.substring(0, decimalIndex) else body)
        .replace(".", "").replace(",", "")
    val fractionPart = if (decimalIndex != null) body.substring(decimalIndex + 1) else ""
    if (integerPart.isEmpty() && fractionPart.isEmpty()) return null
    if (fractionPart.any { !it.isDigit() }) return null

    return runCatching {
        val value = BigDecimal("${integerPart.ifEmpty { "0" }}.${fractionPart.ifEmpty { "0" }}")
            .setScale(digits, RoundingMode.UNNECESSARY)
            .movePointRight(digits)
            .longValueExact()
        if (negative) -value else value
    }.getOrNull()
}
