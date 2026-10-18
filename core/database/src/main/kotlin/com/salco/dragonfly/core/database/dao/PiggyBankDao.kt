package com.salco.dragonfly.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.salco.dragonfly.core.database.model.PiggyBankEntity
import com.salco.dragonfly.core.database.model.PiggyBankEventEntity
import com.salco.dragonfly.core.database.model.PopulatedPiggyBank
import kotlinx.coroutines.flow.Flow

private const val POPULATED_SELECT = """
    SELECT p.*, a.name AS account_name, a.currency_code AS currency_code,
        COALESCE((SELECT SUM(e.amount) FROM piggy_bank_events e WHERE e.piggy_bank_id = p.id), 0) AS saved
    FROM piggy_banks p
    JOIN accounts a ON a.id = p.account_id
"""

@Dao
interface PiggyBankDao {

    @Query("$POPULATED_SELECT ORDER BY p.name COLLATE NOCASE")
    fun observeAll(): Flow<List<PopulatedPiggyBank>>

    @Query("$POPULATED_SELECT WHERE p.id = :id")
    fun observeById(id: Long): Flow<PopulatedPiggyBank?>

    @Query("$POPULATED_SELECT WHERE p.account_id = :accountId ORDER BY p.name COLLATE NOCASE")
    fun observeForAccount(accountId: Long): Flow<List<PopulatedPiggyBank>>

    @Query("SELECT * FROM piggy_bank_events WHERE piggy_bank_id = :piggyBankId ORDER BY date DESC, id DESC")
    fun observeEvents(piggyBankId: Long): Flow<List<PiggyBankEventEntity>>

    @Query("SELECT * FROM piggy_banks WHERE id = :id")
    suspend fun getById(id: Long): PiggyBankEntity?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM piggy_bank_events WHERE piggy_bank_id = :piggyBankId")
    suspend fun savedIn(piggyBankId: Long): Long

    /** Money saved in every piggy bank of the account. */
    @Query(
        """
        SELECT COALESCE(SUM(e.amount), 0) FROM piggy_bank_events e
        JOIN piggy_banks p ON p.id = e.piggy_bank_id WHERE p.account_id = :accountId
        """,
    )
    suspend fun savedInAccount(accountId: Long): Long

    @Query("SELECT * FROM piggy_banks WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): PiggyBankEntity?

    @Upsert
    suspend fun upsert(piggyBank: PiggyBankEntity): Long

    @Insert
    suspend fun insertEvent(event: PiggyBankEventEntity): Long

    @Query("DELETE FROM piggy_banks WHERE id = :id")
    suspend fun deleteById(id: Long)
}
