package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.database.dao.InsightsDao
import com.salco.dragonfly.core.model.CategorySpending
import com.salco.dragonfly.core.model.MonthlySummary
import com.salco.dragonfly.core.model.NetWorthPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject

/** Aggregates for the dashboard and reports. */
interface InsightsRepository {
    /** Income and expenses per month and currency from [from] to [to] inclusive; empty months are omitted. */
    fun observeMonthlySummaries(from: YearMonth, to: YearMonth): Flow<List<MonthlySummary>>

    /** Withdrawals of [month] per category and currency, largest first. */
    fun observeCategorySpending(month: YearMonth): Flow<List<CategorySpending>>

    /** Net worth at the end of each month from [from] to [to] inclusive, per currency. */
    fun observeNetWorthHistory(from: YearMonth, to: YearMonth): Flow<List<NetWorthPoint>>
}

internal class OfflineInsightsRepository @Inject constructor(
    private val insightsDao: InsightsDao,
) : InsightsRepository {

    override fun observeMonthlySummaries(from: YearMonth, to: YearMonth): Flow<List<MonthlySummary>> =
        insightsDao.observeMonthlySummaries(from.atDay(1).toEpochDay(), to.atEndOfMonth().toEpochDay()).map { rows ->
            rows.map { MonthlySummary(YearMonth.parse(it.month), it.currencyCode, it.income, it.expenses) }
        }

    override fun observeCategorySpending(month: YearMonth): Flow<List<CategorySpending>> =
        insightsDao.observeCategorySpending(month.atDay(1).toEpochDay(), month.atEndOfMonth().toEpochDay()).map { rows ->
            rows.map { CategorySpending(it.categoryId, it.categoryName, it.currencyCode, it.amount) }
        }

    override fun observeNetWorthHistory(from: YearMonth, to: YearMonth): Flow<List<NetWorthPoint>> =
        combine(insightsDao.observeOpeningBalances(), insightsDao.observeNetWorthFlows()) { openings, flows ->
            val currencies = (openings.map { it.currencyCode } + flows.map { it.currencyCode }).distinct()
            val flowsByCurrency = flows.groupBy { it.currencyCode }
            currencies.flatMap { currency ->
                val opening = openings.firstOrNull { it.currencyCode == currency }?.total ?: 0
                val deltas = flowsByCurrency[currency].orEmpty().associate { YearMonth.parse(it.month) to it.delta }
                // Everything before the first month shown is folded into the starting value.
                var running = opening + deltas.filterKeys { it < from }.values.sum()
                generateSequence(from) { it.plusMonths(1) }.takeWhile { it <= to }.map { month ->
                    running += deltas[month] ?: 0
                    NetWorthPoint(month, currency, running)
                }.toList()
            }
        }
}
