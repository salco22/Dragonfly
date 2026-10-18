package com.salco.dragonfly.core.designsystem.component

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.salco.dragonfly.core.designsystem.theme.DfTheme

data class DfNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/** The bottom bar of top-level destinations: no tinted pill, the selected item is simply darker. */
@Composable
fun DfNavigationBar(items: List<DfNavItem>) {
    androidx.compose.foundation.layout.Column {
        DfDivider()
        NavigationBar(containerColor = DfTheme.colors.background, tonalElevation = androidx.compose.ui.unit.Dp(0f)) {
            items.forEach { item ->
                NavigationBarItem(
                    selected = item.selected,
                    onClick = item.onClick,
                    icon = { DfIcon(if (item.selected) item.selectedIcon else item.icon, contentDescription = null, tint = if (item.selected) DfTheme.colors.textPrimary else DfTheme.colors.textTertiary) },
                    label = { DfText(item.label, style = DfTheme.typography.caption, tone = if (item.selected) DfTone.Primary else DfTone.Tertiary, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = DfTheme.colors.surfaceMuted),
                )
            }
        }
    }
}

/**
 * The outer frame of the app: only the bottom bar; each screen draws its own top bar and handles
 * its insets. [content] receives the padding left by the bottom bar.
 */
@Composable
fun DfAppFrame(
    bottomBar: @Composable () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    androidx.compose.material3.Scaffold(
        containerColor = DfTheme.colors.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        bottomBar = bottomBar,
        content = content,
    )
}
