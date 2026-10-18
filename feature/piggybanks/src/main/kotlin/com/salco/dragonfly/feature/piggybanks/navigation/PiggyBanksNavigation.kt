package com.salco.dragonfly.feature.piggybanks.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.salco.dragonfly.feature.piggybanks.detail.PiggyBankDetailScreen
import com.salco.dragonfly.feature.piggybanks.edit.PiggyBankEditScreen
import com.salco.dragonfly.feature.piggybanks.list.PiggyBanksScreen
import kotlinx.serialization.Serializable

@Serializable
data object PiggyBanksRoute

@Serializable
data class PiggyBankDetailRoute(val piggyBankId: Long)

/** [piggyBankId] is 0 when creating a new piggy bank. */
@Serializable
data class PiggyBankEditRoute(val piggyBankId: Long = 0L)

fun NavController.navigateToPiggyBanks() = navigate(PiggyBanksRoute)

fun NavController.navigateToPiggyBankDetail(id: Long) = navigate(PiggyBankDetailRoute(id))

fun NavController.navigateToPiggyBankEdit(id: Long = 0L) = navigate(PiggyBankEditRoute(id))

fun NavGraphBuilder.piggyBanksScreens(
    onBack: () -> Unit,
    onPiggyBankClick: (Long) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    composable<PiggyBanksRoute> { PiggyBanksScreen(onBack = onBack, onPiggyBankClick = onPiggyBankClick, onAdd = onAdd) }
    composable<PiggyBankDetailRoute> { PiggyBankDetailScreen(onBack = onBack, onEdit = onEdit) }
    composable<PiggyBankEditRoute> { PiggyBankEditScreen(onBack = onBack) }
}
