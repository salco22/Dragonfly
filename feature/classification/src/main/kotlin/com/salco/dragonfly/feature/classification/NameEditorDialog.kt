package com.salco.dragonfly.feature.classification

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.salco.dragonfly.core.designsystem.component.DfDialog
import com.salco.dragonfly.core.designsystem.component.DfTextField

@Composable
internal fun NameEditorDialog(
    title: String,
    state: NameEditorState,
    deleteMessage: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    DfDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.feature_classification_save),
        onConfirm = onSave,
        dismissText = stringResource(R.string.feature_classification_cancel),
        extraText = if (state.isNew) null else stringResource(R.string.feature_classification_delete),
        onExtra = onDelete,
        extraDestructive = true,
    ) {
        DfTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.feature_classification_name),
            error = when (state.error) {
                NameError.BLANK -> stringResource(R.string.feature_classification_error_blank)
                NameError.DUPLICATE -> stringResource(R.string.feature_classification_error_duplicate)
                null -> null
            },
            helper = if (state.isNew) null else deleteMessage,
        )
    }
}
