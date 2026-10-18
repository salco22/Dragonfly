package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountsRepository {
    fun observeAccounts(): Flow<List<Account>>
    fun observeAccount(id: Long): Flow<Account?>

    /** Inserts or updates [account] and returns its id. */
    suspend fun saveAccount(account: Account): Long
    suspend fun deleteAccount(id: Long)
}
