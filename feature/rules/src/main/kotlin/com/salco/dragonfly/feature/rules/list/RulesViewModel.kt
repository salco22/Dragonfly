package com.salco.dragonfly.feature.rules.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.BillsRepository
import com.salco.dragonfly.core.data.repository.BudgetsRepository
import com.salco.dragonfly.core.data.repository.RulesRepository
import com.salco.dragonfly.core.data.repository.UserDataRepository
import com.salco.dragonfly.core.model.Rule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val rulesRepository: RulesRepository,
    budgetsRepository: BudgetsRepository,
    billsRepository: BillsRepository,
    userDataRepository: UserDataRepository,
) : ViewModel() {

    val uiState: StateFlow<RulesUiState> = combine(
        rulesRepository.observeRules(),
        budgetsRepository.observeBudgets(),
        billsRepository.observeBills(),
        userDataRepository.userPreferences,
    ) { rules, budgets, bills, prefs ->
        RulesUiState.Success(
            rules = rules,
            budgetNames = budgets.associate { it.id.toString() to it.name },
            billNames = bills.associate { it.id.toString() to it.name },
            currencyCode = prefs.defaultCurrencyCode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RulesUiState.Loading)

    /** Number of transactions changed by the last "apply to all", shown once. */
    private val _applied = MutableStateFlow<Int?>(null)
    val applied: StateFlow<Int?> = _applied.asStateFlow()

    fun setActive(rule: Rule, active: Boolean) {
        viewModelScope.launch { rulesRepository.setRuleActive(rule.id, active) }
    }

    fun applyAll() {
        viewModelScope.launch { _applied.value = rulesRepository.applyToExisting() }
    }

    fun appliedShown() {
        _applied.value = null
    }
}

sealed interface RulesUiState {
    data object Loading : RulesUiState
    data class Success(
        val rules: List<Rule>,
        val budgetNames: Map<String, String>,
        val billNames: Map<String, String>,
        val currencyCode: String,
    ) : RulesUiState
}
