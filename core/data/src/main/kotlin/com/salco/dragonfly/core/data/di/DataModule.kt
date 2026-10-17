package com.salco.dragonfly.core.data.di

import com.salco.dragonfly.core.data.repository.AccountsRepository
import com.salco.dragonfly.core.data.repository.OfflineAccountsRepository
import com.salco.dragonfly.core.data.repository.OfflineUserDataRepository
import com.salco.dragonfly.core.data.repository.UserDataRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    abstract fun bindsAccountsRepository(impl: OfflineAccountsRepository): AccountsRepository

    @Binds
    abstract fun bindsUserDataRepository(impl: OfflineUserDataRepository): UserDataRepository
}
