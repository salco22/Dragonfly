package com.salco.dragonfly.feature.csvimport.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.salco.dragonfly.feature.csvimport.CsvImportScreen
import kotlinx.serialization.Serializable

@Serializable
data object CsvImportRoute

fun NavController.navigateToCsvImport() = navigate(CsvImportRoute)

fun NavGraphBuilder.csvImportScreen(onBack: () -> Unit) {
    composable<CsvImportRoute> { CsvImportScreen(onBack = onBack) }
}
