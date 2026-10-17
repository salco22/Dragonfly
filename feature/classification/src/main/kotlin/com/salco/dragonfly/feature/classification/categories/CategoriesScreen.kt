package com.salco.dragonfly.feature.classification.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
        emptyIcon = Icons.Filled.Category,
        emptyTitle = stringResource(R.string.feature_classification_categories_empty_title),
        emptyMessage = stringResource(R.string.feature_classification_categories_empty_message),
        onBack = onBack,
        onAdd = onAdd,
    ) {
        items(categories, key = { it.category.id }) { summary ->
            ListItem(
                modifier = Modifier.clickable { onCategoryClick(summary.category) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
                headlineContent = { Text(summary.category.name) },
                supportingContent = { CategoryTotals(summary) },
            )
        }
    }
}

@Composable
private fun CategoryTotals(summary: CategorySummary) {
    if (summary.spent.isEmpty() && summary.earned.isEmpty()) {
        Text(stringResource(R.string.feature_classification_unused))
        return
    }
    Column {
        summary.spent.forEach { (currency, amount) ->
            Text(
                stringResource(R.string.feature_classification_spent, formatMoney(amount, currency)),
                color = MaterialTheme.colorScheme.error,
            )
        }
        summary.earned.forEach { (currency, amount) ->
            Text(stringResource(R.string.feature_classification_earned, formatMoney(amount, currency)))
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
