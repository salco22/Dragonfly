package com.salco.dragonfly.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.salco.dragonfly.core.data.repository.OfflineRulesRepository
import com.salco.dragonfly.core.data.repository.OfflineTransactionsRepository
import com.salco.dragonfly.core.data.repository.RuleApplier
import com.salco.dragonfly.core.database.DragonflyDatabase
import com.salco.dragonfly.core.database.model.AccountEntity
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.ActionType
import com.salco.dragonfly.core.model.Rule
import com.salco.dragonfly.core.model.RuleAction
import com.salco.dragonfly.core.model.RuleTrigger
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionFilter
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.TriggerField
import com.salco.dragonfly.core.model.TriggerOperator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RulesRepositoryTest {

    private lateinit var db: DragonflyDatabase
    private lateinit var transactions: OfflineTransactionsRepository
    private lateinit var rules: OfflineRulesRepository
    private var checking = 0L

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DragonflyDatabase::class.java).build()
        val applier = RuleApplier(db.ruleDao(), db.accountDao())
        transactions = OfflineTransactionsRepository(db.transactionDao(), applier)
        rules = OfflineRulesRepository(db.ruleDao(), transactions, applier)
        checking = db.accountDao().upsert(
            AccountEntity(
                name = "Conto", type = AccountType.ASSET, currencyCode = "EUR", openingBalance = 0, iban = null, notes = null,
                active = true, includeInNetWorth = true, assetRole = null, liabilityType = null, createdAt = 0,
            ),
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun spend(payee: String, id: Long = 0, category: String? = null) = transactions.saveTransaction(
        TransactionDraft(
            id = id, type = TransactionType.WITHDRAWAL, description = "POS", date = LocalDate.of(2026, 10, 1), amount = 1_000,
            currencyCode = "EUR", source = AccountRef.Existing(checking), destination = AccountRef.New(payee, AccountType.EXPENSE, "EUR"),
            categoryName = category,
        ),
    )

    private val groceries = Rule(
        name = "Spesa",
        triggers = listOf(RuleTrigger(TriggerField.COUNTERPARTY, TriggerOperator.IS, "esselunga")),
        actions = listOf(RuleAction(ActionType.SET_CATEGORY, "Spesa"), RuleAction(ActionType.ADD_TAG, "Casa")),
    )

    @Test
    fun rulesRoundTripAndApplyToNewTransactionsOnly() = runTest {
        val before = spend("Esselunga")
        val ruleId = rules.saveRule(groceries)
        assertEquals(groceries.copy(id = ruleId), rules.observeRule(ruleId).first())

        val after = spend("Esselunga")
        val byId = transactions.searchTransactions(TransactionFilter()).first().associateBy { it.id }
        assertEquals(null, byId.getValue(before).category)
        assertEquals("Spesa", byId.getValue(after).category?.name)
        assertEquals(listOf("Casa"), byId.getValue(after).tags.map { it.name })

        // A manual edit keeps the user's choice.
        spend("Esselunga", id = after, category = "Regali")
        assertEquals("Regali", transactions.observeTransaction(after).first()!!.category?.name)

        // Applying on request covers both: the old one and the manually edited one.
        assertEquals(2, rules.countAffected(groceries))
        assertEquals(2, rules.applyToExisting())
        assertEquals(0, rules.applyToExisting())
    }

    @Test
    fun deletingARuleRemovesItsParts() = runTest {
        val id = rules.saveRule(groceries)
        rules.deleteRule(id)
        assertEquals(emptyList<Rule>(), rules.observeRules().first())
    }

    @Test
    fun setCounterparty_movesTheTransactionToTheNamedPayee() = runTest {
        rules.saveRule(
            Rule(
                name = "Esselunga",
                triggers = listOf(RuleTrigger(TriggerField.DESCRIPTION, TriggerOperator.CONTAINS, "esselunga")),
                actions = listOf(RuleAction(ActionType.SET_COUNTERPARTY, "Esselunga")),
            ),
        )
        val id = transactions.saveTransaction(
            TransactionDraft(
                type = TransactionType.WITHDRAWAL, description = "POS ESSELUNGA MILANO", date = LocalDate.of(2026, 10, 1), amount = 1_000,
                currencyCode = "EUR", source = AccountRef.Existing(checking), destination = AccountRef.New("Senza nome", AccountType.EXPENSE, "EUR"),
            ),
        )
        assertEquals("Esselunga", transactions.observeTransaction(id).first()!!.destination.name)
    }
}
