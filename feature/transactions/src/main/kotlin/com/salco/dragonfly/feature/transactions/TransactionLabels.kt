package com.salco.dragonfly.feature.transactions

import androidx.annotation.StringRes
import com.salco.dragonfly.core.model.TransactionType

@StringRes
internal fun TransactionType.labelRes(): Int = when (this) {
    TransactionType.WITHDRAWAL -> R.string.feature_transactions_type_withdrawal
    TransactionType.DEPOSIT -> R.string.feature_transactions_type_deposit
    TransactionType.TRANSFER -> R.string.feature_transactions_type_transfer
}
