package com.salco.dragonfly.feature.reports.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.salco.dragonfly.feature.reports.ReportsScreen
import kotlinx.serialization.Serializable

@Serializable
data object ReportsRoute

fun NavController.navigateToReports() = navigate(ReportsRoute)

fun NavGraphBuilder.reportsScreen(onBack: () -> Unit) {
    composable<ReportsRoute> { ReportsScreen(onBack = onBack) }
}
