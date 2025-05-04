package com.salco.dragonfly.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.salco.dragonfly.navigation.DragonflyNavHost
import com.salco.dragonfly.ui.theme.DragonflyTheme
import com.salco.navigation.AppComposeNavigator

@Composable
fun MainAppScreen(
    composeNavigator: AppComposeNavigator
) {
    DragonflyTheme {
        val navHostController = rememberNavController()

        LaunchedEffect(Unit) {
            composeNavigator.handleNavigationCommands(navHostController)
        }

        DragonflyNavHost(
            navHostController = navHostController,
            composeNavigator = composeNavigator
        )
    }
}