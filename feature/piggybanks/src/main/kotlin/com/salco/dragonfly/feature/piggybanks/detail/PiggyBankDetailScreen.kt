package com.salco.dragonfly.feature.piggybanks.detail

import com.salco.dragonfly.core.designsystem.component.DfAmount
import com.salco.dragonfly.core.designsystem.component.DfButton
import com.salco.dragonfly.core.designsystem.component.DfButtonStyle
import com.salco.dragonfly.core.designsystem.component.DfCard
import com.salco.dragonfly.core.designsystem.component.DfDialog
import com.salco.dragonfly.core.designsystem.component.DfIconButton
import com.salco.dragonfly.core.designsystem.component.DfKeyboard
import com.salco.dragonfly.core.designsystem.component.DfListItem
import com.salco.dragonfly.core.designsystem.component.DfLoading
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfSectionHeader
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.feature.piggybanks.PiggyBankProgress
import com.salco.dragonfly.feature.piggybanks.R
import com.salco.dragonfly.feature.piggybanks.medium
import com.salco.dragonfly.feature.piggybanks.planLabel
import com.salco.dragonfly.feature.piggybanks.savedLabel

@Composable
internal fun PiggyBankDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: PiggyBankDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    // Leave the screen if the piggy bank was deleted from its edit screen.
    LaunchedEffect(uiState) { if (uiState is PiggyBankDetailUiState.NotFound) onBack() }
    val state = uiState as? PiggyBankDetailUiState.Success

    DfScaffold(
        title = state?.summary?.piggyBank?.name.orEmpty(),
        onBack = onBack,
        actions = { DfIconButton(Icons.Outlined.Edit, stringResource(R.string.feature_piggybanks_edit), { onEdit(viewModel.piggyBankId) }) },
    ) { padding ->
        if (state == null) {
            DfLoading(Modifier.padding(padding))
            return@DfScaffold
        }
        val summary = state.summary
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                DfCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), highlighted = true, padding = 20.dp, spacing = 8.dp) {
                    DfText(summary.savedLabel(), style = DfTheme.typography.headline)
                    PiggyBankProgress(summary)
                    summary.planLabel(state.today)?.let { DfText(it, tone = DfTone.Secondary) }
                    DfText(
                        stringResource(R.string.feature_piggybanks_in_account, summary.accountName) + " · " +
                            stringResource(R.string.feature_piggybanks_available, formatMoney(state.availableInAccount, summary.currencyCode)),
                        style = DfTheme.typography.caption,
                        tone = DfTone.Secondary,
                    )
                    summary.piggyBank.notes?.let { DfText(it, style = DfTheme.typography.bodySmall, tone = DfTone.Secondary) }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
                        DfButton(stringResource(R.string.feature_piggybanks_add_money), viewModel::startAdding)
                        DfButton(
                            stringResource(R.string.feature_piggybanks_remove_money),
                            viewModel::startRemoving,
                            style = DfButtonStyle.Secondary,
                            enabled = summary.saved > 0,
                        )
                    }
                }
            }
            item { DfSectionHeader(stringResource(R.string.feature_piggybanks_history)) }
            if (state.events.isEmpty()) {
                item {
                    DfText(
                        stringResource(R.string.feature_piggybanks_no_history),
                        tone = DfTone.Secondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }
            items(state.events, key = { it.id }) { event ->
                DfListItem(
                    headline = event.date.medium(),
                    trailing = {
                        DfAmount(
                            (if (event.amount > 0) "+" else "−") + formatMoney(kotlin.math.abs(event.amount), summary.currencyCode),
                            tone = if (event.amount > 0) DfTone.Positive else DfTone.Negative,
                        )
                    },
                )
            }
        }

        dialog?.let { move ->
            DfDialog(
                title = stringResource(
                    if (move.adding) R.string.feature_piggybanks_add_money_title else R.string.feature_piggybanks_remove_money_title,
                ),
                onDismiss = viewModel::dismiss,
                confirmText = stringResource(R.string.feature_piggybanks_confirm),
                onConfirm = viewModel::confirm,
                dismissText = stringResource(R.string.feature_piggybanks_cancel),
            ) {
                val available = if (move.adding) state.availableInAccount else summary.saved
                val helper = stringResource(R.string.feature_piggybanks_available, formatMoney(available, summary.currencyCode))
                DfTextField(
                    value = move.amount,
                    onValueChange = viewModel::onAmountChange,
                    label = stringResource(R.string.feature_piggybanks_amount),
                    suffix = summary.currencyCode,
                    keyboard = DfKeyboard.Decimal,
                    helper = helper,
                    error = when (val error = move.error) {
                        MoveMoneyError.Invalid -> stringResource(R.string.feature_piggybanks_error_amount)
                        is MoveMoneyError.Insufficient -> stringResource(
                            R.string.feature_piggybanks_error_insufficient,
                            formatMoney(error.available, summary.currencyCode),
                        )
                        null -> null
                    },
                    onDone = viewModel::confirm,
                )
            }
        }
    }
}
