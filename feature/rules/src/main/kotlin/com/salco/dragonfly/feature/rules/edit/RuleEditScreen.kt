package com.salco.dragonfly.feature.rules.edit

import com.salco.dragonfly.core.designsystem.component.DfButton
import com.salco.dragonfly.core.designsystem.component.DfButtonStyle
import com.salco.dragonfly.core.designsystem.component.DfCard
import com.salco.dragonfly.core.designsystem.component.DfCheckboxRow
import com.salco.dragonfly.core.designsystem.component.DfConfirmDialog
import com.salco.dragonfly.core.designsystem.component.DfFormColumn
import com.salco.dragonfly.core.designsystem.component.DfIconButton
import com.salco.dragonfly.core.designsystem.component.DfKeyboard
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfSegmentedControl
import com.salco.dragonfly.core.designsystem.component.DfSwitchRow
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTextField
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.component.DropdownField
import com.salco.dragonfly.core.designsystem.component.AutocompleteField
import com.salco.dragonfly.core.designsystem.theme.DfTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.model.ActionType
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.TriggerField
import com.salco.dragonfly.core.model.operators
import com.salco.dragonfly.feature.rules.R
import com.salco.dragonfly.feature.rules.labelRes

@Composable
internal fun RuleEditScreen(onBack: () -> Unit, viewModel: RuleEditViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(state.isFinished) { if (state.isFinished) onBack() }

    DfScaffold(
        title = stringResource(if (state.isNew) R.string.feature_rules_new else R.string.feature_rules_edit),
        onBack = onBack,
        actions = {
            if (!state.isNew) DfIconButton(Icons.Outlined.Delete, stringResource(R.string.feature_rules_delete), { confirmDelete = true })
        },
    ) { padding ->
        if (state.isLoading) return@DfScaffold
        DfFormColumn(Modifier.padding(padding)) {
            DfTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = stringResource(R.string.feature_rules_name),
                keyboard = DfKeyboard.Sentences,
                error = stringResource(R.string.feature_rules_error_name).takeIf { state.nameError },
            )

            DfText(stringResource(R.string.feature_rules_conditions).uppercase(), style = DfTheme.typography.overline, tone = DfTone.Tertiary)
            DfSegmentedControl(
                options = listOf(true, false),
                selected = state.matchAll,
                optionLabel = { stringResource(if (it) R.string.feature_rules_match_all else R.string.feature_rules_match_any) },
                onSelect = viewModel::onMatchAllChange,
                modifier = Modifier.fillMaxWidth(),
            )
            state.triggers.forEachIndexed { index, trigger ->
                TriggerCard(index, trigger, state, viewModel)
            }
            if (state.triggersError) {
                DfText(stringResource(R.string.feature_rules_error_conditions), tone = DfTone.Negative, style = DfTheme.typography.bodySmall)
            }
            DfButton(stringResource(R.string.feature_rules_add_condition), viewModel::addTrigger, style = DfButtonStyle.Secondary, icon = Icons.Filled.Add)

            DfText(stringResource(R.string.feature_rules_actions).uppercase(), style = DfTheme.typography.overline, tone = DfTone.Tertiary)
            state.actions.forEachIndexed { index, action ->
                ActionCard(index, action, state, viewModel)
            }
            if (state.actionsError) {
                DfText(stringResource(R.string.feature_rules_error_actions), tone = DfTone.Negative, style = DfTheme.typography.bodySmall)
            }
            DfButton(stringResource(R.string.feature_rules_add_action), viewModel::addAction, style = DfButtonStyle.Secondary, icon = Icons.Filled.Add)

            DfSwitchRow(stringResource(R.string.feature_rules_stop), state.stopProcessing, viewModel::onStopProcessingChange)
            DfSwitchRow(stringResource(R.string.feature_rules_active), state.active, viewModel::onActiveChange)
            DfCheckboxRow(stringResource(R.string.feature_rules_apply_existing), state.applyToExisting, viewModel::onApplyToExistingChange)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                DfButton(stringResource(R.string.feature_rules_preview), viewModel::preview, style = DfButtonStyle.Secondary)
                state.previewCount?.let {
                    DfText(pluralStringResource(R.plurals.feature_rules_preview_result, it, it), style = DfTheme.typography.bodySmall, tone = DfTone.Secondary)
                }
            }
            DfButton(stringResource(R.string.feature_rules_save), viewModel::save, fillWidth = true)
        }
    }

    if (confirmDelete) {
        DfConfirmDialog(
            title = stringResource(R.string.feature_rules_delete_title),
            message = stringResource(R.string.feature_rules_delete_message),
            confirmText = stringResource(R.string.feature_rules_delete),
            dismissText = stringResource(R.string.feature_rules_cancel),
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun TriggerCard(index: Int, trigger: TriggerDraft, state: RuleEditUiState, viewModel: RuleEditViewModel) {
    RowCard(onRemove = { viewModel.removeTrigger(index) }) {
        DropdownField(
            label = stringResource(R.string.feature_rules_field),
            // Payees are not used: an existing condition on them stays, but new ones cannot pick it.
            options = TriggerField.entries.filter { it != TriggerField.COUNTERPARTY || trigger.field == it },
            selected = trigger.field,
            optionLabel = { stringResource(it.labelRes()) },
            onSelected = { viewModel.onTriggerFieldChange(index, it) },
        )
        if (trigger.field.operators.size > 1) {
            DropdownField(
                label = stringResource(R.string.feature_rules_operator),
                options = trigger.field.operators,
                selected = trigger.operator,
                optionLabel = { stringResource(it.labelRes()) },
                onSelected = { viewModel.onTriggerOperatorChange(index, it) },
            )
        }
        val label = stringResource(R.string.feature_rules_value)
        val onValue = { value: String -> viewModel.onTriggerValueChange(index, value) }
        when (trigger.field) {
            TriggerField.TYPE -> DropdownField(
                label = label,
                options = TransactionType.entries,
                selected = TransactionType.entries.firstOrNull { it.name == trigger.value },
                optionLabel = { stringResource(it.labelRes()) },
                onSelected = { onValue(it.name) },
            )
            TriggerField.ACCOUNT -> AutocompleteField(label, trigger.value, onValue, state.accountNames)
            TriggerField.COUNTERPARTY -> AutocompleteField(label, trigger.value, onValue, state.counterpartyNames)
            TriggerField.AMOUNT -> DfTextField(
                value = trigger.value,
                onValueChange = onValue,
                label = label,
                suffix = state.currencyCode,
                keyboard = DfKeyboard.Decimal,
            )
            else -> DfTextField(value = trigger.value, onValueChange = onValue, label = label)
        }
    }
}

@Composable
private fun ActionCard(index: Int, action: ActionDraft, state: RuleEditUiState, viewModel: RuleEditViewModel) {
    RowCard(onRemove = { viewModel.removeAction(index) }) {
        DropdownField(
            label = stringResource(R.string.feature_rules_action),
            options = ActionType.entries.filter { it != ActionType.SET_COUNTERPARTY || action.type == it },
            selected = action.type,
            optionLabel = { stringResource(it.labelRes()) },
            onSelected = { viewModel.onActionTypeChange(index, it) },
        )
        val label = stringResource(R.string.feature_rules_value)
        val onValue = { value: String -> viewModel.onActionValueChange(index, value) }
        when (action.type) {
            ActionType.SET_CATEGORY -> AutocompleteField(label, action.value, onValue, state.categoryNames)
            ActionType.ADD_TAG -> AutocompleteField(label, action.value, onValue, state.tagNames)
            ActionType.SET_BUDGET -> DropdownField(
                label = label,
                options = state.budgets,
                selected = state.budgets.firstOrNull { it.id.toString() == action.value },
                optionLabel = { it.name },
                onSelected = { onValue(it.id.toString()) },
            )
            ActionType.LINK_BILL -> DropdownField(
                label = label,
                options = state.bills,
                selected = state.bills.firstOrNull { it.id.toString() == action.value },
                optionLabel = { it.name },
                onSelected = { onValue(it.id.toString()) },
            )
            ActionType.SET_COUNTERPARTY -> AutocompleteField(label, action.value, onValue, state.counterpartyNames)
            ActionType.SET_DESCRIPTION, ActionType.APPEND_NOTES -> DfTextField(
                value = action.value,
                onValueChange = onValue,
                label = label,
                singleLine = action.type == ActionType.SET_DESCRIPTION,
            )
        }
    }
}

@Composable
private fun RowCard(onRemove: () -> Unit, content: @Composable () -> Unit) {
    DfCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
            DfIconButton(Icons.Filled.Close, stringResource(R.string.feature_rules_remove), onRemove, tone = DfTone.Secondary)
        }
    }
}
