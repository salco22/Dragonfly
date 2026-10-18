package com.salco.dragonfly.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.salco.dragonfly.core.database.dao.AccountDao
import com.salco.dragonfly.core.database.dao.BudgetDao
import com.salco.dragonfly.core.database.dao.CategoryDao
import com.salco.dragonfly.core.database.dao.TagDao
import com.salco.dragonfly.core.database.dao.TransactionDao
import com.salco.dragonfly.core.database.model.AccountEntity
import com.salco.dragonfly.core.database.model.BudgetEntity
import com.salco.dragonfly.core.database.model.CategoryEntity
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
    ],
    version = 4,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
    ],
    exportSchema = true,
)
abstract class DragonflyDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun budgetDao(): BudgetDao
}
