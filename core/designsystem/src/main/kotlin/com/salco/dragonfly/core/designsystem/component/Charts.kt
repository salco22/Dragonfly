package com.salco.dragonfly.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.salco.dragonfly.core.designsystem.theme.DfTheme
import kotlin.math.max
import kotlin.math.min

/**
 * Lightweight charts drawn on a Canvas, so the app needs no charting library. Every chart takes a
 * [description] read by screen readers in place of the drawing.
 */

/**
 * A filled line chart of [values] with one label per point under it (blank labels are skipped).
 * [onHero] draws it in lime with light grid and labels, for a [DfHeroCard].
 */
@Composable
fun LineChart(
    values: List<Float>,
    labels: List<String>,
    description: String,
    modifier: Modifier = Modifier,
    onHero: Boolean = false,
    color: Color = if (onHero) DfTheme.colors.highlight else DfTheme.colors.positive,
) {
    if (values.isEmpty()) return
    val gridColor = if (onHero) DfTheme.colors.heroTrack else DfTheme.colors.border
    val labelColor = if (onHero) DfTheme.colors.onHeroSecondary else DfTheme.colors.textTertiary
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .semantics { contentDescription = description },
        ) {
            val minValue = min(values.min(), 0f)
            val maxValue = max(values.max(), 0f).let { if (it == minValue) it + 1f else it }
            val range = maxValue - minValue
            val stepX = if (values.size > 1) size.width / (values.size - 1) else 0f
            fun y(v: Float) = size.height - (v - minValue) / range * size.height

            // Three faint guides and the zero line, useful when net worth goes negative.
            (1..3).forEach { i ->
                val gy = size.height * i / 4f
                drawLine(gridColor.copy(alpha = 0.6f), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1.dp.toPx())
            }
            drawLine(gridColor, Offset(0f, y(0f)), Offset(size.width, y(0f)), strokeWidth = 1.dp.toPx())

            val line = Path()
            values.forEachIndexed { i, v -> if (i == 0) line.moveTo(0f, y(v)) else line.lineTo(i * stepX, y(v)) }
            val area = Path().apply {
                addPath(line)
                lineTo((values.size - 1) * stepX, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent)))
            drawPath(line, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(color, radius = 4.dp.toPx(), center = Offset((values.size - 1) * stepX, y(values.last())))
        }
        AxisLabels(labels, labelColor)
    }
}

/** Pairs of bars (e.g. income and expenses) per label. */
@Composable
fun GroupedBarChart(
    first: List<Float>,
    second: List<Float>,
    labels: List<String>,
    firstColor: Color,
    secondColor: Color,
    description: String,
    modifier: Modifier = Modifier,
) {
    if (first.isEmpty()) return
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .semantics { contentDescription = description },
        ) {
            val maxValue = (first + second).maxOrNull()?.takeIf { it > 0f } ?: 1f
            val groupWidth = size.width / first.size
            val barWidth = groupWidth * 0.26f
            val radius = CornerRadius(5.dp.toPx())
            first.indices.forEach { i ->
                val left = i * groupWidth + groupWidth * 0.22f
                listOf(first[i] to firstColor, second.getOrElse(i) { 0f } to secondColor).forEachIndexed { j, (value, color) ->
                    val height = value / maxValue * size.height
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left + j * (barWidth + 3.dp.toPx()), size.height - height),
                        size = Size(barWidth, height),
                        cornerRadius = radius,
                    )
                }
            }
        }
        AxisLabels(labels, DfTheme.colors.textTertiary)
    }
}

data class DonutSlice(val label: String, val value: Float, val color: Color, val valueLabel: String)

/** A donut of [slices] with a legend listing each label and value. */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    centerText: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    if (total <= 0f) return
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .size(132.dp)
                    .semantics { contentDescription = description },
            ) {
                val stroke = 20.dp.toPx()
                var start = -90f
                slices.forEach { slice ->
                    val sweep = slice.value / total * 360f
                    drawArc(
                        color = slice.color,
                        startAngle = start,
                        sweepAngle = (sweep - 2f).coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke),
                    )
                    start += sweep
                }
            }
            DfText(centerText, style = DfTheme.typography.label, textAlign = TextAlign.Center)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            slices.forEach { slice ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Canvas(Modifier.size(10.dp)) { drawCircle(slice.color) }
                    DfText(slice.label, style = DfTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1)
                    DfText(slice.valueLabel, style = DfTheme.typography.bodySmall, tone = DfTone.Secondary)
                }
            }
        }
    }
}

/** Forest, sage, olive, lime and sand tones for categorical series, from the current theme. */
val ChartPalette: List<Color>
    @Composable @androidx.compose.runtime.ReadOnlyComposable get() = DfTheme.colors.chartPalette

@Composable
private fun AxisLabels(labels: List<String>, color: Color) {
    Row(Modifier.fillMaxWidth()) {
        labels.forEach { label ->
            DfText(
                label,
                style = DfTheme.typography.caption,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
