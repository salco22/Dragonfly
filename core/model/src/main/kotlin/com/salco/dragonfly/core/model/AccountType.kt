package com.salco.dragonfly.core.model

/**
 * Account types modelled after Firefly III: asset accounts hold the user's money,
 * expense and revenue accounts represent counterparties, liabilities track debts.
 */
enum class AccountType {
    ASSET,
    EXPENSE,
    REVENUE,
    LIABILITY,
}

/** Role of an [AccountType.ASSET] account. */
enum class AssetRole {
    DEFAULT,
    SAVINGS,
    SHARED,
    CREDIT_CARD,
    CASH_WALLET,
}

/** Kind of an [AccountType.LIABILITY] account. */
enum class LiabilityType {
    DEBT,
    LOAN,
    MORTGAGE,
}
