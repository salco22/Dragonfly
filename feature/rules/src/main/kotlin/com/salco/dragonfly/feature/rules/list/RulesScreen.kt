package com.salco.dragonfly.feature.rules.list

import com.salco.dragonfly.core.designsystem.component.DfCard
import com.salco.dragonfly.core.designsystem.component.DfConfirmDialog
import com.salco.dragonfly.core.designsystem.component.DfEmptyState
import com.salco.dragonfly.core.designsystem.component.DfFab
import com.salco.dragonfly.core.designsystem.component.DfIconButton
import com.salco.dragonfly.core.designsystem.component.DfLoading
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfSwitch
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.theme.DfTheme
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.model.ActionType
import com.salco.dragonfly.feature.rules.R
import com.salco.dragonfly.feature.rules.summary

@Composable
internal fun RulesScreen(
    onBack: () -> Unit,
    onRuleClick: (Long) -> Unit,
    onAdd: () -> Unit,
    viewModel: RulesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val applied by viewModel.applied.collectAsStateWithLifecycle()
    var confirmApply by remember { mutableStateOf(false) }

    DfScaffold(
        title = stringResource(R.string.feature_rules_title),
        onBack = onBack,
        actions = {
            DfIconButton(Icons.AutoMirrored.Outlined.PlaylistPlay, stringResource(R.string.feature_rules_apply_all), { confirmApply = true })
        },
        floatingAction = { DfFab(stringResource(R.string.feature_rules_add), Icons.Filled.Add, onAdd) },
        message = applied?.let { pluralStringResource(R.plurals.feature_rules_applied, it, it) },
        onMessageShown = viewModel::appliedShown,
    ) { padding ->
        val state = uiState as? RulesUiState.Success
        when {
            state == null -> DfLoading(Modifier.padding(padding))
            state.rules.isEmpty() -> DfEmptyState(
                icon = Icons.Outlined.AutoFixHigh,
                title = stringResource(R.string.feature_rules_empty_title),
                message = stringResource(R.string.feature_rules_empty_message),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.rules, key = { it.id }) { rule ->
                    DfCard(onClick = { onRuleClick(rule.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                DfText(rule.name, style = DfTheme.typography.subtitle)
                                DfText(
                                    rule.summary(state.currencyCode) { type, value ->
                                        when (type) {
                                            ActionType.SET_BUDGET -> state.budgetNames[value]
                                            ActionType.LINK_BILL -> state.billNames[value]
                                            else -> null
                                        }
                                    },
                                    style = DfTheme.typography.bodySmall,
                                    tone = DfTone.Secondary,
                                )
                            }
                            DfSwitch(checked = rule.active, onCheckedChange = { viewModel.setActive(rule, it) })
                        }
                    }
                }
            }
        }
    }

    if (confirmApply) {
        DfConfirmDialog(
            title = stringResource(R.string.feature_rules_apply_all_title),
            message = stringResource(R.string.feature_rules_apply_all_message),
            confirmText = stringResource(R.string.feature_rules_confirm),
            dismissText = stringResource(R.string.feature_rules_cancel),
            onConfirm = {
                confirmApply = false
                viewModel.applyAll()
            },
            onDismiss = { confirmApply = false },
            destructive = false,
        )
    }
}
