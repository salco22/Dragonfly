package com.salco.dragonfly.feature.transactions.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.RecurringTransactionsRepository
import com.salco.dragonfly.core.model.RecurringTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RecurringListViewModel @Inject constructor(
    repository: RecurringTransactionsRepository,
) : ViewModel() {

    val uiState: StateFlow<RecurringListUiState> = repository.observeRecurringTransactions()
        .map<List<RecurringTransaction>, RecurringListUiState>(RecurringListUiState::Success)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringListUiState.Loading)
}

sealed interface RecurringListUiState {
    data object Loading : RecurringListUiState
    data class Success(val items: List<RecurringTransaction>) : RecurringListUiState
}
