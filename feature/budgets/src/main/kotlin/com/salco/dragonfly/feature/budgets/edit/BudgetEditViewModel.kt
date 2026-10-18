package com.salco.dragonfly.feature.budgets.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.BudgetsRepository
import com.salco.dragonfly.core.data.repository.DuplicateNameException
import com.salco.dragonfly.core.data.repository.UserDataRepository
import com.salco.dragonfly.core.model.Budget
import com.salco.dragonfly.core.model.minorUnitsToInput
import com.salco.dragonfly.core.model.parseAmountToMinorUnits
import com.salco.dragonfly.feature.budgets.navigation.BudgetEditRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val budgetsRepository: BudgetsRepository,
    userDataRepository: UserDataRepository,
) : ViewModel() {

    // Read the argument by name so the ViewModel stays testable without Android's Bundle.
    private val budgetId: Long = savedStateHandle[BudgetEditRoute::budgetId.name] ?: 0L

    private val _uiState = MutableStateFlow(BudgetEditUiState(isNew = budgetId == 0L, isLoading = true))
    val uiState: StateFlow<BudgetEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val budget = if (budgetId != 0L) budgetsRepository.observeBudget(budgetId).first() else null
            val defaultCurrency = userDataRepository.userPreferences.first().defaultCurrencyCode
            _uiState.update { state ->
                if (budget == null) {
                    state.copy(isLoading = false, currencyCode = defaultCurrency)
                } else {
                    state.copy(
                        isLoading = false,
                        name = budget.name,
                        amount = minorUnitsToInput(budget.amount, budget.currencyCode),
                        currencyCode = budget.currencyCode,
                        active = budget.active,
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameError = null) }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amount = value, amountError = false) }
    fun onCurrencyChange(value: String) = _uiState.update { it.copy(currencyCode = value) }
    fun onActiveChange(value: Boolean) = _uiState.update { it.copy(active = value) }

    fun save() {
        val state = _uiState.value
        val amount = parseAmountToMinorUnits(state.amount, state.currencyCode)?.takeIf { it > 0 }
        val nameError = if (state.name.isBlank()) NameError.BLANK else null
        if (nameError != null || amount == null) {
            _uiState.update { it.copy(nameError = nameError, amountError = amount == null) }
            return
        }
        viewModelScope.launch {
            try {
                budgetsRepository.saveBudget(Budget(budgetId, state.name, amount, state.currencyCode, state.active))
                _uiState.update { it.copy(isFinished = true) }
            } catch (_: DuplicateNameException) {
                _uiState.update { it.copy(nameError = NameError.DUPLICATE) }
            }
        }
    }

    fun delete() {
        if (budgetId == 0L) return
        viewModelScope.launch {
            budgetsRepository.deleteBudget(budgetId)
            _uiState.update { it.copy(isFinished = true) }
        }
    }
}

enum class NameError { BLANK, DUPLICATE }

data class BudgetEditUiState(
    val isNew: Boolean,
    val isLoading: Boolean = false,
    val name: String = "",
    val amount: String = "",
    val currencyCode: String = "EUR",
    val active: Boolean = true,
    val nameError: NameError? = null,
    val amountError: Boolean = false,
    /** True once the budget was saved or deleted and the screen should close. */
    val isFinished: Boolean = false,
)
