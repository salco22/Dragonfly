package com.salco.dragonfly.core.ui

import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import com.salco.dragonfly.core.model.RecurrenceFrequency

@PluralsRes
fun RecurrenceFrequency.everyRes(): Int = when (this) {
    RecurrenceFrequency.DAILY -> R.plurals.core_ui_every_day
    RecurrenceFrequency.WEEKLY -> R.plurals.core_ui_every_week
    RecurrenceFrequency.MONTHLY -> R.plurals.core_ui_every_month
    RecurrenceFrequency.YEARLY -> R.plurals.core_ui_every_year
}

/** "Ogni mese", "Ogni 2 settimane"… */
@Composable
fun everyLabel(frequency: RecurrenceFrequency, interval: Int): String =
    pluralStringResource(frequency.everyRes(), interval, interval)

/** Unit name agreeing with [interval]: "mese" / "mesi"… */
@Composable
fun unitLabel(frequency: RecurrenceFrequency, interval: Int): String = pluralStringResource(
    when (frequency) {
        RecurrenceFrequency.DAILY -> R.plurals.core_ui_unit_day
        RecurrenceFrequency.WEEKLY -> R.plurals.core_ui_unit_week
        RecurrenceFrequency.MONTHLY -> R.plurals.core_ui_unit_month
        RecurrenceFrequency.YEARLY -> R.plurals.core_ui_unit_year
    },
    interval,
)
