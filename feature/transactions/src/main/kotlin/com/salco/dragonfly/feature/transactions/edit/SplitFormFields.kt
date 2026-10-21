package com.salco.dragonfly.feature.transactions.edit

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.salco.dragonfly.core.designsystem.component.AutocompleteField
import com.salco.dragonfly.core.designsystem.component.DatePickerField
import com.salco.dragonfly.core.designsystem.component.DfButton
import com.salco.dragonfly.core.designsystem.component.DfButtonStyle
import com.salco.dragonfly.core.designsystem.component.DfCard
import com.salco.dragonfly.core.designsystem.component.DfIconButton
import com.salco.dragonfly.core.designsystem.component.DfKeyboard
import com.salco.dragonfly.core.designsystem.component.DfSegmentedControl
import com.salco.dragonfly.core.designsystem.component.DfStat
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTextField
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.theme.DfTheme
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.feature.transactions.R
import com.salco.dragonfly.feature.transactions.labelRes

/** Callbacks for the parts of a split transaction; indexes start at 0 for part 2. */
internal data class SplitActions(
    val onAdd: () -> Unit = {},
    val onRemove: (Int) -> Unit = {},
    val onTitleChange: (String) -> Unit = {},
    val onDescriptionChange: (Int, String) -> Unit = { _, _ -> },
    val onAmountChange: (Int, String) -> Unit = { _, _ -> },
    val onCategoryChange: (Int, String) -> Unit = { _, _ -> },
    val onBudgetChange: (Int, Long?) -> Unit = { _, _ -> },
    val onTagInputChange: (Int, String) -> Unit = { _, _ -> },
    val onAddTag: (Int, String) -> Unit = { _, _ -> },
    val onRemoveTag: (Int, String) -> Unit = { _, _ -> },
)

/**
 * The form of a split transaction: what every part shares (type, title, date, own account),
 * then one card per part. Part 1 keeps notes and bill, like the transaction it started from.
 */
@Composable
internal fun SplitFormFields(uiState: TransactionEditUiState, actions: TransactionEditActions, split: SplitActions) {
    val types = TransactionType.entries.filter { it != TransactionType.TRANSFER }
    DfSegmentedControl(
        options = types,
        selected = uiState.type,
        optionLabel = { stringResource(it.labelRes()) },
        onSelect = actions.onTypeChange,
    )
    DfTextField(
        value = uiState.groupTitle,
        onValueChange = split.onTitleChange,
        label = stringResource(R.string.feature_transactions_split_title),
        helper = stringResource(R.string.feature_transactions_split_title_hint),
        error = stringResource(R.string.feature_transactions_error_split_title).takeIf { uiState.groupTitleError },
    )
    DatePickerField(label = stringResource(R.string.feature_transactions_date), date = uiState.date, onDateChange = actions.onDateChange)
    AccountDropdown(
        label = stringResource(
            if (uiState.type == TransactionType.DEPOSIT) R.string.feature_transactions_to_account
            else R.string.feature_transactions_from_account,
        ),
        accounts = uiState.ownAccounts,
        selected = uiState.account,
        onSelected = actions.onAccountChange,
    )

    val currency = uiState.currencyCode.orEmpty()
    val amountError = stringResource(R.string.feature_transactions_error_amount)
    val descriptionError = stringResource(R.string.feature_transactions_error_description)

    PartCard(number = 1, onRemove = null) {
        DfTextField(
            value = uiState.amount,
            onValueChange = actions.onAmountChange,
            label = stringResource(R.string.feature_transactions_amount),
            suffix = currency,
            keyboard = DfKeyboard.Decimal,
            error = amountError.takeIf { uiState.amountError },
        )
        DfTextField(
            value = uiState.description,
            onValueChange = actions.onDescriptionChange,
            label = stringResource(R.string.feature_transactions_description),
            error = descriptionError.takeIf { uiState.descriptionError },
        )
        AutocompleteField(
            label = stringResource(R.string.feature_transactions_category),
            value = uiState.category,
            onValueChange = actions.onCategoryChange,
            suggestions = uiState.categoryNames,
        )
        if (uiState.type == TransactionType.WITHDRAWAL && uiState.budgets.isNotEmpty()) {
            BudgetDropdown(uiState.budgets, uiState.budgetId, actions.onBudgetChange)
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

    uiState.splits.forEachIndexed { index, part ->
        PartCard(number = index + 2, onRemove = { split.onRemove(index) }) {
            DfTextField(
                value = part.amount,
                onValueChange = { split.onAmountChange(index, it) },
                label = stringResource(R.string.feature_transactions_amount),
                suffix = currency,
                keyboard = DfKeyboard.Decimal,
                error = amountError.takeIf { part.amountError },
            )
            DfTextField(
                value = part.description,
                onValueChange = { split.onDescriptionChange(index, it) },
                label = stringResource(R.string.feature_transactions_description),
                error = descriptionError.takeIf { part.descriptionError },
            )
            AutocompleteField(
                label = stringResource(R.string.feature_transactions_category),
                value = part.category,
                onValueChange = { split.onCategoryChange(index, it) },
                suggestions = uiState.categoryNames,
            )
            if (uiState.type == TransactionType.WITHDRAWAL && uiState.budgets.isNotEmpty()) {
                BudgetDropdown(uiState.budgets, part.budgetId) { split.onBudgetChange(index, it) }
            }
            TagsField(
                tags = part.tags,
                input = part.tagInput,
                suggestions = uiState.tagSuggestions.filterNot { s -> part.tags.any { it.equals(s, true) } },
                onInputChange = { split.onTagInputChange(index, it) },
                onAdd = { split.onAddTag(index, it) },
                onRemove = { split.onRemoveTag(index, it) },
            )
        }
    }

    DfButton(
        stringResource(R.string.feature_transactions_add_split),
        split.onAdd,
        style = DfButtonStyle.Secondary,
        icon = Icons.Filled.Add,
    )
    uiState.splitTotal?.let { total ->
        DfCard(highlighted = true) {
            DfStat(stringResource(R.string.feature_transactions_split_total), formatMoney(total, currency))
        }
    }
}

@Composable
private fun PartCard(number: Int, onRemove: (() -> Unit)?, content: @Composable () -> Unit) {
    DfCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DfText(
                stringResource(R.string.feature_transactions_split_part, number),
                style = DfTheme.typography.subtitle,
                modifier = Modifier.weight(1f),
            )
            if (onRemove != null) {
                DfIconButton(
                    Icons.Filled.Close,
                    stringResource(R.string.feature_transactions_remove_split, number),
                    onRemove,
                    tone = DfTone.Secondary,
                )
            }
        }
        content()
    }
}
