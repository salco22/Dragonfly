package com.salco.dragonfly.feature.piggybanks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.salco.dragonfly.core.model.PiggyBankSummary
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.core.ui.IncomeColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal fun LocalDate.medium(): String = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

/** "€250.00 di €1,000.00" or "€250.00 risparmiati" without a target. */
@Composable
internal fun PiggyBankSummary.savedLabel(): String {
    val target = piggyBank.targetAmount
    return if (target != null) {
        stringResource(R.string.feature_piggybanks_saved_of, formatMoney(saved, currencyCode), formatMoney(target, currencyCode))
    } else {
        stringResource(R.string.feature_piggybanks_saved, formatMoney(saved, currencyCode))
    }
}

/** Suggestion to reach the target, or when it is due. */
@Composable
internal fun PiggyBankSummary.planLabel(today: LocalDate): String? {
    if (isComplete) return stringResource(R.string.feature_piggybanks_complete)
    val targetDate = piggyBank.targetDate ?: return null
    val monthly = monthlyNeeded(today)
    return if (monthly != null && monthly > 0) {
        stringResource(R.string.feature_piggybanks_monthly, formatMoney(monthly, currencyCode), targetDate.medium())
    } else {
        stringResource(R.string.feature_piggybanks_target_by, targetDate.medium())
    }
}

@Composable
internal fun PiggyBankProgress(summary: PiggyBankSummary, modifier: Modifier = Modifier) {
    val progress = summary.progress ?: return
    LinearProgressIndicator(
        progress = { progress },
        color = if (summary.isComplete) IncomeColor else MaterialTheme.colorScheme.primary,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal fun PiggyBankCard(summary: PiggyBankSummary, today: LocalDate, onClick: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(summary.piggyBank.name, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.feature_piggybanks_in_account, summary.accountName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(summary.savedLabel(), style = MaterialTheme.typography.bodyLarge)
            PiggyBankProgress(summary)
            summary.planLabel(today)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (summary.isComplete) IncomeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
