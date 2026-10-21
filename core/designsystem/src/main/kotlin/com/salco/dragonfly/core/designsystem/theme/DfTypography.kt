package com.salco.dragonfly.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.salco.dragonfly.core.designsystem.R

/** A small type scale: few sizes, weight and color do the rest. */
@Immutable
data class DfTypography(
    /** Large amounts, such as a balance. */
    val display: TextStyle,
    /** Emotional serif titles, only on the login and lock screens. */
    val displaySerif: TextStyle,
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

/** Inter, bundled under the SIL Open Font License, for all UI text and numbers. */
private val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val base = TextStyle(fontFamily = Inter)

internal val DefaultDfTypography = DfTypography(
    display = base.copy(fontSize = 38.sp, lineHeight = 44.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.8).sp),
    displaySerif = TextStyle(fontFamily = FontFamily.Serif, fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.4).sp),
    headline = base.copy(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
    title = base.copy(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    subtitle = base.copy(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
    body = base.copy(fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = base.copy(fontSize = 13.sp, lineHeight = 18.sp),
    label = base.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    caption = base.copy(fontSize = 12.sp, lineHeight = 16.sp),
    overline = base.copy(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
)
