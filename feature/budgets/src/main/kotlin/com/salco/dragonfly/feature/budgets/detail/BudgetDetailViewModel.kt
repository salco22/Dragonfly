package com.salco.dragonfly.feature.budgets.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.BudgetsRepository
import com.salco.dragonfly.core.data.repository.TransactionsRepository
import com.salco.dragonfly.core.model.BudgetSpending
import com.salco.dragonfly.core.model.Transaction
import com.salco.dragonfly.core.model.TransactionFilter
import com.salco.dragonfly.feature.budgets.navigation.BudgetDetailRoute
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
class BudgetDetailViewModel internal constructor(
    savedStateHandle: SavedStateHandle,
    budgetsRepository: BudgetsRepository,
    transactionsRepository: TransactionsRepository,
    private val clock: Clock,
) : ViewModel() {

    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        budgetsRepository: BudgetsRepository,
        transactionsRepository: TransactionsRepository,
    ) : this(savedStateHandle, budgetsRepository, transactionsRepository, Clock.systemDefaultZone())

    // Read the arguments by name so the ViewModel stays testable without Android's Bundle.
    val budgetId: Long = savedStateHandle[BudgetDetailRoute::budgetId.name] ?: 0L
    private val month = MutableStateFlow(
        savedStateHandle.get<String>(BudgetDetailRoute::month.name)?.let(YearMonth::parse) ?: YearMonth.now(clock),
    )

    val uiState: StateFlow<BudgetDetailUiState> = month
        .flatMapLatest { month ->
            val from = month.atDay(1)
            val to = month.atEndOfMonth()
            combine(
                budgetsRepository.observeBudgetSpending(from, to),
                transactionsRepository.searchTransactions(TransactionFilter(budgetId = budgetId, from = from, to = to)),
            ) { spending, transactions ->
                val budget = spending.firstOrNull { it.budget.id == budgetId }
                if (budget == null) {
                    BudgetDetailUiState.NotFound
                } else {
                    BudgetDetailUiState.Success(
                        month = month,
                        spending = budget,
                        days = transactions.groupBy { it.date }.toList(),
                        daysLeft = daysLeftIn(month),
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetDetailUiState.Loading)

    fun previousMonth() = month.update { it.minusMonths(1) }
    fun nextMonth() = month.update { it.plusMonths(1) }

    private fun daysLeftIn(month: YearMonth): Int? {
        val today = LocalDate.now(clock)
        if (YearMonth.from(today) != month) return null
        return ChronoUnit.DAYS.between(today, month.atEndOfMonth()).toInt() + 1
    }
}

sealed interface BudgetDetailUiState {
    data object Loading : BudgetDetailUiState

    /** The budget was deleted while on screen. */
    data object NotFound : BudgetDetailUiState

    data class Success(
        val month: YearMonth,
        val spending: BudgetSpending,
        val days: List<Pair<LocalDate, List<Transaction>>>,
        val daysLeft: Int?,
    ) : BudgetDetailUiState
}
