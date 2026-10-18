package com.salco.dragonfly.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salco.dragonfly.core.designsystem.theme.DfTheme

/** Centered message for screens without content, with an optional action. */
@Composable
fun DfEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .background(DfTheme.colors.surfaceMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) { DfIcon(icon, contentDescription = null, tone = DfTone.Secondary, modifier = Modifier.size(28.dp)) }
        DfText(title, style = DfTheme.typography.title, textAlign = TextAlign.Center)
        DfText(message, style = DfTheme.typography.bodySmall, tone = DfTone.Secondary, textAlign = TextAlign.Center)
        if (actionText != null && onAction != null) {
            DfButton(actionText, onAction, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** Kept for existing callers; prefer [DfEmptyState]. */
@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) =
    DfEmptyState(icon, title, message, modifier)

@Composable
fun DfLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = DfTheme.colors.accent, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
    }
}

/** A thin indeterminate bar for work in progress. */
@Composable
fun DfLinearLoading(modifier: Modifier = Modifier) {
    LinearProgressIndicator(modifier.fillMaxWidth(), color = DfTheme.colors.accent, trackColor = DfTheme.colors.surfaceMuted)
}

/** A dialog with free content and up to three actions. */
@Composable
fun DfDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String? = null,
    destructive: Boolean = false,
    extraText: String? = null,
    onExtra: (() -> Unit)? = null,
    extraDestructive: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DfTheme.colors.surface,
        title = { DfText(title, style = DfTheme.typography.title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content) },
        confirmButton = {
            DfButton(confirmText, onConfirm, style = if (destructive) DfButtonStyle.Danger else DfButtonStyle.Text)
        },
        dismissButton = {
            Row {
                if (extraText != null && onExtra != null) {
                    DfButton(extraText, onExtra, style = if (extraDestructive) DfButtonStyle.Danger else DfButtonStyle.Text)
                }
                if (dismissText != null) DfButton(dismissText, onDismiss, style = DfButtonStyle.Text)
            }
        },
    )
}

/** A yes/no question, e.g. before deleting something. */
@Composable
fun DfConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    DfDialog(title, onDismiss, confirmText, onConfirm, dismissText, destructive) {
        DfText(message, style = DfTheme.typography.body, tone = DfTone.Secondary)
    }
}

/** The usual body of a form or detail screen: scrollable, with screen margins and even spacing. */
@Composable
fun DfFormColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = DfTheme.spacing.screen, vertical = DfTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(DfTheme.spacing.l),
        content = content,
    )
}
