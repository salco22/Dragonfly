package com.salco.dragonfly.feature.rules.list

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.component.EmptyState
import com.salco.dragonfly.core.model.ActionType
import com.salco.dragonfly.feature.rules.R
import com.salco.dragonfly.feature.rules.summary

@OptIn(ExperimentalMaterial3Api::class)
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
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalContext.current.resources
    LaunchedEffect(applied) {
        applied?.let {
            snackbar.showSnackbar(resources.getQuantityString(R.plurals.feature_rules_applied, it, it))
            viewModel.appliedShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_rules_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.feature_rules_back)) }
                },
                actions = {
                    IconButton(onClick = { confirmApply = true }) {
                        Icon(Icons.Filled.PlaylistPlay, stringResource(R.string.feature_rules_apply_all))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.feature_rules_add)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val state = uiState as? RulesUiState.Success
        when {
            state == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
            state.rules.isEmpty() -> EmptyState(
                icon = Icons.Filled.AutoFixHigh,
                title = stringResource(R.string.feature_rules_empty_title),
                message = stringResource(R.string.feature_rules_empty_message),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.rules, key = { it.id }) { rule ->
                    OutlinedCard(Modifier.fillMaxWidth().clickable { onRuleClick(rule.id) }) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(rule.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    rule.summary(state.currencyCode) { type, value ->
                                        when (type) {
                                            ActionType.SET_BUDGET -> state.budgetNames[value]
                                            ActionType.LINK_BILL -> state.billNames[value]
                                            else -> null
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(checked = rule.active, onCheckedChange = { viewModel.setActive(rule, it) })
                        }
                    }
                }
            }
        }
    }

    if (confirmApply) {
        AlertDialog(
            onDismissRequest = { confirmApply = false },
            title = { Text(stringResource(R.string.feature_rules_apply_all_title)) },
            text = { Text(stringResource(R.string.feature_rules_apply_all_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmApply = false
                    viewModel.applyAll()
                }) { Text(stringResource(R.string.feature_rules_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmApply = false }) { Text(stringResource(R.string.feature_rules_cancel)) } },
        )
    }
}
