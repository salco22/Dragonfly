package com.salco.dragonfly.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.salco.dragonfly.core.designsystem.theme.DfTheme

/** How a budget is going, which sets the color of its bar and status. */
enum class DfBudgetStatus { OnTrack, NearLimit, Over }

/**
 * A budget: icon, [name] and "spent / limit" on the first line, a progress bar, then the
 * [statusText] on the left and the [remainingText] on the right, both colored by [status].
 */
@Composable
fun DfBudgetCard(
    icon: ImageVector,
    name: String,
    amounts: String,
    progress: Float,
    status: DfBudgetStatus,
    statusText: String,
    remainingText: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val (tone, tile) = when (status) {
        DfBudgetStatus.OnTrack -> DfTone.Positive to DfTheme.colors.surfaceMuted
        DfBudgetStatus.NearLimit -> DfTone.Warning to DfTheme.colors.warningContainer
        DfBudgetStatus.Over -> DfTone.Negative to DfTheme.colors.warningContainer
    }
    DfCard(modifier = modifier, onClick = onClick, padding = DfTheme.spacing.l, spacing = DfTheme.spacing.m) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DfTheme.spacing.m)) {
            Box(
                Modifier
                    .size(36.dp)
                    .background(tile, RoundedCornerShape(DfTheme.spacing.radiusIcon - 2.dp)),
                contentAlignment = Alignment.Center,
            ) { DfIcon(icon, contentDescription = null, tone = if (status == DfBudgetStatus.OnTrack) DfTone.Positive else tone, modifier = Modifier.size(18.dp)) }
            DfText(name, style = DfTheme.typography.subtitle, modifier = Modifier.weight(1f), maxLines = 1)
            DfText(amounts, style = DfTheme.typography.bodySmall, maxLines = 1)
        }
        DfProgressBar(progress, tone = tone)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            DfText(statusText, style = DfTheme.typography.caption, tone = if (status == DfBudgetStatus.OnTrack) DfTone.Secondary else tone, modifier = Modifier.weight(1f))
            DfText(remainingText, style = DfTheme.typography.caption, tone = tone)
        }
    }
}
