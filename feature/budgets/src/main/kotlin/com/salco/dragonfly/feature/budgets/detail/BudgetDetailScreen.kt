package com.salco.dragonfly.feature.budgets.detail

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state?.spending?.budget?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_budgets_back))
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(viewModel.budgetId) }) {
                        Icon(Icons.Filled.Edit, stringResource(R.string.feature_budgets_edit))
                    }
                },
            )
        },
    ) { padding ->
        if (state == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val spending = state.spending
        val currency = spending.budget.currencyCode
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {
            item { MonthSelector(state.month, viewModel::previousMonth, viewModel::nextMonth) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(
                                R.string.feature_budgets_spent_of,
                                formatMoney(spending.spent, currency),
                                formatMoney(spending.budget.amount, currency),
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        BudgetProgress(spending)
                        Text(
                            remainingText(spending, state.daysLeft),
                            color = if (spending.isOverspent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            if (state.days.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.feature_budgets_no_transactions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
