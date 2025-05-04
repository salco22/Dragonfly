package com.salco.navigation.di

import com.salco.navigation.AppComposeNavigator
import com.salco.navigation.DragonflyAppNavigator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NavigationModule {

    @Binds
    @Singleton
    abstract fun bindAppComposeNavigator(
        impl: DragonflyAppNavigator
    ):AppComposeNavigator
}