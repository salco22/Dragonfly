package com.salco.dragonfly.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.salco.dragonfly.core.model.RecurrenceFrequency
import com.salco.dragonfly.core.model.TransactionType

/**
 * A recurring transaction template and its schedule. Each side is either an existing account id
 * or, for a payee/payer typed for the first time, a name resolved when transactions are created.
 */
@Entity(
    tableName = "recurring_transactions",
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
            entity = BudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budget_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("source_account_id"), Index("destination_account_id"), Index("budget_id"), Index("next_date")],
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val description: String,
    val amount: Long,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "foreign_amount") val foreignAmount: Long?,
    @ColumnInfo(name = "source_account_id") val sourceAccountId: Long?,
    @ColumnInfo(name = "source_name") val sourceName: String?,
    @ColumnInfo(name = "destination_account_id") val destinationAccountId: Long?,
    @ColumnInfo(name = "destination_name") val destinationName: String?,
    val notes: String?,
    @ColumnInfo(name = "category_name") val categoryName: String?,
    /** Tag names separated by new lines. */
    @ColumnInfo(name = "tag_names") val tagNames: String,
    @ColumnInfo(name = "budget_id") val budgetId: Long?,
    val frequency: RecurrenceFrequency,
    val interval: Int,
    /** Days since the epoch. */
    @ColumnInfo(name = "start_date") val startDate: Long,
    @ColumnInfo(name = "end_date") val endDate: Long?,
    @ColumnInfo(name = "max_occurrences") val maxOccurrences: Int?,
    @ColumnInfo(name = "occurrences_created") val occurrencesCreated: Int,
    /** Days since the epoch of the next transaction to create; null once the rule has ended. */
    @ColumnInfo(name = "next_date") val nextDate: Long?,
    val active: Boolean,
)

/** A recurring transaction with the display names of both sides. */
data class PopulatedRecurringTransaction(
    @androidx.room.Embedded val recurring: RecurringTransactionEntity,
    @ColumnInfo(name = "resolved_source_name") val resolvedSourceName: String?,
    @ColumnInfo(name = "resolved_destination_name") val resolvedDestinationName: String?,
)
