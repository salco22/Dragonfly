package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.database.dao.BudgetDao
import com.salco.dragonfly.core.database.model.asEntity
import com.salco.dragonfly.core.database.model.asExternalModel
import com.salco.dragonfly.core.model.Budget
import com.salco.dragonfly.core.model.BudgetSpending
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

interface BudgetsRepository {
    fun observeBudgets(): Flow<List<Budget>>
    fun observeBudget(id: Long): Flow<Budget?>

    /** Every budget with what was spent in it between [from] and [to] inclusive. */
    fun observeBudgetSpending(from: LocalDate, to: LocalDate): Flow<List<BudgetSpending>>

    /** Withdrawals without a budget between [from] and [to] inclusive, per currency. */
    fun observeUnbudgetedSpending(from: LocalDate, to: LocalDate): Flow<Map<String, Long>>

    /** Saves [budget] and returns its id; fails with [DuplicateNameException] if the name is taken. */
    suspend fun saveBudget(budget: Budget): Long

    /** Deletes the budget; its withdrawals stay but lose the budget. */
    suspend fun deleteBudget(id: Long)
}

internal class OfflineBudgetsRepository @Inject constructor(
    private val budgetDao: BudgetDao,
) : BudgetsRepository {

    override fun observeBudgets(): Flow<List<Budget>> = budgetDao.observeAll().map { list -> list.map { it.asExternalModel() } }

    override fun observeBudget(id: Long): Flow<Budget?> = budgetDao.observeById(id).map { it?.asExternalModel() }

    override fun observeBudgetSpending(from: LocalDate, to: LocalDate): Flow<List<BudgetSpending>> =
        budgetDao.observeSpending(from.toEpochDay(), to.toEpochDay()).map { rows -> rows.map { it.asExternalModel() } }

    override fun observeUnbudgetedSpending(from: LocalDate, to: LocalDate): Flow<Map<String, Long>> =
        budgetDao.observeUnbudgeted(from.toEpochDay(), to.toEpochDay()).map { rows -> rows.associate { it.currencyCode to it.total } }

    override suspend fun saveBudget(budget: Budget): Long {
        val name = budget.name.trim()
        val existing = budgetDao.findByName(name)
        if (existing != null && existing.id != budget.id) throw DuplicateNameException(name)
        val rowId = budgetDao.upsert(budget.copy(name = name).asEntity())
        return if (budget.id != 0L) budget.id else rowId
    }

    override suspend fun deleteBudget(id: Long) = budgetDao.deleteById(id)
}
