package com.salco.dragonfly.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.salco.dragonfly.core.database.model.PopulatedRecurringTransaction
import com.salco.dragonfly.core.database.model.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

private const val POPULATED_SELECT = """
    SELECT r.*,
        COALESCE(s.name, r.source_name) AS resolved_source_name,
        COALESCE(d.name, r.destination_name) AS resolved_destination_name
    FROM recurring_transactions r
    LEFT JOIN accounts s ON s.id = r.source_account_id
    LEFT JOIN accounts d ON d.id = r.destination_account_id
"""

@Dao
interface RecurringTransactionDao {

    @Query("$POPULATED_SELECT ORDER BY r.active DESC, r.next_date IS NULL, r.next_date, r.description COLLATE NOCASE")
    fun observeAll(): Flow<List<PopulatedRecurringTransaction>>

    @Query("$POPULATED_SELECT WHERE r.id = :id")
    fun observeById(id: Long): Flow<PopulatedRecurringTransaction?>

    /** Active recurrences with a transaction due on or before [todayEpochDay]. */
    @Query("SELECT * FROM recurring_transactions WHERE active = 1 AND next_date IS NOT NULL AND next_date <= :todayEpochDay")
    suspend fun getDue(todayEpochDay: Long): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getById(id: Long): RecurringTransactionEntity?

    @Upsert
    suspend fun upsert(recurring: RecurringTransactionEntity): Long

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
