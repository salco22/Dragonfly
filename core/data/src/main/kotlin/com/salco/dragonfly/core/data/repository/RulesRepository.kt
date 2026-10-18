package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.database.dao.RuleDao
import com.salco.dragonfly.core.database.model.asExternalModel
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.Rule
import com.salco.dragonfly.core.model.Transaction
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface RulesRepository {
    fun observeRules(): Flow<List<Rule>>
    fun observeRule(id: Long): Flow<Rule?>
    suspend fun saveRule(rule: Rule): Long
    suspend fun setRuleActive(id: Long, active: Boolean)
    suspend fun deleteRule(id: Long)

    /** How many existing transactions [rule] would change. */
    suspend fun countAffected(rule: Rule): Int

    /**
     * Applies [rule] (or every active rule when null) to all existing transactions and returns
     * how many were changed. New transactions get the active rules automatically when saved.
     */
    suspend fun applyToExisting(rule: Rule? = null): Int
}

internal class OfflineRulesRepository @Inject constructor(
    private val ruleDao: RuleDao,
    private val transactionsRepository: TransactionsRepository,
    private val ruleApplier: RuleApplier,
) : RulesRepository {

    override fun observeRules(): Flow<List<Rule>> = ruleDao.observeAll().map { list -> list.map { it.asExternalModel() } }

    override fun observeRule(id: Long): Flow<Rule?> = ruleDao.observeById(id).map { it?.asExternalModel() }

    override suspend fun saveRule(rule: Rule): Long = ruleDao.save(rule)

    override suspend fun setRuleActive(id: Long, active: Boolean) = ruleDao.setActive(id, active)

    override suspend fun deleteRule(id: Long) = ruleDao.deleteById(id)

    override suspend fun countAffected(rule: Rule): Int =
        changes(listOf(rule.copy(active = true))).size

    override suspend fun applyToExisting(rule: Rule?): Int {
        val rules = rule?.let { listOf(it.copy(active = true)) } ?: ruleDao.getActive().map { it.asExternalModel() }
        val changed = changes(rules)
        changed.forEach { transactionsRepository.saveTransaction(it) }
        return changed.size
    }

    private suspend fun changes(rules: List<Rule>): List<TransactionDraft> =
        transactionsRepository.searchTransactions(TransactionFilter()).first().mapNotNull { transaction ->
            val draft = transaction.asDraft()
            ruleApplier.apply(draft, rules).takeIf { it != draft }
        }
}

private fun Transaction.asDraft() = TransactionDraft(
    id = id,
    type = type,
    description = description,
    date = date,
    amount = amount,
    currencyCode = currencyCode,
    foreignAmount = foreignAmount,
    source = AccountRef.Existing(source.id),
    destination = AccountRef.Existing(destination.id),
    notes = notes,
    categoryName = category?.name,
    tagNames = tags.map { it.name },
    budgetId = budget?.id,
    billId = bill?.id,
)
