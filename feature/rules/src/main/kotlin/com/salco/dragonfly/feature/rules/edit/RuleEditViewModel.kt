package com.salco.dragonfly.feature.rules.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.AccountsRepository
import com.salco.dragonfly.core.data.repository.BillsRepository
import com.salco.dragonfly.core.data.repository.BudgetsRepository
import com.salco.dragonfly.core.data.repository.CategoriesRepository
import com.salco.dragonfly.core.data.repository.RulesRepository
import com.salco.dragonfly.core.data.repository.TagsRepository
import com.salco.dragonfly.core.data.repository.UserDataRepository
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.ActionType
import com.salco.dragonfly.core.model.Bill
import com.salco.dragonfly.core.model.Budget
import com.salco.dragonfly.core.model.Rule
import com.salco.dragonfly.core.model.RuleAction
import com.salco.dragonfly.core.model.RuleTrigger
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.TriggerField
import com.salco.dragonfly.core.model.TriggerOperator
import com.salco.dragonfly.core.model.minorUnitsToInput
import com.salco.dragonfly.core.model.operators
import com.salco.dragonfly.core.model.parseAmountToMinorUnits
import com.salco.dragonfly.feature.rules.navigation.RuleEditRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RuleEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val rulesRepository: RulesRepository,
    accountsRepository: AccountsRepository,
    categoriesRepository: CategoriesRepository,
    tagsRepository: TagsRepository,
    budgetsRepository: BudgetsRepository,
    billsRepository: BillsRepository,
    userDataRepository: UserDataRepository,
) : ViewModel() {

    // Read the argument by name so the ViewModel stays testable without Android's Bundle.
    private val ruleId: Long = savedStateHandle[RuleEditRoute::ruleId.name] ?: 0L

    private val _uiState = MutableStateFlow(RuleEditUiState(isNew = ruleId == 0L, isLoading = true))
    val uiState: StateFlow<RuleEditUiState> = _uiState.asStateFlow()

    private var existing: Rule? = null

    init {
        viewModelScope.launch {
            val accounts = accountsRepository.observeAccounts().first()
            val currency = userDataRepository.userPreferences.first().defaultCurrencyCode
            existing = if (ruleId != 0L) rulesRepository.observeRule(ruleId).first() else null
            _uiState.update { state ->
                val base = state.copy(
                    isLoading = false,
                    currencyCode = currency,
                    accountNames = accounts.filter { it.type == AccountType.ASSET || it.type == AccountType.LIABILITY }.map { it.name },
                    counterpartyNames = accounts.filter { it.type == AccountType.EXPENSE || it.type == AccountType.REVENUE }.map { it.name },
                    categoryNames = categoriesRepository.observeCategories().first().map { it.name },
                    tagNames = tagsRepository.observeTags().first().map { it.name },
                    budgets = budgetsRepository.observeBudgets().first(),
                    bills = billsRepository.observeBills().first(),
                )
                val rule = existing ?: return@update base.copy(
                    triggers = listOf(TriggerDraft(TriggerField.DESCRIPTION, TriggerOperator.CONTAINS, "")),
                    actions = listOf(ActionDraft(ActionType.SET_CATEGORY, "")),
                )
                base.copy(
                    name = rule.name,
                    matchAll = rule.matchAll,
                    active = rule.active,
                    stopProcessing = rule.stopProcessing,
                    triggers = rule.triggers.map { t ->
                        // Amounts are stored in minor units but edited as decimals.
                        val value = if (t.field == TriggerField.AMOUNT) t.value.toLongOrNull()?.let { minorUnitsToInput(it, currency) } ?: "" else t.value
                        TriggerDraft(t.field, t.operator, value)
                    },
                    actions = rule.actions.map { ActionDraft(it.type, it.value) },
                )
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameError = false) }
    fun onMatchAllChange(value: Boolean) = update { it.copy(matchAll = value) }
    fun onActiveChange(value: Boolean) = _uiState.update { it.copy(active = value) }
    fun onStopProcessingChange(value: Boolean) = _uiState.update { it.copy(stopProcessing = value) }
    fun onApplyToExistingChange(value: Boolean) = _uiState.update { it.copy(applyToExisting = value) }

    fun addTrigger() = update { it.copy(triggers = it.triggers + TriggerDraft(TriggerField.DESCRIPTION, TriggerOperator.CONTAINS, "")) }
    fun removeTrigger(index: Int) = update { it.copy(triggers = it.triggers.filterIndexed { i, _ -> i != index }) }

    fun onTriggerFieldChange(index: Int, field: TriggerField) = updateTrigger(index) {
        // Keep the operator when the new field supports it, and give TYPE a valid value.
        TriggerDraft(
            field = field,
            operator = it.operator.takeIf { op -> op in field.operators } ?: field.operators.first(),
            value = if (field == TriggerField.TYPE) TransactionType.WITHDRAWAL.name else if (it.field == TriggerField.TYPE) "" else it.value,
        )
    }

    fun onTriggerOperatorChange(index: Int, operator: TriggerOperator) = updateTrigger(index) { it.copy(operator = operator) }
    fun onTriggerValueChange(index: Int, value: String) = updateTrigger(index) { it.copy(value = value) }

    fun addAction() = update { it.copy(actions = it.actions + ActionDraft(ActionType.ADD_TAG, "")) }
    fun removeAction(index: Int) = update { it.copy(actions = it.actions.filterIndexed { i, _ -> i != index }) }
    fun onActionTypeChange(index: Int, type: ActionType) = updateAction(index) { ActionDraft(type, "") }
    fun onActionValueChange(index: Int, value: String) = updateAction(index) { it.copy(value = value) }

    /** Counts the existing transactions the rule as written would change. */
    fun preview() {
        val rule = buildRule() ?: return
        viewModelScope.launch {
            val count = rulesRepository.countAffected(rule)
            _uiState.update { it.copy(previewCount = count) }
        }
    }

    fun save() {
        val rule = buildRule() ?: return
        viewModelScope.launch {
            val id = rulesRepository.saveRule(rule)
            if (_uiState.value.applyToExisting) rulesRepository.applyToExisting(rule.copy(id = id))
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    fun delete() {
        if (ruleId == 0L) return
        viewModelScope.launch {
            rulesRepository.deleteRule(ruleId)
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    /** Validates the form, showing errors, and returns the rule it describes or null. */
    private fun buildRule(): Rule? {
        val state = _uiState.value
        val triggers = state.triggers.mapNotNull { t ->
            val value = if (t.field == TriggerField.AMOUNT) {
                parseAmountToMinorUnits(t.value, state.currencyCode)?.toString()
            } else {
                t.value.trim().takeIf { it.isNotEmpty() }
            }
            value?.let { RuleTrigger(t.field, t.operator, it) }
        }
        val actions = state.actions.mapNotNull { a -> a.value.trim().takeIf { it.isNotEmpty() }?.let { RuleAction(a.type, it) } }
        val errors = state.copy(
            nameError = state.name.isBlank(),
            triggersError = triggers.isEmpty() || triggers.size < state.triggers.size,
            actionsError = actions.isEmpty() || actions.size < state.actions.size,
            previewCount = null,
        )
        if (errors.nameError || errors.triggersError || errors.actionsError) {
            _uiState.value = errors
            return null
        }
        return Rule(
            id = ruleId,
            name = state.name.trim(),
            triggers = triggers,
            actions = actions,
            matchAll = state.matchAll,
            active = state.active,
            stopProcessing = state.stopProcessing,
            position = existing?.position ?: 0,
        )
    }

    private fun update(block: (RuleEditUiState) -> RuleEditUiState) = _uiState.update {
        block(it).copy(triggersError = false, actionsError = false, previewCount = null)
    }

    private fun updateTrigger(index: Int, block: (TriggerDraft) -> TriggerDraft) =
        update { state -> state.copy(triggers = state.triggers.mapIndexed { i, t -> if (i == index) block(t) else t }) }

    private fun updateAction(index: Int, block: (ActionDraft) -> ActionDraft) =
        update { state -> state.copy(actions = state.actions.mapIndexed { i, a -> if (i == index) block(a) else a }) }
}

/** A condition being edited; amounts are typed as decimals in the default currency. */
data class TriggerDraft(val field: TriggerField, val operator: TriggerOperator, val value: String)

/** An action being edited; budgets and bills hold their id as text. */
data class ActionDraft(val type: ActionType, val value: String)

data class RuleEditUiState(
    val isNew: Boolean,
    val isLoading: Boolean = false,
    val name: String = "",
    val matchAll: Boolean = true,
    val active: Boolean = true,
    val stopProcessing: Boolean = false,
    val applyToExisting: Boolean = false,
    val triggers: List<TriggerDraft> = emptyList(),
    val actions: List<ActionDraft> = emptyList(),
    val currencyCode: String = "EUR",
    val accountNames: List<String> = emptyList(),
    val counterpartyNames: List<String> = emptyList(),
    val categoryNames: List<String> = emptyList(),
    val tagNames: List<String> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val nameError: Boolean = false,
    val triggersError: Boolean = false,
    val actionsError: Boolean = false,
    /** Transactions the rule would change, after "Prova". */
    val previewCount: Int? = null,
    /** True once the rule was saved or deleted and the screen should close. */
    val isFinished: Boolean = false,
)
