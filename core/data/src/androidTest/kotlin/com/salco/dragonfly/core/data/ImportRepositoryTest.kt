package com.salco.dragonfly.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.salco.dragonfly.core.data.repository.ImportResult
import com.salco.dragonfly.core.data.repository.OfflineImportRepository
import com.salco.dragonfly.core.data.repository.OfflineRulesRepository
import com.salco.dragonfly.core.data.repository.OfflineTransactionsRepository
import com.salco.dragonfly.core.data.repository.RuleApplier
import com.salco.dragonfly.core.database.DragonflyDatabase
import com.salco.dragonfly.core.database.RoomTransactionRunner
import com.salco.dragonfly.core.database.model.AccountEntity
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.ActionType
import com.salco.dragonfly.core.model.ColumnRole
import com.salco.dragonfly.core.model.ImportRow
import com.salco.dragonfly.core.model.Rule
import com.salco.dragonfly.core.model.RuleAction
import com.salco.dragonfly.core.model.RuleTrigger
import com.salco.dragonfly.core.model.TransactionFilter
import com.salco.dragonfly.core.model.TriggerField
import com.salco.dragonfly.core.model.TriggerOperator
import com.salco.dragonfly.core.model.mapImportRows
import com.salco.dragonfly.core.model.parseCsv
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImportRepositoryTest {

    private lateinit var db: DragonflyDatabase
    private lateinit var transactions: OfflineTransactionsRepository
    private lateinit var importer: OfflineImportRepository
    private lateinit var rules: OfflineRulesRepository
    private var checking = 0L

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DragonflyDatabase::class.java).build()
        val applier = RuleApplier(db.ruleDao(), db.accountDao())
        transactions = OfflineTransactionsRepository(db.transactionDao(), applier)
        importer = OfflineImportRepository(db.transactionDao(), db.accountDao(), transactions, RoomTransactionRunner(db))
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

    private fun rows(csv: String) = parseCsv(csv).drop(1).let {
        mapImportRows(it, listOf(ColumnRole.DATE, ColumnRole.DESCRIPTION, ColumnRole.AMOUNT), "dd/MM/yyyy", "EUR", firstLine = 2)
            .filterIsInstance<ImportRow.Valid>()
    }

    @Test
    fun importsApplyingRulesAndSkippingDuplicates() = runTest {
        rules.saveRule(
            Rule(
                name = "Esselunga",
                triggers = listOf(RuleTrigger(TriggerField.DESCRIPTION, TriggerOperator.CONTAINS, "esselunga")),
                actions = listOf(RuleAction(ActionType.SET_COUNTERPARTY, "Esselunga"), RuleAction(ActionType.SET_CATEGORY, "Spesa")),
            ),
        )
        val file = rows(
            """
            Data;Descrizione;Importo
            01/10/2026;POS ESSELUNGA MILANO;-42,50
            01/10/2026;POS ESSELUNGA MILANO;-42,50
            02/10/2026;BONIFICO STIPENDIO;2000,00
            """.trimIndent(),
        )

        assertEquals(ImportResult(imported = 2, duplicates = 1), importer.importRows(checking, file, skipDuplicates = true, unnamedCounterparty = "Senza nome"))
        val saved = transactions.searchTransactions(TransactionFilter()).first()
        val grocery = saved.single { it.amount == 4_250L }
        assertEquals("Esselunga", grocery.destination.name)
        assertEquals("Spesa", grocery.category?.name)
        assertEquals("Senza nome", saved.single { it.amount == 200_000L }.source.name)
        assertEquals(200_000L - 4_250, db.accountDao().observeById(checking).first()!!.balance)

        assertEquals(setOf(2, 3, 4), importer.findDuplicates(checking, file))
        assertEquals(ImportResult(imported = 0, duplicates = 3), importer.importRows(checking, file, true, "Senza nome"))
    }
}
