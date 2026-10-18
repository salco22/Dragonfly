package com.salco.dragonfly.feature.rules

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.salco.dragonfly.core.model.ActionType
import com.salco.dragonfly.core.model.Rule
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.TriggerField
import com.salco.dragonfly.core.model.TriggerOperator
import com.salco.dragonfly.core.model.formatMoney

@StringRes
internal fun TriggerField.labelRes(): Int = when (this) {
    TriggerField.DESCRIPTION -> R.string.feature_rules_field_description
    TriggerField.COUNTERPARTY -> R.string.feature_rules_field_counterparty
    TriggerField.ACCOUNT -> R.string.feature_rules_field_account
    TriggerField.AMOUNT -> R.string.feature_rules_field_amount
    TriggerField.TYPE -> R.string.feature_rules_field_type
    TriggerField.NOTES -> R.string.feature_rules_field_notes
}

@StringRes
internal fun TriggerOperator.labelRes(): Int = when (this) {
    TriggerOperator.CONTAINS -> R.string.feature_rules_operator_contains
    TriggerOperator.STARTS_WITH -> R.string.feature_rules_operator_starts_with
    TriggerOperator.ENDS_WITH -> R.string.feature_rules_operator_ends_with
    TriggerOperator.IS -> R.string.feature_rules_operator_is
    TriggerOperator.LESS_THAN -> R.string.feature_rules_operator_less_than
    TriggerOperator.MORE_THAN -> R.string.feature_rules_operator_more_than
}

@StringRes
internal fun TransactionType.labelRes(): Int = when (this) {
    TransactionType.WITHDRAWAL -> R.string.feature_rules_type_withdrawal
    TransactionType.DEPOSIT -> R.string.feature_rules_type_deposit
    TransactionType.TRANSFER -> R.string.feature_rules_type_transfer
}

@StringRes
internal fun ActionType.labelRes(): Int = when (this) {
    ActionType.SET_CATEGORY -> R.string.feature_rules_action_set_category
    ActionType.ADD_TAG -> R.string.feature_rules_action_add_tag
    ActionType.SET_BUDGET -> R.string.feature_rules_action_set_budget
    ActionType.LINK_BILL -> R.string.feature_rules_action_link_bill
    ActionType.SET_DESCRIPTION -> R.string.feature_rules_action_set_description
    ActionType.APPEND_NOTES -> R.string.feature_rules_action_append_notes
}

/**
 * "Se descrizione contiene «esselunga» e importo maggiore di €50.00, allora imposta categoria
 * «Spesa»". Budgets and bills are shown by name through [namesById].
 */
@Composable
internal fun Rule.summary(currencyCode: String, namesById: (ActionType, String) -> String?): String {
    val joiner = " ${stringResource(if (matchAll) R.string.feature_rules_and else R.string.feature_rules_or)} "
    val conditions = triggers.map { t ->
        val value = when (t.field) {
            TriggerField.AMOUNT -> t.value.toLongOrNull()?.let { formatMoney(it, currencyCode) } ?: t.value
            TriggerField.TYPE -> TransactionType.entries.firstOrNull { it.name == t.value }?.let { stringResource(it.labelRes()) } ?: t.value
            else -> "«${t.value}»"
        }
        "${stringResource(t.field.labelRes()).lowercase()} ${stringResource(t.operator.labelRes())} $value"
    }.joinToString(joiner)
    val effects = actions.map { a ->
        val value = namesById(a.type, a.value) ?: a.value
        "${stringResource(a.type.labelRes()).lowercase()} «$value»"
    }.joinToString(", ")
    return "${stringResource(R.string.feature_rules_if)} $conditions, ${stringResource(R.string.feature_rules_then)} $effects"
}
