package com.salco.dragonfly.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing steps on a 4dp grid, plus the shared corner radius. */
@Immutable
data class DfSpacing(
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    /** Horizontal margin of screen content. */
    val screen: Dp = 20.dp,
    val radius: Dp = 14.dp,
    val radiusSmall: Dp = 10.dp,
)
