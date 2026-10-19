package com.salco.dragonfly.core.model

import java.time.LocalDate

/** Transaction types modelled after Firefly III. */
enum class TransactionType {
    /** Money leaves an asset or liability towards an expense account. */
    WITHDRAWAL,

    /** Money arrives on an asset or liability from a revenue account. */
    DEPOSIT,

    /** Money moves between two of the user's own asset or liability accounts. */
    TRANSFER,
}

/** Minimal account data shown alongside a transaction. */
data class AccountSummary(
    val id: Long,
    val name: String,
    val type: AccountType,
    val currencyCode: String,
)

data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val description: String,
    val date: LocalDate,
    /**
     * Positive amount in minor units, in [currencyCode]: the currency of the user's own account
     * (the source for withdrawals and transfers, the destination for deposits).
     */
    val amount: Long,
    val currencyCode: String,
    /** Amount received by the destination of a transfer between accounts in different currencies. */
    val foreignAmount: Long? = null,
    val source: AccountSummary,
    val destination: AccountSummary,
    val notes: String? = null,
    val category: Category? = null,
    val tags: List<Tag> = emptyList(),
    /** Budget of a withdrawal; only id and name are filled. */
    val budget: BudgetRef? = null,
    /** Bill or subscription paid by a withdrawal. */
    val bill: BillRef? = null,
    /** Split transaction this is a part of, as in Firefly III's transaction groups. */
    val groupId: Long? = null,
    val groupTitle: String? = null,
)

data class BudgetRef(val id: Long, val name: String)

/**
 * Reference to a transaction's counterparty: an existing account, or an expense/revenue account
 * to be created by name (as Firefly III does when typing a new payee).
 */
sealed interface AccountRef {
    data class Existing(val id: Long) : AccountRef
    data class New(val name: String, val type: AccountType, val currencyCode: String) : AccountRef
}

data class TransactionDraft(
    val id: Long = 0,
    val type: TransactionType,
    val description: String,
    val date: LocalDate,
    val amount: Long,
    val currencyCode: String,
    val foreignAmount: Long? = null,
    val source: AccountRef,
    val destination: AccountRef,
    val notes: String? = null,
    /** Category matched by name (ignoring case) or created when new; null leaves it uncategorized. */
    val categoryName: String? = null,
    /** Tags matched by name (ignoring case) or created when new. */
    val tagNames: List<String> = emptyList(),
    /** Budget of a withdrawal; ignored for other types. */
    val budgetId: Long? = null,
    /** Bill paid by a withdrawal; ignored for other types. */
    val billId: Long? = null,
)

/**
 * A split transaction: one movement of money divided into parts, each with its own description,
 * amount, counterparty, category, budget and tags. Every part shares type, date and own account.
 */
data class TransactionGroupDraft(
    val id: Long = 0,
    val title: String,
    /** Parts to keep; an existing part has its id set, a new one has id 0. */
    val splits: List<TransactionDraft>,
)

/**
 * Shows each split transaction as one entry: the parts of a group found in this list are replaced,
 * at the position of the first one, by a transaction with the group's title, the total amount and
 * what the parts have in common (category, budget) plus all their counterparties and tags. The id
 * is the first part's (in entry order), so opening it edits the whole group. A group with a single part in the
 * list (for example when filtering by category) is left as that part.
 */
fun List<Transaction>.mergingSplits(): List<Transaction> {
    val parts = filter { it.groupId != null }.groupBy { it.groupId }
    if (parts.values.none { it.size > 1 }) return this
    val emitted = mutableSetOf<Long>()
    return mapNotNull { transaction ->
        val groupId = transaction.groupId ?: return@mapNotNull transaction
        val group = parts.getValue(groupId)
        when {
            group.size == 1 -> transaction
            !emitted.add(groupId) -> null
            else -> group.merged()
        }
    }
}

private fun List<Transaction>.merged(): Transaction = sortedBy { it.id }.run {
    val first = first()
    fun names(of: (Transaction) -> AccountSummary) = map(of).distinctBy { it.id }
        .let { accounts -> if (accounts.size == 1) accounts.single() else accounts.first().copy(name = accounts.joinToString(", ") { it.name }) }
    first.copy(
        description = first.groupTitle ?: first.description,
        amount = sumOf { it.amount },
        foreignAmount = null,
        source = names { it.source },
        destination = names { it.destination },
        notes = null,
        category = map { it.category }.distinct().singleOrNull(),
        tags = flatMap { it.tags }.distinctBy { it.id },
        budget = map { it.budget }.distinct().singleOrNull(),
        bill = map { it.bill }.distinct().singleOrNull(),
    )
}

/** Criteria for searching transactions; null fields do not filter. */
data class TransactionFilter(
    /** Text matched against description, notes, account names, category and tags, ignoring case. */
    val query: String? = null,
    val type: TransactionType? = null,
    val categoryId: Long? = null,
    val tagId: Long? = null,
    val budgetId: Long? = null,
    val billId: Long? = null,
    /** Matches transactions where the account is either source or destination. */
    val accountId: Long? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
) {
    /** True when the user narrowed the results beyond a date range. */
    val hasCriteria: Boolean
        get() = !query.isNullOrBlank() || type != null || categoryId != null || tagId != null || budgetId != null || billId != null || accountId != null
}
