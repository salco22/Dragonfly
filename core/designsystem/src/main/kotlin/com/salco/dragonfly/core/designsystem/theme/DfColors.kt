package com.salco.dragonfly.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors of the minimal Dragonfly look: neutral surfaces, hairline borders and a single
 * green accent. Screens never use raw colors, only these roles.
 */
@Immutable
data class DfColors(
    val background: Color,
    val surface: Color,
    /** Subtle fill for grouped content, chips and inputs. */
    val surfaceMuted: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    /** Light accent fill for highlighted cards and selected items. */
    val accentMuted: Color,
    /** Money coming in, completed goals. */
    val positive: Color,
    /** Money going out, overspending, errors and destructive actions. */
    val negative: Color,
    /** Something due soon. */
    val warning: Color,
    val isDark: Boolean,
)

internal val LightDfColors = DfColors(
    background = Color(0xFFFAFAF9),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF3F3F1),
    border = Color(0xFFE6E6E3),
    textPrimary = Color(0xFF18181B),
    textSecondary = Color(0xFF6B6B67),
    textTertiary = Color(0xFFA1A19C),
    accent = Color(0xFF2F9E5B),
    onAccent = Color(0xFFFFFFFF),
    accentMuted = Color(0xFFE8F5EC),
    positive = Color(0xFF2F9E5B),
    negative = Color(0xFFD14343),
    warning = Color(0xFFB7791F),
    isDark = false,
)

internal val DarkDfColors = DfColors(
    background = Color(0xFF0E0E0F),
    surface = Color(0xFF161617),
    surfaceMuted = Color(0xFF1E1E20),
    border = Color(0xFF2A2A2D),
    textPrimary = Color(0xFFF4F4F5),
    textSecondary = Color(0xFFA3A3A6),
    textTertiary = Color(0xFF6B6B70),
    accent = Color(0xFF4CC27A),
    onAccent = Color(0xFF0B1F12),
    accentMuted = Color(0xFF15281C),
    positive = Color(0xFF4CC27A),
    negative = Color(0xFFF07171),
    warning = Color(0xFFE0A93B),
    isDark = true,
)
