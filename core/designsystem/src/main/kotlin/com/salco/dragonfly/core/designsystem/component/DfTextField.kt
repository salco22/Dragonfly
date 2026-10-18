package com.salco.dragonfly.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.salco.dragonfly.core.designsystem.theme.DfTheme

/** What the keyboard should offer for a text field. */
enum class DfKeyboard { Text, Sentences, Characters, Number, Decimal }

private fun DfKeyboard.options(imeAction: ImeAction) = KeyboardOptions(
    capitalization = when (this) {
        DfKeyboard.Sentences -> KeyboardCapitalization.Sentences
        DfKeyboard.Characters -> KeyboardCapitalization.Characters
        else -> KeyboardCapitalization.None
    },
    keyboardType = when (this) {
        DfKeyboard.Number -> KeyboardType.Number
        DfKeyboard.Decimal -> KeyboardType.Decimal
        else -> KeyboardType.Text
    },
    imeAction = imeAction,
)

@Composable
internal fun dfFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DfTheme.colors.accent,
    unfocusedBorderColor = DfTheme.colors.border,
    focusedContainerColor = DfTheme.colors.surface,
    unfocusedContainerColor = DfTheme.colors.surface,
    focusedLabelColor = DfTheme.colors.accent,
    unfocusedLabelColor = DfTheme.colors.textSecondary,
    cursorColor = DfTheme.colors.accent,
    focusedTextColor = DfTheme.colors.textPrimary,
    unfocusedTextColor = DfTheme.colors.textPrimary,
    errorBorderColor = DfTheme.colors.negative,
    errorLabelColor = DfTheme.colors.negative,
    errorSupportingTextColor = DfTheme.colors.negative,
    unfocusedSupportingTextColor = DfTheme.colors.textSecondary,
    focusedSupportingTextColor = DfTheme.colors.textSecondary,
    focusedTrailingIconColor = DfTheme.colors.textSecondary,
    unfocusedTrailingIconColor = DfTheme.colors.textSecondary,
    unfocusedSuffixColor = DfTheme.colors.textSecondary,
    focusedSuffixColor = DfTheme.colors.textSecondary,
)

/**
 * A labelled text input. [error] replaces [helper] and turns the field red; [onDone] shows a
 * Done key on the keyboard.
 */
@Composable
fun DfTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helper: String? = null,
    error: String? = null,
    suffix: String? = null,
    keyboard: DfKeyboard = DfKeyboard.Sentences,
    singleLine: Boolean = true,
    minLines: Int = 1,
    readOnly: Boolean = false,
    large: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = DfTheme.colors.textTertiary) } },
        supportingText = (error ?: helper)?.takeIf { it.isNotEmpty() }?.let { { Text(it) } },
        isError = error != null,
        suffix = suffix?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { { DfIcon(it, contentDescription = null) } },
        trailingIcon = trailing,
        singleLine = singleLine,
        minLines = minLines,
        readOnly = readOnly,
        textStyle = if (large) DfTheme.typography.headline else DfTheme.typography.body,
        keyboardOptions = keyboard.options(if (onDone != null) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = onDone?.let { { it() } }),
        shape = RoundedCornerShape(DfTheme.spacing.radiusSmall),
        colors = dfFieldColors(),
        interactionSource = interactionSource,
    )
}

/** A borderless search input for top bars. */
@Composable
fun DfSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = DfTheme.colors.textTertiary) },
        singleLine = true,
        textStyle = DfTheme.typography.body.merge(TextStyle(color = DfTheme.colors.textPrimary)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = DfTheme.colors.accent,
        ),
        modifier = modifier.fillMaxWidth().let { if (focusRequester != null) it.focusRequester(focusRequester) else it },
    )
}
