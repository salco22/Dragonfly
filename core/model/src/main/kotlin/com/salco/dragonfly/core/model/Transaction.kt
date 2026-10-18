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
