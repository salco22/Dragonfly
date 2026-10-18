package com.salco.dragonfly.feature.budgets.detail

import com.salco.dragonfly.core.designsystem.component.DfCard
import com.salco.dragonfly.core.designsystem.component.DfIconButton
import com.salco.dragonfly.core.designsystem.component.DfLoading
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.theme.DfTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.core.ui.DayHeader
import com.salco.dragonfly.core.ui.TransactionRow
import com.salco.dragonfly.feature.budgets.BudgetProgress
import com.salco.dragonfly.feature.budgets.MonthSelector
import com.salco.dragonfly.feature.budgets.R
import com.salco.dragonfly.feature.budgets.remainingText

@Composable
internal fun BudgetDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: BudgetDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Leave the screen if the budget was deleted from its edit screen.
    LaunchedEffect(uiState) { if (uiState is BudgetDetailUiState.NotFound) onBack() }
    val state = uiState as? BudgetDetailUiState.Success

    DfScaffold(
        title = state?.spending?.budget?.name.orEmpty(),
        onBack = onBack,
        actions = { DfIconButton(Icons.Outlined.Edit, stringResource(R.string.feature_budgets_edit), { onEdit(viewModel.budgetId) }) },
    ) { padding ->
        if (state == null) {
            DfLoading(Modifier.padding(padding))
            return@DfScaffold
        }
        val spending = state.spending
        val currency = spending.budget.currencyCode
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {
            item { MonthSelector(state.month, viewModel::previousMonth, viewModel::nextMonth) }
            item {
                DfCard(modifier = Modifier.padding(horizontal = 20.dp), highlighted = true, padding = 20.dp, spacing = 10.dp) {
                    DfText(
                        stringResource(R.string.feature_budgets_spent_of, formatMoney(spending.spent, currency), formatMoney(spending.budget.amount, currency)),
                        style = DfTheme.typography.headline,
                    )
                    BudgetProgress(spending)
                    DfText(remainingText(spending, state.daysLeft), style = DfTheme.typography.bodySmall, tone = if (spending.isOverspent) DfTone.Negative else DfTone.Secondary)
                }
            }
            if (state.days.isEmpty()) {
                item {
                    DfText(
                        stringResource(R.string.feature_budgets_no_transactions),
                        tone = DfTone.Secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }
            state.days.forEach { (date, transactions) ->
                item(key = "day_$date") { DayHeader(date) }
                items(transactions, key = { it.id }) { transaction ->
                    TransactionRow(transaction, onClick = { onTransactionClick(transaction.id) })
                }
            }
        }
    }
}
