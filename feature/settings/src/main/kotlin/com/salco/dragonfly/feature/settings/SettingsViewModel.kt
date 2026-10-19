package com.salco.dragonfly.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.UserDataRepository
import com.salco.dragonfly.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = userDataRepository.userPreferences
        .map<UserPreferences, SettingsUiState>(SettingsUiState::Success)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState.Loading)

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { userDataRepository.setAppLockEnabled(enabled) }
    }

    fun setRecurringNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { userDataRepository.setRecurringNotificationsEnabled(enabled) }
    }

    fun setHideInRecents(hide: Boolean) {
        viewModelScope.launch { userDataRepository.setHideInRecents(hide) }
    }

    fun setDefaultCurrency(currencyCode: String) {
        viewModelScope.launch { userDataRepository.setDefaultCurrencyCode(currencyCode) }
    }
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(val preferences: UserPreferences) : SettingsUiState
}
