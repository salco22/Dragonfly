package com.salco.dragonfly.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.salco.dragonfly.core.database.model.AccountEntity
import com.salco.dragonfly.core.database.model.CategoryEntity
import com.salco.dragonfly.core.database.model.PopulatedTransaction
import com.salco.dragonfly.core.database.model.TagEntity
import com.salco.dragonfly.core.database.model.TransactionEntity
import com.salco.dragonfly.core.database.model.TransactionTagCrossRef
import com.salco.dragonfly.core.model.AccountType
import kotlinx.coroutines.flow.Flow

private const val POPULATED_SELECT = """
    SELECT t.*,
        s.name AS source_name, s.type AS source_type, s.currency_code AS source_currency,
        d.name AS destination_name, d.type AS destination_type, d.currency_code AS destination_currency,
        c.name AS category_name,
        b.name AS budget_name,
        bl.name AS bill_name
    FROM transactions t
    JOIN accounts s ON s.id = t.source_account_id
    JOIN accounts d ON d.id = t.destination_account_id
    LEFT JOIN categories c ON c.id = t.category_id
    LEFT JOIN budgets b ON b.id = t.budget_id
    LEFT JOIN bills bl ON bl.id = t.bill_id
"""

@Dao
abstract class TransactionDao {

    /** Transactions between two epoch days (inclusive), newest first. */
    @Transaction
    @Query("$POPULATED_SELECT WHERE t.date BETWEEN :fromEpochDay AND :toEpochDay ORDER BY t.date DESC, t.id DESC")
    abstract fun observeBetween(fromEpochDay: Long, toEpochDay: Long): Flow<List<PopulatedTransaction>>

    @Transaction
    @Query("$POPULATED_SELECT WHERE t.source_account_id = :accountId OR t.destination_account_id = :accountId ORDER BY t.date DESC, t.id DESC")
    abstract fun observeForAccount(accountId: Long): Flow<List<PopulatedTransaction>>

    /** Every parameter is optional: null skips that condition. [query] is matched with LIKE, ignoring case. */
    @Transaction
    @Query(
        """
        $POPULATED_SELECT
        WHERE (:query IS NULL
                OR t.description LIKE '%' || :query || '%'
                OR t.notes LIKE '%' || :query || '%'
                OR s.name LIKE '%' || :query || '%'
                OR d.name LIKE '%' || :query || '%'
                OR c.name LIKE '%' || :query || '%'
                OR EXISTS (SELECT 1 FROM transaction_tags tt JOIN tags g ON g.id = tt.tag_id
                           WHERE tt.transaction_id = t.id AND g.name LIKE '%' || :query || '%'))
            AND (:type IS NULL OR t.type = :type)
            AND (:categoryId IS NULL OR t.category_id = :categoryId)
            AND (:budgetId IS NULL OR t.budget_id = :budgetId)
            AND (:billId IS NULL OR t.bill_id = :billId)
            AND (:tagId IS NULL OR EXISTS (SELECT 1 FROM transaction_tags tt WHERE tt.transaction_id = t.id AND tt.tag_id = :tagId))
            AND (:accountId IS NULL OR t.source_account_id = :accountId OR t.destination_account_id = :accountId)
            AND (:fromEpochDay IS NULL OR t.date >= :fromEpochDay)
            AND (:toEpochDay IS NULL OR t.date <= :toEpochDay)
        ORDER BY t.date DESC, t.id DESC
        """,
    )
    abstract fun search(
        query: String?,
        type: String?,
        categoryId: Long?,
        tagId: Long?,
        budgetId: Long?,
        billId: Long?,
        accountId: Long?,
        fromEpochDay: Long?,
        toEpochDay: Long?,
    ): Flow<List<PopulatedTransaction>>

    @Transaction
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

    @Query("SELECT * FROM categories WHERE name = :name COLLATE NOCASE LIMIT 1")
    protected abstract suspend fun findCategoryByName(name: String): CategoryEntity?

    @Insert
    protected abstract suspend fun insertCategory(category: CategoryEntity): Long

    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    protected abstract suspend fun findTagByName(name: String): TagEntity?

    @Insert
    protected abstract suspend fun insertTag(tag: TagEntity): Long

    @Query("DELETE FROM transaction_tags WHERE transaction_id = :transactionId")
    protected abstract suspend fun clearTags(transactionId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertTagRefs(refs: List<TransactionTagCrossRef>)

    /**
     * Saves [transaction] atomically, resolving everything given by name: counterparties
     * ([newSource], [newDestination]), [categoryName] and [tagNames] reuse an existing row with
     * the same name (ignoring case) or create a new one, as Firefly III does.
     */
    @Transaction
    open suspend fun save(
        transaction: TransactionEntity,
        newSource: AccountEntity? = null,
        newDestination: AccountEntity? = null,
        categoryName: String? = null,
        tagNames: List<String> = emptyList(),
    ): Long {
        val resolved = transaction.copy(
            sourceAccountId = newSource?.let { resolveAccount(it) } ?: transaction.sourceAccountId,
            destinationAccountId = newDestination?.let { resolveAccount(it) } ?: transaction.destinationAccountId,
            categoryId = categoryName?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
                findCategoryByName(name)?.id ?: insertCategory(CategoryEntity(name = name))
            },
        )
        val rowId = upsert(resolved)
        // Upsert returns -1 when it updated an existing row.
        val id = if (transaction.id != 0L) transaction.id else rowId

        clearTags(id)
        val tagIds = tagNames.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
            .map { name -> findTagByName(name)?.id ?: insertTag(TagEntity(name = name)) }
        insertTagRefs(tagIds.map { TransactionTagCrossRef(transactionId = id, tagId = it) })
        return id
    }

    private suspend fun resolveAccount(account: AccountEntity): Long =
        findAccountByName(account.name, account.type)?.id ?: insertAccount(account)
}
