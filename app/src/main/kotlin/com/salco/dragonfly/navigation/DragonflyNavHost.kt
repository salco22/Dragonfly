package com.salco.dragonfly.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.salco.navigation.AppComposeNavigator
import com.salco.navigation.DragonflyScreen

@Composable
fun DragonflyNavHost(
    navHostController: NavHostController,
    composeNavigator: AppComposeNavigator
) {
    NavHost(
        navController = navHostController,
        startDestination = DragonflyScreen.Login.route
    ){
        dragonflyHomeNavigation(
            composeNavigator
        )
    }
}