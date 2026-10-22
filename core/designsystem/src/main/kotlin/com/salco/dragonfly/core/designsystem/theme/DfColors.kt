package com.salco.dragonfly.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic colors of the Dragonfly look: warm cream background, borderless white cards, a deep
 * forest green and a lime highlight. Screens never use raw colors, only these roles.
 */
@Immutable
data class DfColors(
    val background: Color,
    val surface: Color,
    /** Subtle sage fill for icon tiles, chips, tracks and grouped content. */
    val surfaceMuted: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    /** Primary buttons, selected chips, focused fields. */
    val accent: Color,
    val onAccent: Color,
    /** Sage fill for highlighted cards, info banners and selected items. */
    val accentMuted: Color,
    /** Text buttons and links. */
    val link: Color,
    /** Money coming in, budgets on track, completed goals. */
    val positive: Color,
    /** Overspending, errors and destructive actions. */
    val negative: Color,
    /** Something due soon or close to its limit. */
    val warning: Color,
    val warningContainer: Color,
    /** Second accent: active navigation pill, chart line and highlighted values on hero cards. */
    val highlight: Color,
    val onHighlight: Color,
    /** The dark card holding the one number that matters most on a screen. */
    val hero: Color,
    val onHero: Color,
    val onHeroSecondary: Color,
    /** Tracks and grid lines drawn on a hero card. */
    val heroTrack: Color,
    val chartIncome: Color,
    val chartExpense: Color,
    /** Series of categorical charts, darkest first. */
    val chartPalette: List<Color>,
    val isDark: Boolean,
)

private val ForestGreen = Color(0xFF174233)
private val MidGreen = Color(0xFF26764D)
private val Lime = Color(0xFFD7F395)
private val Sage = Color(0xFFDEE5D8)
private val SageLight = Color(0xFFE9EEE5)
private val Sand = Color(0xFFD8C6A0)

internal val LightDfColors = DfColors(
    background = Color(0xFFF7F5ED),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = SageLight,
    border = Color(0xFFE5E3D9),
    textPrimary = ForestGreen,
    textSecondary = Color(0xFF6B786F),
    textTertiary = Color(0xFF9DA69E),
    accent = ForestGreen,
    onAccent = Color(0xFFFFFFFF),
    accentMuted = Sage,
    link = MidGreen,
    positive = MidGreen,
    negative = Color(0xFFB4432F),
    warning = Color(0xFF9D632C),
    warningContainer = Color(0xFFF4EBD9),
    highlight = Lime,
    onHighlight = ForestGreen,
    hero = ForestGreen,
    onHero = Color(0xFFFFFFFF),
    onHeroSecondary = Color(0xFFB5C7BC),
    heroTrack = Color(0xFF2D5A48),
    chartIncome = MidGreen,
    chartExpense = Sand,
    chartPalette = listOf(ForestGreen, Color(0xFF8FA88F), Color(0xFFA9AD6B), Color(0xFFC5E486), Sand, Color(0xFF5E8C70), Color(0xFFC9CEC4)),
    isDark = false,
)

internal val DarkDfColors = DfColors(
    background = Color(0xFF0D1813),
    surface = Color(0xFF15241D),
    surfaceMuted = Color(0xFF1E3128),
    border = Color(0xFF263A31),
    textPrimary = Color(0xFFEDF2EA),
    textSecondary = Color(0xFFA2B3A8),
    textTertiary = Color(0xFF6C7F74),
    accent = Lime,
    onAccent = ForestGreen,
    accentMuted = Color(0xFF213A2E),
    link = Lime,
    positive = Color(0xFF7FCF9C),
    negative = Color(0xFFF08A75),
    warning = Color(0xFFE0B07A),
    warningContainer = Color(0xFF3A2E1E),
    highlight = Lime,
    onHighlight = ForestGreen,
    hero = Color(0xFF1D4636),
    onHero = Color(0xFFF4F7EF),
    onHeroSecondary = Color(0xFFA9C2B2),
    heroTrack = Color(0xFF2F5E4B),
    chartIncome = Color(0xFF5FB585),
    chartExpense = Color(0xFFC8B48C),
    chartPalette = listOf(Color(0xFF5FB585), Color(0xFF8FA88F), Color(0xFFA9AD6B), Lime, Color(0xFFC8B48C), Color(0xFF3E7A5C), Color(0xFF6C7F74)),
    isDark = true,
)
