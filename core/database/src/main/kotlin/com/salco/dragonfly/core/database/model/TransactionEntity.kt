package com.salco.dragonfly.core.database.model

import androidx.room.ColumnInfo
import kotlinx.serialization.Serializable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.Relation
import androidx.room.PrimaryKey
import com.salco.dragonfly.core.model.AccountSummary
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.BillRef
import com.salco.dragonfly.core.model.BudgetRef
import com.salco.dragonfly.core.model.Category
import com.salco.dragonfly.core.model.Transaction
import com.salco.dragonfly.core.model.TransactionType
import java.time.LocalDate

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_account_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["destination_account_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budget_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = BillEntity::class,
            parentColumns = ["id"],
            childColumns = ["bill_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("source_account_id"),
        Index("destination_account_id"),
        Index("date"),
        Index("category_id"),
        Index("budget_id"),
        Index("bill_id"),
    ],
)
@Serializable
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val description: String,
    /** Days since the epoch, see [LocalDate.toEpochDay]. */
    val date: Long,
    val amount: Long,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "foreign_amount") val foreignAmount: Long?,
    @ColumnInfo(name = "source_account_id") val sourceAccountId: Long,
    @ColumnInfo(name = "destination_account_id") val destinationAccountId: Long,
    val notes: String?,
    @ColumnInfo(name = "category_id", defaultValue = "NULL") val categoryId: Long? = null,
    @ColumnInfo(name = "budget_id", defaultValue = "NULL") val budgetId: Long? = null,
    @ColumnInfo(name = "bill_id", defaultValue = "NULL") val billId: Long? = null,
)

/** A transaction joined with the summary of both its accounts. */
data class PopulatedTransaction(
    @Embedded val transaction: TransactionEntity,
    @ColumnInfo(name = "source_name") val sourceName: String,
    @ColumnInfo(name = "source_type") val sourceType: AccountType,
    @ColumnInfo(name = "source_currency") val sourceCurrency: String,
    @ColumnInfo(name = "destination_name") val destinationName: String,
    @ColumnInfo(name = "destination_type") val destinationType: AccountType,
    @ColumnInfo(name = "destination_currency") val destinationCurrency: String,
    @ColumnInfo(name = "category_name") val categoryName: String?,
    @ColumnInfo(name = "budget_name") val budgetName: String?,
    @ColumnInfo(name = "bill_name") val billName: String?,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(TransactionTagCrossRef::class, parentColumn = "transaction_id", entityColumn = "tag_id"),
    )
    val tags: List<TagEntity>,
)

fun PopulatedTransaction.asExternalModel() = Transaction(
    id = transaction.id,
    type = transaction.type,
    description = transaction.description,
    date = LocalDate.ofEpochDay(transaction.date),
    amount = transaction.amount,
    currencyCode = transaction.currencyCode,
    foreignAmount = transaction.foreignAmount,
    source = AccountSummary(transaction.sourceAccountId, sourceName, sourceType, sourceCurrency),
    destination = AccountSummary(transaction.destinationAccountId, destinationName, destinationType, destinationCurrency),
    notes = transaction.notes,
    category = transaction.categoryId?.let { id -> categoryName?.let { Category(id, it) } },
    tags = tags.map { it.asExternalModel() }.sortedBy { it.name.lowercase() },
    budget = transaction.budgetId?.let { id -> budgetName?.let { BudgetRef(id, it) } },
    bill = transaction.billId?.let { id -> billName?.let { BillRef(id, it) } },
)
