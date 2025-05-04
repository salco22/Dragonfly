package com.salco.dragonfly.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.salco.login.feature.screen.LoginScreen
import com.salco.navigation.AppComposeNavigator
import com.salco.navigation.DragonflyScreen

fun NavGraphBuilder.dragonflyHomeNavigation(
    composeNavigator: AppComposeNavigator
){
    composable(DragonflyScreen.Login.route){
        LoginScreen(composeNavigator)
    }
}