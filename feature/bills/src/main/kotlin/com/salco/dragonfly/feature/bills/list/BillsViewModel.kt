package com.salco.dragonfly.feature.bills.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.BillsRepository
import com.salco.dragonfly.core.model.Bill
import com.salco.dragonfly.core.model.BillStatus
import com.salco.dragonfly.core.model.dueDatesBetween
import com.salco.dragonfly.core.model.statusOn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class BillsViewModel internal constructor(
    billsRepository: BillsRepository,
    clock: Clock,
) : ViewModel() {

    @Inject
    constructor(billsRepository: BillsRepository) : this(billsRepository, Clock.systemDefaultZone())

    val uiState: StateFlow<BillsUiState> = combine(billsRepository.observeBills(), billsRepository.observePayments()) { bills, payments ->
        val today = LocalDate.now(clock)
        val month = YearMonth.from(today)
        val items = bills.map { BillItem(it, it.statusOn(today, payments)) }.sortedWith(compareBy({ it.status.order }, { it.status.sortDate }))
        val paymentsInMonth = payments.filter { YearMonth.from(it.date) == month }
        val currencyOf = bills.associate { it.id to it.currencyCode }
        BillsUiState.Success(
            items = items,
            expectedThisMonth = bills.filter { it.active }
                .groupBy { it.currencyCode }
                .mapValues { (_, list) -> list.sumOf { it.amountMax * it.dueDatesBetween(month.atDay(1), month.atEndOfMonth()).size } }
                .filterValues { it > 0 },
            paidThisMonth = paymentsInMonth.groupBy { currencyOf[it.billId] ?: "" }
                .filterKeys { it.isNotEmpty() }
                .mapValues { (_, list) -> list.sumOf { it.amount } },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillsUiState.Loading)
}

data class BillItem(val bill: Bill, val status: BillStatus)

/** Overdue first, then due, not started, paid and inactive. */
private val BillStatus.order: Int
    get() = when (this) {
        is BillStatus.Overdue -> 0
        is BillStatus.Due -> 1
        is BillStatus.NotStarted -> 2
        is BillStatus.Paid -> 3
        BillStatus.Inactive -> 4
    }

private val BillStatus.sortDate: LocalDate
    get() = when (this) {
        is BillStatus.Overdue -> dueDate
        is BillStatus.Due -> dueDate
        is BillStatus.NotStarted -> firstDate
        is BillStatus.Paid -> nextDue ?: LocalDate.MAX
        BillStatus.Inactive -> LocalDate.MAX
    }

sealed interface BillsUiState {
    data object Loading : BillsUiState
    data class Success(
        val items: List<BillItem>,
        /** Maximum amounts of the bills due this month, per currency. */
        val expectedThisMonth: Map<String, Long>,
        /** Payments linked to bills this month, per currency. */
        val paidThisMonth: Map<String, Long>,
    ) : BillsUiState
}
