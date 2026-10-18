package com.salco.dragonfly.feature.bills.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.BillsRepository
import com.salco.dragonfly.core.data.repository.TransactionsRepository
import com.salco.dragonfly.core.model.Bill
import com.salco.dragonfly.core.model.BillStatus
import com.salco.dragonfly.core.model.Transaction
import com.salco.dragonfly.core.model.TransactionFilter
import com.salco.dragonfly.core.model.statusOn
import com.salco.dragonfly.feature.bills.navigation.BillDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class BillDetailViewModel internal constructor(
    savedStateHandle: SavedStateHandle,
    billsRepository: BillsRepository,
    transactionsRepository: TransactionsRepository,
    clock: Clock,
) : ViewModel() {

    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        billsRepository: BillsRepository,
        transactionsRepository: TransactionsRepository,
    ) : this(savedStateHandle, billsRepository, transactionsRepository, Clock.systemDefaultZone())

    // Read the argument by name so the ViewModel stays testable without Android's Bundle.
    val billId: Long = savedStateHandle[BillDetailRoute::billId.name] ?: 0L

    val uiState: StateFlow<BillDetailUiState> = combine(
        billsRepository.observeBill(billId),
        billsRepository.observePayments(),
        transactionsRepository.searchTransactions(TransactionFilter(billId = billId)),
    ) { bill, payments, transactions ->
        if (bill == null) return@combine BillDetailUiState.NotFound
        val today = LocalDate.now(clock)
        BillDetailUiState.Success(
            bill = bill,
            status = bill.statusOn(today, payments),
            upcoming = generateSequence(0) { it + 1 }
                .map { bill.rule.nextAfter(it) }
                .takeWhile { it != null }
                .filterNotNull()
                .dropWhile { it.isBefore(today) }
                .take(3)
                .toList(),
            payments = transactions,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillDetailUiState.Loading)
}

sealed interface BillDetailUiState {
    data object Loading : BillDetailUiState

    /** The bill was deleted while on screen. */
    data object NotFound : BillDetailUiState

    data class Success(
        val bill: Bill,
        val status: BillStatus,
        /** The next due dates from today. */
        val upcoming: List<LocalDate>,
        /** Withdrawals linked to the bill, newest first. */
        val payments: List<Transaction>,
    ) : BillDetailUiState
}
