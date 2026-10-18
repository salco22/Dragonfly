package com.salco.dragonfly.feature.accounts.list

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.component.AmountText
import com.salco.dragonfly.core.designsystem.component.EmptyState
import com.salco.dragonfly.core.designsystem.theme.DragonflyTheme
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.AssetRole
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.feature.accounts.R
import com.salco.dragonfly.feature.accounts.hasBalance
import com.salco.dragonfly.feature.accounts.labelRes

@Composable
internal fun AccountsScreen(
    onAccountClick: (Long) -> Unit,
    onAddAccountClick: () -> Unit,
    onPiggyBanksClick: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AccountsContent(uiState, onAccountClick, onAddAccountClick, onPiggyBanksClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountsContent(
    uiState: AccountsUiState,
    onAccountClick: (Long) -> Unit,
    onAddAccountClick: () -> Unit,
    onPiggyBanksClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_accounts_title)) },
                actions = {
                    IconButton(onClick = onPiggyBanksClick) {
                        Icon(Icons.Filled.Savings, stringResource(R.string.feature_accounts_piggy_banks))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddAccountClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.feature_accounts_add)) },
            )
        },
    ) { padding ->
        when (uiState) {
            AccountsUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            AccountsUiState.Empty -> EmptyState(
                icon = Icons.Filled.AccountBalance,
                title = stringResource(R.string.feature_accounts_empty_title),
                message = stringResource(R.string.feature_accounts_empty_message),
                modifier = Modifier.padding(padding),
            )
            is AccountsUiState.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                if (uiState.netWorthByCurrency.isNotEmpty()) {
                    item { NetWorthCard(uiState.netWorthByCurrency) }
                }
                uiState.sections.forEach { (type, accounts) ->
                    item(key = "header_$type") {
                        Text(
                            text = stringResource(type.labelRes()),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
                        )
                    }
                    items(accounts, key = { it.id }) { account ->
                        AccountRow(account, onClick = { onAccountClick(account.id) })
                        HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NetWorthCard(netWorthByCurrency: Map<String, Long>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.feature_accounts_net_worth),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            netWorthByCurrency.forEach { (currency, amount) ->
                AmountText(
                    formatted = formatMoney(amount, currency),
                    negative = amount < 0,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
    }
}

@Composable
private fun AccountRow(account: Account, onClick: () -> Unit) {
    val assetRole = account.assetRole
    val liabilityType = account.liabilityType
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(account.icon(), contentDescription = null) },
        headlineContent = { Text(account.name) },
        supportingContent = when {
            !account.active -> { { Text(stringResource(R.string.feature_accounts_inactive)) } }
            assetRole != null -> { { Text(stringResource(assetRole.labelRes())) } }
            liabilityType != null -> { { Text(stringResource(liabilityType.labelRes())) } }
            else -> null
        },
        trailingContent = if (account.type.hasBalance) {
            {
                AmountText(
                    formatted = formatMoney(account.balance, account.currencyCode),
                    negative = account.balance < 0,
                )
            }
        } else {
            null
        },
    )
}

private fun Account.icon(): ImageVector = when (type) {
    AccountType.ASSET -> when (assetRole) {
        AssetRole.SAVINGS -> Icons.Filled.Savings
        AssetRole.CREDIT_CARD -> Icons.Filled.CreditCard
        AssetRole.CASH_WALLET -> Icons.Filled.Payments
        else -> Icons.Filled.AccountBalanceWallet
    }
    AccountType.EXPENSE -> Icons.Filled.ShoppingCart
    AccountType.REVENUE -> Icons.Filled.Work
    AccountType.LIABILITY -> Icons.Filled.RequestQuote
}

@Preview
@Composable
private fun AccountsContentPreview() {
    val checking = Account(id = 1, name = "Conto corrente", type = AccountType.ASSET, currencyCode = "EUR", openingBalance = 152_030, assetRole = AssetRole.DEFAULT)
    val mortgage = Account(id = 2, name = "Mutuo casa", type = AccountType.LIABILITY, currencyCode = "EUR", openingBalance = -9_500_000)
    DragonflyTheme {
        AccountsContent(
            uiState = AccountsUiState.Success(
                netWorthByCurrency = mapOf("EUR" to checking.balance + mortgage.balance),
                sections = mapOf(AccountType.ASSET to listOf(checking), AccountType.LIABILITY to listOf(mortgage)),
            ),
            onAccountClick = {},
            onAddAccountClick = {},
        )
    }
}
