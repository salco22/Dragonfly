package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.database.dao.CategoryDao
import com.salco.dragonfly.core.database.model.asEntity
import com.salco.dragonfly.core.database.model.asExternalModel
import com.salco.dragonfly.core.model.Category
import com.salco.dragonfly.core.model.CategorySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

interface CategoriesRepository {
    fun observeCategories(): Flow<List<Category>>

    /** Every category with what was spent and earned between [from] and [to] inclusive. */
    fun observeCategorySummaries(from: LocalDate, to: LocalDate): Flow<List<CategorySummary>>

    /** Saves [category] and returns its id; fails with [DuplicateNameException] if the name is taken. */
    suspend fun saveCategory(category: Category): Long

    /** Deletes the category; its transactions become uncategorized. */
    suspend fun deleteCategory(id: Long)
}

class DuplicateNameException(name: String) : IllegalArgumentException("\"$name\" already exists")

internal class OfflineCategoriesRepository @Inject constructor(
    private val categoryDao: CategoryDao,
) : CategoriesRepository {

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.asExternalModel() } }

    override fun observeCategorySummaries(from: LocalDate, to: LocalDate): Flow<List<CategorySummary>> =
        categoryDao.observeTotals(from.toEpochDay(), to.toEpochDay()).map { rows ->
            rows.groupBy { it.id }.map { (id, perCurrency) ->
                val used = perCurrency.filter { it.currencyCode != null }
                CategorySummary(
                    category = Category(id, perCurrency.first().name),
                    spent = used.filter { it.spent != 0L }.associate { it.currencyCode!! to it.spent },
                    earned = used.filter { it.earned != 0L }.associate { it.currencyCode!! to it.earned },
                )
            }
        }

    override suspend fun saveCategory(category: Category): Long {
        val name = category.name.trim()
        val existing = categoryDao.findByName(name)
        if (existing != null && existing.id != category.id) throw DuplicateNameException(name)
        val rowId = categoryDao.upsert(category.copy(name = name).asEntity())
        return if (category.id != 0L) category.id else rowId
    }

    override suspend fun deleteCategory(id: Long) = categoryDao.deleteById(id)
}
