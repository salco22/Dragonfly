package com.salco.dragonfly.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.salco.dragonfly.core.database.model.AccountEntity
import com.salco.dragonfly.core.database.model.PopulatedTransaction
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.database.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

private const val POPULATED_SELECT = """
    SELECT t.*,
        s.name AS source_name, s.type AS source_type, s.currency_code AS source_currency,
        d.name AS destination_name, d.type AS destination_type, d.currency_code AS destination_currency
    FROM transactions t
    JOIN accounts s ON s.id = t.source_account_id
    JOIN accounts d ON d.id = t.destination_account_id
"""

@Dao
abstract class TransactionDao {

    /** Transactions between two epoch days (inclusive), newest first. */
    @Query("$POPULATED_SELECT WHERE t.date BETWEEN :fromEpochDay AND :toEpochDay ORDER BY t.date DESC, t.id DESC")
    abstract fun observeBetween(fromEpochDay: Long, toEpochDay: Long): Flow<List<PopulatedTransaction>>

    @Query("$POPULATED_SELECT WHERE t.source_account_id = :accountId OR t.destination_account_id = :accountId ORDER BY t.date DESC, t.id DESC")
    abstract fun observeForAccount(accountId: Long): Flow<List<PopulatedTransaction>>

    @Query("$POPULATED_SELECT WHERE t.id = :id")
    abstract fun observeById(id: Long): Flow<PopulatedTransaction?>

    @Upsert
    abstract suspend fun upsert(transaction: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Query("SELECT * FROM accounts WHERE type = :type AND name = :name COLLATE NOCASE LIMIT 1")
    protected abstract suspend fun findAccountByName(name: String, type: AccountType): AccountEntity?

    @Insert
    protected abstract suspend fun insertAccount(account: AccountEntity): Long

    /**
     * Saves [transaction], first resolving counterparties given by name: an existing expense or
     * revenue account with the same name is reused, otherwise a new one is created. Runs atomically.
     */
    @Transaction
    open suspend fun upsertResolvingAccounts(
        transaction: TransactionEntity,
        newSource: AccountEntity?,
        newDestination: AccountEntity?,
    ): Long {
        val sourceId = newSource?.let { resolve(it) } ?: transaction.sourceAccountId
        val destinationId = newDestination?.let { resolve(it) } ?: transaction.destinationAccountId
        val rowId = upsert(transaction.copy(sourceAccountId = sourceId, destinationAccountId = destinationId))
        // Upsert returns -1 when it updated an existing row.
        return if (transaction.id != 0L) transaction.id else rowId
    }

    private suspend fun resolve(account: AccountEntity): Long =
        findAccountByName(account.name, account.type)?.id ?: insertAccount(account)
}
