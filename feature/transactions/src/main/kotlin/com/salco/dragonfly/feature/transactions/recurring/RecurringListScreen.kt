package com.salco.dragonfly.feature.transactions.recurring

import com.salco.dragonfly.core.ui.rememberNotificationPermission
import com.salco.dragonfly.core.designsystem.component.DfCard
import com.salco.dragonfly.core.designsystem.component.DfButtonStyle
import com.salco.dragonfly.core.designsystem.component.DfButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import com.salco.dragonfly.core.designsystem.component.DfAmount
import com.salco.dragonfly.core.designsystem.component.DfEmptyState
import com.salco.dragonfly.core.designsystem.component.DfFab
import com.salco.dragonfly.core.designsystem.component.DfListItem
import com.salco.dragonfly.core.designsystem.component.DfLoading
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.theme.DfTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.theme.DragonflyTheme
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.RecurrenceFrequency
import com.salco.dragonfly.core.model.RecurrenceRule
import com.salco.dragonfly.core.model.RecurringTransaction
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.core.ui.IncomeColor
import com.salco.dragonfly.core.ui.everyLabel
import com.salco.dragonfly.feature.transactions.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun RecurringListScreen(
    onBack: () -> Unit,
    onItemClick: (Long) -> Unit,
    onAdd: () -> Unit,
    viewModel: RecurringListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notificationPermission = rememberNotificationPermission()
    RecurringListContent(
        uiState = uiState,
        onBack = onBack,
        onItemClick = onItemClick,
        onAdd = onAdd,
        askForNotifications = !notificationPermission.granted,
        onAllowNotifications = notificationPermission::request,
        onDeclineNotifications = viewModel::declineNotifications,
    )
}

@Composable
internal fun RecurringListContent(
    uiState: RecurringListUiState,
    onBack: () -> Unit,
    onItemClick: (Long) -> Unit,
    onAdd: () -> Unit,
    askForNotifications: Boolean = false,
    onAllowNotifications: () -> Unit = {},
    onDeclineNotifications: () -> Unit = {},
) {
    DfScaffold(
        title = stringResource(R.string.feature_transactions_recurring_title),
        onBack = onBack,
        floatingAction = { DfFab(stringResource(R.string.feature_transactions_add), Icons.Filled.Add, onAdd) },
    ) { padding ->
        val items = (uiState as? RecurringListUiState.Success)?.items
        when {
            items == null -> DfLoading(Modifier.padding(padding))
            items.isEmpty() -> DfEmptyState(
                icon = Icons.Outlined.EventRepeat,
                title = stringResource(R.string.feature_transactions_recurring_empty_title),
                message = stringResource(R.string.feature_transactions_recurring_empty_message),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
                if (askForNotifications && (uiState as RecurringListUiState.Success).notificationsWanted) {
                    item(key = "notifications") { NotificationsCard(onAllowNotifications, onDeclineNotifications) }
                }
                items(items, key = { it.id }) { item -> RecurringRow(item, onClick = { onItemClick(item.id) }) }
            }
        }
    }
}

/** Offers to notify when transactions are created in the background; shown until the user decides. */
@Composable
private fun NotificationsCard(onAllow: () -> Unit, onDecline: () -> Unit) {
    DfCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), highlighted = true) {
        DfText(stringResource(R.string.feature_transactions_recurring_notify_title), style = DfTheme.typography.subtitle)
        DfText(stringResource(R.string.feature_transactions_recurring_notify_message), style = DfTheme.typography.bodySmall, tone = DfTone.Secondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DfButton(stringResource(R.string.feature_transactions_recurring_notify_allow), onAllow)
            DfButton(stringResource(R.string.feature_transactions_recurring_notify_decline), onDecline, style = DfButtonStyle.Text)
        }
    }
}

@Composable
private fun RecurringRow(item: RecurringTransaction, onClick: () -> Unit) {
    val template = item.template
    val (sign, tone) = when (template.type) {
        TransactionType.WITHDRAWAL -> "−" to DfTone.Primary
        TransactionType.DEPOSIT -> "+" to DfTone.Positive
        TransactionType.TRANSFER -> "" to DfTone.Secondary
    }
    val next = item.nextDate
    DfListItem(
        headline = template.description,
        icon = if (item.active && next != null) Icons.Outlined.EventRepeat else Icons.Outlined.PauseCircle,
        onClick = onClick,
        supportingContent = {
            Column {
                DfText("${item.sourceName} → ${item.destinationName}", style = DfTheme.typography.bodySmall, tone = DfTone.Secondary, maxLines = 1)
                DfText(
                    text = everyLabel(item.rule.frequency, item.rule.interval) + " · " + when {
                        next == null -> stringResource(R.string.feature_transactions_recurring_ended)
                        !item.active -> stringResource(R.string.feature_transactions_recurring_paused)
                        else -> stringResource(
                            R.string.feature_transactions_recurring_next,
                            next.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                        )
                    },
                    style = DfTheme.typography.caption,
                    tone = DfTone.Accent,
                )
            }
        },
        trailing = { DfAmount(sign + formatMoney(template.amount, template.currencyCode), tone = tone) },
    )
}

@Preview
@Composable
private fun RecurringListContentPreview() {
    val start = LocalDate.of(2026, 10, 27)
    DragonflyTheme {
        RecurringListContent(
            uiState = RecurringListUiState.Success(
                listOf(
                    RecurringTransaction(
                        id = 1,
                        template = TransactionDraft(
                            type = TransactionType.DEPOSIT, description = "Stipendio", date = start, amount = 200_000,
                            currencyCode = "EUR", source = AccountRef.Existing(2), destination = AccountRef.Existing(1),
                        ),
                        rule = RecurrenceRule(RecurrenceFrequency.MONTHLY, startDate = start),
                        sourceName = "Azienda",
                        destinationName = "Conto corrente",
                    ),
                ),
            ),
            onBack = {},
            onItemClick = {},
            onAdd = {},
        )
    }
}
