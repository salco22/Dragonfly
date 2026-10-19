package com.salco.dragonfly.feature.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.component.DfButton
import com.salco.dragonfly.core.designsystem.component.DfButtonStyle
import com.salco.dragonfly.core.designsystem.component.DfCard
import com.salco.dragonfly.core.designsystem.component.DfConfirmDialog
import com.salco.dragonfly.core.designsystem.component.DfEmptyState
import com.salco.dragonfly.core.designsystem.component.DfFormColumn
import com.salco.dragonfly.core.designsystem.component.DfKeyboard
import com.salco.dragonfly.core.designsystem.component.DfLinearLoading
import com.salco.dragonfly.core.designsystem.component.DfLoading
import com.salco.dragonfly.core.designsystem.component.DfScaffold
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTextField
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.theme.DfTheme
import com.salco.dragonfly.core.designsystem.theme.DragonflyTheme
import com.salco.dragonfly.core.model.SyncAccount
import com.salco.dragonfly.core.model.SyncError
import com.salco.dragonfly.core.model.SyncStatus
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun SyncScreen(onBack: () -> Unit, viewModel: SyncViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SyncContent(uiState = uiState, actions = rememberSyncActions(viewModel, onBack))
}

/** Callbacks wired to [viewModel], including the Google account picker when it is set up. */
@Composable
internal fun rememberSyncActions(viewModel: SyncViewModel, onBack: () -> Unit = {}): SyncActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleClientId = viewModel.googleClientId
    return SyncActions(
        onBack = onBack,
        onGoogle = googleClientId?.let { clientId ->
            { scope.launch { viewModel.onGoogleResult(requestGoogleIdToken(context, clientId)) } }
        },
        onEmailChange = viewModel::onEmailChange,
        onSendCode = viewModel::sendCode,
        onCodeChange = viewModel::onCodeChange,
        onVerify = viewModel::verifyCode,
        onChangeEmail = viewModel::changeEmail,
        onSyncNow = viewModel::syncNow,
        onSignOut = viewModel::signOut,
        onMessageShown = viewModel::messageShown,
    )
}

internal data class SyncActions(
    val onBack: () -> Unit = {},
    /** Null when Google sign-in is not set up. */
    val onGoogle: (() -> Unit)? = null,
    val onEmailChange: (String) -> Unit = {},
    val onSendCode: () -> Unit = {},
    val onCodeChange: (String) -> Unit = {},
    val onVerify: () -> Unit = {},
    val onChangeEmail: () -> Unit = {},
    val onSyncNow: () -> Unit = {},
    val onSignOut: () -> Unit = {},
    val onMessageShown: () -> Unit = {},
)

@Composable
internal fun SyncContent(uiState: SyncUiState, actions: SyncActions) {
    val form = uiState.form
    DfScaffold(
        title = stringResource(R.string.feature_sync_title),
        onBack = actions.onBack,
        // Sign-in errors show under the fields; the rest as a message.
        message = form.error?.takeUnless { uiState.account is SyncAccount.SignedOut }?.let { stringResource(it.messageRes()) },
        onMessageShown = actions.onMessageShown,
    ) { padding ->
        when (val account = uiState.account) {
            SyncAccount.Loading -> DfLoading(Modifier.padding(padding))
            SyncAccount.NotConfigured -> DfEmptyState(
                icon = Icons.Outlined.CloudOff,
                title = stringResource(R.string.feature_sync_not_configured_title),
                message = stringResource(R.string.feature_sync_not_configured_message),
                modifier = Modifier.padding(padding),
            )
            SyncAccount.SignedOut -> DfFormColumn(Modifier.padding(padding)) { SignInForm(form, actions) }
            is SyncAccount.SignedIn -> DfFormColumn(Modifier.padding(padding)) {
                SignedIn(account, uiState.status, busy = form.busy, actions)
            }
        }
    }
}

@Composable
internal fun SignInForm(form: SignInForm, actions: SyncActions) {
    if (form.busy) DfLinearLoading()
    DfText(stringResource(R.string.feature_sync_intro), tone = DfTone.Secondary)
    val onGoogle = actions.onGoogle
    if (onGoogle != null && !form.codeSent) {
        DfButton(stringResource(R.string.feature_sync_google), onGoogle, enabled = !form.busy, fillWidth = true)
        form.googleError?.let {
            DfText(
                stringResource(if (it == GoogleError.NoAccount) R.string.feature_sync_google_no_account else R.string.feature_sync_google_failed),
                style = DfTheme.typography.bodySmall,
                tone = DfTone.Negative,
            )
        }
        DfText(stringResource(R.string.feature_sync_or_email), style = DfTheme.typography.caption, tone = DfTone.Tertiary)
    }
    if (!form.codeSent) {
        DfTextField(
            value = form.email,
            onValueChange = actions.onEmailChange,
            label = stringResource(R.string.feature_sync_email),
            keyboard = DfKeyboard.Email,
            error = when {
                form.emailError -> stringResource(R.string.feature_sync_error_email)
                form.error != null -> stringResource(form.error.messageRes())
                else -> null
            },
            onDone = actions.onSendCode,
        )
        DfButton(
            stringResource(R.string.feature_sync_send_code),
            actions.onSendCode,
            enabled = !form.busy,
            fillWidth = true,
            style = if (onGoogle != null) DfButtonStyle.Secondary else DfButtonStyle.Primary,
        )
    } else {
        DfText(stringResource(R.string.feature_sync_code_sent, form.email.trim()))
        DfTextField(
            value = form.code,
            onValueChange = actions.onCodeChange,
            label = stringResource(R.string.feature_sync_code),
            keyboard = DfKeyboard.Number,
            large = true,
            error = form.error?.let { stringResource(it.messageRes()) },
            onDone = actions.onVerify,
        )
        DfButton(stringResource(R.string.feature_sync_sign_in), actions.onVerify, enabled = !form.busy, fillWidth = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DfButton(stringResource(R.string.feature_sync_change_email), actions.onChangeEmail, style = DfButtonStyle.Text)
            DfButton(stringResource(R.string.feature_sync_resend), actions.onSendCode, style = DfButtonStyle.Text, enabled = !form.busy)
        }
    }
}

@Composable
private fun SignedIn(account: SyncAccount.SignedIn, status: SyncStatus, busy: Boolean, actions: SyncActions) {
    var confirmSignOut by remember { mutableStateOf(false) }
    val running = busy || status.running
    if (running) DfLinearLoading()
    DfCard(highlighted = true, spacing = 4.dp) {
        DfText(stringResource(R.string.feature_sync_signed_in_as), style = DfTheme.typography.caption, tone = DfTone.Secondary)
        DfText(account.email.orEmpty(), style = DfTheme.typography.title)
    }
    val lastSyncAt = status.lastSyncAt
    DfCard(spacing = 6.dp) {
        DfText(
            when {
                running -> stringResource(R.string.feature_sync_syncing)
                lastSyncAt != null -> stringResource(R.string.feature_sync_last_sync, lastSyncAt.formatted())
                else -> stringResource(R.string.feature_sync_never)
            },
            style = DfTheme.typography.subtitle,
        )
        DfText(
            if (status.pendingChanges > 0) {
                pluralStringResource(R.plurals.feature_sync_pending, status.pendingChanges, status.pendingChanges)
            } else {
                stringResource(R.string.feature_sync_up_to_date)
            },
            style = DfTheme.typography.bodySmall,
            tone = DfTone.Secondary,
        )
        status.lastError?.let { DfText(stringResource(it.messageRes()), style = DfTheme.typography.bodySmall, tone = DfTone.Negative) }
    }
    DfButton(stringResource(R.string.feature_sync_now), actions.onSyncNow, icon = Icons.Outlined.Sync, enabled = !running, fillWidth = true)
    DfText(stringResource(R.string.feature_sync_automatic), style = DfTheme.typography.caption, tone = DfTone.Tertiary)
    DfButton(stringResource(R.string.feature_sync_sign_out), { confirmSignOut = true }, style = DfButtonStyle.Danger, enabled = !running)

    if (confirmSignOut) {
        DfConfirmDialog(
            title = stringResource(R.string.feature_sync_sign_out_title),
            message = stringResource(R.string.feature_sync_sign_out_message),
            confirmText = stringResource(R.string.feature_sync_sign_out),
            dismissText = stringResource(R.string.feature_sync_cancel),
            onConfirm = {
                confirmSignOut = false
                actions.onSignOut()
            },
            onDismiss = { confirmSignOut = false },
        )
    }
}

private fun Long.formatted(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))

private fun SyncError.messageRes(): Int = when (this) {
    SyncError.Offline -> R.string.feature_sync_error_offline
    SyncError.InvalidCode -> R.string.feature_sync_error_invalid_code
    SyncError.RateLimited -> R.string.feature_sync_error_rate_limited
    SyncError.SignedOut -> R.string.feature_sync_error_signed_out
    SyncError.Server -> R.string.feature_sync_error_server
}

@Preview
@Composable
private fun SyncSignedInPreview() {
    DragonflyTheme {
        SyncContent(
            uiState = SyncUiState(
                account = SyncAccount.SignedIn("luca@example.com"),
                status = SyncStatus(pendingChanges = 3, lastSyncAt = 1_792_000_000_000),
            ),
            actions = SyncActions(),
        )
    }
}
