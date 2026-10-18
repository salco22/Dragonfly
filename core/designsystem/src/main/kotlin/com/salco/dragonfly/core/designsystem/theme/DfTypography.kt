package com.salco.dragonfly.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** A small type scale: few sizes, weight and color do the rest. */
@Immutable
data class DfTypography(
    /** Large amounts, such as a balance. */
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val subtitle: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    /** Small uppercase section headers. */
    val overline: TextStyle,
)

private val base = TextStyle(fontFamily = FontFamily.Default)

internal val DefaultDfTypography = DfTypography(
    display = base.copy(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.6).sp),
    headline = base.copy(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp),
    title = base.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    subtitle = base.copy(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium),
    body = base.copy(fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = base.copy(fontSize = 13.sp, lineHeight = 18.sp),
    label = base.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    caption = base.copy(fontSize = 12.sp, lineHeight = 16.sp),
    overline = base.copy(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.9.sp),
)
