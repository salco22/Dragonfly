package com.salco.dragonfly.feature.budgets.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.BudgetsRepository
import com.salco.dragonfly.core.model.BudgetSpending
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class BudgetsViewModel internal constructor(
    budgetsRepository: BudgetsRepository,
    private val clock: Clock,
) : ViewModel() {

    @Inject
    constructor(budgetsRepository: BudgetsRepository) : this(budgetsRepository, Clock.systemDefaultZone())

    private val month = MutableStateFlow(YearMonth.now(clock))

    val uiState: StateFlow<BudgetsUiState> = month
        .flatMapLatest { month ->
            val from = month.atDay(1)
            val to = month.atEndOfMonth()
            combine(
                budgetsRepository.observeBudgetSpending(from, to),
                budgetsRepository.observeUnbudgetedSpending(from, to),
            ) { spending, unbudgeted ->
                val active = spending.filter { it.budget.active }
                BudgetsUiState.Success(
                    month = month,
                    budgets = active,
                    inactive = spending.filterNot { it.budget.active },
                    totals = active.groupBy { it.budget.currencyCode }.mapValues { (_, list) ->
                        BudgetTotals(budgeted = list.sumOf { it.budget.amount }, spent = list.sumOf { it.spent })
                    },
                    unbudgeted = unbudgeted,
                    daysLeft = daysLeftIn(month),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsUiState.Loading)

    fun previousMonth() = month.update { it.minusMonths(1) }
    fun nextMonth() = month.update { it.plusMonths(1) }

    /** Days left in the current month, today included; null for other months. */
    private fun daysLeftIn(month: YearMonth): Int? {
        val today = LocalDate.now(clock)
        if (YearMonth.from(today) != month) return null
        return ChronoUnit.DAYS.between(today, month.atEndOfMonth()).toInt() + 1
    }
}

data class BudgetTotals(val budgeted: Long, val spent: Long) {
    val left: Long get() = budgeted - spent
}

sealed interface BudgetsUiState {
    data object Loading : BudgetsUiState
    data class Success(
        val month: YearMonth,
        val budgets: List<BudgetSpending>,
        val inactive: List<BudgetSpending>,
        /** Active budgets summed per currency. */
        val totals: Map<String, BudgetTotals>,
        /** Withdrawals of the month without a budget, per currency. */
        val unbudgeted: Map<String, Long>,
        /** Days left in the month when it is the current one, to suggest a daily allowance. */
        val daysLeft: Int?,
    ) : BudgetsUiState {
        val isEmpty: Boolean get() = budgets.isEmpty() && inactive.isEmpty()
    }
}
