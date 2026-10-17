package com.salco.dragonfly.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.salco.dragonfly.core.database.dao.AccountDao
import com.salco.dragonfly.core.database.dao.TransactionDao
import com.salco.dragonfly.core.database.model.AccountEntity
import com.salco.dragonfly.core.database.model.TransactionEntity

@Database(
    entities = [AccountEntity::class, TransactionEntity::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
    exportSchema = true,
)
abstract class DragonflyDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
}
