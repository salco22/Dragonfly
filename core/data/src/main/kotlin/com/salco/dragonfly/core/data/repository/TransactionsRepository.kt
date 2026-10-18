package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.model.Transaction
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionFilter
import com.salco.dragonfly.core.model.TransactionGroupDraft
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TransactionsRepository {
    /** Transactions dated between [from] and [to] inclusive, newest first. */
    fun observeTransactions(from: LocalDate, to: LocalDate): Flow<List<Transaction>>
    fun observeAccountTransactions(accountId: Long): Flow<List<Transaction>>
    fun observeTransaction(id: Long): Flow<Transaction?>

    /** Transactions matching [filter], newest first. */
    fun searchTransactions(filter: TransactionFilter): Flow<List<Transaction>>

    /** Inserts or updates [draft], creating counterparties given by name, and returns its id. */
    suspend fun saveTransaction(draft: TransactionDraft): Long
    suspend fun deleteTransaction(id: Long)

    /** Parts of the split transaction [groupId], in entry order. */
    fun observeSplitTransaction(groupId: Long): Flow<List<Transaction>>

    /**
     * Saves a split transaction with all its parts and returns the group id. Parts no longer
     * listed are deleted; with a single part the group is dissolved (returns 0).
     */
    suspend fun saveSplitTransaction(group: TransactionGroupDraft): Long

    /** Deletes a split transaction with all its parts. */
    suspend fun deleteSplitTransaction(groupId: Long)
}
