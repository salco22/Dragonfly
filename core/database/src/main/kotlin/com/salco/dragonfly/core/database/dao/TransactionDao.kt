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
import com.salco.dragonfly.core.database.model.TransactionGroupEntity
import com.salco.dragonfly.core.database.model.TransactionTagCrossRef
import com.salco.dragonfly.core.model.AccountType
import kotlinx.coroutines.flow.Flow

private const val POPULATED_SELECT = """
    SELECT t.*,
        s.name AS source_name, s.type AS source_type, s.currency_code AS source_currency,
        d.name AS destination_name, d.type AS destination_type, d.currency_code AS destination_currency,
        c.name AS category_name,
        b.name AS budget_name,
        bl.name AS bill_name,
        g.title AS group_title
    FROM transactions t
    JOIN accounts s ON s.id = t.source_account_id
    JOIN accounts d ON d.id = t.destination_account_id
    LEFT JOIN categories c ON c.id = t.category_id
    LEFT JOIN budgets b ON b.id = t.budget_id
    LEFT JOIN bills bl ON bl.id = t.bill_id
    LEFT JOIN transaction_groups g ON g.id = t.group_id
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
                OR g.title LIKE '%' || :query || '%'
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

    /** Transactions on [accountId] with the same day, amount and description, to spot duplicates. */
    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE (source_account_id = :accountId OR destination_account_id = :accountId)
            AND date = :epochDay AND amount = :amount AND description = :description COLLATE NOCASE
        """,
    )
    abstract suspend fun countSimilar(accountId: Long, epochDay: Long, amount: Long, description: String): Int

    /** Parts of a split transaction, in the order they were entered. */
    @Transaction
    @Query("$POPULATED_SELECT WHERE t.group_id = :groupId ORDER BY t.id")
    abstract fun observeGroup(groupId: Long): Flow<List<PopulatedTransaction>>

    @Upsert
    abstract suspend fun upsert(transaction: TransactionEntity): Long

    @Upsert
    protected abstract suspend fun upsertGroup(group: TransactionGroupEntity): Long

    @Query("SELECT id FROM transactions WHERE group_id = :groupId")
    protected abstract suspend fun splitIds(groupId: Long): List<Long>

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    protected abstract suspend fun deleteByIds(ids: List<Long>)

    /** Deletes a split transaction with all its parts. */
    @Query("DELETE FROM transaction_groups WHERE id = :groupId")
    abstract suspend fun deleteGroup(groupId: Long)

    /** Deletes a group left with fewer than two parts, turning a remaining part into an ordinary transaction. */
    @Query("UPDATE transactions SET group_id = NULL WHERE group_id = :groupId")
    protected abstract suspend fun ungroup(groupId: Long)

    /**
     * Saves a split transaction atomically: the group, then each part through [save] (see
     * [SplitPart]); parts that were in the group but are no longer listed are deleted. With a
     * single part the group is dissolved and the part becomes an ordinary transaction.
     * Returns the group id, or 0 when it was dissolved.
     */
    @Transaction
    open suspend fun saveGroup(groupId: Long, title: String, parts: List<SplitPart>): Long {
        val savedGroupId = if (parts.size > 1) {
            upsertGroup(TransactionGroupEntity(id = groupId, title = title)).let { if (groupId != 0L) groupId else it }
        } else {
            0L
        }
        val keptIds = parts.map { part ->
            save(
                transaction = part.transaction.copy(groupId = savedGroupId.takeIf { it != 0L }),
                newSource = part.newSource,
                newDestination = part.newDestination,
                categoryName = part.categoryName,
                tagNames = part.tagNames,
            )
        }
        if (groupId != 0L) {
            deleteByIds(splitIds(groupId).filter { it !in keptIds })
            if (savedGroupId == 0L) {
                ungroup(groupId)
                deleteGroup(groupId)
            }
        }
        return savedGroupId
    }

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

/** One part of a split transaction, with what [TransactionDao.save] resolves by name. */
data class SplitPart(
    val transaction: TransactionEntity,
    val newSource: AccountEntity? = null,
    val newDestination: AccountEntity? = null,
    val categoryName: String? = null,
    val tagNames: List<String> = emptyList(),
)
