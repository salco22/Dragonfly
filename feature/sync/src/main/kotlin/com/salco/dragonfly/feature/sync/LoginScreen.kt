package com.salco.dragonfly.feature.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.component.DfButton
import com.salco.dragonfly.core.designsystem.component.DfButtonStyle
import com.salco.dragonfly.core.designsystem.component.DfSurface
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.theme.DfTheme
import com.salco.dragonfly.core.designsystem.theme.DragonflyTheme

/**
 * Shown at start until the user signs in or chooses to go on without an account. Signing in
 * replaces it with the app; the account is managed later from More.
 */
@Composable
fun LoginScreen(onSkip: () -> Unit, viewModel: SyncViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LoginContent(uiState.form, rememberSyncActions(viewModel), onSkip)
}

@Composable
internal fun LoginContent(form: SignInForm, actions: SyncActions, onSkip: () -> Unit) {
    DfSurface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = DfTheme.spacing.screen, vertical = DfTheme.spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AppIcon(Modifier.size(64.dp))
                DfText(stringResource(R.string.feature_sync_login_title), style = DfTheme.typography.display)
                DfText(stringResource(R.string.feature_sync_login_subtitle), style = DfTheme.typography.title, tone = DfTone.Secondary)
                SignInForm(form, actions)
                DfButton(
                    stringResource(R.string.feature_sync_login_skip),
                    onSkip,
                    style = DfButtonStyle.Text,
                    enabled = !form.busy,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                DfText(
                    stringResource(R.string.feature_sync_login_skip_hint),
                    style = DfTheme.typography.caption,
                    tone = DfTone.Tertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** The launcher icon, so the login screen looks like Dragonfly whatever icon the app ships with. */
@Composable
private fun AppIcon(modifier: Modifier) {
    val context = LocalContext.current
    val icon = remember(context) {
        runCatching { context.packageManager.getApplicationIcon(context.packageName).toBitmap().asImageBitmap() }.getOrNull()
    }
    if (icon != null) Image(icon, contentDescription = null, modifier = modifier.clip(CircleShape))
}

@Preview
@Composable
private fun LoginContentPreview() {
    DragonflyTheme {
        LoginContent(SignInForm(), SyncActions(onGoogle = {}), onSkip = {})
    }
}
