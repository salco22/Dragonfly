package com.salco.dragonfly.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.salco.dragonfly.core.database.model.CategoryEntity
import com.salco.dragonfly.core.database.model.CategoryTotalsRow
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CategoryEntity>>

    /** Withdrawals and deposits per category and currency between two epoch days (inclusive). */
    @Query(
        """
        SELECT c.id, c.name, t.currency_code,
            COALESCE(SUM(CASE WHEN t.type = 'WITHDRAWAL' THEN t.amount END), 0) AS spent,
            COALESCE(SUM(CASE WHEN t.type = 'DEPOSIT' THEN t.amount END), 0) AS earned
        FROM categories c
        LEFT JOIN transactions t ON t.category_id = c.id AND t.date BETWEEN :fromEpochDay AND :toEpochDay
        GROUP BY c.id, t.currency_code
        ORDER BY c.name COLLATE NOCASE
        """,
    )
    fun observeTotals(fromEpochDay: Long, toEpochDay: Long): Flow<List<CategoryTotalsRow>>

    @Query("SELECT * FROM categories WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): CategoryEntity?

    @Upsert
    suspend fun upsert(category: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)
}
