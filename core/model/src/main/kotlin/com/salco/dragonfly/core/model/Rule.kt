package com.salco.dragonfly.core.model

/**
 * An automatic rule, like Firefly III's: when a transaction matches its [triggers] (all of them,
 * or any when [matchAll] is false) the [actions] are applied in order. Rules run by [position];
 * [stopProcessing] skips the rules after this one when it matches.
 */
data class Rule(
    val id: Long = 0,
    val name: String,
    val triggers: List<RuleTrigger>,
    val actions: List<RuleAction>,
    val matchAll: Boolean = true,
    val active: Boolean = true,
    val stopProcessing: Boolean = false,
    val position: Int = 0,
)

enum class TriggerField { DESCRIPTION, COUNTERPARTY, ACCOUNT, AMOUNT, TYPE, NOTES }

enum class TriggerOperator { CONTAINS, STARTS_WITH, ENDS_WITH, IS, LESS_THAN, MORE_THAN }

/**
 * A condition on [field]. [value] is text for text fields, minor units for [TriggerField.AMOUNT]
 * and a [TransactionType] name for [TriggerField.TYPE].
 */
data class RuleTrigger(
    val field: TriggerField,
    val operator: TriggerOperator,
    val value: String,
)

enum class ActionType { SET_CATEGORY, ADD_TAG, SET_BUDGET, LINK_BILL, SET_DESCRIPTION, APPEND_NOTES, SET_COUNTERPARTY }

/** [value] is a name or text, or the id of the budget or bill to set. */
data class RuleAction(
    val type: ActionType,
    val value: String,
)

/** Operators that make sense for each field. */
val TriggerField.operators: List<TriggerOperator>
    get() = when (this) {
        TriggerField.DESCRIPTION, TriggerField.COUNTERPARTY, TriggerField.NOTES ->
            listOf(TriggerOperator.CONTAINS, TriggerOperator.STARTS_WITH, TriggerOperator.ENDS_WITH, TriggerOperator.IS)
        TriggerField.ACCOUNT, TriggerField.TYPE -> listOf(TriggerOperator.IS)
        TriggerField.AMOUNT -> listOf(TriggerOperator.MORE_THAN, TriggerOperator.LESS_THAN, TriggerOperator.IS)
    }

/** What rules look at and change: a transaction with its accounts already named. */
data class RuleSubject(
    val type: TransactionType,
    val description: String,
    val amount: Long,
    val notes: String?,
    /** The user's own account (source of withdrawals and transfers, destination of deposits). */
    val accountName: String,
    /** Payee of a withdrawal, payer of a deposit, or the destination of a transfer. */
    val counterpartyName: String,
    val categoryName: String?,
    val tagNames: List<String>,
    val budgetId: Long?,
    val billId: Long?,
)

fun RuleTrigger.matches(subject: RuleSubject): Boolean {
    if (field == TriggerField.AMOUNT) {
        val limit = value.toLongOrNull() ?: return false
        return when (operator) {
            TriggerOperator.MORE_THAN -> subject.amount > limit
            TriggerOperator.LESS_THAN -> subject.amount < limit
            TriggerOperator.IS -> subject.amount == limit
            else -> false
        }
    }
    val text = when (field) {
        TriggerField.DESCRIPTION -> subject.description
        TriggerField.COUNTERPARTY -> subject.counterpartyName
        TriggerField.ACCOUNT -> subject.accountName
        TriggerField.NOTES -> subject.notes.orEmpty()
        TriggerField.TYPE -> subject.type.name
        TriggerField.AMOUNT -> error("handled above")
    }
    val expected = value.trim()
    return when (operator) {
        TriggerOperator.CONTAINS -> expected.isNotEmpty() && text.contains(expected, ignoreCase = true)
        TriggerOperator.STARTS_WITH -> expected.isNotEmpty() && text.startsWith(expected, ignoreCase = true)
        TriggerOperator.ENDS_WITH -> expected.isNotEmpty() && text.endsWith(expected, ignoreCase = true)
        TriggerOperator.IS -> text.trim().equals(expected, ignoreCase = true)
        TriggerOperator.LESS_THAN, TriggerOperator.MORE_THAN -> false
    }
}

fun Rule.matches(subject: RuleSubject): Boolean =
    triggers.isNotEmpty() && if (matchAll) triggers.all { it.matches(subject) } else triggers.any { it.matches(subject) }

fun RuleAction.applyTo(subject: RuleSubject): RuleSubject = when (type) {
    ActionType.SET_CATEGORY -> subject.copy(categoryName = value.trim().ifEmpty { null })
    ActionType.ADD_TAG -> value.trim().let { tag ->
        if (tag.isEmpty() || subject.tagNames.any { it.equals(tag, ignoreCase = true) }) subject else subject.copy(tagNames = subject.tagNames + tag)
    }
    // Budgets and bills only apply to withdrawals.
    ActionType.SET_BUDGET -> if (subject.type == TransactionType.WITHDRAWAL) subject.copy(budgetId = value.toLongOrNull()) else subject
    ActionType.LINK_BILL -> if (subject.type == TransactionType.WITHDRAWAL) subject.copy(billId = value.toLongOrNull()) else subject
    ActionType.SET_DESCRIPTION -> value.trim().takeIf { it.isNotEmpty() }?.let { subject.copy(description = it) } ?: subject
    // Transfers move money between own accounts, so they have no payee to change.
    ActionType.SET_COUNTERPARTY -> value.trim().takeIf { it.isNotEmpty() && subject.type != TransactionType.TRANSFER }
        ?.let { subject.copy(counterpartyName = it) } ?: subject
    ActionType.APPEND_NOTES -> value.trim().takeIf { it.isNotEmpty() }?.let { extra ->
        subject.copy(notes = listOfNotNull(subject.notes?.takeIf { it.isNotBlank() }, extra).joinToString("\n"))
    } ?: subject
}

/** Applies the active [rules] in order and returns the changed subject. */
fun applyRules(rules: List<Rule>, subject: RuleSubject): RuleSubject {
    var current = subject
    for (rule in rules.filter { it.active }.sortedWith(compareBy({ it.position }, { it.id }))) {
        if (!rule.matches(current)) continue
        current = rule.actions.fold(current) { acc, action -> action.applyTo(acc) }
        if (rule.stopProcessing) break
    }
    return current
}
