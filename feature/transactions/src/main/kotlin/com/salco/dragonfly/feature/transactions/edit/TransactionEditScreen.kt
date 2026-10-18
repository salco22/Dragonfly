package com.salco.dragonfly.feature.transactions.edit

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.component.AutocompleteField
import com.salco.dragonfly.core.designsystem.component.DropdownField
import com.salco.dragonfly.core.designsystem.component.EmptyState
import com.salco.dragonfly.core.designsystem.theme.DragonflyTheme
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.Budget
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.feature.transactions.R
import com.salco.dragonfly.feature.transactions.labelRes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun TransactionEditScreen(
    onBack: () -> Unit,
    viewModel: TransactionEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isFinished) { if (uiState.isFinished) onBack() }

    TransactionEditContent(
        uiState = uiState,
        actions = TransactionEditActions(
            onBack = onBack,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
            onTypeChange = viewModel::onTypeChange,
            onAmountChange = viewModel::onAmountChange,
            onForeignAmountChange = viewModel::onForeignAmountChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onDateChange = viewModel::onDateChange,
            onAccountChange = viewModel::onAccountChange,
            onToAccountChange = viewModel::onToAccountChange,
            onCounterpartyChange = viewModel::onCounterpartyChange,
            onNotesChange = viewModel::onNotesChange,
            onCategoryChange = viewModel::onCategoryChange,
            onBudgetChange = viewModel::onBudgetChange,
            onTagInputChange = viewModel::onTagInputChange,
            onAddTag = viewModel::addTag,
            onRemoveTag = viewModel::removeTag,
        ),
    )
}

internal data class TransactionEditActions(
    val onBack: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onTypeChange: (TransactionType) -> Unit = {},
    val onAmountChange: (String) -> Unit = {},
    val onForeignAmountChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onDateChange: (LocalDate) -> Unit = {},
    val onAccountChange: (Long) -> Unit = {},
    val onToAccountChange: (Long) -> Unit = {},
    val onCounterpartyChange: (String) -> Unit = {},
    val onNotesChange: (String) -> Unit = {},
    val onCategoryChange: (String) -> Unit = {},
    val onBudgetChange: (Long?) -> Unit = {},
    val onTagInputChange: (String) -> Unit = {},
    val onAddTag: (String) -> Unit = {},
    val onRemoveTag: (String) -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionEditContent(uiState: TransactionEditUiState, actions: TransactionEditActions) {
    var confirmDelete by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (uiState.isNew) R.string.feature_transactions_new else R.string.feature_transactions_edit))
                },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_transactions_back))
                    }
                },
                actions = {
                    if (!uiState.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, stringResource(R.string.feature_transactions_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) return@Scaffold
        if (uiState.ownAccounts.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.AccountBalance,
                title = stringResource(R.string.feature_transactions_no_accounts_title),
                message = stringResource(R.string.feature_transactions_no_accounts_message),
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val types = TransactionType.entries
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                types.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = uiState.type == type,
                        onClick = { actions.onTypeChange(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, types.size),
                    ) { Text(stringResource(type.labelRes())) }
                }
            }

            AmountField(
                label = stringResource(R.string.feature_transactions_amount),
                value = uiState.amount,
                currencyCode = uiState.currencyCode.orEmpty(),
                onValueChange = actions.onAmountChange,
                error = stringResource(R.string.feature_transactions_error_amount).takeIf { uiState.amountError },
                large = true,
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = actions.onDescriptionChange,
                label = { Text(stringResource(R.string.feature_transactions_description)) },
                isError = uiState.descriptionError,
                supportingText = if (uiState.descriptionError) {
                    { Text(stringResource(R.string.feature_transactions_error_description)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            DateField(date = uiState.date, onClick = { pickDate = true })

            val accountLabel = stringResource(
                if (uiState.type == TransactionType.DEPOSIT) R.string.feature_transactions_to_account
                else R.string.feature_transactions_from_account,
            )
            val counterpartyField = @Composable {
                AutocompleteField(
                    label = stringResource(
                        if (uiState.type == TransactionType.DEPOSIT) R.string.feature_transactions_payer
                        else R.string.feature_transactions_payee,
                    ),
                    value = uiState.counterparty,
                    onValueChange = actions.onCounterpartyChange,
                    suggestions = uiState.counterpartySuggestions,
                    isError = uiState.counterpartyError,
                    supportingText = stringResource(R.string.feature_transactions_error_counterparty)
                        .takeIf { uiState.counterpartyError },
                )
            }
            val accountField = @Composable {
                AccountDropdown(accountLabel, uiState.ownAccounts, uiState.account, actions.onAccountChange)
            }

            when (uiState.type) {
                TransactionType.WITHDRAWAL -> {
                    accountField()
                    counterpartyField()
                }
                TransactionType.DEPOSIT -> {
                    counterpartyField()
                    accountField()
                }
                TransactionType.TRANSFER -> {
                    accountField()
                    AccountDropdown(
                        label = stringResource(R.string.feature_transactions_to_account),
                        accounts = uiState.ownAccounts,
                        selected = uiState.toAccount,
                        onSelected = actions.onToAccountChange,
                        error = when {
                            uiState.sameAccountError -> stringResource(R.string.feature_transactions_error_same_account)
                            uiState.accountError -> stringResource(R.string.feature_transactions_error_account)
                            else -> null
                        },
                    )
                    val toAccount = uiState.toAccount
                    if (uiState.needsForeignAmount && toAccount != null) {
                        AmountField(
                            label = stringResource(R.string.feature_transactions_foreign_amount, toAccount.currencyCode),
                            value = uiState.foreignAmount,
                            currencyCode = toAccount.currencyCode,
                            onValueChange = actions.onForeignAmountChange,
                            error = stringResource(R.string.feature_transactions_error_amount)
                                .takeIf { uiState.foreignAmountError },
                        )
                    }
                }
            }

            AutocompleteField(
                label = stringResource(R.string.feature_transactions_category),
                value = uiState.category,
                onValueChange = actions.onCategoryChange,
                suggestions = uiState.categoryNames,
            )

            if (uiState.type == TransactionType.WITHDRAWAL && uiState.budgets.isNotEmpty()) {
                val noBudget = stringResource(R.string.feature_transactions_no_budget)
                DropdownField(
                    label = stringResource(R.string.feature_transactions_budget),
                    options = listOf<Budget?>(null) + uiState.budgets,
                    selected = uiState.budgets.firstOrNull { it.id == uiState.budgetId },
                    optionLabel = { it?.name ?: noBudget },
                    onSelected = { actions.onBudgetChange(it?.id) },
                )
            }

            TagsField(
                tags = uiState.tags,
                input = uiState.tagInput,
                suggestions = uiState.tagSuggestions.filterNot { s -> uiState.tags.any { it.equals(s, true) } },
                onInputChange = actions.onTagInputChange,
                onAdd = actions.onAddTag,
                onRemove = actions.onRemoveTag,
            )

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = actions.onNotesChange,
                label = { Text(stringResource(R.string.feature_transactions_notes)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(onClick = actions.onSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.feature_transactions_save))
            }
        }
    }

    if (pickDate) {
        // DatePicker works in UTC milliseconds.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = uiState.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        actions.onDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    pickDate = false
                }) { Text(stringResource(R.string.feature_transactions_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.feature_transactions_cancel)) }
            },
        ) { DatePicker(state = state) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.feature_transactions_delete_title)) },
            text = { Text(stringResource(R.string.feature_transactions_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.onDelete()
                }) { Text(stringResource(R.string.feature_transactions_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.feature_transactions_cancel))
                }
            },
        )
    }
}

@Composable
private fun AmountField(
    label: String,
    value: String,
    currencyCode: String,
    onValueChange: (String) -> Unit,
    error: String?,
    large: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(currencyCode) },
        textStyle = if (large) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyLarge,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DateField(date: LocalDate, onClick: () -> Unit) {
    // A read-only text field swallows clicks, so listen to its press interactions instead.
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { if (it is PressInteraction.Release) onClick() }
    }
    OutlinedTextField(
        value = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.feature_transactions_date)) },
        trailingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsField(
    tags: List<String>,
    input: String,
    suggestions: List<String>,
    onInputChange: (String) -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AutocompleteField(
            label = stringResource(R.string.feature_transactions_tags),
            value = input,
            onValueChange = onInputChange,
            suggestions = suggestions,
            onSuggestionClick = onAdd,
            onDone = { onAdd(input) },
            trailingIcon = {
                IconButton(onClick = { onAdd(input) }, enabled = input.isNotBlank()) {
                    Icon(Icons.Filled.Add, stringResource(R.string.feature_transactions_add_tag))
                }
            },
        )
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    InputChip(
                        selected = false,
                        onClick = { onRemove(tag) },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                stringResource(R.string.feature_transactions_remove_tag, tag),
                                modifier = Modifier.size(InputChipDefaults.IconSize),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountDropdown(
    label: String,
    accounts: List<Account>,
    selected: Account?,
    onSelected: (Long) -> Unit,
    error: String? = null,
) {
    DropdownField(
        label = label,
        options = accounts,
        selected = selected,
        optionLabel = { "${it.name} · ${formatMoney(it.balance, it.currencyCode)}" },
        onSelected = { onSelected(it.id) },
        isError = error != null,
        supportingText = error,
    )
}

@Preview
@Composable
private fun TransactionEditContentPreview() {
    val checking = Account(id = 1, name = "Conto corrente", type = AccountType.ASSET, currencyCode = "EUR", openingBalance = 152_030)
    DragonflyTheme {
        TransactionEditContent(
            uiState = TransactionEditUiState(
                isNew = true,
                date = LocalDate.of(2026, 10, 17),
                amount = "42,50",
                description = "Spesa",
                accountId = 1,
                counterparty = "Supermercato",
                ownAccounts = listOf(checking),
            ),
            actions = TransactionEditActions(),
        )
    }
}
