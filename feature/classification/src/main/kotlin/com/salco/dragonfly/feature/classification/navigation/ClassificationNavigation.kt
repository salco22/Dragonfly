package com.salco.dragonfly.feature.classification.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.salco.dragonfly.feature.classification.categories.CategoriesScreen
import com.salco.dragonfly.feature.classification.tags.TagsScreen
import kotlinx.serialization.Serializable

@Serializable
data object CategoriesRoute

@Serializable
data object TagsRoute

fun NavController.navigateToCategories() = navigate(CategoriesRoute)

fun NavController.navigateToTags() = navigate(TagsRoute)

fun NavGraphBuilder.classificationScreens(onBack: () -> Unit) {
    composable<CategoriesRoute> { CategoriesScreen(onBack = onBack) }
    composable<TagsRoute> { TagsScreen(onBack = onBack) }
}
