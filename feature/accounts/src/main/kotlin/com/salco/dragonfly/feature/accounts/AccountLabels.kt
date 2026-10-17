package com.salco.dragonfly.feature.accounts

import androidx.annotation.StringRes
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.AssetRole
import com.salco.dragonfly.core.model.LiabilityType

@StringRes
internal fun AccountType.labelRes(): Int = when (this) {
    AccountType.ASSET -> R.string.feature_accounts_type_asset
    AccountType.EXPENSE -> R.string.feature_accounts_type_expense
    AccountType.REVENUE -> R.string.feature_accounts_type_revenue
    AccountType.LIABILITY -> R.string.feature_accounts_type_liability
}

@StringRes
internal fun AssetRole.labelRes(): Int = when (this) {
    AssetRole.DEFAULT -> R.string.feature_accounts_role_default
    AssetRole.SAVINGS -> R.string.feature_accounts_role_savings
    AssetRole.SHARED -> R.string.feature_accounts_role_shared
    AssetRole.CREDIT_CARD -> R.string.feature_accounts_role_credit_card
    AssetRole.CASH_WALLET -> R.string.feature_accounts_role_cash_wallet
}

@StringRes
internal fun LiabilityType.labelRes(): Int = when (this) {
    LiabilityType.DEBT -> R.string.feature_accounts_liability_debt
    LiabilityType.LOAN -> R.string.feature_accounts_liability_loan
    LiabilityType.MORTGAGE -> R.string.feature_accounts_liability_mortgage
}

/** Only asset and liability accounts carry a balance of the user's own money. */
internal val AccountType.hasBalance: Boolean
    get() = this == AccountType.ASSET || this == AccountType.LIABILITY
