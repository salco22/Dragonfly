package com.salco.dragonfly.feature.sync.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.salco.dragonfly.feature.sync.SyncScreen
import kotlinx.serialization.Serializable

@Serializable
data object SyncRoute

fun NavController.navigateToSync() = navigate(SyncRoute)

fun NavGraphBuilder.syncScreen(onBack: () -> Unit) {
    composable<SyncRoute> { SyncScreen(onBack = onBack) }
}
