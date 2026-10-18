package com.salco.dragonfly.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.salco.dragonfly.core.designsystem.theme.DfTheme

enum class DfButtonStyle {
    /** The main action of a screen, filled with the accent. */
    Primary,

    /** Secondary actions, outlined. */
    Secondary,

    /** Low-emphasis actions, text only. */
    Text,

    /** Destructive actions, text in the negative color. */
    Danger,
}

@Composable
fun DfButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DfButtonStyle = DfButtonStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
) {
    val shape = RoundedCornerShape(DfTheme.spacing.radiusSmall)
    val sized = if (fillWidth) modifier.fillMaxWidth().height(52.dp) else modifier.height(if (style == DfButtonStyle.Text || style == DfButtonStyle.Danger) 40.dp else 48.dp)
    val padding = PaddingValues(horizontal = 18.dp)
    val content: @Composable () -> Unit = {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = DfTheme.typography.label)
    }
    when (style) {
        DfButtonStyle.Primary -> Button(
            onClick = onClick, modifier = sized, enabled = enabled, shape = shape, contentPadding = padding,
            colors = ButtonDefaults.buttonColors(
                containerColor = DfTheme.colors.accent,
                contentColor = DfTheme.colors.onAccent,
                disabledContainerColor = DfTheme.colors.surfaceMuted,
                disabledContentColor = DfTheme.colors.textTertiary,
            ),
            elevation = null,
        ) { content() }
        DfButtonStyle.Secondary -> OutlinedButton(
            onClick = onClick, modifier = sized, enabled = enabled, shape = shape, contentPadding = padding,
            border = BorderStroke(1.dp, DfTheme.colors.border),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DfTheme.colors.textPrimary, disabledContentColor = DfTheme.colors.textTertiary),
        ) { content() }
        DfButtonStyle.Text, DfButtonStyle.Danger -> TextButton(
            onClick = onClick, modifier = sized, enabled = enabled, shape = shape, contentPadding = PaddingValues(horizontal = 12.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (style == DfButtonStyle.Danger) DfTheme.colors.negative else DfTheme.colors.accent,
                disabledContentColor = DfTheme.colors.textTertiary,
            ),
        ) { content() }
    }
}

/** The floating "add" action of a list screen. */
@Composable
fun DfFab(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        icon = { Icon(icon, contentDescription = null) },
        text = { Text(text, style = DfTheme.typography.label) },
        shape = RoundedCornerShape(DfTheme.spacing.radius),
        containerColor = DfTheme.colors.textPrimary,
        contentColor = DfTheme.colors.background,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
    )
}
