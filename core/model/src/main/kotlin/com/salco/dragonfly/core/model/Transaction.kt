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
)

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
)
