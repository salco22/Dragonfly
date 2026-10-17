package com.salco.dragonfly.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.salco.dragonfly.core.database.model.AccountEntity
import com.salco.dragonfly.core.database.model.AccountWithBalance
import kotlinx.coroutines.flow.Flow

// A transfer between currencies credits the destination with the foreign amount.
private const val WITH_BALANCE_SELECT = """
    SELECT a.*, a.opening_balance
        + COALESCE((SELECT SUM(COALESCE(t.foreign_amount, t.amount)) FROM transactions t WHERE t.destination_account_id = a.id), 0)
        - COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.source_account_id = a.id), 0)
        AS balance
    FROM accounts a
"""

@Dao
interface AccountDao {

    @Query("$WITH_BALANCE_SELECT ORDER BY a.active DESC, a.name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<AccountWithBalance>>

    @Query("$WITH_BALANCE_SELECT WHERE a.id = :id")
    fun observeById(id: Long): Flow<AccountWithBalance?>

    @Upsert
    suspend fun upsert(account: AccountEntity): Long

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
