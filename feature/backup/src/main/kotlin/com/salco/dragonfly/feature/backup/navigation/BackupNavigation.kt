package com.salco.dragonfly.feature.backup.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.salco.dragonfly.feature.backup.BackupScreen
import kotlinx.serialization.Serializable

@Serializable
data object BackupRoute

fun NavController.navigateToBackup() = navigate(BackupRoute)

fun NavGraphBuilder.backupScreen(onBack: () -> Unit, onImportCsv: () -> Unit) {
    composable<BackupRoute> { BackupScreen(onBack = onBack, onImportCsv = onImportCsv) }
}
