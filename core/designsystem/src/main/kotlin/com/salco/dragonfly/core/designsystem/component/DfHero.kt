package com.salco.dragonfly.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.salco.dragonfly.core.designsystem.theme.DfTheme

/**
 * The dark green card holding the one number that matters most on a screen: a small [title],
 * a large [value] (or several, one per currency) and free [content] below, such as a chart, a
 * bar or a lime note ([DfHeroNote]). [trailing] sits next to the title.
 */
@Composable
fun DfHeroCard(
    title: String,
    values: List<String>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val shape = RoundedCornerShape(DfTheme.spacing.radius)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = shape,
        color = DfTheme.colors.hero,
        contentColor = DfTheme.colors.onHero,
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DfText(title, style = DfTheme.typography.bodySmall, color = DfTheme.colors.onHeroSecondary, modifier = Modifier.weight(1f))
                trailing?.invoke()
            }
            values.forEach { DfText(it, style = DfTheme.typography.display, color = DfTheme.colors.onHero, maxLines = 1) }
            content()
        }
    }
}

@Composable
fun DfHeroCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) = DfHeroCard(title, listOf(value), modifier, onClick, trailing, content)

/** Secondary text on a hero card; [highlighted] draws it in lime. */
@Composable
fun DfHeroNote(text: String, modifier: Modifier = Modifier, highlighted: Boolean = false) {
    DfText(
        text,
        modifier = modifier,
        style = DfTheme.typography.caption,
        color = if (highlighted) DfTheme.colors.highlight else DfTheme.colors.onHeroSecondary,
    )
}

/** Two notes on one line of a hero card, e.g. "Spent €X" and "of €Y". */
@Composable
fun DfHeroNotes(start: String, end: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        DfHeroNote(start, Modifier.weight(1f))
        DfHeroNote(end)
    }
}

/** A lime progress bar on a hero card's dark track. */
@Composable
fun DfHeroProgressBar(progress: Float, modifier: Modifier = Modifier) {
    DfTrackBar(progress, DfTheme.colors.highlight, DfTheme.colors.heroTrack, modifier)
}

/**
 * A bar split into [shares] (e.g. the balance of each account), drawn with small gaps in the
 * hero palette. Shares need not add up to 1; non-positive ones are skipped.
 */
@Composable
fun DfSegmentedBar(
    shares: List<Float>,
    modifier: Modifier = Modifier,
    colors: List<Color> = DfSegmentedBarDefaults.heroColors(),
) {
    val visible = shares.filter { it > 0f }
    if (visible.isEmpty()) return
    Row(modifier.fillMaxWidth().height(8.dp)) {
        visible.forEachIndexed { i, share ->
            if (i > 0) Spacer(Modifier.width(4.dp))
            Spacer(
                Modifier
                    .weight(share)
                    .height(8.dp)
                    .background(colors[i % colors.size], RoundedCornerShape(50)),
            )
        }
    }
}

object DfSegmentedBarDefaults {
    /** Lime, sage, sand and white: readable on the dark hero card. */
    @Composable
    fun heroColors(): List<Color> = listOf(
        DfTheme.colors.highlight,
        Color(0xFF8FA88F),
        DfTheme.colors.chartExpense,
        DfTheme.colors.onHero,
        DfTheme.colors.onHeroSecondary,
    )
}
