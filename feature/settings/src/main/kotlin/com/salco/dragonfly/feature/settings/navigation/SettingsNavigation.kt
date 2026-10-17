package com.salco.dragonfly.feature.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.salco.dragonfly.feature.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
data object SettingsRoute

fun NavController.navigateToSettings(navOptions: NavOptions? = null) =
    navigate(route = SettingsRoute, navOptions = navOptions)

fun NavGraphBuilder.settingsScreen(onCategoriesClick: () -> Unit, onTagsClick: () -> Unit) {
    composable<SettingsRoute> {
        SettingsScreen(onCategoriesClick = onCategoriesClick, onTagsClick = onTagsClick)
    }
}
