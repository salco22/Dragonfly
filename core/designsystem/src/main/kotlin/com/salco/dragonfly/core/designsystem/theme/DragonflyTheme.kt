package com.salco.dragonfly.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

private val LocalDfColors = staticCompositionLocalOf { LightDfColors }
private val LocalDfTypography = staticCompositionLocalOf { DefaultDfTypography }
private val LocalDfSpacing = staticCompositionLocalOf { DfSpacing() }

/** Access to the current design tokens: `DfTheme.colors.accent`, `DfTheme.typography.body`… */
object DfTheme {
    val colors: DfColors
        @Composable @ReadOnlyComposable get() = LocalDfColors.current
    val typography: DfTypography
        @Composable @ReadOnlyComposable get() = LocalDfTypography.current
    val spacing: DfSpacing
        @Composable @ReadOnlyComposable get() = LocalDfSpacing.current
}

/** Applies the Dragonfly theme, following the system light/dark setting. */
@Composable
fun DragonflyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkDfColors else LightDfColors
    val typography = DefaultDfTypography
    CompositionLocalProvider(
        LocalDfColors provides colors,
        LocalDfTypography provides typography,
        LocalDfSpacing provides DfSpacing(),
    ) {
        // Material components used inside the design system pick up the same tokens.
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = Typography(
                displaySmall = typography.display,
                headlineSmall = typography.headline,
                titleLarge = typography.title,
                titleMedium = typography.subtitle,
                titleSmall = typography.label,
                bodyLarge = typography.body,
                bodyMedium = typography.body,
                bodySmall = typography.bodySmall,
                labelLarge = typography.label,
                labelMedium = typography.caption,
                labelSmall = typography.caption,
            ),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(16.dp),
                large = RoundedCornerShape(24.dp),
                extraLarge = RoundedCornerShape(28.dp),
            ),
            content = content,
        )
    }
}

private fun DfColors.toMaterial() = if (isDark) {
    darkColorScheme(
        primary = accent, onPrimary = onAccent, primaryContainer = accentMuted, onPrimaryContainer = textPrimary,
        secondary = textSecondary, onSecondary = background, secondaryContainer = surfaceMuted, onSecondaryContainer = textPrimary,
        tertiary = highlight, onTertiary = onHighlight, tertiaryContainer = warningContainer, background = background, onBackground = textPrimary, surface = surface, onSurface = textPrimary,
        surfaceVariant = surfaceMuted, onSurfaceVariant = textSecondary, outline = border, outlineVariant = border,
        error = negative, onError = onAccent, surfaceTint = surface,
        surfaceContainerLowest = background, surfaceContainerLow = surface, surfaceContainer = surface,
        surfaceContainerHigh = surfaceMuted, surfaceContainerHighest = surfaceMuted, inverseSurface = textPrimary, inverseOnSurface = background,
    )
} else {
    lightColorScheme(
        primary = accent, onPrimary = onAccent, primaryContainer = accentMuted, onPrimaryContainer = textPrimary,
        secondary = textSecondary, onSecondary = surface, secondaryContainer = surfaceMuted, onSecondaryContainer = textPrimary,
        tertiary = highlight, onTertiary = onHighlight, tertiaryContainer = warningContainer, background = background, onBackground = textPrimary, surface = surface, onSurface = textPrimary,
        surfaceVariant = surfaceMuted, onSurfaceVariant = textSecondary, outline = border, outlineVariant = border,
        error = negative, onError = onAccent, surfaceTint = surface,
        surfaceContainerLowest = surface, surfaceContainerLow = surface, surfaceContainer = surface,
        surfaceContainerHigh = surfaceMuted, surfaceContainerHighest = surfaceMuted, inverseSurface = textPrimary, inverseOnSurface = surface,
    )
}
