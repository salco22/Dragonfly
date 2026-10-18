package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.database.dao.BillDao
import com.salco.dragonfly.core.database.model.asEntity
import com.salco.dragonfly.core.database.model.asExternalModel
import com.salco.dragonfly.core.model.Bill
import com.salco.dragonfly.core.model.BillPayment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface BillsRepository {
    fun observeBills(): Flow<List<Bill>>
    fun observeBill(id: Long): Flow<Bill?>

    /** Every withdrawal linked to a bill, newest first. */
    fun observePayments(): Flow<List<BillPayment>>

    /** Saves [bill] and returns its id; fails with [DuplicateNameException] if the name is taken. */
    suspend fun saveBill(bill: Bill): Long

    /** Deletes the bill; its payments stay as ordinary withdrawals. */
    suspend fun deleteBill(id: Long)
}

internal class OfflineBillsRepository @Inject constructor(
    private val billDao: BillDao,
) : BillsRepository {

    override fun observeBills(): Flow<List<Bill>> = billDao.observeAll().map { list -> list.map { it.asExternalModel() } }

    override fun observeBill(id: Long): Flow<Bill?> = billDao.observeById(id).map { it?.asExternalModel() }

    override fun observePayments(): Flow<List<BillPayment>> =
        billDao.observePayments().map { rows -> rows.map { it.asExternalModel() } }

    override suspend fun saveBill(bill: Bill): Long {
        val name = bill.name.trim()
        val existing = billDao.findByName(name)
        if (existing != null && existing.id != bill.id) throw DuplicateNameException(name)
        val rowId = billDao.upsert(bill.copy(name = name).asEntity())
        return if (bill.id != 0L) bill.id else rowId
    }

    override suspend fun deleteBill(id: Long) = billDao.deleteById(id)
}
