package com.salco.dragonfly.feature.bills

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.salco.dragonfly.core.model.Bill
import com.salco.dragonfly.core.model.BillStatus
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.core.ui.IncomeColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal fun LocalDate.medium(): String = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

/** "€80.00", or "€40.00 – €60.00" when the amount varies. */
internal fun Bill.amountLabel(): String =
    if (amountMin == amountMax) formatMoney(amountMax, currencyCode)
    else "${formatMoney(amountMin, currencyCode)} – ${formatMoney(amountMax, currencyCode)}"

@Composable
internal fun BillStatus.label(): String = when (this) {
    is BillStatus.Paid -> stringResource(R.string.feature_bills_status_paid, date.medium())
    is BillStatus.Due -> stringResource(R.string.feature_bills_status_due, dueDate.medium())
    is BillStatus.Overdue -> stringResource(R.string.feature_bills_status_overdue, dueDate.medium())
    is BillStatus.NotStarted -> stringResource(R.string.feature_bills_status_not_started, firstDate.medium())
    BillStatus.Inactive -> stringResource(R.string.feature_bills_status_inactive)
}

@Composable
internal fun BillStatus.color(): Color = when (this) {
    is BillStatus.Paid -> IncomeColor
    is BillStatus.Overdue -> MaterialTheme.colorScheme.error
    is BillStatus.Due -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

internal val BillStatus.canPay: Boolean get() = this is BillStatus.Due || this is BillStatus.Overdue
