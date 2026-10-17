package com.salco.dragonfly.feature.classification

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization

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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.feature_classification_name)) },
                singleLine = true,
                isError = state.error != null,
                supportingText = when (state.error) {
                    NameError.BLANK -> { { Text(stringResource(R.string.feature_classification_error_blank)) } }
                    NameError.DUPLICATE -> { { Text(stringResource(R.string.feature_classification_error_duplicate)) } }
                    null -> if (state.isNew) null else { { Text(deleteMessage) } }
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text(stringResource(R.string.feature_classification_save)) }
        },
        dismissButton = {
            Row {
                if (!state.isNew) {
                    TextButton(onClick = onDelete) {
                        Text(stringResource(R.string.feature_classification_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_classification_cancel)) }
            }
        },
    )
}
