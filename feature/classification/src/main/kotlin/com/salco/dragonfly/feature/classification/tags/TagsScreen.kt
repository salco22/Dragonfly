package com.salco.dragonfly.feature.classification.tags

import com.salco.dragonfly.core.designsystem.component.DfListItem
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.theme.DragonflyTheme
import com.salco.dragonfly.core.model.Tag
import com.salco.dragonfly.core.model.TagSummary
import com.salco.dragonfly.feature.classification.ClassificationScaffold
import com.salco.dragonfly.feature.classification.NameEditorDialog
import com.salco.dragonfly.feature.classification.R

@Composable
internal fun TagsScreen(onBack: () -> Unit, viewModel: TagsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()

    TagsContent(uiState, onBack, onAdd = viewModel::startAdding, onTagClick = viewModel::startEditing)

    editor?.let { state ->
        NameEditorDialog(
            title = stringResource(
                if (state.isNew) R.string.feature_classification_new_tag else R.string.feature_classification_edit_tag,
            ),
            state = state,
            deleteMessage = stringResource(R.string.feature_classification_delete_tag_message),
            onNameChange = viewModel::onNameChange,
            onSave = viewModel::save,
            onDelete = viewModel::delete,
            onDismiss = viewModel::dismiss,
        )
    }
}

@Composable
internal fun TagsContent(
    uiState: TagsUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onTagClick: (Tag) -> Unit,
) {
    val tags = (uiState as? TagsUiState.Success)?.tags.orEmpty()
    ClassificationScaffold(
        title = stringResource(R.string.feature_classification_tags),
        isLoading = uiState is TagsUiState.Loading,
        isEmpty = tags.isEmpty(),
        emptyIcon = Icons.Outlined.Sell,
        emptyTitle = stringResource(R.string.feature_classification_tags_empty_title),
        emptyMessage = stringResource(R.string.feature_classification_tags_empty_message),
        onBack = onBack,
        onAdd = onAdd,
    ) {
        items(tags, key = { it.tag.id }) { summary ->
            DfListItem(
                headline = "#${summary.tag.name}",
                icon = Icons.Outlined.Sell,
                onClick = { onTagClick(summary.tag) },
                supporting = pluralStringResource(
                    R.plurals.feature_classification_transaction_count,
                    summary.transactionCount,
                    summary.transactionCount,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun TagsContentPreview() {
    DragonflyTheme {
        TagsContent(
            uiState = TagsUiState.Success(listOf(TagSummary(Tag(1, "Vacanze"), 4), TagSummary(Tag(2, "Casa"), 1))),
            onBack = {},
            onAdd = {},
            onTagClick = {},
        )
    }
}
