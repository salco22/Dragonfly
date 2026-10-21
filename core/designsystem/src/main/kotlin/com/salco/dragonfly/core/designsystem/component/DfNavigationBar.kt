package com.salco.dragonfly.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.salco.dragonfly.core.designsystem.theme.DfTheme

data class DfNavItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/** The bottom bar of top-level destinations: the selected icon sits on a lime pill. */
@Composable
fun DfNavigationBar(items: List<DfNavItem>) {
    Column {
        DfDivider()
        NavigationBar(containerColor = DfTheme.colors.background, tonalElevation = 0.dp) {
            items.forEach { item ->
                NavigationBarItem(
                    selected = item.selected,
                    onClick = item.onClick,
                    icon = {
                        DfIcon(
                            if (item.selected) item.selectedIcon else item.icon,
                            contentDescription = null,
                            tint = if (item.selected) DfTheme.colors.onHighlight else DfTheme.colors.textSecondary,
                        )
                    },
                    label = {
                        DfText(
                            item.label,
                            style = DfTheme.typography.caption,
                            tone = if (item.selected) DfTone.Primary else DfTone.Secondary,
                            fontWeight = if (item.selected) FontWeight.SemiBold else null,
                            maxLines = 1,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = DfTheme.colors.highlight),
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
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = DfTheme.colors.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = bottomBar,
        content = content,
    )
}
