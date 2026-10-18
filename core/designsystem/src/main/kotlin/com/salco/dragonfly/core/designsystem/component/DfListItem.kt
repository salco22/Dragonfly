package com.salco.dragonfly.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.salco.dragonfly.core.designsystem.theme.DfTheme

/**
 * A row of a list: optional round icon, a headline with an optional supporting line and a
 * trailing slot (an amount, a switch, a chevron…).
 */
@Composable
fun DfListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    supportingTone: DfTone = DfTone.Secondary,
    icon: ImageVector? = null,
    iconTone: DfTone = DfTone.Secondary,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    supportingContent: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(enabled = enabled, onClick = onClick) else it }
            .padding(horizontal = DfTheme.spacing.screen, vertical = DfTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DfTheme.spacing.m),
    ) {
        if (icon != null) {
            Box(
                Modifier
                    .size(36.dp)
                    .background(DfTheme.colors.surfaceMuted, CircleShape),
                contentAlignment = Alignment.Center,
            ) { DfIcon(icon, contentDescription = null, tone = iconTone, modifier = Modifier.size(18.dp)) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            DfText(headline, style = DfTheme.typography.subtitle, tone = if (enabled) DfTone.Primary else DfTone.Tertiary, maxLines = 2)
            if (supportingContent != null) {
                supportingContent()
            } else if (supporting != null) {
                DfText(supporting, style = DfTheme.typography.bodySmall, tone = supportingTone)
            }
        }
        trailing?.invoke()
    }
}

/** A small uppercase title above a group of items, with an optional action on the right. */
@Composable
fun DfSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(start = DfTheme.spacing.screen, end = DfTheme.spacing.s, top = DfTheme.spacing.xl, bottom = DfTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DfText(text.uppercase(), style = DfTheme.typography.overline, tone = DfTone.Tertiary, modifier = Modifier.weight(1f))
        if (actionText != null && onAction != null) {
            DfButton(actionText, onAction, style = DfButtonStyle.Text)
        }
    }
}

@Composable
fun DfDivider(modifier: Modifier = Modifier, inset: Boolean = false) {
    HorizontalDivider(
        modifier.padding(start = if (inset) DfTheme.spacing.screen else 0.dp),
        thickness = 1.dp,
        color = DfTheme.colors.border,
    )
}
