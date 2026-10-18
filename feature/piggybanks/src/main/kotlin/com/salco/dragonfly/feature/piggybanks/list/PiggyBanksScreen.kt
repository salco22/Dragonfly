package com.salco.dragonfly.feature.piggybanks.list

import com.salco.dragonfly.core.designsystem.component.DfCard
import com.salco.dragonfly.core.designsystem.component.DfEmptyState
import com.salco.dragonfly.core.designsystem.component.DfFab
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.model.formatMoney
import com.salco.dragonfly.feature.piggybanks.PiggyBankCard
import com.salco.dragonfly.feature.piggybanks.R

@Composable
internal fun PiggyBanksScreen(
    onBack: () -> Unit,
    onPiggyBankClick: (Long) -> Unit,
    onAdd: () -> Unit,
    viewModel: PiggyBanksViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DfScaffold(
        title = stringResource(R.string.feature_piggybanks_title),
        onBack = onBack,
        floatingAction = { DfFab(stringResource(R.string.feature_piggybanks_add), Icons.Filled.Add, onAdd) },
    ) { padding ->
        val state = uiState as? PiggyBanksUiState.Success
        when {
            state == null -> DfLoading(Modifier.padding(padding))
            state.items.isEmpty() -> DfEmptyState(
                icon = Icons.Outlined.Savings,
                title = stringResource(R.string.feature_piggybanks_empty_title),
                message = stringResource(R.string.feature_piggybanks_empty_message),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    DfCard(highlighted = true, spacing = 2.dp) {
                        DfText(stringResource(R.string.feature_piggybanks_saved_total), style = DfTheme.typography.caption, tone = DfTone.Secondary)
                        state.totalSaved.forEach { (currency, amount) ->
                            DfText(formatMoney(amount, currency), style = DfTheme.typography.headline)
                        }
                    }
                }
                items(state.items, key = { it.piggyBank.id }) { summary ->
                    PiggyBankCard(summary, state.today, onClick = { onPiggyBankClick(summary.piggyBank.id) })
                }
            }
        }
    }
}
