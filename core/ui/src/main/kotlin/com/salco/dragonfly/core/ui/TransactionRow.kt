package com.salco.dragonfly.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salco.dragonfly.core.model.Transaction
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.formatMoney
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Green used for money coming in; matches the primary green of the Dragonfly palette. */
val IncomeColor = Color(0xFF2E9B48)

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
    val (sign, color) = when (incoming) {
        true -> "+" to IncomeColor
        false -> "−" to MaterialTheme.colorScheme.error
        null -> "" to MaterialTheme.colorScheme.onSurface
    }
    val icon = when (transaction.type) {
        TransactionType.WITHDRAWAL -> Icons.Filled.ArrowUpward
        TransactionType.DEPOSIT -> Icons.Filled.ArrowDownward
        TransactionType.TRANSFER -> Icons.Filled.SwapHoriz
    }

    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null, tint = color) },
        headlineContent = { Text(transaction.description) },
        supportingContent = {
            Column {
                Text("${transaction.source.name} → ${transaction.destination.name}")
                val labels = listOfNotNull(transaction.category?.name) + transaction.tags.map { "#${it.name}" }
                if (labels.isNotEmpty()) {
                    Text(
                        text = labels.joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        trailingContent = {
            Text(text = sign + formatMoney(amount, currency), color = color, fontWeight = FontWeight.SemiBold)
        },
    )
}

@Composable
fun DayHeader(date: LocalDate) {
    Text(
        text = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)).replaceFirstChar { it.uppercase() },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

/** Transactions grouped by date, keeping the incoming order (newest first). */
fun List<Transaction>.groupByDay(): List<Pair<LocalDate, List<Transaction>>> = groupBy { it.date }.toList()
