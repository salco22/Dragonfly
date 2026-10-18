package com.salco.dragonfly.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.salco.dragonfly.core.designsystem.theme.DfTheme

/** Read-only field that opens a menu of [options]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    isError: Boolean = false,
    supportingText: String? = null,
    /** Shown when nothing is selected. */
    placeholder: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected?.let { optionLabel(it) } ?: placeholder.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = isError,
            supportingText = supportingText?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            textStyle = DfTheme.typography.body,
            shape = RoundedCornerShape(DfTheme.spacing.radiusSmall),
            colors = dfFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = DfTheme.colors.surface) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { DfText(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Free-text field that suggests matching entries from [suggestions] while typing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutocompleteField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    modifier: Modifier = Modifier.fillMaxWidth(),
    isError: Boolean = false,
    supportingText: String? = null,
    onSuggestionClick: (String) -> Unit = onValueChange,
    /** When set, the keyboard shows a Done action that calls it. */
    onDone: (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val matches = remember(value, suggestions) {
        suggestions.filter { it.contains(value.trim(), ignoreCase = true) && !it.equals(value.trim(), ignoreCase = true) }
            .take(5)
    }
    ExposedDropdownMenuBox(
        expanded = expanded && matches.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            singleLine = true,
            isError = isError,
            supportingText = supportingText?.let { { Text(it) } },
            trailingIcon = trailingIcon,
            textStyle = DfTheme.typography.body,
            shape = RoundedCornerShape(DfTheme.spacing.radiusSmall),
            colors = dfFieldColors(),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = if (onDone != null) ImeAction.Done else ImeAction.Default,
            ),
            keyboardActions = KeyboardActions(onDone = onDone?.let { { it() } }),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded && matches.isNotEmpty(),
            onDismissRequest = { expanded = false },
            containerColor = DfTheme.colors.surface,
        ) {
            matches.forEach { suggestion ->
                DropdownMenuItem(
                    text = { DfText(suggestion) },
                    onClick = {
                        onSuggestionClick(suggestion)
                        expanded = false
                    },
                )
            }
        }
    }
}
