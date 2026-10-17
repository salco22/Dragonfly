package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.database.dao.TransactionDao
import com.salco.dragonfly.core.database.model.AccountEntity
import com.salco.dragonfly.core.database.model.PopulatedTransaction
import com.salco.dragonfly.core.database.model.TransactionEntity
import com.salco.dragonfly.core.database.model.asExternalModel
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.Transaction
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

internal class OfflineTransactionsRepository @Inject constructor(
    private val transactionDao: TransactionDao,
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
            accountId = filter.accountId,
            fromEpochDay = filter.from?.toEpochDay(),
            toEpochDay = filter.to?.toEpochDay(),
        ).map(List<PopulatedTransaction>::asModels)

    override suspend fun saveTransaction(draft: TransactionDraft): Long =
        transactionDao.save(
            transaction = TransactionEntity(
                id = draft.id,
                type = draft.type,
                description = draft.description,
                date = draft.date.toEpochDay(),
                amount = draft.amount,
                currencyCode = draft.currencyCode,
                foreignAmount = draft.foreignAmount,
                sourceAccountId = (draft.source as? AccountRef.Existing)?.id ?: 0,
                destinationAccountId = (draft.destination as? AccountRef.Existing)?.id ?: 0,
                notes = draft.notes,
            ),
            newSource = (draft.source as? AccountRef.New)?.asEntity(),
            newDestination = (draft.destination as? AccountRef.New)?.asEntity(),
            categoryName = draft.categoryName,
            tagNames = draft.tagNames,
        )

    override suspend fun deleteTransaction(id: Long) = transactionDao.deleteById(id)
}

private fun List<PopulatedTransaction>.asModels() = map { it.asExternalModel() }

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
