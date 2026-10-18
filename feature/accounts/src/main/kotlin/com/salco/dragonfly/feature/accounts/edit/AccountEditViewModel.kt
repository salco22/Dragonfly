package com.salco.dragonfly.feature.accounts.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.AccountsRepository
import com.salco.dragonfly.core.data.repository.UserDataRepository
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.AssetRole
import com.salco.dragonfly.core.model.LiabilityType
import com.salco.dragonfly.core.model.minorUnitsToInput
import com.salco.dragonfly.core.model.parseAmountToMinorUnits
import com.salco.dragonfly.feature.accounts.hasBalance
import com.salco.dragonfly.feature.accounts.navigation.AccountEditRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import kotlin.math.absoluteValue

@HiltViewModel
class AccountEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountsRepository: AccountsRepository,
    private val userDataRepository: UserDataRepository,
) : ViewModel() {

    // Read the argument by name so the ViewModel stays testable without Android's Bundle.
    private val accountId: Long = savedStateHandle[AccountEditRoute::accountId.name] ?: 0L

    private val _uiState = MutableStateFlow(AccountEditUiState(isNew = accountId == 0L, isLoading = true))
    val uiState: StateFlow<AccountEditUiState> = _uiState.asStateFlow()

    private var createdAt: Instant = Instant.now()

    init {
        viewModelScope.launch {
            val account = if (accountId != 0L) accountsRepository.observeAccount(accountId).first() else null
            _uiState.update { state ->
                if (account == null) {
                    state.copy(
                        isLoading = false,
                        currencyCode = userDataRepository.userPreferences.first().defaultCurrencyCode,
                    )
                } else {
                    createdAt = account.createdAt
                    state.copy(
                        isLoading = false,
                        name = account.name,
                        type = account.type,
                        assetRole = account.assetRole ?: AssetRole.DEFAULT,
                        liabilityType = account.liabilityType ?: LiabilityType.DEBT,
                        currencyCode = account.currencyCode,
                        // Liabilities are stored as negative balances but entered as amounts owed.
                        openingBalance = if (account.openingBalance == 0L) "" else minorUnitsToInput(
                            if (account.type == AccountType.LIABILITY) account.openingBalance.absoluteValue else account.openingBalance,
                            account.currencyCode,
                        ),
                        iban = account.iban.orEmpty(),
                        notes = account.notes.orEmpty(),
                        active = account.active,
                        includeInNetWorth = account.includeInNetWorth,
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameError = false) }
    fun onTypeChange(value: AccountType) = _uiState.update { it.copy(type = value) }
    fun onAssetRoleChange(value: AssetRole) = _uiState.update { it.copy(assetRole = value) }
    fun onLiabilityTypeChange(value: LiabilityType) = _uiState.update { it.copy(liabilityType = value) }
    fun onCurrencyChange(value: String) = _uiState.update { it.copy(currencyCode = value) }
    fun onOpeningBalanceChange(value: String) = _uiState.update { it.copy(openingBalance = value, amountError = false) }
    fun onIbanChange(value: String) = _uiState.update { it.copy(iban = value) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }
    fun onActiveChange(value: Boolean) = _uiState.update { it.copy(active = value) }
    fun onIncludeInNetWorthChange(value: Boolean) = _uiState.update { it.copy(includeInNetWorth = value) }

    fun save() {
        val state = _uiState.value
        val amount = if (state.type.hasBalance && state.openingBalance.isNotBlank()) {
            parseAmountToMinorUnits(state.openingBalance, state.currencyCode)
        } else {
            0L
        }
        val nameError = state.name.isBlank()
        if (nameError || amount == null) {
            _uiState.update { it.copy(nameError = nameError, amountError = amount == null) }
            return
        }
        val account = Account(
            id = accountId,
            name = state.name.trim(),
            type = state.type,
            currencyCode = state.currencyCode,
            openingBalance = if (state.type == AccountType.LIABILITY) -amount.absoluteValue else amount,
            iban = state.iban.trim().takeIf { state.type == AccountType.ASSET && it.isNotEmpty() },
            notes = state.notes.trim().ifEmpty { null },
            active = state.active,
            includeInNetWorth = state.includeInNetWorth && state.type.hasBalance,
            assetRole = state.assetRole.takeIf { state.type == AccountType.ASSET },
            liabilityType = state.liabilityType.takeIf { state.type == AccountType.LIABILITY },
            createdAt = createdAt,
        )
        viewModelScope.launch {
            accountsRepository.saveAccount(account)
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    fun delete() {
        if (accountId == 0L) return
        viewModelScope.launch {
            accountsRepository.deleteAccount(accountId)
            _uiState.update { it.copy(isFinished = true) }
        }
    }
}

data class AccountEditUiState(
    val isNew: Boolean,
    val isLoading: Boolean = false,
    val name: String = "",
    val type: AccountType = AccountType.ASSET,
    val assetRole: AssetRole = AssetRole.DEFAULT,
    val liabilityType: LiabilityType = LiabilityType.DEBT,
    val currencyCode: String = "EUR",
    val openingBalance: String = "",
    val iban: String = "",
    val notes: String = "",
    val active: Boolean = true,
    val includeInNetWorth: Boolean = true,
    val nameError: Boolean = false,
    val amountError: Boolean = false,
    /** True once the account was saved or deleted and the screen should close. */
    val isFinished: Boolean = false,
)
