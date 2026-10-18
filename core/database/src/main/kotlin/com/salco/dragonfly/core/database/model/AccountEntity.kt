package com.salco.dragonfly.core.database.model

import androidx.room.ColumnInfo
import kotlinx.serialization.Serializable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.AssetRole
import com.salco.dragonfly.core.model.LiabilityType
import java.time.Instant

@Entity(tableName = "accounts")
@Serializable
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "opening_balance") val openingBalance: Long,
    val iban: String?,
    val notes: String?,
    val active: Boolean,
    @ColumnInfo(name = "include_in_net_worth") val includeInNetWorth: Boolean,
    @ColumnInfo(name = "asset_role") val assetRole: AssetRole?,
    @ColumnInfo(name = "liability_type") val liabilityType: LiabilityType?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** An account with its balance: opening balance plus incoming minus outgoing transactions. */
data class AccountWithBalance(
    @Embedded val account: AccountEntity,
    val balance: Long,
)

fun AccountWithBalance.asExternalModel() = account.asExternalModel(balance)

fun AccountEntity.asExternalModel(balance: Long = openingBalance) = Account(
    id = id,
    name = name,
    type = type,
    currencyCode = currencyCode,
    openingBalance = openingBalance,
    balance = balance,
    iban = iban,
    notes = notes,
    active = active,
    includeInNetWorth = includeInNetWorth,
    assetRole = assetRole,
    liabilityType = liabilityType,
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun Account.asEntity() = AccountEntity(
    id = id,
    name = name,
    type = type,
    currencyCode = currencyCode,
    openingBalance = openingBalance,
    iban = iban,
    notes = notes,
    active = active,
    includeInNetWorth = includeInNetWorth,
    assetRole = assetRole,
    liabilityType = liabilityType,
    createdAt = createdAt.toEpochMilli(),
)
