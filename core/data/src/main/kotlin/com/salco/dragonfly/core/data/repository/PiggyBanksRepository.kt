package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.database.DatabaseTransactionRunner
import com.salco.dragonfly.core.database.dao.AccountDao
import com.salco.dragonfly.core.database.dao.PiggyBankDao
import com.salco.dragonfly.core.database.model.PiggyBankEventEntity
import com.salco.dragonfly.core.database.model.asEntity
import com.salco.dragonfly.core.database.model.asExternalModel
import com.salco.dragonfly.core.model.PiggyBank
import com.salco.dragonfly.core.model.PiggyBankEvent
import com.salco.dragonfly.core.model.PiggyBankSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

interface PiggyBanksRepository {
    fun observePiggyBanks(): Flow<List<PiggyBankSummary>>
    fun observePiggyBank(id: Long): Flow<PiggyBankSummary?>
    fun observeAccountPiggyBanks(accountId: Long): Flow<List<PiggyBankSummary>>
    fun observeEvents(piggyBankId: Long): Flow<List<PiggyBankEvent>>

    /** Saves [piggyBank] and returns its id; fails with [DuplicateNameException] if the name is taken. */
    suspend fun savePiggyBank(piggyBank: PiggyBank): Long

    /** Deletes the piggy bank; the money it held becomes available in its account again. */
    suspend fun deletePiggyBank(id: Long)

    /**
     * Puts [amount] aside (positive) or takes it back (negative). Fails with
     * [InsufficientFundsException] when adding more than the account has available, or removing
     * more than the piggy bank holds.
     */
    suspend fun moveMoney(piggyBankId: Long, amount: Long, date: LocalDate)
}

/** [available] is what could be moved, in minor units. */
class InsufficientFundsException(val available: Long) : IllegalArgumentException("Only $available available")

internal class OfflinePiggyBanksRepository @Inject constructor(
    private val piggyBankDao: PiggyBankDao,
    private val accountDao: AccountDao,
    private val runInTransaction: DatabaseTransactionRunner,
) : PiggyBanksRepository {

    override fun observePiggyBanks(): Flow<List<PiggyBankSummary>> =
        piggyBankDao.observeAll().map { list -> list.map { it.asExternalModel() } }

    override fun observePiggyBank(id: Long): Flow<PiggyBankSummary?> = piggyBankDao.observeById(id).map { it?.asExternalModel() }

    override fun observeAccountPiggyBanks(accountId: Long): Flow<List<PiggyBankSummary>> =
        piggyBankDao.observeForAccount(accountId).map { list -> list.map { it.asExternalModel() } }

    override fun observeEvents(piggyBankId: Long): Flow<List<PiggyBankEvent>> =
        piggyBankDao.observeEvents(piggyBankId).map { list -> list.map { it.asExternalModel() } }

    override suspend fun savePiggyBank(piggyBank: PiggyBank): Long {
        val name = piggyBank.name.trim()
        val existing = piggyBankDao.findByName(name)
        if (existing != null && existing.id != piggyBank.id) throw DuplicateNameException(name)
        val rowId = piggyBankDao.upsert(piggyBank.copy(name = name).asEntity())
        return if (piggyBank.id != 0L) piggyBank.id else rowId
    }

    override suspend fun deletePiggyBank(id: Long) = piggyBankDao.deleteById(id)

    override suspend fun moveMoney(piggyBankId: Long, amount: Long, date: LocalDate) = runInTransaction {
        if (amount == 0L) return@runInTransaction
        val piggyBank = piggyBankDao.getById(piggyBankId) ?: return@runInTransaction
        if (amount > 0) {
            val balance = accountDao.observeById(piggyBank.accountId).first()?.balance ?: 0
            val available = (balance - piggyBankDao.savedInAccount(piggyBank.accountId)).coerceAtLeast(0)
            if (amount > available) throw InsufficientFundsException(available)
        } else {
            val saved = piggyBankDao.savedIn(piggyBankId)
            if (-amount > saved) throw InsufficientFundsException(saved)
        }
        piggyBankDao.insertEvent(PiggyBankEventEntity(piggyBankId = piggyBankId, date = date.toEpochDay(), amount = amount))
    }
}
