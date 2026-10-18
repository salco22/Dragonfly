package com.salco.dragonfly.feature.accounts.detail

import com.salco.dragonfly.core.designsystem.component.DfAmount
import com.salco.dragonfly.core.designsystem.component.DfCard
import com.salco.dragonfly.core.designsystem.component.DfFab
import com.salco.dragonfly.core.designsystem.component.DfIconButton
import com.salco.dragonfly.core.designsystem.component.DfKeyboard
import com.salco.dragonfly.core.designsystem.component.DfLoading
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfStat
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTextField
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.theme.DfTheme
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
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
    DfScaffold(
        title = success?.account?.name.orEmpty(),
        onBack = onBack,
        actions = { DfIconButton(Icons.Outlined.Edit, stringResource(R.string.feature_accounts_edit), onEdit) },
        floatingAction = { DfFab(stringResource(R.string.feature_accounts_new_transaction), Icons.Filled.Add, onAddTransaction) },
    ) { padding ->
        if (success == null) {
            DfLoading(Modifier.padding(padding))
            return@DfScaffold
        }
        val account = success.account
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item { AccountHeader(account, success.totals, success.savedInPiggyBanks) }
            item {
                DfTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = stringResource(R.string.feature_accounts_search_transactions),
                    leadingIcon = Icons.Outlined.Search,
                    keyboard = DfKeyboard.Text,
                    trailing = if (query.isNotEmpty()) {
                        { DfIconButton(Icons.Filled.Close, stringResource(R.string.feature_accounts_clear_search), { onQueryChange("") }) }
                    } else {
                        null
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            if (success.days.isEmpty()) {
                item {
                    DfText(
                        text = stringResource(
                            if (query.isEmpty()) R.string.feature_accounts_no_transactions
                            else R.string.feature_accounts_no_results,
                        ),
                        tone = DfTone.Secondary,
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
private fun AccountHeader(account: Account, totals: FlowTotals, savedInPiggyBanks: Long) {
    DfCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), highlighted = true, padding = 20.dp) {
        val subtitle = account.assetRole?.labelRes() ?: account.liabilityType?.labelRes() ?: account.type.labelRes()
        DfText(stringResource(subtitle), style = DfTheme.typography.label, tone = DfTone.Secondary)
        if (account.type.hasBalance) {
            DfAmount(formatMoney(account.balance, account.currencyCode), negative = account.balance < 0, style = DfTheme.typography.display)
        }
        if (savedInPiggyBanks > 0) {
            DfText(
                stringResource(
                    R.string.feature_accounts_in_piggy_banks,
                    formatMoney(savedInPiggyBanks, account.currencyCode),
                    formatMoney(account.balance - savedInPiggyBanks, account.currencyCode),
                ),
                style = DfTheme.typography.bodySmall,
                tone = DfTone.Secondary,
            )
        }
        account.iban?.let { DfText(it, style = DfTheme.typography.bodySmall, tone = DfTone.Secondary) }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            DfStat(stringResource(R.string.feature_accounts_incoming), "+" + formatMoney(totals.incoming, account.currencyCode), tone = DfTone.Positive)
            DfStat(stringResource(R.string.feature_accounts_outgoing), "−" + formatMoney(totals.outgoing, account.currencyCode))
        }
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
