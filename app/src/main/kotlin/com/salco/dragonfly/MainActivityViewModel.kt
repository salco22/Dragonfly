package com.salco.dragonfly

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.UserDataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    userDataRepository: UserDataRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainActivityUiState>(MainActivityUiState.Loading)
    val uiState: StateFlow<MainActivityUiState> = _uiState.asStateFlow()

    private var lockEnabled = false

    // Locks again whenever the whole app goes to the background (not on rotation).
    private val processObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            if (lockEnabled) _uiState.value = MainActivityUiState.Ready(locked = true)
        }
    }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(processObserver)
        viewModelScope.launch {
            userDataRepository.userPreferences.collect { prefs ->
                lockEnabled = prefs.appLockEnabled
                _uiState.value = when (val current = _uiState.value) {
                    // Cold start: lock immediately if the user enabled it.
                    MainActivityUiState.Loading -> MainActivityUiState.Ready(locked = prefs.appLockEnabled)
                    // Turning the lock off releases it; turning it on waits for the next background.
                    is MainActivityUiState.Ready -> current.copy(locked = current.locked && prefs.appLockEnabled)
                }
            }
        }
    }

    fun unlock() {
        _uiState.value = MainActivityUiState.Ready(locked = false)
    }

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processObserver)
    }
}

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState
    data class Ready(val locked: Boolean) : MainActivityUiState
}
