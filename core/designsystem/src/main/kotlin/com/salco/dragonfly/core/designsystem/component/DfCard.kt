package com.salco.dragonfly.core.designsystem.component

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
import androidx.compose.ui.unit.Dp
import com.salco.dragonfly.core.designsystem.theme.DfTheme

/**
 * A flat white container without border or shadow. [highlighted] cards use the sage fill, for
 * secondary summaries; the most important number of a screen goes in a [DfHeroCard].
 */
@Composable
fun DfCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    padding: Dp = DfTheme.spacing.screen,
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
    ) {
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
    }
}
