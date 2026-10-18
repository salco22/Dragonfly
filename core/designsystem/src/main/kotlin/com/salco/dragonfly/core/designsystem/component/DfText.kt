package com.salco.dragonfly.core.designsystem.component

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.salco.dragonfly.core.designsystem.theme.DfTheme

/** Meaning of a piece of text or an icon, mapped to a theme color. */
enum class DfTone { Primary, Secondary, Tertiary, Accent, Positive, Negative, Warning, OnAccent }

@Composable
@ReadOnlyComposable
fun DfTone.color(): Color = with(DfTheme.colors) {
    when (this@color) {
        DfTone.Primary -> textPrimary
        DfTone.Secondary -> textSecondary
        DfTone.Tertiary -> textTertiary
        DfTone.Accent -> accent
        DfTone.Positive -> positive
        DfTone.Negative -> negative
        DfTone.Warning -> warning
        DfTone.OnAccent -> onAccent
    }
}

/** Text in one of the theme styles; [style] defaults to body text. */
@Composable
fun DfText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = DfTheme.typography.body,
    tone: DfTone = DfTone.Primary,
    color: Color = tone.color(),
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
    )
}

@Composable
fun DfIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tone: DfTone = DfTone.Secondary,
    tint: Color = tone.color(),
) {
    Icon(icon, contentDescription, modifier, tint)
}

@Composable
fun DfIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: DfTone = DfTone.Primary,
) {
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        DfIcon(icon, contentDescription, tint = if (enabled) tone.color() else DfTheme.colors.textTertiary)
    }
}

/** An amount such as "€1,234.56", red when [negative]; [tone] overrides the color. */
@Composable
fun DfAmount(
    formatted: String,
    modifier: Modifier = Modifier,
    negative: Boolean = false,
    style: TextStyle = DfTheme.typography.subtitle,
    tone: DfTone = if (negative) DfTone.Negative else DfTone.Primary,
) {
    DfText(formatted, modifier, style = style, tone = tone, fontWeight = FontWeight.SemiBold, maxLines = 1)
}
