package com.salco.dragonfly.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.salco.dragonfly.core.designsystem.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Read-only date field that opens a date picker; [onClear] adds a button to remove the date. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate) -> Unit,
    onClear: (() -> Unit)? = null,
    placeholder: String? = null,
) {
    var pickDate by remember { mutableStateOf(false) }
    // A read-only text field swallows clicks, so listen to its press interactions instead.
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { if (it is PressInteraction.Release) pickDate = true }
    }
    OutlinedTextField(
        value = date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)).orEmpty(),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        trailingIcon = {
            if (onClear != null && date != null) {
                IconButton(onClick = onClear) { Icon(Icons.Filled.Close, stringResource(R.string.core_designsystem_clear)) }
            } else {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
            }
        },
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth(),
    )

    if (pickDate) {
        // DatePicker works in UTC milliseconds.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    pickDate = false
                }) { Text(stringResource(R.string.core_designsystem_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.core_designsystem_cancel)) }
            },
        ) { DatePicker(state = state) }
    }
}
