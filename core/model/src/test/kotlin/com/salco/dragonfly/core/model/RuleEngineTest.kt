package com.salco.dragonfly.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    private val coffee = RuleSubject(
        type = TransactionType.WITHDRAWAL,
        description = "POS ESSELUNGA MILANO",
        amount = 4_250,
        notes = null,
        accountName = "Conto",
        counterpartyName = "Esselunga",
        categoryName = null,
        tagNames = emptyList(),
        budgetId = null,
        billId = null,
    )

    private fun rule(vararg triggers: RuleTrigger, actions: List<RuleAction>, matchAll: Boolean = true, stop: Boolean = false, position: Int = 0) =
        Rule(name = "r", triggers = triggers.toList(), actions = actions, matchAll = matchAll, stopProcessing = stop, position = position)

    @Test
    fun textTriggers_ignoreCase() {
        assertTrue(RuleTrigger(TriggerField.DESCRIPTION, TriggerOperator.CONTAINS, "esselunga").matches(coffee))
        assertTrue(RuleTrigger(TriggerField.DESCRIPTION, TriggerOperator.STARTS_WITH, "pos ").matches(coffee))
        assertTrue(RuleTrigger(TriggerField.COUNTERPARTY, TriggerOperator.IS, " esselunga ").matches(coffee))
        assertFalse(RuleTrigger(TriggerField.DESCRIPTION, TriggerOperator.ENDS_WITH, "roma").matches(coffee))
        assertFalse(RuleTrigger(TriggerField.NOTES, TriggerOperator.CONTAINS, "").matches(coffee))
        assertTrue(RuleTrigger(TriggerField.TYPE, TriggerOperator.IS, "WITHDRAWAL").matches(coffee))
    }

    @Test
    fun amountTriggers_compareMinorUnits() {
        assertTrue(RuleTrigger(TriggerField.AMOUNT, TriggerOperator.MORE_THAN, "4000").matches(coffee))
        assertFalse(RuleTrigger(TriggerField.AMOUNT, TriggerOperator.LESS_THAN, "4250").matches(coffee))
        assertTrue(RuleTrigger(TriggerField.AMOUNT, TriggerOperator.IS, "4250").matches(coffee))
    }

    @Test
    fun matchAllOrAny() {
        val contains = RuleTrigger(TriggerField.DESCRIPTION, TriggerOperator.CONTAINS, "esselunga")
        val big = RuleTrigger(TriggerField.AMOUNT, TriggerOperator.MORE_THAN, "10000")
        assertFalse(rule(contains, big, actions = emptyList()).matches(coffee))
        assertTrue(rule(contains, big, actions = emptyList(), matchAll = false).matches(coffee))
        assertFalse(rule(actions = emptyList()).matches(coffee))
    }

    @Test
    fun actionsApplyInOrderAndStopProcessingWorks() {
        val groceries = rule(
            RuleTrigger(TriggerField.COUNTERPARTY, TriggerOperator.IS, "Esselunga"),
            actions = listOf(
                RuleAction(ActionType.SET_CATEGORY, "Spesa"),
                RuleAction(ActionType.ADD_TAG, "Casa"),
                RuleAction(ActionType.ADD_TAG, "casa"),
                RuleAction(ActionType.SET_BUDGET, "3"),
                RuleAction(ActionType.SET_DESCRIPTION, "Spesa Esselunga"),
                RuleAction(ActionType.APPEND_NOTES, "auto"),
            ),
            stop = true,
            position = 0,
        )
        val later = rule(
            RuleTrigger(TriggerField.DESCRIPTION, TriggerOperator.CONTAINS, "spesa"),
            actions = listOf(RuleAction(ActionType.SET_CATEGORY, "Altro")),
            position = 1,
        )
        val result = applyRules(listOf(later, groceries), coffee)
        assertEquals("Spesa", result.categoryName)
        assertEquals(listOf("Casa"), result.tagNames)
        assertEquals(3L, result.budgetId)
        assertEquals("Spesa Esselunga", result.description)
        assertEquals("auto", result.notes)

        val inactive = applyRules(listOf(groceries.copy(active = false)), coffee)
        assertNull(inactive.categoryName)
    }

    @Test
    fun budgetAndBill_onlyForWithdrawals() {
        val deposit = coffee.copy(type = TransactionType.DEPOSIT)
        val result = RuleAction(ActionType.LINK_BILL, "5").applyTo(deposit)
        assertNull(result.billId)
    }

    @Test
    fun setCounterparty_renamesThePayeeExceptForTransfers() {
        val action = RuleAction(ActionType.SET_COUNTERPARTY, "Esselunga")
        assertEquals("Esselunga", action.applyTo(coffee.copy(counterpartyName = "POS 123")).counterpartyName)
        assertEquals("Risparmi", action.applyTo(coffee.copy(type = TransactionType.TRANSFER, counterpartyName = "Risparmi")).counterpartyName)
    }
}
