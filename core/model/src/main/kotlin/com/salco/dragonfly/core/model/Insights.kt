package com.salco.dragonfly.core.model

import java.time.YearMonth

/** Income and expenses of one month in one currency, in minor units; transfers are excluded. */
data class MonthlySummary(
    val month: YearMonth,
    val currencyCode: String,
    val income: Long,
    val expenses: Long,
) {
    val net: Long get() = income - expenses
}

/** Withdrawals of a period in one category ([categoryId] null for uncategorized). */
data class CategorySpending(
    val categoryId: Long?,
    val categoryName: String?,
    val currencyCode: String,
    val amount: Long,
)

/** Net worth at the end of [month] in one currency. */
data class NetWorthPoint(
    val month: YearMonth,
    val currencyCode: String,
    val amount: Long,
)

/** A total for a category, payee/payer or tag ([id] null for "none"). */
data class NamedTotal(
    val id: Long?,
    val name: String?,
    val currencyCode: String,
    val amount: Long,
    /** Number of transactions summed. */
    val count: Int,
)

enum class PeriodLength { MONTH, QUARTER, YEAR }

/** A calendar month, quarter or year containing [anchor]. */
data class ReportPeriod(val length: PeriodLength, val anchor: java.time.LocalDate) {
    val start: java.time.LocalDate
        get() = when (length) {
            PeriodLength.MONTH -> anchor.withDayOfMonth(1)
            PeriodLength.QUARTER -> java.time.LocalDate.of(anchor.year, (anchor.monthValue - 1) / 3 * 3 + 1, 1)
            PeriodLength.YEAR -> anchor.withDayOfYear(1)
        }

    val end: java.time.LocalDate
        get() = when (length) {
            PeriodLength.MONTH -> start.plusMonths(1).minusDays(1)
            PeriodLength.QUARTER -> start.plusMonths(3).minusDays(1)
            PeriodLength.YEAR -> start.plusYears(1).minusDays(1)
        }

    val months: List<YearMonth>
        get() = generateSequence(YearMonth.from(start)) { it.plusMonths(1) }.takeWhile { !it.isAfter(YearMonth.from(end)) }.toList()

    fun previous(): ReportPeriod = copy(anchor = step(-1))
    fun next(): ReportPeriod = copy(anchor = step(1))

    private fun step(n: Long) = when (length) {
        PeriodLength.MONTH -> start.plusMonths(n)
        PeriodLength.QUARTER -> start.plusMonths(3 * n)
        PeriodLength.YEAR -> start.plusYears(n)
    }
}

/** Percent change from [previous] to [current]; null when there is nothing to compare with. */
fun percentChange(current: Long, previous: Long): Int? =
    if (previous == 0L) null else Math.round((current - previous) * 100.0 / kotlin.math.abs(previous)).toInt()
