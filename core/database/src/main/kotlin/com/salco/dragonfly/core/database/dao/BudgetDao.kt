package com.salco.dragonfly.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.salco.dragonfly.core.database.model.BudgetEntity
import com.salco.dragonfly.core.database.model.BudgetSpendingRow
import com.salco.dragonfly.core.database.model.CurrencyTotalRow
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets ORDER BY active DESC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    fun observeById(id: Long): Flow<BudgetEntity?>

    /**
     * Every budget with the withdrawals assigned to it between two epoch days (inclusive). Only
     * withdrawals in the budget's own currency count towards it.
     */
    @Query(
        """
        SELECT b.*, COALESCE((
            SELECT SUM(t.amount) FROM transactions t
            WHERE t.budget_id = b.id AND t.type = 'WITHDRAWAL' AND t.currency_code = b.currency_code
                AND t.date BETWEEN :fromEpochDay AND :toEpochDay
        ), 0) AS spent
        FROM budgets b
        ORDER BY b.active DESC, b.name COLLATE NOCASE
        """,
    )
    fun observeSpending(fromEpochDay: Long, toEpochDay: Long): Flow<List<BudgetSpendingRow>>

    /** Withdrawals without a budget between two epoch days (inclusive), per currency. */
    @Query(
        """
        SELECT currency_code, SUM(amount) AS total FROM transactions
        WHERE type = 'WITHDRAWAL' AND budget_id IS NULL AND date BETWEEN :fromEpochDay AND :toEpochDay
        GROUP BY currency_code
        """,
    )
    fun observeUnbudgeted(fromEpochDay: Long, toEpochDay: Long): Flow<List<CurrencyTotalRow>>

    @Query("SELECT * FROM budgets WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): BudgetEntity?

    @Upsert
    suspend fun upsert(budget: BudgetEntity): Long

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)
}
