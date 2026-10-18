package com.salco.dragonfly.feature.piggybanks.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.AccountsRepository
import com.salco.dragonfly.core.data.repository.DuplicateNameException
import com.salco.dragonfly.core.data.repository.PiggyBanksRepository
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.PiggyBank
import com.salco.dragonfly.core.model.minorUnitsToInput
import com.salco.dragonfly.core.model.parseAmountToMinorUnits
import com.salco.dragonfly.feature.piggybanks.navigation.PiggyBankEditRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PiggyBankEditViewModel internal constructor(
    savedStateHandle: SavedStateHandle,
    private val piggyBanksRepository: PiggyBanksRepository,
    accountsRepository: AccountsRepository,
    clock: Clock,
) : ViewModel() {

    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        piggyBanksRepository: PiggyBanksRepository,
        accountsRepository: AccountsRepository,
    ) : this(savedStateHandle, piggyBanksRepository, accountsRepository, Clock.systemDefaultZone())

    // Read the argument by name so the ViewModel stays testable without Android's Bundle.
    private val piggyBankId: Long = savedStateHandle[PiggyBankEditRoute::piggyBankId.name] ?: 0L

    private val _uiState = MutableStateFlow(
        PiggyBankEditUiState(isNew = piggyBankId == 0L, isLoading = true, startDate = LocalDate.now(clock)),
    )
    val uiState: StateFlow<PiggyBankEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val all = accountsRepository.observeAccounts().first()
            val existing = if (piggyBankId != 0L) piggyBanksRepository.observePiggyBank(piggyBankId).first() else null
            val accounts = all.filter { (it.type == AccountType.ASSET && it.active) || it.id == existing?.piggyBank?.accountId }
            _uiState.update { state ->
                if (existing == null) {
                    state.copy(isLoading = false, accounts = accounts, accountId = accounts.firstOrNull()?.id)
                } else {
                    val bank = existing.piggyBank
                    state.copy(
                        isLoading = false,
                        accounts = accounts,
                        name = bank.name,
                        accountId = bank.accountId,
                        target = bank.targetAmount?.let { minorUnitsToInput(it, existing.currencyCode) }.orEmpty(),
                        startDate = bank.startDate,
                        targetDate = bank.targetDate,
                        notes = bank.notes.orEmpty(),
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameError = null) }
    fun onAccountChange(id: Long) = _uiState.update { it.copy(accountId = id, accountError = false) }
    fun onTargetChange(value: String) = _uiState.update { it.copy(target = value, targetError = false) }
    fun onStartDateChange(value: LocalDate) = _uiState.update { it.copy(startDate = value) }
    fun onTargetDateChange(value: LocalDate?) = _uiState.update { it.copy(targetDate = value) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }

    fun save() {
        val state = _uiState.value
        val account = state.account
        val target = if (state.target.isBlank()) null else parseAmountToMinorUnits(state.target, account?.currencyCode ?: "EUR")
        val targetError = state.target.isNotBlank() && (target == null || target <= 0)
        val nameError = if (state.name.isBlank()) NameError.BLANK else null
        if (nameError != null || account == null || targetError) {
            _uiState.update { it.copy(nameError = nameError, accountError = account == null, targetError = targetError) }
            return
        }
        val piggyBank = PiggyBank(
            id = piggyBankId,
            name = state.name,
            accountId = account.id,
            targetAmount = target,
            startDate = state.startDate,
            targetDate = state.targetDate,
            notes = state.notes.trim().ifEmpty { null },
        )
        viewModelScope.launch {
            try {
                piggyBanksRepository.savePiggyBank(piggyBank)
                _uiState.update { it.copy(isFinished = true) }
            } catch (_: DuplicateNameException) {
                _uiState.update { it.copy(nameError = NameError.DUPLICATE) }
            }
        }
    }

    fun delete() {
        if (piggyBankId == 0L) return
        viewModelScope.launch {
            piggyBanksRepository.deletePiggyBank(piggyBankId)
            _uiState.update { it.copy(isFinished = true) }
        }
    }
}

enum class NameError { BLANK, DUPLICATE }

data class PiggyBankEditUiState(
    val isNew: Boolean,
    val isLoading: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val name: String = "",
    val accountId: Long? = null,
    val target: String = "",
    val startDate: LocalDate,
    val targetDate: LocalDate? = null,
    val notes: String = "",
    val nameError: NameError? = null,
    val accountError: Boolean = false,
    val targetError: Boolean = false,
    /** True once the piggy bank was saved or deleted and the screen should close. */
    val isFinished: Boolean = false,
) {
    val account: Account? get() = accounts.firstOrNull { it.id == accountId }
}
