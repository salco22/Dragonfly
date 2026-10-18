package com.salco.dragonfly.feature.settings

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.component.DropdownField
import com.salco.dragonfly.core.designsystem.theme.DragonflyTheme
import com.salco.dragonfly.core.model.SupportedCurrencyCodes
import com.salco.dragonfly.core.model.UserPreferences
import com.salco.dragonfly.feature.lock.isAppLockAvailable
import com.salco.dragonfly.feature.lock.showAppLockPrompt

@Composable
internal fun SettingsScreen(
    onCategoriesClick: () -> Unit,
    onTagsClick: () -> Unit,
    onBillsClick: () -> Unit,
    onPiggyBanksClick: () -> Unit,
    onRecurringClick: () -> Unit,
    onRulesClick: () -> Unit,
    onBackupClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lockAvailable = remember(context) { isAppLockAvailable(context) }

    SettingsContent(
        uiState = uiState,
        lockAvailable = lockAvailable,
        onAppLockChange = { enabled ->
            val activity = context.findFragmentActivity()
            if (enabled && activity != null) {
                // Confirm the user can unlock before turning the lock on.
                showAppLockPrompt(activity, onSuccess = { viewModel.setAppLockEnabled(true) })
            } else {
                viewModel.setAppLockEnabled(false)
            }
        },
        onDefaultCurrencyChange = viewModel::setDefaultCurrency,
        onCategoriesClick = onCategoriesClick,
        onTagsClick = onTagsClick,
        onBillsClick = onBillsClick,
        onPiggyBanksClick = onPiggyBanksClick,
        onRecurringClick = onRecurringClick,
        onRulesClick = onRulesClick,
        onBackupClick = onBackupClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    uiState: SettingsUiState,
    lockAvailable: Boolean,
    onAppLockChange: (Boolean) -> Unit,
    onDefaultCurrencyChange: (String) -> Unit,
    onCategoriesClick: () -> Unit = {},
    onTagsClick: () -> Unit = {},
    onBillsClick: () -> Unit = {},
    onPiggyBanksClick: () -> Unit = {},
    onRecurringClick: () -> Unit = {},
    onRulesClick: () -> Unit = {},
    onBackupClick: () -> Unit = {},
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.feature_settings_title)) }) },
    ) { padding ->
        val preferences = (uiState as? SettingsUiState.Success)?.preferences ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle(stringResource(R.string.feature_settings_planning))
            NavigationItem(Icons.AutoMirrored.Filled.EventNote, stringResource(R.string.feature_settings_bills), onBillsClick)
            NavigationItem(Icons.Filled.Savings, stringResource(R.string.feature_settings_piggy_banks), onPiggyBanksClick)
            NavigationItem(Icons.Filled.EventRepeat, stringResource(R.string.feature_settings_recurring), onRecurringClick)

            SectionTitle(stringResource(R.string.feature_settings_security))
            ListItem(
                headlineContent = { Text(stringResource(R.string.feature_settings_app_lock)) },
                supportingContent = {
                    Text(
                        stringResource(
                            if (lockAvailable) R.string.feature_settings_app_lock_description
                            else R.string.feature_settings_app_lock_unavailable,
                        ),
                    )
                },
                trailingContent = {
                    Switch(
                        checked = preferences.appLockEnabled,
                        onCheckedChange = onAppLockChange,
                        enabled = lockAvailable || preferences.appLockEnabled,
                    )
                },
            )

            SectionTitle(stringResource(R.string.feature_settings_organization))
            NavigationItem(Icons.Filled.Category, stringResource(R.string.feature_settings_categories), onCategoriesClick)
            NavigationItem(Icons.Filled.Sell, stringResource(R.string.feature_settings_tags), onTagsClick)
            NavigationItem(Icons.Filled.AutoFixHigh, stringResource(R.string.feature_settings_rules), onRulesClick)

            SectionTitle(stringResource(R.string.feature_settings_general))
            NavigationItem(Icons.Filled.Backup, stringResource(R.string.feature_settings_backup), onBackupClick)
            DropdownField(
                label = stringResource(R.string.feature_settings_default_currency),
                options = SupportedCurrencyCodes,
                selected = preferences.defaultCurrencyCode,
                optionLabel = { it },
                onSelected = onDefaultCurrencyChange,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun NavigationItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(label) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    DragonflyTheme {
        SettingsContent(
            uiState = SettingsUiState.Success(UserPreferences(appLockEnabled = true, defaultCurrencyCode = "EUR")),
            lockAvailable = true,
            onAppLockChange = {},
            onDefaultCurrencyChange = {},
        )
    }
}
