package com.salco.dragonfly.feature.budgets

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salco.dragonfly.core.model.BudgetSpending
import com.salco.dragonfly.core.model.formatMoney
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
internal fun MonthSelector(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_budgets_previous_month))
        }
        Text(
            text = month.format(DateTimeFormatter.ofPattern("LLLL yyyy")).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.feature_budgets_next_month))
        }
    }
}

/** Progress bar, red when the budget is overspent. */
@Composable
internal fun BudgetProgress(spending: BudgetSpending, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { spending.progress.coerceIn(0f, 1f) },
        color = if (spending.isOverspent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = modifier.fillMaxWidth(),
    )
}

/** "Rimangono €X · €Y al giorno", or how much it is overspent. */
@Composable
internal fun remainingText(spending: BudgetSpending, daysLeft: Int?): String {
    val currency = spending.budget.currencyCode
    return when {
        spending.isOverspent -> stringResource(R.string.feature_budgets_overspent, formatMoney(-spending.remaining, currency))
        daysLeft != null && daysLeft > 0 -> stringResource(
            R.string.feature_budgets_remaining_per_day,
            formatMoney(spending.remaining, currency),
            formatMoney(spending.remaining / daysLeft, currency),
        )
        else -> stringResource(R.string.feature_budgets_remaining, formatMoney(spending.remaining, currency))
    }
}
