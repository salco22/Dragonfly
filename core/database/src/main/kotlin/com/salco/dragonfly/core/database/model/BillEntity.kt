package com.salco.dragonfly.core.database.model

import androidx.room.ColumnInfo
import kotlinx.serialization.Serializable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.salco.dragonfly.core.model.Bill
import com.salco.dragonfly.core.model.BillPayment
import com.salco.dragonfly.core.model.RecurrenceFrequency
import com.salco.dragonfly.core.model.RecurrenceRule
import java.time.LocalDate

@Entity(tableName = "bills")
@Serializable
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "amount_min") val amountMin: Long,
    @ColumnInfo(name = "amount_max") val amountMax: Long,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    val frequency: RecurrenceFrequency,
    val interval: Int,
    /** Days since the epoch of the first expected date. */
    @ColumnInfo(name = "start_date") val startDate: Long,
    @ColumnInfo(name = "end_date") val endDate: Long?,
    val active: Boolean,
    val notes: String?,
)

fun BillEntity.asExternalModel() = Bill(
    id = id,
    name = name,
    amountMin = amountMin,
    amountMax = amountMax,
    currencyCode = currencyCode,
    rule = RecurrenceRule(frequency, interval, LocalDate.ofEpochDay(startDate), endDate?.let(LocalDate::ofEpochDay)),
    active = active,
    notes = notes,
)

fun Bill.asEntity() = BillEntity(
    id = id,
    name = name,
    amountMin = amountMin,
    amountMax = amountMax,
    currencyCode = currencyCode,
    frequency = rule.frequency,
    interval = rule.interval,
    startDate = rule.startDate.toEpochDay(),
    endDate = rule.endDate?.toEpochDay(),
    active = active,
    notes = notes,
)

data class BillPaymentRow(
    @ColumnInfo(name = "bill_id") val billId: Long,
    val id: Long,
    val date: Long,
    val amount: Long,
)

fun BillPaymentRow.asExternalModel() = BillPayment(billId, id, LocalDate.ofEpochDay(date), amount)
