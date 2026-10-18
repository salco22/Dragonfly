package com.salco.dragonfly.core.database.di

import android.content.Context
import androidx.room.Room
import com.salco.dragonfly.core.database.DragonflyDatabase
import com.salco.dragonfly.core.database.dao.AccountDao
import com.salco.dragonfly.core.database.dao.BillDao
import com.salco.dragonfly.core.database.dao.BudgetDao
import com.salco.dragonfly.core.database.dao.CategoryDao
import com.salco.dragonfly.core.database.dao.RecurringTransactionDao
import com.salco.dragonfly.core.database.dao.TagDao
import com.salco.dragonfly.core.database.dao.TransactionDao
import com.salco.dragonfly.core.database.DatabaseTransactionRunner
import com.salco.dragonfly.core.database.RoomTransactionRunner
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface DatabaseBindings {
    @Binds
    fun bindsTransactionRunner(impl: RoomTransactionRunner): DatabaseTransactionRunner
}

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

    @Provides
    fun providesTransactionDao(database: DragonflyDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun providesCategoryDao(database: DragonflyDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun providesBillDao(database: DragonflyDatabase): BillDao = database.billDao()

    @Provides
    fun providesBudgetDao(database: DragonflyDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun providesRecurringTransactionDao(database: DragonflyDatabase): RecurringTransactionDao =
        database.recurringTransactionDao()

    @Provides
    fun providesTagDao(database: DragonflyDatabase): TagDao = database.tagDao()
}
