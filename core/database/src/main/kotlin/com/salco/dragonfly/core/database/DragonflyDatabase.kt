package com.salco.dragonfly.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
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
import com.salco.dragonfly.core.database.model.AccountEntity
import com.salco.dragonfly.core.database.model.BillEntity
import com.salco.dragonfly.core.database.model.BudgetEntity
import com.salco.dragonfly.core.database.model.CategoryEntity
import com.salco.dragonfly.core.database.model.PiggyBankEntity
import com.salco.dragonfly.core.database.model.PiggyBankEventEntity
import com.salco.dragonfly.core.database.model.RecurringTransactionEntity
import com.salco.dragonfly.core.database.model.RuleActionEntity
import com.salco.dragonfly.core.database.model.RuleEntity
import com.salco.dragonfly.core.database.model.RuleTriggerEntity
import com.salco.dragonfly.core.database.model.TagEntity
import com.salco.dragonfly.core.database.model.TransactionEntity
import com.salco.dragonfly.core.database.model.TransactionTagCrossRef

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        TransactionTagCrossRef::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        BillEntity::class,
        PiggyBankEntity::class,
        PiggyBankEventEntity::class,
        RuleEntity::class,
        RuleTriggerEntity::class,
        RuleActionEntity::class,
    ],
    version = DragonflyDatabase.VERSION,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
    ],
    exportSchema = true,
)
abstract class DragonflyDatabase : RoomDatabase() {
    companion object {
        /** Keep in sync with [Database.version]; written into backups. */
        const val VERSION = 8
    }


    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun budgetDao(): BudgetDao
    abstract fun billDao(): BillDao
    abstract fun insightsDao(): InsightsDao
    abstract fun ruleDao(): RuleDao
    abstract fun backupDao(): BackupDao
    abstract fun piggyBankDao(): PiggyBankDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
}
