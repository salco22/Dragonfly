package com.salco.dragonfly.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.salco.dragonfly.core.database.model.BillEntity
import com.salco.dragonfly.core.database.model.BillPaymentRow
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {

    @Query("SELECT * FROM bills ORDER BY active DESC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :id")
    fun observeById(id: Long): Flow<BillEntity?>

    /** Every withdrawal linked to a bill, newest first. */
    @Query("SELECT bill_id, id, date, amount FROM transactions WHERE bill_id IS NOT NULL AND type = 'WITHDRAWAL' ORDER BY date DESC")
    fun observePayments(): Flow<List<BillPaymentRow>>

    @Query("SELECT * FROM bills WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): BillEntity?

    @Upsert
    suspend fun upsert(bill: BillEntity): Long

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteById(id: Long)
}
