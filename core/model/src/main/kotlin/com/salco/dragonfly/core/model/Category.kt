package com.salco.dragonfly.core.model

data class Category(
    val id: Long = 0,
    val name: String,
)

/** A category with what was spent and earned in it over a period, per currency in minor units. */
data class CategorySummary(
    val category: Category,
    val spent: Map<String, Long>,
    val earned: Map<String, Long>,
)

data class Tag(
    val id: Long = 0,
    val name: String,
)

data class TagSummary(
    val tag: Tag,
    val transactionCount: Int,
)
