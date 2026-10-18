package com.salco.dragonfly.feature.backup.di

import com.salco.dragonfly.feature.backup.AndroidDocumentAccess
import com.salco.dragonfly.feature.backup.DocumentAccess
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface BackupModule {
    @Binds
    fun bindsDocumentAccess(impl: AndroidDocumentAccess): DocumentAccess
}
