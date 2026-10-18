package com.salco.dragonfly.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.BudgetsRepository
import com.salco.dragonfly.core.data.repository.InsightsRepository
import com.salco.dragonfly.core.data.repository.UserDataRepository
import com.salco.dragonfly.core.model.MonthlySummary
import com.salco.dragonfly.core.model.NamedTotal
import com.salco.dragonfly.core.model.NetWorthPoint
import com.salco.dragonfly.core.model.PeriodLength
import com.salco.dragonfly.core.model.ReportPeriod
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.percentChange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel internal constructor(
    private val insights: InsightsRepository,
    private val budgets: BudgetsRepository,
    userDataRepository: UserDataRepository,
    clock: Clock,
) : ViewModel() {

    @Inject
    constructor(
        insights: InsightsRepository,
        budgets: BudgetsRepository,
        userDataRepository: UserDataRepository,
    ) : this(insights, budgets, userDataRepository, Clock.systemDefaultZone())

    private val period = MutableStateFlow(ReportPeriod(PeriodLength.MONTH, LocalDate.now(clock)))

    val uiState: StateFlow<ReportsUiState> = combine(period, userDataRepository.userPreferences.map { it.defaultCurrencyCode }, ::Pair)
        .flatMapLatest { (period, currency) -> report(period, currency) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState.Loading)

    fun setLength(length: PeriodLength) = period.update { ReportPeriod(length, it.anchor) }
    fun previous() = period.update { it.previous() }
    fun next() = period.update { it.next() }

    private fun report(period: ReportPeriod, currency: String): Flow<ReportsUiState> {
        val previous = period.previous()
        val months = period.months
        val cashFlow = combine(
            insights.observeMonthlySummaries(months.first(), months.last()),
            insights.observeMonthlySummaries(previous.months.first(), previous.months.last()),
            insights.observeNetWorthHistory(months.first(), months.last()),
        ) { current, before, netWorth -> Triple(current, before, netWorth) }
        val categories = combine(
            insights.observeCategoryTotals(TransactionType.WITHDRAWAL, period.start, period.end),
            insights.observeCategoryTotals(TransactionType.WITHDRAWAL, previous.start, previous.end),
            insights.observeCategoryTotals(TransactionType.DEPOSIT, period.start, period.end),
        ) { expenses, previousExpenses, income -> Triple(expenses, previousExpenses, income) }
        val others = combine(
            insights.observeCounterpartyTotals(TransactionType.WITHDRAWAL, period.start, period.end),
            insights.observeCounterpartyTotals(TransactionType.DEPOSIT, period.start, period.end),
            insights.observeTagTotals(TransactionType.WITHDRAWAL, period.start, period.end),
            budgets.observeBudgetSpending(period.start, period.end),
        ) { payees, payers, tags, budgetSpending -> Others(payees, payers, tags, budgetSpending) }

        return combine(cashFlow, categories, others) { (current, before, netWorth), (expenses, previousExpenses, income), other ->
            fun List<MonthlySummary>.mine() = filter { it.currencyCode == currency }
            fun List<NamedTotal>.mine() = filter { it.currencyCode == currency }
            val monthly = months.map { m -> current.mine().firstOrNull { it.month == m } ?: MonthlySummary(m, currency, 0, 0) }
            val totalExpenses = expenses.mine().sumOf { it.amount }
            val previousByCategory = previousExpenses.mine().associateBy { it.id }
            ReportsUiState.Success(
                period = period,
                currencyCode = currency,
                income = monthly.sumOf { it.income },
                expenses = monthly.sumOf { it.expenses },
                previousIncome = before.mine().sumOf { it.income },
                previousExpenses = before.mine().sumOf { it.expenses },
                monthly = monthly,
                netWorth = netWorth.filter { it.currencyCode == currency },
                expenseCategories = expenses.mine().map { total ->
                    CategoryRow(
                        total = total,
                        share = if (totalExpenses == 0L) 0f else total.amount.toFloat() / totalExpenses,
                        change = previousByCategory[total.id]?.let { percentChange(total.amount, it.amount) },
                    )
                },
                incomeCategories = income.mine(),
                payees = other.payees.mine(),
                payers = other.payers.mine(),
                tags = other.tags.mine(),
                budgets = other.budgets.filter { it.budget.active && it.budget.currencyCode == currency }.map {
                    BudgetRow(it.budget.name, budgeted = it.budget.amount * months.size, spent = it.spent)
                },
            )
        }
    }

    private data class Others(
        val payees: List<NamedTotal>,
        val payers: List<NamedTotal>,
        val tags: List<NamedTotal>,
        val budgets: List<com.salco.dragonfly.core.model.BudgetSpending>,
    )
}

data class CategoryRow(
    val total: NamedTotal,
    /** Share of the period's expenses, between 0 and 1. */
    val share: Float,
    /** Percent change from the previous period; null when the category had no spending then. */
    val change: Int?,
)

data class BudgetRow(val name: String, val budgeted: Long, val spent: Long) {
    val progress: Float get() = if (budgeted <= 0) 0f else spent.toFloat() / budgeted
}

sealed interface ReportsUiState {
    data object Loading : ReportsUiState

    /** Everything in [currencyCode], the default currency. */
    data class Success(
        val period: ReportPeriod,
        val currencyCode: String,
        val income: Long,
        val expenses: Long,
        val previousIncome: Long,
        val previousExpenses: Long,
        /** One entry per month of the period. */
        val monthly: List<MonthlySummary>,
        val netWorth: List<NetWorthPoint>,
        val expenseCategories: List<CategoryRow>,
        val incomeCategories: List<NamedTotal>,
        val payees: List<NamedTotal>,
        val payers: List<NamedTotal>,
        val tags: List<NamedTotal>,
        /** Budgets with their limit over the whole period (monthly limit × months). */
        val budgets: List<BudgetRow>,
    ) : ReportsUiState {
        val net: Long get() = income - expenses

        /** Share of income not spent, in percent; null without income. */
        val savingsRate: Int? get() = if (income <= 0) null else Math.round(net * 100.0 / income).toInt()
        val averageMonthlyExpenses: Long get() = expenses / monthly.size.coerceAtLeast(1)
    }
}
