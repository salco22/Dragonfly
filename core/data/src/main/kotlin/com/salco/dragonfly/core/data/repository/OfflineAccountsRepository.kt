package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.database.dao.AccountDao
import com.salco.dragonfly.core.database.model.asEntity
import com.salco.dragonfly.core.database.model.asExternalModel
import com.salco.dragonfly.core.model.Account
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class OfflineAccountsRepository @Inject constructor(
    private val accountDao: AccountDao,
) : AccountsRepository {

    override fun observeAccounts(): Flow<List<Account>> =
        accountDao.observeAll().map { entities -> entities.map { it.asExternalModel() } }

    override fun observeAccount(id: Long): Flow<Account?> =
        accountDao.observeById(id).map { it?.asExternalModel() }

    override suspend fun saveAccount(account: Account): Long {
        val rowId = accountDao.upsert(account.asEntity())
        // Upsert returns -1 when it updated an existing row.
        return if (account.id != 0L) account.id else rowId
    }

    override suspend fun deleteAccount(id: Long) = accountDao.deleteById(id)
}
