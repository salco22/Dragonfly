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
