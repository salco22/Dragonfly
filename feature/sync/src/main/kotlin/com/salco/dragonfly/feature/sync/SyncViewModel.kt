package com.salco.dragonfly.feature.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.SyncRepository
import com.salco.dragonfly.core.model.SyncAccount
import com.salco.dragonfly.core.model.SyncError
import com.salco.dragonfly.core.model.SyncException
import com.salco.dragonfly.core.model.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val repository: SyncRepository,
) : ViewModel() {

    private val form = MutableStateFlow(SignInForm())

    val uiState: StateFlow<SyncUiState> = combine(repository.account, repository.status, form) { account, status, form ->
        SyncUiState(account, status, form)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncUiState())

    fun onEmailChange(value: String) = form.update { it.copy(email = value, error = null) }
    fun onCodeChange(value: String) = form.update { it.copy(code = value.filter(Char::isDigit), error = null) }

    /** Back to the email field, e.g. after a typo. */
    fun changeEmail() = form.update { it.copy(codeSent = false, code = "", error = null) }

    fun sendCode() {
        val email = form.value.email.trim()
        if (!EMAIL.matches(email)) {
            form.update { it.copy(emailError = true) }
            return
        }
        launchBusy {
            repository.sendCode(email)
            form.update { it.copy(codeSent = true, code = "", emailError = false) }
        }
    }

    fun verifyCode() {
        val state = form.value
        if (state.code.length < MIN_CODE_LENGTH) {
            form.update { it.copy(error = SyncError.InvalidCode) }
            return
        }
        launchBusy {
            repository.verifyCode(state.email, state.code)
            form.value = SignInForm()
            // First sync right away, so the user sees their data arrive.
            runCatching { repository.sync() }
        }
    }

    /** Google client id for the account picker, or null when Google sign-in is not set up. */
    val googleClientId: String? get() = repository.googleClientId

    internal fun onGoogleResult(result: GoogleSignInResult) {
        when (result) {
            is GoogleSignInResult.Token -> launchBusy {
                repository.signInWithGoogle(result.idToken, result.rawNonce)
                form.value = SignInForm()
                runCatching { repository.sync() }
            }
            GoogleSignInResult.Cancelled -> Unit
            GoogleSignInResult.NoAccount -> form.update { it.copy(googleError = GoogleError.NoAccount) }
            GoogleSignInResult.Failed -> form.update { it.copy(googleError = GoogleError.Failed) }
        }
    }

    fun syncNow() = launchBusy { repository.sync() }

    fun signOut() = launchBusy { repository.signOut() }

    fun messageShown() = form.update { it.copy(error = null, googleError = null) }

    private fun launchBusy(block: suspend () -> Unit) {
        if (form.value.busy) return
        form.update { it.copy(busy = true, error = null, googleError = null) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: SyncException) {
                form.update { it.copy(error = e.error) }
            } finally {
                form.update { it.copy(busy = false) }
            }
        }
    }

    private companion object {
        val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        const val MIN_CODE_LENGTH = 6
    }
}

data class SignInForm(
    val email: String = "",
    val emailError: Boolean = false,
    val codeSent: Boolean = false,
    val code: String = "",
    val busy: Boolean = false,
    /** Error of the last action, shown once. */
    val error: SyncError? = null,
    val googleError: GoogleError? = null,
)

enum class GoogleError { NoAccount, Failed }

data class SyncUiState(
    val account: SyncAccount = SyncAccount.Loading,
    val status: SyncStatus = SyncStatus(),
    val form: SignInForm = SignInForm(),
)
