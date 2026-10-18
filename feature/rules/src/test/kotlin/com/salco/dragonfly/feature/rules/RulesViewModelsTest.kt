package com.salco.dragonfly.feature.rules

import androidx.lifecycle.SavedStateHandle
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.ActionType
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.TriggerField
import com.salco.dragonfly.core.model.TriggerOperator
import com.salco.dragonfly.core.testing.FakeAccountsRepository
import com.salco.dragonfly.core.testing.FakeBillsRepository
import com.salco.dragonfly.core.testing.FakeBudgetsRepository
import com.salco.dragonfly.core.testing.FakeCategoriesRepository
import com.salco.dragonfly.core.testing.FakeRulesRepository
import com.salco.dragonfly.core.testing.FakeTagsRepository
import com.salco.dragonfly.core.testing.FakeTransactionsRepository
import com.salco.dragonfly.core.testing.FakeUserDataRepository
import com.salco.dragonfly.core.testing.MainDispatcherRule
import com.salco.dragonfly.feature.rules.edit.RuleEditViewModel
import com.salco.dragonfly.feature.rules.list.RulesViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class RulesViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accounts = FakeAccountsRepository()
    private val categories = FakeCategoriesRepository()
    private val tags = FakeTagsRepository()
    private val budgets = FakeBudgetsRepository()
    private val bills = FakeBillsRepository()
    private val transactions = FakeTransactionsRepository(accounts, categories, tags, budgets, bills)
    private val rules = FakeRulesRepository(transactions, categories, tags)

    @Before
    fun setUp() = runTest {
        accounts.saveAccount(Account(name = "Conto", type = AccountType.ASSET, currencyCode = "EUR"))
        listOf("Esselunga" to 4_250L, "Esselunga" to 12_000L, "Bar" to 150L).forEach { (shop, amount) ->
            transactions.saveTransaction(
                TransactionDraft(
                    type = TransactionType.WITHDRAWAL, description = "POS $shop", date = LocalDate.of(2026, 10, 1), amount = amount,
                    currencyCode = "EUR", source = AccountRef.Existing(1), destination = AccountRef.New(shop, AccountType.EXPENSE, "EUR"),
                ),
            )
        }
    }

    private fun editViewModel(id: Long = 0L) = RuleEditViewModel(
        SavedStateHandle(mapOf("ruleId" to id)), rules, accounts, categories, tags, budgets, bills, FakeUserDataRepository(),
    )

    @Test
    fun newRule_startsWithOneConditionAndOneAction() = runTest {
        with(editViewModel().uiState.value) {
            assertEquals(1, triggers.size)
            assertEquals(1, actions.size)
            assertEquals(listOf("Esselunga", "Bar"), counterpartyNames)
        }
    }

    @Test
    fun incompleteRule_showsErrors() = runTest {
        val vm = editViewModel()
        vm.save()
        with(vm.uiState.value) {
            assertTrue(nameError)
            assertTrue(triggersError)
            assertTrue(actionsError)
            assertFalse(isFinished)
        }
    }

    @Test
    fun previewAndSaveApplyingToExisting() = runTest {
        val vm = editViewModel()
        vm.onNameChange("Spesa grande")
        vm.onTriggerFieldChange(0, TriggerField.COUNTERPARTY)
        vm.onTriggerOperatorChange(0, TriggerOperator.IS)
        vm.onTriggerValueChange(0, "esselunga")
        vm.addTrigger()
        vm.onTriggerFieldChange(1, TriggerField.AMOUNT)
        assertEquals(TriggerOperator.MORE_THAN, vm.uiState.value.triggers[1].operator)
        vm.onTriggerValueChange(1, "50")
        vm.onActionValueChange(0, "Spesa")
        vm.addAction()
        vm.onActionTypeChange(1, ActionType.ADD_TAG)
        vm.onActionValueChange(1, "Grande")

        vm.preview()
        assertEquals(1, vm.uiState.value.previewCount)

        vm.onApplyToExistingChange(true)
        vm.save()
        assertTrue(vm.uiState.value.isFinished)
        val saved = rules.rules.value.single()
        assertEquals("5000", saved.triggers[1].value)
        val big = transactions.transactions.value.single { it.amount == 12_000L }
        assertEquals("Spesa", big.category?.name)
        assertEquals(listOf("Grande"), big.tags.map { it.name })
        assertEquals(null, transactions.transactions.value.single { it.amount == 4_250L }.category)

        // Reopening shows the amount as typed.
        assertEquals("50.00", editViewModel(saved.id).uiState.value.triggers[1].value)
    }

    @Test
    fun applyAll_reportsChangedTransactions() = runTest {
        val vm = editViewModel()
        vm.onNameChange("Bar")
        vm.onTriggerValueChange(0, "bar")
        vm.onActionValueChange(0, "Svago")
        vm.save()

        val list = RulesViewModel(rules, budgets, bills, FakeUserDataRepository())
        list.applyAll()
        assertEquals(1, list.applied.value)
        list.appliedShown()
        list.applyAll()
        assertEquals(0, list.applied.value)
    }
}
