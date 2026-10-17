package com.salco.dragonfly.core.database.di

import android.content.Context
import androidx.room.Room
import com.salco.dragonfly.core.database.DragonflyDatabase
import com.salco.dragonfly.core.database.dao.AccountDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun providesDragonflyDatabase(
        @ApplicationContext context: Context,
    ): DragonflyDatabase = Room.databaseBuilder(
        context,
        DragonflyDatabase::class.java,
        "dragonfly-database",
    ).build()

    @Provides
    fun providesAccountDao(database: DragonflyDatabase): AccountDao = database.accountDao()
}
