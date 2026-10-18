package com.salco.dragonfly.feature.budgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.salco.dragonfly.core.designsystem.component.DfPeriodSelector
import com.salco.dragonfly.core.designsystem.component.DfProgressBar
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.model.BudgetSpending
import com.salco.dragonfly.core.model.formatMoney
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
internal fun MonthSelector(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    DfPeriodSelector(
        label = month.format(DateTimeFormatter.ofPattern("LLLL yyyy")).replaceFirstChar { it.uppercase() },
        onPrevious = onPrevious,
        onNext = onNext,
        previousDescription = stringResource(R.string.feature_budgets_previous_month),
        nextDescription = stringResource(R.string.feature_budgets_next_month),
    )
}

/** Progress bar, red when the budget is overspent. */
@Composable
internal fun BudgetProgress(spending: BudgetSpending, modifier: Modifier = Modifier) {
    DfProgressBar(spending.progress, modifier, tone = if (spending.isOverspent) DfTone.Negative else DfTone.Accent)
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
