package com.salco.dragonfly.core.database.model

import androidx.room.ColumnInfo
import kotlinx.serialization.Serializable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.salco.dragonfly.core.model.PiggyBank
import com.salco.dragonfly.core.model.PiggyBankEvent
import com.salco.dragonfly.core.model.PiggyBankSummary
import java.time.LocalDate

@Entity(
    tableName = "piggy_banks",
    foreignKeys = [
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["account_id"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("account_id")],
)
@Serializable
data class PiggyBankEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "account_id") val accountId: Long,
    @ColumnInfo(name = "target_amount") val targetAmount: Long?,
    @ColumnInfo(name = "start_date") val startDate: Long,
    @ColumnInfo(name = "target_date") val targetDate: Long?,
    val notes: String?,
)

@Entity(
    tableName = "piggy_bank_events",
    foreignKeys = [
        ForeignKey(
            entity = PiggyBankEntity::class,
            parentColumns = ["id"],
            childColumns = ["piggy_bank_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("piggy_bank_id")],
)
@Serializable
data class PiggyBankEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "piggy_bank_id") val piggyBankId: Long,
    val date: Long,
    val amount: Long,
)

data class PopulatedPiggyBank(
    @Embedded val piggyBank: PiggyBankEntity,
    @ColumnInfo(name = "account_name") val accountName: String,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    val saved: Long,
)

fun PiggyBankEntity.asExternalModel() = PiggyBank(
    id = id,
    name = name,
    accountId = accountId,
    targetAmount = targetAmount,
    startDate = LocalDate.ofEpochDay(startDate),
    targetDate = targetDate?.let(LocalDate::ofEpochDay),
    notes = notes,
)

fun PiggyBank.asEntity() = PiggyBankEntity(
    id = id,
    name = name,
    accountId = accountId,
    targetAmount = targetAmount,
    startDate = startDate.toEpochDay(),
    targetDate = targetDate?.toEpochDay(),
    notes = notes,
)

fun PopulatedPiggyBank.asExternalModel() = PiggyBankSummary(piggyBank.asExternalModel(), accountName, currencyCode, saved)

fun PiggyBankEventEntity.asExternalModel() = PiggyBankEvent(id, piggyBankId, LocalDate.ofEpochDay(date), amount)
