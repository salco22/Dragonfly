package com.salco.dragonfly.feature.accounts.list

import com.salco.dragonfly.core.designsystem.component.DfAmount
import com.salco.dragonfly.core.designsystem.component.DfCard
import com.salco.dragonfly.core.designsystem.component.DfEmptyState
import com.salco.dragonfly.core.designsystem.component.DfFab
import com.salco.dragonfly.core.designsystem.component.DfIconButton
import com.salco.dragonfly.core.designsystem.component.DfListItem
import com.salco.dragonfly.core.designsystem.component.DfLoading
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfSectionHeader
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.theme.DfTheme
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
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Work
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

@Composable
internal fun AccountsContent(
    uiState: AccountsUiState,
    onAccountClick: (Long) -> Unit,
    onAddAccountClick: () -> Unit,
    onPiggyBanksClick: () -> Unit = {},
) {
    DfScaffold(
        title = stringResource(R.string.feature_accounts_title),
        actions = {
            DfIconButton(Icons.Outlined.Savings, stringResource(R.string.feature_accounts_piggy_banks), onPiggyBanksClick)
        },
        floatingAction = { DfFab(stringResource(R.string.feature_accounts_add), Icons.Filled.Add, onAddAccountClick) },
    ) { padding ->
        when (uiState) {
            AccountsUiState.Loading -> DfLoading(Modifier.padding(padding))
            AccountsUiState.Empty -> DfEmptyState(
                icon = Icons.Outlined.AccountBalance,
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
                    item(key = "header_$type") { DfSectionHeader(stringResource(type.labelRes())) }
                    items(accounts, key = { it.id }) { account ->
                        AccountRow(account, onClick = { onAccountClick(account.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NetWorthCard(netWorthByCurrency: Map<String, Long>) {
    DfCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), highlighted = true, padding = 20.dp) {
        DfText(stringResource(R.string.feature_accounts_net_worth), style = DfTheme.typography.label, tone = DfTone.Secondary)
        netWorthByCurrency.forEach { (currency, amount) ->
            DfAmount(formatMoney(amount, currency), negative = amount < 0, style = DfTheme.typography.display)
        }
    }
}

@Composable
private fun AccountRow(account: Account, onClick: () -> Unit) {
    val assetRole = account.assetRole
    val liabilityType = account.liabilityType
    DfListItem(
        headline = account.name,
        icon = account.icon(),
        onClick = onClick,
        supporting = when {
            !account.active -> stringResource(R.string.feature_accounts_inactive)
            assetRole != null -> stringResource(assetRole.labelRes())
            liabilityType != null -> stringResource(liabilityType.labelRes())
            else -> null
        },
        trailing = if (account.type.hasBalance) {
            { DfAmount(formatMoney(account.balance, account.currencyCode), negative = account.balance < 0) }
        } else {
            null
        },
    )
}

private fun Account.icon(): ImageVector = when (type) {
    AccountType.ASSET -> when (assetRole) {
        AssetRole.SAVINGS -> Icons.Outlined.Savings
        AssetRole.CREDIT_CARD -> Icons.Outlined.CreditCard
        AssetRole.CASH_WALLET -> Icons.Outlined.Payments
        else -> Icons.Outlined.AccountBalanceWallet
    }
    AccountType.EXPENSE -> Icons.Outlined.ShoppingCart
    AccountType.REVENUE -> Icons.Outlined.Work
    AccountType.LIABILITY -> Icons.Outlined.RequestQuote
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
