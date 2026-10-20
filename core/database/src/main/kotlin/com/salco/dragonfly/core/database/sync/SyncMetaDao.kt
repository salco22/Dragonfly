package com.salco.dragonfly.core.database.sync

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncMetaDao {
    /** Local changes waiting to be sent. */
    @Query("SELECT COUNT(*) FROM sync_meta WHERE dirty = 1")
    fun observePendingCount(): Flow<Int>
}
