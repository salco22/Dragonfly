package com.salco.dragonfly.feature.rules.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.salco.dragonfly.feature.rules.edit.RuleEditScreen
import com.salco.dragonfly.feature.rules.list.RulesScreen
import kotlinx.serialization.Serializable

@Serializable
data object RulesRoute

/** [ruleId] is 0 when creating a new rule. */
@Serializable
data class RuleEditRoute(val ruleId: Long = 0L)

fun NavController.navigateToRules() = navigate(RulesRoute)

fun NavController.navigateToRuleEdit(ruleId: Long = 0L) = navigate(RuleEditRoute(ruleId))

fun NavGraphBuilder.rulesScreens(onBack: () -> Unit, onRuleClick: (Long) -> Unit, onAdd: () -> Unit) {
    composable<RulesRoute> { RulesScreen(onBack = onBack, onRuleClick = onRuleClick, onAdd = onAdd) }
    composable<RuleEditRoute> { RuleEditScreen(onBack = onBack) }
}
