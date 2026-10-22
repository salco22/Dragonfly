package com.salco.dragonfly.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.salco.dragonfly.core.designsystem.component.DfAmount
import com.salco.dragonfly.core.designsystem.component.DfDateHeader
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
    val icon = if (transaction.groupId != null) Icons.AutoMirrored.Outlined.CallSplit else when (transaction.type) {
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
                    transaction.accountsLabel(),
                    style = DfTheme.typography.bodySmall,
                    tone = DfTone.Secondary,
                    maxLines = 1,
                )
                val labels = listOfNotNull(transaction.category?.name) + transaction.tags.map { "#${it.name}" }
                if (labels.isNotEmpty()) {
                    DfText(labels.joinToString(" · "), style = DfTheme.typography.caption, tone = DfTone.Secondary, maxLines = 1)
                }
            }
        },
        trailing = { DfAmount(sign + formatMoney(amount, currency), tone = tone, style = DfTheme.typography.subtitle) },
    )
}

/** "Oggi · 21 ottobre", "Ieri · 20 ottobre", "19 ottobre", or with the year when it is not this one. */
@Composable
fun DayHeader(date: LocalDate) {
    val today = LocalDate.now()
    val day = date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "d MMMM" else "d MMMM yyyy"))
    DfDateHeader(
        when (date) {
            today -> stringResource(R.string.core_ui_today, day)
            today.minusDays(1) -> stringResource(R.string.core_ui_yesterday, day)
            else -> day
        },
    )
}

/**
 * The user's own accounts of a transaction: where a spending came from, where an income went,
 * or both ends of a transfer. Payees and payers are not shown.
 */
fun Transaction.accountsLabel(): String = when (type) {
    TransactionType.WITHDRAWAL -> source.name
    TransactionType.DEPOSIT -> destination.name
    TransactionType.TRANSFER -> "${source.name} → ${destination.name}"
}

/** Transactions grouped by date, keeping the incoming order (newest first). */
fun List<Transaction>.groupByDay(): List<Pair<LocalDate, List<Transaction>>> = groupBy { it.date }.toList()
