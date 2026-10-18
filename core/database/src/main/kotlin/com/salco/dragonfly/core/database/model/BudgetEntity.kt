package com.salco.dragonfly.core.database.model

import androidx.room.ColumnInfo
import kotlinx.serialization.Serializable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.salco.dragonfly.core.model.Budget
import com.salco.dragonfly.core.model.BudgetSpending

@Entity(tableName = "budgets")
@Serializable
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Long,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    val active: Boolean,
)

fun BudgetEntity.asExternalModel() = Budget(id, name, amount, currencyCode, active)
fun Budget.asEntity() = BudgetEntity(id, name, amount, currencyCode, active)

data class BudgetSpendingRow(
    @Embedded val budget: BudgetEntity,
    val spent: Long,
)

fun BudgetSpendingRow.asExternalModel() = BudgetSpending(budget.asExternalModel(), spent)

data class CurrencyTotalRow(
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    val total: Long,
)
