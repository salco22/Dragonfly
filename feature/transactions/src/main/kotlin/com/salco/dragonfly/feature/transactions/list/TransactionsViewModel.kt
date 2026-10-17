package com.salco.dragonfly.feature.transactions.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.TransactionsRepository
import com.salco.dragonfly.core.model.Transaction
import com.salco.dragonfly.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel internal constructor(
    transactionsRepository: TransactionsRepository,
    clock: Clock,
) : ViewModel() {

    @Inject
    constructor(transactionsRepository: TransactionsRepository) : this(transactionsRepository, Clock.systemDefaultZone())

    private val month = MutableStateFlow(YearMonth.now(clock))

    val uiState: StateFlow<TransactionsUiState> = month
        .flatMapLatest { month ->
            transactionsRepository.observeTransactions(month.atDay(1), month.atEndOfMonth())
                .map { transactions -> transactions.toUiState(month) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState.Loading)

    fun previousMonth() = month.update { it.minusMonths(1) }
    fun nextMonth() = month.update { it.plusMonths(1) }
}

private fun List<Transaction>.toUiState(month: YearMonth): TransactionsUiState.Success {
    // Transfers move money between own accounts, so they count as neither income nor expenses.
    val totals = filter { it.type != TransactionType.TRANSFER }
        .groupBy { it.currencyCode }
        .mapValues { (_, list) ->
            MonthTotals(
                income = list.filter { it.type == TransactionType.DEPOSIT }.sumOf { it.amount },
                expenses = list.filter { it.type == TransactionType.WITHDRAWAL }.sumOf { it.amount },
            )
        }
    return TransactionsUiState.Success(
        month = month,
        days = groupBy { it.date }.map { (date, list) -> DaySection(date, list) },
        totals = totals,
    )
}

data class MonthTotals(val income: Long, val expenses: Long)

data class DaySection(val date: LocalDate, val transactions: List<Transaction>)

sealed interface TransactionsUiState {
    data object Loading : TransactionsUiState
    data class Success(
        val month: YearMonth,
        /** Days with at least one transaction, newest first. */
        val days: List<DaySection>,
        /** Income and expenses of the month per currency, in minor units. */
        val totals: Map<String, MonthTotals>,
    ) : TransactionsUiState
}
