package com.salco.navigation

sealed class DragonflyScreen(val route: String) {

    data object Login: DragonflyScreen("login")

}