package com.salco.dragonfly.feature.settings

import com.salco.dragonfly.core.designsystem.component.DfMenuGroup
import com.salco.dragonfly.core.designsystem.component.DfMenuItem
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfSectionHeader
import com.salco.dragonfly.core.designsystem.component.DfSwitch
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.component.DropdownField
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.VisibilityOff
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
import com.salco.dragonfly.core.designsystem.theme.DragonflyTheme
import com.salco.dragonfly.core.model.SupportedCurrencyCodes
import com.salco.dragonfly.core.model.UserPreferences
import com.salco.dragonfly.core.ui.rememberNotificationPermission
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
    onReportsClick: () -> Unit,
    onSyncClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lockAvailable = remember(context) { isAppLockAvailable(context) }
    val notificationPermission = rememberNotificationPermission { granted ->
        if (granted) viewModel.setRecurringNotificationsEnabled(true)
    }

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
        notificationsAllowed = notificationPermission.granted,
        onRecurringNotificationsChange = { enabled ->
            viewModel.setRecurringNotificationsEnabled(enabled)
            if (enabled && !notificationPermission.granted) notificationPermission.request()
        },
        onAllowNotifications = notificationPermission::request,
        onHideInRecentsChange = viewModel::setHideInRecents,
        onCategoriesClick = onCategoriesClick,
        onTagsClick = onTagsClick,
        onBillsClick = onBillsClick,
        onPiggyBanksClick = onPiggyBanksClick,
        onRecurringClick = onRecurringClick,
        onRulesClick = onRulesClick,
        onBackupClick = onBackupClick,
        onReportsClick = onReportsClick,
        onSyncClick = onSyncClick,
    )
}

@Composable
internal fun SettingsContent(
    uiState: SettingsUiState,
    lockAvailable: Boolean,
    onAppLockChange: (Boolean) -> Unit,
    onDefaultCurrencyChange: (String) -> Unit,
    notificationsAllowed: Boolean = true,
    onRecurringNotificationsChange: (Boolean) -> Unit = {},
    onAllowNotifications: () -> Unit = {},
    onHideInRecentsChange: (Boolean) -> Unit = {},
    onCategoriesClick: () -> Unit = {},
    onTagsClick: () -> Unit = {},
    onBillsClick: () -> Unit = {},
    onPiggyBanksClick: () -> Unit = {},
    onRecurringClick: () -> Unit = {},
    onRulesClick: () -> Unit = {},
    onBackupClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onSyncClick: () -> Unit = {},
) {
    DfScaffold(
        title = stringResource(R.string.feature_settings_title),
        subtitle = stringResource(R.string.feature_settings_subtitle),
    ) { padding ->
        val preferences = (uiState as? SettingsUiState.Success)?.preferences ?: return@DfScaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            DfSectionHeader(stringResource(R.string.feature_settings_account))
            DfMenuGroup {
                DfMenuItem(
                    title = stringResource(R.string.feature_settings_sync),
                    subtitle = stringResource(R.string.feature_settings_sync_description),
                    icon = Icons.Outlined.Sync,
                    onClick = onSyncClick,
                )
            }

            DfSectionHeader(stringResource(R.string.feature_settings_analysis))
            DfMenuGroup {
                DfMenuItem(stringResource(R.string.feature_settings_reports), icon = Icons.Outlined.BarChart, onClick = onReportsClick)
            }

            DfSectionHeader(stringResource(R.string.feature_settings_planning))
            DfMenuGroup {
                DfMenuItem(stringResource(R.string.feature_settings_bills), icon = Icons.AutoMirrored.Outlined.EventNote, onClick = onBillsClick)
                DfMenuItem(stringResource(R.string.feature_settings_piggy_banks), icon = Icons.Outlined.Savings, onClick = onPiggyBanksClick, divider = true)
                DfMenuItem(stringResource(R.string.feature_settings_recurring), icon = Icons.Outlined.EventRepeat, onClick = onRecurringClick, divider = true)
            }

            DfSectionHeader(stringResource(R.string.feature_settings_organization))
            DfMenuGroup {
                DfMenuItem(stringResource(R.string.feature_settings_categories), icon = Icons.Outlined.Category, onClick = onCategoriesClick)
                DfMenuItem(stringResource(R.string.feature_settings_tags), icon = Icons.Outlined.Sell, onClick = onTagsClick, divider = true)
                DfMenuItem(stringResource(R.string.feature_settings_rules), icon = Icons.Outlined.AutoFixHigh, onClick = onRulesClick, divider = true)
            }

            DfSectionHeader(stringResource(R.string.feature_settings_notifications))
            val blocked = preferences.recurringNotificationsEnabled && !notificationsAllowed
            DfMenuGroup {
                DfMenuItem(
                    title = stringResource(R.string.feature_settings_recurring_notifications),
                    subtitle = stringResource(
                        if (blocked) R.string.feature_settings_notifications_blocked
                        else R.string.feature_settings_recurring_notifications_description,
                    ),
                    subtitleTone = if (blocked) DfTone.Negative else DfTone.Secondary,
                    icon = Icons.Outlined.Notifications,
                    onClick = if (blocked) onAllowNotifications else null,
                    trailing = {
                        DfSwitch(
                            checked = preferences.recurringNotificationsEnabled,
                            onCheckedChange = onRecurringNotificationsChange,
                        )
                    },
                )
            }

            DfSectionHeader(stringResource(R.string.feature_settings_security))
            DfMenuGroup {
                DfMenuItem(
                    title = stringResource(R.string.feature_settings_app_lock),
                    subtitle = stringResource(
                        if (lockAvailable) R.string.feature_settings_app_lock_description
                        else R.string.feature_settings_app_lock_unavailable,
                    ),
                    icon = Icons.Outlined.Fingerprint,
                    trailing = {
                        DfSwitch(
                            checked = preferences.appLockEnabled,
                            onCheckedChange = onAppLockChange,
                            enabled = lockAvailable || preferences.appLockEnabled,
                        )
                    },
                )
                DfMenuItem(
                    title = stringResource(R.string.feature_settings_hide_in_recents),
                    subtitle = stringResource(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) R.string.feature_settings_hide_in_recents_description
                        else R.string.feature_settings_hide_in_recents_description_legacy,
                    ),
                    icon = Icons.Outlined.VisibilityOff,
                    onClick = { onHideInRecentsChange(!preferences.hideInRecents) },
                    divider = true,
                    trailing = { DfSwitch(checked = preferences.hideInRecents, onCheckedChange = onHideInRecentsChange) },
                )
            }

            DfSectionHeader(stringResource(R.string.feature_settings_general))
            DfMenuGroup {
                DfMenuItem(stringResource(R.string.feature_settings_backup), icon = Icons.Outlined.Backup, onClick = onBackupClick)
            }
            DropdownField(
                label = stringResource(R.string.feature_settings_default_currency),
                options = SupportedCurrencyCodes,
                selected = preferences.defaultCurrencyCode,
                optionLabel = { it },
                onSelected = onDefaultCurrencyChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            )
        }
    }
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
