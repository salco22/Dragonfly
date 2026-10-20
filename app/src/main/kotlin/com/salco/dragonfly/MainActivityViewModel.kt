package com.salco.dragonfly

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.SyncRepository
import com.salco.dragonfly.core.data.repository.UserDataRepository
import com.salco.dragonfly.core.model.SyncAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val userDataRepository: UserDataRepository,
    syncRepository: SyncRepository,
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
            combine(userDataRepository.userPreferences, syncRepository.account, ::Pair)
                // Wait for the saved session, so the login screen does not flash before it.
                .filter { (_, account) -> account != SyncAccount.Loading }
                .collect { (prefs, account) ->
                    lockEnabled = prefs.appLockEnabled
                    val showLogin = account == SyncAccount.SignedOut && !prefs.loginSkipped
                    _uiState.value = when (val current = _uiState.value) {
                        // Cold start: lock immediately if the user enabled it.
                        MainActivityUiState.Loading -> MainActivityUiState.Ready(
                            locked = prefs.appLockEnabled,
                            hideInRecents = prefs.hideInRecents,
                            showLogin = showLogin,
                        )
                        // Turning the lock off releases it; turning it on waits for the next background.
                        is MainActivityUiState.Ready -> current.copy(
                            locked = current.locked && prefs.appLockEnabled,
                            hideInRecents = prefs.hideInRecents,
                            showLogin = showLogin,
                        )
                    }
                }
        }
    }

    /** Use the app without an account; it can be added later from More. */
    fun skipLogin() {
        viewModelScope.launch { userDataRepository.setLoginSkipped(true) }
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

    data class Ready(
        val locked: Boolean,
        override val hideInRecents: Boolean = true,
        /** Not signed in and not chosen to go on without an account. */
        val showLogin: Boolean = false,
    ) : MainActivityUiState
}
