package com.salco.dragonfly.feature.piggybanks.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.AccountsRepository
import com.salco.dragonfly.core.data.repository.InsufficientFundsException
import com.salco.dragonfly.core.data.repository.PiggyBanksRepository
import com.salco.dragonfly.core.model.PiggyBankEvent
import com.salco.dragonfly.core.model.PiggyBankSummary
import com.salco.dragonfly.core.model.parseAmountToMinorUnits
import com.salco.dragonfly.feature.piggybanks.navigation.PiggyBankDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PiggyBankDetailViewModel internal constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: PiggyBanksRepository,
    accountsRepository: AccountsRepository,
    private val clock: Clock,
) : ViewModel() {

    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        repository: PiggyBanksRepository,
        accountsRepository: AccountsRepository,
    ) : this(savedStateHandle, repository, accountsRepository, Clock.systemDefaultZone())

    // Read the argument by name so the ViewModel stays testable without Android's Bundle.
    val piggyBankId: Long = savedStateHandle[PiggyBankDetailRoute::piggyBankId.name] ?: 0L

    val uiState: StateFlow<PiggyBankDetailUiState> = combine(
        repository.observePiggyBank(piggyBankId),
        repository.observeEvents(piggyBankId),
        repository.observePiggyBanks(),
        accountsRepository.observeAccounts(),
    ) { summary, events, all, accounts ->
        if (summary == null) return@combine PiggyBankDetailUiState.NotFound
        val accountId = summary.piggyBank.accountId
        val balance = accounts.firstOrNull { it.id == accountId }?.balance ?: 0
        val savedInAccount = all.filter { it.piggyBank.accountId == accountId }.sumOf { it.saved }
        PiggyBankDetailUiState.Success(
            summary = summary,
            events = events,
            availableInAccount = (balance - savedInAccount).coerceAtLeast(0),
            today = LocalDate.now(clock),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PiggyBankDetailUiState.Loading)

    private val _dialog = MutableStateFlow<MoveMoneyState?>(null)
    val dialog: StateFlow<MoveMoneyState?> = _dialog.asStateFlow()

    fun startAdding() {
        _dialog.value = MoveMoneyState(adding = true)
    }

    fun startRemoving() {
        _dialog.value = MoveMoneyState(adding = false)
    }

    fun onAmountChange(value: String) = _dialog.update { it?.copy(amount = value, error = null) }

    fun dismiss() {
        _dialog.value = null
    }

    fun confirm() {
        val dialog = _dialog.value ?: return
        val summary = (uiState.value as? PiggyBankDetailUiState.Success)?.summary ?: return
        val amount = parseAmountToMinorUnits(dialog.amount, summary.currencyCode)?.takeIf { it > 0 }
        if (amount == null) {
            _dialog.value = dialog.copy(error = MoveMoneyError.Invalid)
            return
        }
        viewModelScope.launch {
            try {
                repository.moveMoney(piggyBankId, if (dialog.adding) amount else -amount, LocalDate.now(clock))
                _dialog.value = null
            } catch (e: InsufficientFundsException) {
                _dialog.value = dialog.copy(error = MoveMoneyError.Insufficient(e.available))
            }
        }
    }
}

data class MoveMoneyState(
    val adding: Boolean,
    val amount: String = "",
    val error: MoveMoneyError? = null,
)

sealed interface MoveMoneyError {
    data object Invalid : MoveMoneyError
    data class Insufficient(val available: Long) : MoveMoneyError
}

sealed interface PiggyBankDetailUiState {
    data object Loading : PiggyBankDetailUiState

    /** The piggy bank was deleted while on screen. */
    data object NotFound : PiggyBankDetailUiState

    data class Success(
        val summary: PiggyBankSummary,
        /** Money added and removed, newest first. */
        val events: List<PiggyBankEvent>,
        /** Account money not yet put aside in any piggy bank. */
        val availableInAccount: Long,
        val today: LocalDate,
    ) : PiggyBankDetailUiState
}
