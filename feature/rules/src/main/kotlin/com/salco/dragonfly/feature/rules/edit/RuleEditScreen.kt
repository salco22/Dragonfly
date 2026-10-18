package com.salco.dragonfly.feature.rules.edit

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.salco.dragonfly.core.designsystem.component.AutocompleteField
import com.salco.dragonfly.core.designsystem.component.DropdownField
import com.salco.dragonfly.core.model.ActionType
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.TriggerField
import com.salco.dragonfly.core.model.operators
import com.salco.dragonfly.feature.rules.R
import com.salco.dragonfly.feature.rules.labelRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RuleEditScreen(onBack: () -> Unit, viewModel: RuleEditViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(state.isFinished) { if (state.isFinished) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isNew) R.string.feature_rules_new else R.string.feature_rules_edit)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_rules_back)) }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.feature_rules_delete)) }
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.feature_rules_name)) },
                isError = state.nameError,
                supportingText = if (state.nameError) {
                    { Text(stringResource(R.string.feature_rules_error_name)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.feature_rules_conditions), style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(true to R.string.feature_rules_match_all, false to R.string.feature_rules_match_any).forEachIndexed { index, (all, label) ->
                    SegmentedButton(
                        selected = state.matchAll == all,
                        onClick = { viewModel.onMatchAllChange(all) },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                    ) { Text(stringResource(label)) }
                }
            }
            state.triggers.forEachIndexed { index, trigger ->
                TriggerCard(index, trigger, state, viewModel)
            }
            if (state.triggersError) {
                Text(stringResource(R.string.feature_rules_error_conditions), color = MaterialTheme.colorScheme.error)
            }
            OutlinedButton(onClick = viewModel::addTrigger) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.feature_rules_add_condition))
            }

            Text(stringResource(R.string.feature_rules_actions), style = MaterialTheme.typography.titleSmall)
            state.actions.forEachIndexed { index, action ->
                ActionCard(index, action, state, viewModel)
            }
            if (state.actionsError) {
                Text(stringResource(R.string.feature_rules_error_actions), color = MaterialTheme.colorScheme.error)
            }
            OutlinedButton(onClick = viewModel::addAction) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.feature_rules_add_action))
            }

            SwitchRow(stringResource(R.string.feature_rules_stop), state.stopProcessing, viewModel::onStopProcessingChange)
            SwitchRow(stringResource(R.string.feature_rules_active), state.active, viewModel::onActiveChange)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.applyToExisting, onCheckedChange = viewModel::onApplyToExistingChange)
                Text(stringResource(R.string.feature_rules_apply_existing))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = viewModel::preview) { Text(stringResource(R.string.feature_rules_preview)) }
                state.previewCount?.let {
                    Text(pluralStringResource(R.plurals.feature_rules_preview_result, it, it), style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_rules_save))
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.feature_rules_delete_title)) },
            text = { Text(stringResource(R.string.feature_rules_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text(stringResource(R.string.feature_rules_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.feature_rules_cancel)) } },
        )
    }
}

@Composable
private fun TriggerCard(index: Int, trigger: TriggerDraft, state: RuleEditUiState, viewModel: RuleEditViewModel) {
    RowCard(onRemove = { viewModel.removeTrigger(index) }) {
        DropdownField(
            label = stringResource(R.string.feature_rules_field),
            options = TriggerField.entries,
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
            TriggerField.AMOUNT -> OutlinedTextField(
                value = trigger.value,
                onValueChange = onValue,
                label = { Text(label) },
                suffix = { Text(state.currencyCode) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            else -> OutlinedTextField(
                value = trigger.value,
                onValueChange = onValue,
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ActionCard(index: Int, action: ActionDraft, state: RuleEditUiState, viewModel: RuleEditViewModel) {
    RowCard(onRemove = { viewModel.removeAction(index) }) {
        DropdownField(
            label = stringResource(R.string.feature_rules_action),
            options = ActionType.entries,
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
            ActionType.SET_DESCRIPTION, ActionType.APPEND_NOTES -> OutlinedTextField(
                value = action.value,
                onValueChange = onValue,
                label = { Text(label) },
                singleLine = action.type == ActionType.SET_DESCRIPTION,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RowCard(onRemove: () -> Unit, content: @Composable () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, stringResource(R.string.feature_rules_remove)) }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
