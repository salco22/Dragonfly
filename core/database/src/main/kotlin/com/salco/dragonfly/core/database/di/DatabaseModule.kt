package com.salco.dragonfly.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.salco.dragonfly.core.database.sync.SyncTriggers
import com.salco.dragonfly.core.database.DragonflyDatabase
import com.salco.dragonfly.core.database.dao.AccountDao
import com.salco.dragonfly.core.database.dao.BackupDao
import com.salco.dragonfly.core.database.dao.BillDao
import com.salco.dragonfly.core.database.dao.BudgetDao
import com.salco.dragonfly.core.database.dao.CategoryDao
import com.salco.dragonfly.core.database.dao.InsightsDao
import com.salco.dragonfly.core.database.dao.PiggyBankDao
import com.salco.dragonfly.core.database.dao.RecurringTransactionDao
import com.salco.dragonfly.core.database.dao.RuleDao
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
    ).addCallback(
        object : RoomDatabase.Callback() {
            // On every open, so a migrated database gets its triggers too.
            override fun onOpen(db: SupportSQLiteDatabase) = SyncTriggers.install(db)
        },
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
    fun providesPiggyBankDao(database: DragonflyDatabase): PiggyBankDao = database.piggyBankDao()

    @Provides
    fun providesInsightsDao(database: DragonflyDatabase): InsightsDao = database.insightsDao()

    @Provides
    fun providesRuleDao(database: DragonflyDatabase): RuleDao = database.ruleDao()

    @Provides
    fun providesBackupDao(database: DragonflyDatabase): BackupDao = database.backupDao()

    @Provides
    fun providesBudgetDao(database: DragonflyDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun providesRecurringTransactionDao(database: DragonflyDatabase): RecurringTransactionDao =
        database.recurringTransactionDao()

    @Provides
    fun providesTagDao(database: DragonflyDatabase): TagDao = database.tagDao()
}
