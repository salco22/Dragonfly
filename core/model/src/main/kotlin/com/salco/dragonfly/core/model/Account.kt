package com.salco.dragonfly.core.model

import java.time.Instant

data class Account(
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val currencyCode: String,
    /** Opening balance in minor units (e.g. cents). */
    val openingBalance: Long = 0,
    /** Current balance in minor units: opening balance plus every transaction on the account. */
    val balance: Long = openingBalance,
    val iban: String? = null,
    val notes: String? = null,
    val active: Boolean = true,
    val includeInNetWorth: Boolean = true,
    val assetRole: AssetRole? = null,
    val liabilityType: LiabilityType? = null,
    val createdAt: Instant = Instant.now(),
)
