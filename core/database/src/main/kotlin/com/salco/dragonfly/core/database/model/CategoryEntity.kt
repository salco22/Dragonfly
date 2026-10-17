package com.salco.dragonfly.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.salco.dragonfly.core.model.Category
import com.salco.dragonfly.core.model.Tag

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

fun CategoryEntity.asExternalModel() = Category(id = id, name = name)
fun Category.asEntity() = CategoryEntity(id = id, name = name)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

fun TagEntity.asExternalModel() = Tag(id = id, name = name)
fun Tag.asEntity() = TagEntity(id = id, name = name)

@Entity(
    tableName = "transaction_tags",
    primaryKeys = ["transaction_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("tag_id")],
)
data class TransactionTagCrossRef(
    @ColumnInfo(name = "transaction_id") val transactionId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long,
)

/** One row per category and currency with the totals of a period; currency is null for unused categories. */
data class CategoryTotalsRow(
    val id: Long,
    val name: String,
    @ColumnInfo(name = "currency_code") val currencyCode: String?,
    val spent: Long,
    val earned: Long,
)

data class TagUsageRow(
    val id: Long,
    val name: String,
    @ColumnInfo(name = "transaction_count") val transactionCount: Int,
)
