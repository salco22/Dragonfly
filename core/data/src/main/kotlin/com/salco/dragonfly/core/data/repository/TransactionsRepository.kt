package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.model.Transaction
import com.salco.dragonfly.core.model.TransactionDraft
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TransactionsRepository {
    /** Transactions dated between [from] and [to] inclusive, newest first. */
    fun observeTransactions(from: LocalDate, to: LocalDate): Flow<List<Transaction>>
    fun observeAccountTransactions(accountId: Long): Flow<List<Transaction>>
    fun observeTransaction(id: Long): Flow<Transaction?>

    /** Inserts or updates [draft], creating counterparties given by name, and returns its id. */
    suspend fun saveTransaction(draft: TransactionDraft): Long
    suspend fun deleteTransaction(id: Long)
}
