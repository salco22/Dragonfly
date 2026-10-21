package com.salco.dragonfly.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.salco.dragonfly.core.designsystem.theme.DfTheme

/**
 * The header of a main screen: a large bold [title] with a short [subtitle] below and round
 * [actions] on the right.
 */
@Composable
fun DfPageHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = DfTheme.spacing.screen, end = DfTheme.spacing.screen, top = DfTheme.spacing.l, bottom = DfTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            DfText(title, style = DfTheme.typography.headline, maxLines = 1)
            subtitle?.let { DfText(it, style = DfTheme.typography.bodySmall, tone = DfTone.Secondary, maxLines = 1) }
        }
        CompositionLocalProvider(LocalCircleIconButtons provides true) {
            Row(horizontalArrangement = Arrangement.spacedBy(DfTheme.spacing.s), verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

/** A day label above the transactions of that date, e.g. "Oggi · 21 ottobre". */
@Composable
fun DfDateHeader(text: String, modifier: Modifier = Modifier) {
    DfText(
        text = text,
        style = DfTheme.typography.label,
        tone = DfTone.Primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = DfTheme.spacing.screen, end = DfTheme.spacing.screen, top = DfTheme.spacing.l, bottom = DfTheme.spacing.xs),
    )
}

/** A sage banner with an icon and a short informative or encouraging message. */
@Composable
fun DfInfoBanner(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(DfTheme.colors.accentMuted, RoundedCornerShape(DfTheme.spacing.radiusSmall))
            .padding(DfTheme.spacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DfTheme.spacing.m),
    ) {
        DfIcon(icon, contentDescription = null, tone = DfTone.Positive, modifier = Modifier.size(20.dp))
        DfText(text, style = DfTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
}

/** A white rounded group of [DfMenuItem]s separated by hairlines. */
@Composable
fun DfMenuGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(DfTheme.spacing.radius)
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = DfTheme.spacing.screen)
            .clip(shape)
            .background(DfTheme.colors.surface, shape)
            .padding(vertical = DfTheme.spacing.xs),
        content = content,
    )
}

/**
 * An entry of a [DfMenuGroup]: line icon, [title] with an optional [subtitle] and a chevron, or
 * a custom [trailing] such as a switch. [divider] draws the hairline above it.
 */
@Composable
fun DfMenuItem(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    subtitleTone: DfTone = DfTone.Secondary,
    onClick: (() -> Unit)? = null,
    divider: Boolean = false,
    trailing: (@Composable () -> Unit)? = if (onClick != null) {
        { DfIcon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tone = DfTone.Secondary) }
    } else {
        null
    },
) {
    Column(modifier.fillMaxWidth()) {
        if (divider) DfDivider(Modifier.padding(horizontal = DfTheme.spacing.l))
        Row(
            Modifier
                .fillMaxWidth()
                .let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(horizontal = DfTheme.spacing.l, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DfTheme.spacing.l),
        ) {
            if (icon != null) DfIcon(icon, contentDescription = null, tone = DfTone.Positive, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                DfText(title, style = DfTheme.typography.body, maxLines = 2)
                subtitle?.let { DfText(it, style = DfTheme.typography.caption, tone = subtitleTone) }
            }
            trailing?.let { Box { it() } }
        }
    }
}
