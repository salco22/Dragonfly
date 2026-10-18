package com.salco.dragonfly.feature.transactions.recurring

import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import com.salco.dragonfly.core.model.RecurrenceFrequency
import com.salco.dragonfly.feature.transactions.R

@PluralsRes
internal fun RecurrenceFrequency.everyRes(): Int = when (this) {
    RecurrenceFrequency.DAILY -> R.plurals.feature_transactions_every_day
    RecurrenceFrequency.WEEKLY -> R.plurals.feature_transactions_every_week
    RecurrenceFrequency.MONTHLY -> R.plurals.feature_transactions_every_month
    RecurrenceFrequency.YEARLY -> R.plurals.feature_transactions_every_year
}

/** "Ogni mese", "Ogni 2 settimane"… */
@Composable
internal fun everyLabel(frequency: RecurrenceFrequency, interval: Int): String =
    pluralStringResource(frequency.everyRes(), interval, interval)
