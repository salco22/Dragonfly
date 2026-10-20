package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.database.dao.SplitPart
import com.salco.dragonfly.core.database.dao.TransactionDao
import com.salco.dragonfly.core.database.model.AccountEntity
import com.salco.dragonfly.core.database.model.PopulatedTransaction
import com.salco.dragonfly.core.database.model.TransactionEntity
import com.salco.dragonfly.core.database.model.asExternalModel
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.Transaction
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionFilter
import com.salco.dragonfly.core.model.TransactionGroupDraft
import com.salco.dragonfly.core.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

internal class OfflineTransactionsRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val ruleApplier: RuleApplier,
) : TransactionsRepository {

    override fun observeTransactions(from: LocalDate, to: LocalDate): Flow<List<Transaction>> =
        transactionDao.observeBetween(from.toEpochDay(), to.toEpochDay()).map(List<PopulatedTransaction>::asModels)

    override fun observeAccountTransactions(accountId: Long): Flow<List<Transaction>> =
        transactionDao.observeForAccount(accountId).map(List<PopulatedTransaction>::asModels)

    override fun observeTransaction(id: Long): Flow<Transaction?> =
        transactionDao.observeById(id).map { it?.asExternalModel() }

    override fun searchTransactions(filter: TransactionFilter): Flow<List<Transaction>> =
        transactionDao.search(
            query = filter.query?.trim()?.takeIf { it.isNotEmpty() },
            type = filter.type?.name,
            categoryId = filter.categoryId,
            tagId = filter.tagId,
            budgetId = filter.budgetId,
            billId = filter.billId,
            accountId = filter.accountId,
            fromEpochDay = filter.from?.toEpochDay(),
            toEpochDay = filter.to?.toEpochDay(),
        ).map(List<PopulatedTransaction>::asModels)

    // Rules run on new transactions only, so manual edits are never overridden.
    override suspend fun saveTransaction(draft: TransactionDraft, applyRules: Boolean): Long = saveResolved(
        if (draft.id == 0L && applyRules) ruleApplier.apply(draft) else draft,
    )

    private suspend fun saveResolved(draft: TransactionDraft): Long = draft.asSplitPart().let { part ->
        transactionDao.save(part.transaction, part.newSource, part.newDestination, part.categoryName, part.tagNames)
    }

    override fun observeSplitTransaction(groupId: Long): Flow<List<Transaction>> =
        transactionDao.observeGroup(groupId).map(List<PopulatedTransaction>::asModels)

    override suspend fun saveSplitTransaction(group: TransactionGroupDraft, applyRules: Boolean): Long = transactionDao.saveGroup(
        groupId = group.id,
        title = group.title,
        parts = group.splits.map { split -> (if (split.id == 0L && applyRules) ruleApplier.apply(split) else split).asSplitPart() },
    )

    override suspend fun deleteSplitTransaction(groupId: Long) = transactionDao.deleteGroup(groupId)

    override suspend fun deleteTransaction(id: Long) = transactionDao.deleteById(id)
}

private fun List<PopulatedTransaction>.asModels() = map { it.asExternalModel() }

private fun TransactionDraft.asSplitPart() = SplitPart(
    transaction = TransactionEntity(
        id = id,
        type = type,
        description = description,
        date = date.toEpochDay(),
        amount = amount,
        currencyCode = currencyCode,
        foreignAmount = foreignAmount,
        sourceAccountId = (source as? AccountRef.Existing)?.id ?: 0,
        destinationAccountId = (destination as? AccountRef.Existing)?.id ?: 0,
        notes = notes,
        budgetId = budgetId.takeIf { type == TransactionType.WITHDRAWAL },
        billId = billId.takeIf { type == TransactionType.WITHDRAWAL },
    ),
    newSource = (source as? AccountRef.New)?.asEntity(),
    newDestination = (destination as? AccountRef.New)?.asEntity(),
    categoryName = categoryName,
    tagNames = tagNames,
)

private fun AccountRef.New.asEntity() = AccountEntity(
    name = name.trim(),
    type = type,
    currencyCode = currencyCode,
    openingBalance = 0,
    iban = null,
    notes = null,
    active = true,
    includeInNetWorth = false,
    assetRole = null,
    liabilityType = null,
    createdAt = System.currentTimeMillis(),
)
