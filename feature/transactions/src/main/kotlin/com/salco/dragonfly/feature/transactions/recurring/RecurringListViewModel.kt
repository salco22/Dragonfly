package com.salco.dragonfly.feature.transactions.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.RecurringTransactionsRepository
import com.salco.dragonfly.core.data.repository.UserDataRepository
import com.salco.dragonfly.core.model.RecurringTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecurringListViewModel @Inject constructor(
    repository: RecurringTransactionsRepository,
    private val userDataRepository: UserDataRepository,
) : ViewModel() {

    val uiState: StateFlow<RecurringListUiState> = combine(
        repository.observeRecurringTransactions(),
        userDataRepository.userPreferences,
    ) { items, preferences ->
        RecurringListUiState.Success(items, notificationsWanted = preferences.recurringNotificationsEnabled)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringListUiState.Loading)

    /** The user does not want to be told about recurring transactions. */
    fun declineNotifications() {
        viewModelScope.launch { userDataRepository.setRecurringNotificationsEnabled(false) }
    }
}

sealed interface RecurringListUiState {
    data object Loading : RecurringListUiState
    data class Success(
        val items: List<RecurringTransaction>,
        /** The user has not turned off the notifications in settings. */
        val notificationsWanted: Boolean = true,
    ) : RecurringListUiState
}
