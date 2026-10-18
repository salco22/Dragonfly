package com.salco.dragonfly.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.salco.dragonfly.core.database.model.TagEntity
import com.salco.dragonfly.core.database.model.TagUsageRow
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TagEntity>>

    @Query(
        """
        SELECT tg.id, tg.name, COUNT(tt.transaction_id) AS transaction_count
        FROM tags tg
        LEFT JOIN transaction_tags tt ON tt.tag_id = tg.id
        GROUP BY tg.id
        ORDER BY tg.name COLLATE NOCASE
        """,
    )
    fun observeUsage(): Flow<List<TagUsageRow>>

    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): TagEntity?

    @Upsert
    suspend fun upsert(tag: TagEntity): Long

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long)
}
