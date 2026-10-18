package com.salco.dragonfly.feature.classification.categories

import com.salco.dragonfly.core.designsystem.component.DfListItem
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.theme.DfTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Category
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.theme.DragonflyTheme
import com.salco.dragonfly.core.model.Category
import com.salco.dragonfly.core.model.CategorySummary
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.feature.classification.ClassificationScaffold
import com.salco.dragonfly.feature.classification.NameEditorDialog
import com.salco.dragonfly.feature.classification.R

@Composable
internal fun CategoriesScreen(onBack: () -> Unit, viewModel: CategoriesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()

    CategoriesContent(uiState, onBack, onAdd = viewModel::startAdding, onCategoryClick = viewModel::startEditing)

    editor?.let { state ->
        NameEditorDialog(
            title = stringResource(
                if (state.isNew) R.string.feature_classification_new_category else R.string.feature_classification_edit_category,
            ),
            state = state,
            deleteMessage = stringResource(R.string.feature_classification_delete_category_message),
            onNameChange = viewModel::onNameChange,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
            onDismiss = viewModel::dismiss,
        )
    }
}

@Composable
internal fun CategoriesContent(
    uiState: CategoriesUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onCategoryClick: (Category) -> Unit,
) {
    val categories = (uiState as? CategoriesUiState.Success)?.categories.orEmpty()
    ClassificationScaffold(
        title = stringResource(R.string.feature_classification_categories),
        isLoading = uiState is CategoriesUiState.Loading,
        isEmpty = categories.isEmpty(),
        emptyIcon = Icons.Outlined.Category,
        emptyTitle = stringResource(R.string.feature_classification_categories_empty_title),
        emptyMessage = stringResource(R.string.feature_classification_categories_empty_message),
        onBack = onBack,
        onAdd = onAdd,
    ) {
        items(categories, key = { it.category.id }) { summary ->
            DfListItem(
                headline = summary.category.name,
                icon = Icons.AutoMirrored.Outlined.Label,
                onClick = { onCategoryClick(summary.category) },
                supportingContent = { CategoryTotals(summary) },
            )
        }
    }
}

@Composable
private fun CategoryTotals(summary: CategorySummary) {
    if (summary.spent.isEmpty() && summary.earned.isEmpty()) {
        DfText(stringResource(R.string.feature_classification_unused), style = DfTheme.typography.bodySmall, tone = DfTone.Tertiary)
        return
    }
    Column {
        summary.spent.forEach { (currency, amount) ->
            DfText(stringResource(R.string.feature_classification_spent, formatMoney(amount, currency)), style = DfTheme.typography.bodySmall, tone = DfTone.Secondary)
        }
        summary.earned.forEach { (currency, amount) ->
            DfText(stringResource(R.string.feature_classification_earned, formatMoney(amount, currency)), style = DfTheme.typography.bodySmall, tone = DfTone.Positive)
        }
    }
}

@Preview
@Composable
private fun CategoriesContentPreview() {
    DragonflyTheme {
        CategoriesContent(
            uiState = CategoriesUiState.Success(
                listOf(
                    CategorySummary(Category(1, "Spesa alimentare"), spent = mapOf("EUR" to 32_450), earned = emptyMap()),
                    CategorySummary(Category(2, "Viaggi"), spent = emptyMap(), earned = emptyMap()),
                ),
            ),
            onBack = {},
            onAdd = {},
            onCategoryClick = {},
        )
    }
}
