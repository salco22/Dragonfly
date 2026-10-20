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
            val current = _uiState.value
            if (lockEnabled && current is MainActivityUiState.Ready) _uiState.value = current.copy(locked = true)
        }
    }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(processObserver)
        viewModelScope.launch {
            userDataRepository.userPreferences.collect { prefs ->
                lockEnabled = prefs.appLockEnabled
                _uiState.value = when (val current = _uiState.value) {
                    // Cold start: lock immediately if the user enabled it.
                    MainActivityUiState.Loading -> MainActivityUiState.Ready(locked = prefs.appLockEnabled, hideInRecents = prefs.hideInRecents)
                    // Turning the lock off releases it; turning it on waits for the next background.
                    is MainActivityUiState.Ready -> current.copy(
                        locked = current.locked && prefs.appLockEnabled,
                        hideInRecents = prefs.hideInRecents,
                    )
                }
            }
        }
    }

    fun unlock() {
        val current = _uiState.value
        if (current is MainActivityUiState.Ready) _uiState.value = current.copy(locked = false)
    }

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processObserver)
    }
}

sealed interface MainActivityUiState {
    /** Hides the app in recents until preferences are read, so nothing shows meanwhile. */
    val hideInRecents: Boolean

    data object Loading : MainActivityUiState {
        override val hideInRecents = true
    }

    data class Ready(val locked: Boolean, override val hideInRecents: Boolean = true) : MainActivityUiState
}
