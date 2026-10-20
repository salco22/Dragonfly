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
        importer = OfflineImportRepository(db.transactionDao(), db.accountDao(), db.budgetDao(), db.billDao(), transactions, RoomTransactionRunner(db))
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

    private val firefly = """
        user_id,group_id,journal_id,group_title,type,amount,foreign_amount,currency_code,foreign_currency_code,description,date,source_name,source_type,destination_name,destination_type,category,budget,bill,tags,notes
        1,10,100,,Withdrawal,-42.500000000000,,EUR,,pranzo - La Farcia,2026-09-25T12:05:00+02:00,ISP,Asset account,Cash account,Cash account,Spese Variabili,Spese essenziali,,"Ristoranti,Lavoro",
        1,11,101,,Deposit,-2027.000000000000,,EUR,,Stipendio Settembre,2026-09-27T09:00:00+02:00,Cash account,Cash account,ISP,Asset account,Entrate,,,Stipendio,
        1,12,102,,Transfer,-300.000000000000,,EUR,,invio soldi a Revolut,2026-09-28T10:00:00+02:00,ISP,Asset account,Revolut,Asset account,,,,,
        1,13,103,,Opening balance,-1000.000000000000,,EUR,,Saldo iniziale ISP,2024-02-03T00:00:00+01:00,ISP initial balance,Initial balance account,ISP,Asset account,,,,,
        1,14,104,,Reconciliation,-1.000000000000,,EUR,,Riconciliazione,2025-05-01T00:00:00+02:00,ISP,Asset account,ISP riconciliazione (EUR),Reconciliation account,,,,,
        1,15,105,Spesa grande,Withdrawal,-30.000000000000,,EUR,,Pane,2026-09-29T10:00:00+02:00,Revolut,Asset account,Forno,Expense account,,,,,
        1,15,106,Spesa grande,Withdrawal,-12.345000000000,,EUR,,Latte,2026-09-29T10:00:00+02:00,Revolut,Asset account,Latteria,Expense account,,,,,
        1,16,107,,Withdrawal,-5.000000000000,,EUR,,Caffè,2026-09-30T08:00:00+02:00,ISP,Asset account,Cash account,Cash account,,,,,
        1,17,108,,Withdrawal,-5.000000000000,,EUR,,Caffè,2026-09-30T08:00:00+02:00,ISP,Asset account,Cash account,Cash account,,,,,
    """.trimIndent()

    @Test
    fun fireflyExport_createsAccountsAndMovements_withoutRules_andOnlyOnce() = runTest {
        // A rule that would rename the lunch: imported data must stay as it is.
        rules.saveRule(
            Rule(
                name = "Pranzo",
                triggers = listOf(RuleTrigger(TriggerField.DESCRIPTION, TriggerOperator.CONTAINS, "pranzo")),
                actions = listOf(RuleAction(ActionType.SET_CATEGORY, "Altro")),
            ),
        )
        val export = com.salco.dragonfly.core.model.parseFireflyExport(parseCsv(firefly))
        with(importer.previewFirefly(export)) {
            assertEquals(listOf("ISP", "Revolut"), newAccounts)
            assertEquals(0, duplicates)
        }

        val result = importer.importFirefly(export, skipDuplicates = true, unnamedCounterparty = "Senza nome", reconciliationName = "Riconciliazione")
        // The two identical coffees are both kept: duplicates are only rows already here before.
        assertEquals(ImportResult(imported = 8, duplicates = 0, accountsCreated = 2), result)

        val balances = db.accountDao().observeAll().first().associate { it.account.name to it.balance }
        // 1000 opening - 42.50 + 2027 - 300 - 1 (reconciliation) - 2 × 5
        assertEquals(267_350L, balances["ISP"])
        assertEquals(30_000L - 3_000 - 1_235, balances["Revolut"])
        assertEquals(100_000L, db.accountDao().observeAll().first().single { it.account.name == "ISP" }.account.openingBalance)

        val all = transactions.searchTransactions(TransactionFilter()).first()
        val lunch = all.single { it.description == "pranzo - La Farcia" }
        assertEquals("Spese Variabili", lunch.category?.name)
        assertEquals("Senza nome", lunch.destination.name)
        assertEquals(listOf("Lavoro", "Ristoranti"), lunch.tags.map { it.name })
        assertEquals("Spese essenziali", lunch.budget?.name)
        assertEquals(setOf("Spesa grande"), all.filter { it.groupId != null }.map { it.groupTitle }.toSet())
        assertEquals(2, all.count { it.groupId != null })
        assertEquals("Riconciliazione", all.single { it.description == "Riconciliazione" }.destination.name)
        // The budget exists but stays out of the monthly view until the user gives it an amount.
        assertEquals(false, db.budgetDao().findByName("Spese essenziali")!!.active)

        // Importing the same file again finds every movement already there.
        assertEquals(8, importer.previewFirefly(export).duplicates)
        assertEquals(ImportResult(imported = 0, duplicates = 8, accountsCreated = 0), importer.importFirefly(export, true, "Senza nome", "Riconciliazione"))
    }
}
