package com.salco.dragonfly.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.salco.dragonfly.core.designsystem.component.DfAmount
import com.salco.dragonfly.core.designsystem.component.DfListItem
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.component.color
import com.salco.dragonfly.core.designsystem.theme.DfTheme
import com.salco.dragonfly.core.model.Transaction
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.formatMoney
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Green used for money coming in. */
val IncomeColor: Color
    @Composable get() = DfTone.Positive.color()

/**
 * A transaction in a list. With [perspectiveAccountId] the amount is signed from that account's
 * point of view (incoming positive, outgoing negative, in that account's currency); without it
 * withdrawals are negative, deposits positive and transfers neutral.
 */
@Composable
fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit,
    perspectiveAccountId: Long? = null,
) {
    val incoming = when {
        perspectiveAccountId != null -> transaction.destination.id == perspectiveAccountId
        transaction.type == TransactionType.DEPOSIT -> true
        transaction.type == TransactionType.WITHDRAWAL -> false
        else -> null
    }
    // A transfer between currencies credits the destination with the foreign amount.
    val (amount, currency) = if (incoming == true && perspectiveAccountId != null && transaction.foreignAmount != null) {
        transaction.foreignAmount!! to transaction.destination.currencyCode
    } else {
        transaction.amount to transaction.currencyCode
    }
    val (sign, tone) = when (incoming) {
        true -> "+" to DfTone.Positive
        false -> "−" to DfTone.Primary
        null -> "" to DfTone.Secondary
    }
    val icon = when (transaction.type) {
        TransactionType.WITHDRAWAL -> Icons.Outlined.ArrowUpward
        TransactionType.DEPOSIT -> Icons.Outlined.ArrowDownward
        TransactionType.TRANSFER -> Icons.Outlined.SwapHoriz
    }

    DfListItem(
        headline = transaction.description,
        icon = icon,
        iconTone = if (incoming == true) DfTone.Positive else DfTone.Secondary,
        onClick = onClick,
        supportingContent = {
            Column {
                DfText(
                    "${transaction.source.name} → ${transaction.destination.name}",
                    style = DfTheme.typography.bodySmall,
                    tone = DfTone.Secondary,
                    maxLines = 1,
                )
                val labels = listOfNotNull(transaction.category?.name) + transaction.tags.map { "#${it.name}" }
                if (labels.isNotEmpty()) {
                    DfText(labels.joinToString(" · "), style = DfTheme.typography.caption, tone = DfTone.Accent, maxLines = 1)
                }
            }
        },
        trailing = { DfAmount(sign + formatMoney(amount, currency), tone = tone, style = DfTheme.typography.subtitle) },
    )
}

@Composable
fun DayHeader(date: LocalDate) {
    DfText(
        text = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)).replaceFirstChar { it.uppercase() },
        style = DfTheme.typography.label,
        tone = DfTone.Tertiary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = DfTheme.spacing.screen, end = DfTheme.spacing.screen, top = DfTheme.spacing.l, bottom = DfTheme.spacing.xs),
    )
}

/** Transactions grouped by date, keeping the incoming order (newest first). */
fun List<Transaction>.groupByDay(): List<Pair<LocalDate, List<Transaction>>> = groupBy { it.date }.toList()
