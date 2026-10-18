package com.salco.dragonfly.core.model

/**
 * A monthly spending limit, like a Firefly III budget with a "reset" auto-budget: every month
 * starts again from [amount]. Withdrawals are assigned to a budget explicitly.
 */
data class Budget(
    val id: Long = 0,
    val name: String,
    /** Monthly limit in minor units of [currencyCode]. */
    val amount: Long,
    val currencyCode: String,
    val active: Boolean = true,
)

/** A budget with what its withdrawals spent in one period, in the budget's currency. */
data class BudgetSpending(
    val budget: Budget,
    val spent: Long,
) {
    val remaining: Long get() = budget.amount - spent
    val isOverspent: Boolean get() = spent > budget.amount

    /** Share of the limit already spent; above 1 when overspent. */
    val progress: Float get() = if (budget.amount <= 0) (if (spent > 0) 1f else 0f) else spent.toFloat() / budget.amount
}
