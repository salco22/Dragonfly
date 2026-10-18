package com.salco.dragonfly.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.salco.dragonfly.core.designsystem.theme.DfTheme

/**
 * A flat container with a hairline border. [highlighted] cards use the light accent fill, for
 * the one summary that matters most on a screen.
 */
@Composable
fun DfCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    padding: Dp = DfTheme.spacing.l,
    spacing: Dp = DfTheme.spacing.s,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(DfTheme.spacing.radius)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = shape,
        color = if (highlighted) DfTheme.colors.accentMuted else DfTheme.colors.surface,
        contentColor = DfTheme.colors.textPrimary,
        border = if (highlighted) null else BorderStroke(1.dp, DfTheme.colors.border),
    ) {
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
    }
}
