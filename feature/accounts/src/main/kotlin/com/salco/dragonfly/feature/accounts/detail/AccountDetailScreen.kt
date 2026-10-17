package com.salco.dragonfly.feature.accounts.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.component.AmountText
import com.salco.dragonfly.core.designsystem.theme.DragonflyTheme
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountSummary
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.AssetRole
import com.salco.dragonfly.core.model.Transaction
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.core.ui.DayHeader
import com.salco.dragonfly.core.ui.IncomeColor
import com.salco.dragonfly.core.ui.TransactionRow
import com.salco.dragonfly.feature.accounts.R
import com.salco.dragonfly.feature.accounts.hasBalance
import com.salco.dragonfly.feature.accounts.labelRes
import java.time.LocalDate

@Composable
internal fun AccountDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onTransactionClick: (Long) -> Unit,
    onAddTransaction: (Long) -> Unit,
    viewModel: AccountDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    // Leave the screen if the account was deleted from its edit screen.
    LaunchedEffect(uiState) { if (uiState is AccountDetailUiState.NotFound) onBack() }

    AccountDetailContent(
        uiState = uiState,
        query = query,
        onBack = onBack,
        onEdit = { onEdit(viewModel.accountId) },
        onQueryChange = viewModel::onQueryChange,
        onTransactionClick = onTransactionClick,
        onAddTransaction = { onAddTransaction(viewModel.accountId) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountDetailContent(
    uiState: AccountDetailUiState,
    query: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onQueryChange: (String) -> Unit,
    onTransactionClick: (Long) -> Unit,
    onAddTransaction: () -> Unit,
) {
    val success = uiState as? AccountDetailUiState.Success
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(success?.account?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_accounts_back))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, stringResource(R.string.feature_accounts_edit)) }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransaction,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.feature_accounts_new_transaction)) },
            )
        },
    ) { padding ->
        if (success == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val account = success.account
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item { AccountHeader(account, success.totals) }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text(stringResource(R.string.feature_accounts_search_transactions)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Filled.Close, stringResource(R.string.feature_accounts_clear_search))
                            }
                        }
                    } else {
                        null
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (success.days.isEmpty()) {
                item {
                    Text(
                        text = stringResource(
                            if (success.query.isEmpty()) R.string.feature_accounts_no_transactions
                            else R.string.feature_accounts_no_results,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }
            success.days.forEach { (date, transactions) ->
                item(key = "day_$date") { DayHeader(date) }
                items(transactions, key = { it.id }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction.id) },
                        perspectiveAccountId = account.id,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountHeader(account: Account, totals: FlowTotals) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val subtitle = account.assetRole?.labelRes() ?: account.liabilityType?.labelRes() ?: account.type.labelRes()
            Text(
                text = stringResource(subtitle),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (account.type.hasBalance) {
                AmountText(
                    formatted = formatMoney(account.balance, account.currencyCode),
                    negative = account.balance < 0,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            account.iban?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                FlowColumn(stringResource(R.string.feature_accounts_incoming), "+" + formatMoney(totals.incoming, account.currencyCode), true)
                FlowColumn(stringResource(R.string.feature_accounts_outgoing), "−" + formatMoney(totals.outgoing, account.currencyCode), false)
            }
        }
    }
}

@Composable
private fun FlowColumn(label: String, value: String, incoming: Boolean) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (incoming) IncomeColor else MaterialTheme.colorScheme.error,
        )
    }
}

@Preview
@Composable
private fun AccountDetailContentPreview() {
    val account = Account(id = 1, name = "Conto corrente", type = AccountType.ASSET, currencyCode = "EUR", openingBalance = 152_030, assetRole = AssetRole.DEFAULT, iban = "IT60X0542811101000000123456")
    val shop = AccountSummary(2, "Supermercato", AccountType.EXPENSE, "EUR")
    val own = AccountSummary(1, account.name, AccountType.ASSET, "EUR")
    val today = LocalDate.of(2026, 10, 17)
    DragonflyTheme {
        AccountDetailContent(
            query = "",
            uiState = AccountDetailUiState.Success(
                account = account,
                query = "",
                days = listOf(today to listOf(Transaction(1, TransactionType.WITHDRAWAL, "Spesa", today, 4_250, "EUR", source = own, destination = shop))),
                totals = FlowTotals(incoming = 0, outgoing = 4_250),
            ),
            onBack = {},
            onEdit = {},
            onQueryChange = {},
            onTransactionClick = {},
            onAddTransaction = {},
        )
    }
}
