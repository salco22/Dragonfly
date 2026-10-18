package com.salco.dragonfly.feature.transactions.edit

import com.salco.dragonfly.core.designsystem.component.DfButton
import com.salco.dragonfly.core.designsystem.component.DfButtonStyle
import com.salco.dragonfly.core.designsystem.component.DfChipFlow
import com.salco.dragonfly.core.designsystem.component.DfConfirmDialog
import com.salco.dragonfly.core.designsystem.component.DfEmptyState
import com.salco.dragonfly.core.designsystem.component.DfFormColumn
import com.salco.dragonfly.core.designsystem.component.DfIconButton
import com.salco.dragonfly.core.designsystem.component.DfKeyboard
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfSegmentedControl
import com.salco.dragonfly.core.designsystem.component.DfTextField
import com.salco.dragonfly.core.designsystem.component.DropdownField
import com.salco.dragonfly.core.designsystem.component.AutocompleteField
import com.salco.dragonfly.core.designsystem.component.DatePickerField
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
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Delete
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
import com.salco.dragonfly.core.designsystem.theme.DragonflyTheme
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.Bill
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
            onBillChange = viewModel::onBillChange,
            onTagInputChange = viewModel::onTagInputChange,
            onAddTag = viewModel::addTag,
            onRemoveTag = viewModel::removeTag,
            split = SplitActions(
                onAdd = viewModel::addSplit,
                onRemove = viewModel::removeSplit,
                onTitleChange = viewModel::onGroupTitleChange,
                onDescriptionChange = viewModel::onSplitDescriptionChange,
                onAmountChange = viewModel::onSplitAmountChange,
                onCounterpartyChange = viewModel::onSplitCounterpartyChange,
                onCategoryChange = viewModel::onSplitCategoryChange,
                onBudgetChange = viewModel::onSplitBudgetChange,
                onTagInputChange = viewModel::onSplitTagInputChange,
                onAddTag = viewModel::addSplitTag,
                onRemoveTag = viewModel::removeSplitTag,
            ),
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
    val onBillChange: (Long?) -> Unit = {},
    val onTagInputChange: (String) -> Unit = {},
    val onAddTag: (String) -> Unit = {},
    val onRemoveTag: (String) -> Unit = {},
    /** Null where transactions cannot be split, as in recurring templates. */
    val split: SplitActions? = null,
)

@Composable
internal fun TransactionEditContent(uiState: TransactionEditUiState, actions: TransactionEditActions) {
    var confirmDelete by remember { mutableStateOf(false) }

    DfScaffold(
        title = stringResource(if (uiState.isNew) R.string.feature_transactions_new else R.string.feature_transactions_edit),
        onBack = actions.onBack,
        actions = {
            if (!uiState.isNew) {
                DfIconButton(Icons.Outlined.Delete, stringResource(R.string.feature_transactions_delete), { confirmDelete = true })
            }
        },
    ) { padding ->
        if (uiState.isLoading) return@DfScaffold
        if (uiState.ownAccounts.isEmpty()) {
            DfEmptyState(
                icon = Icons.Outlined.AccountBalance,
                title = stringResource(R.string.feature_transactions_no_accounts_title),
                message = stringResource(R.string.feature_transactions_no_accounts_message),
                modifier = Modifier.padding(padding),
            )
            return@DfScaffold
        }
        DfFormColumn(Modifier.padding(padding)) {
            val split = actions.split
            if (split != null && uiState.isSplit && uiState.canSplit) {
                SplitFormFields(uiState, actions, split)
            } else {
                TransactionFormFields(uiState, actions)
                if (split != null && uiState.canSplit) {
                    DfButton(
                        stringResource(R.string.feature_transactions_split),
                        split.onAdd,
                        style = DfButtonStyle.Secondary,
                        icon = Icons.AutoMirrored.Outlined.CallSplit,
                    )
                }
            }
            DfButton(stringResource(R.string.feature_transactions_save), actions.onSave, fillWidth = true)
        }
    }

    if (confirmDelete) {
        DfConfirmDialog(
            title = stringResource(R.string.feature_transactions_delete_title),
            message = stringResource(
                if (uiState.groupId != 0L) R.string.feature_transactions_delete_split_message else R.string.feature_transactions_delete_message,
            ),
            confirmText = stringResource(R.string.feature_transactions_delete),
            dismissText = stringResource(R.string.feature_transactions_cancel),
            onConfirm = {
                confirmDelete = false
                actions.onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Every field of a transaction; shared by single and recurring transactions. */
@Composable
internal fun TransactionFormFields(
    uiState: TransactionEditUiState,
    actions: TransactionEditActions,
    dateLabel: String = stringResource(R.string.feature_transactions_date),
) {
    DfSegmentedControl(
        options = TransactionType.entries,
        selected = uiState.type,
        optionLabel = { stringResource(it.labelRes()) },
        onSelect = actions.onTypeChange,
    )

    DfTextField(
        value = uiState.amount,
        onValueChange = actions.onAmountChange,
        label = stringResource(R.string.feature_transactions_amount),
        suffix = uiState.currencyCode.orEmpty(),
        keyboard = DfKeyboard.Decimal,
        large = true,
        error = stringResource(R.string.feature_transactions_error_amount).takeIf { uiState.amountError },
    )

    DfTextField(
        value = uiState.description,
        onValueChange = actions.onDescriptionChange,
        label = stringResource(R.string.feature_transactions_description),
        error = stringResource(R.string.feature_transactions_error_description).takeIf { uiState.descriptionError },
    )

    DatePickerField(label = dateLabel, date = uiState.date, onDateChange = actions.onDateChange)

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
            supportingText = stringResource(R.string.feature_transactions_error_counterparty).takeIf { uiState.counterpartyError },
        )
    }
    val accountField = @Composable { AccountDropdown(accountLabel, uiState.ownAccounts, uiState.account, actions.onAccountChange) }

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
                DfTextField(
                    value = uiState.foreignAmount,
                    onValueChange = actions.onForeignAmountChange,
                    label = stringResource(R.string.feature_transactions_foreign_amount, toAccount.currencyCode),
                    suffix = toAccount.currencyCode,
                    keyboard = DfKeyboard.Decimal,
                    error = stringResource(R.string.feature_transactions_error_amount).takeIf { uiState.foreignAmountError },
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
        BudgetDropdown(uiState.budgets, uiState.budgetId, actions.onBudgetChange)
    }

    if (uiState.type == TransactionType.WITHDRAWAL && uiState.bills.isNotEmpty()) {
        val noBill = stringResource(R.string.feature_transactions_no_bill)
        DropdownField(
            label = stringResource(R.string.feature_transactions_bill),
            options = listOf<Bill?>(null) + uiState.bills,
            selected = uiState.bills.firstOrNull { it.id == uiState.billId },
            optionLabel = { it?.name ?: noBill },
            onSelected = { actions.onBillChange(it?.id) },
            placeholder = noBill,
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

    DfTextField(
        value = uiState.notes,
        onValueChange = actions.onNotesChange,
        label = stringResource(R.string.feature_transactions_notes),
        singleLine = false,
        minLines = 2,
    )
}

@Composable
internal fun BudgetDropdown(budgets: List<Budget>, selectedId: Long?, onSelected: (Long?) -> Unit) {
    val noBudget = stringResource(R.string.feature_transactions_no_budget)
    DropdownField(
        label = stringResource(R.string.feature_transactions_budget),
        options = listOf<Budget?>(null) + budgets,
        selected = budgets.firstOrNull { it.id == selectedId },
        optionLabel = { it?.name ?: noBudget },
        onSelected = { onSelected(it?.id) },
        placeholder = noBudget,
    )
}

@Composable
internal fun TagsField(
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
                DfIconButton(Icons.Filled.Add, stringResource(R.string.feature_transactions_add_tag), { onAdd(input) }, enabled = input.isNotBlank())
            },
        )
        if (tags.isNotEmpty()) {
            val removeLabel = stringResource(R.string.feature_transactions_remove_tag, "%s")
            DfChipFlow(tags, onRemove = onRemove, removeDescription = { removeLabel.replace("%s", it) })
        }
    }
}

@Composable
internal fun AccountDropdown(
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
