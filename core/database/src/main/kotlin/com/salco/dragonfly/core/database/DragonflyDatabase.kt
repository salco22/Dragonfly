package com.salco.dragonfly.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.salco.dragonfly.core.database.dao.AccountDao
import com.salco.dragonfly.core.database.model.AccountEntity

@Database(
    entities = [AccountEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DragonflyDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
}
