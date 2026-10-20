package com.salco.dragonfly.core.network.di

import com.salco.dragonfly.core.network.SupabaseSyncNetworkDataSource
import com.salco.dragonfly.core.network.SyncNetworkDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface NetworkModule {
    @Binds
    fun bindsSyncNetworkDataSource(impl: SupabaseSyncNetworkDataSource): SyncNetworkDataSource
}
