package com.salco.dragonfly.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.salco.dragonfly.core.database.model.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts ORDER BY active DESC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun observeById(id: Long): Flow<AccountEntity?>

    @Upsert
    suspend fun upsert(account: AccountEntity): Long

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
