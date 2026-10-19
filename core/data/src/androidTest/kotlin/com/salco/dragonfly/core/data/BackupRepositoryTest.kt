package com.salco.dragonfly.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.salco.dragonfly.core.data.repository.InvalidBackupException
import com.salco.dragonfly.core.data.repository.OfflineBackupRepository
import com.salco.dragonfly.core.data.repository.OfflineBillsRepository
import com.salco.dragonfly.core.data.repository.OfflineBudgetsRepository
import com.salco.dragonfly.core.data.repository.OfflinePiggyBanksRepository
import com.salco.dragonfly.core.data.repository.OfflineRecurringTransactionsRepository
import com.salco.dragonfly.core.data.repository.OfflineRulesRepository
import com.salco.dragonfly.core.data.repository.OfflineTransactionsRepository
import com.salco.dragonfly.core.data.repository.RuleApplier
import com.salco.dragonfly.core.database.DragonflyDatabase
import com.salco.dragonfly.core.database.RoomTransactionRunner
import com.salco.dragonfly.core.database.model.AccountEntity
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.ActionType
import com.salco.dragonfly.core.model.Bill
import com.salco.dragonfly.core.model.Budget
import com.salco.dragonfly.core.model.PiggyBank
import com.salco.dragonfly.core.model.RecurrenceFrequency
import com.salco.dragonfly.core.model.RecurrenceRule
import com.salco.dragonfly.core.model.RecurringTransaction
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
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {

    private lateinit var db: DragonflyDatabase
    private lateinit var transactions: OfflineTransactionsRepository
    private lateinit var backup: OfflineBackupRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DragonflyDatabase::class.java).build()
        val runner = RoomTransactionRunner(db)
        transactions = OfflineTransactionsRepository(db.transactionDao(), RuleApplier(db.ruleDao(), db.accountDao()))
        backup = OfflineBackupRepository(db.backupDao(), transactions, runner)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun populate() {
        val runner = RoomTransactionRunner(db)
        val checking = db.accountDao().upsert(
            AccountEntity(
                name = "Conto", type = AccountType.ASSET, currencyCode = "EUR", openingBalance = 100_000, iban = "IT00", notes = null,
                active = true, includeInNetWorth = true, assetRole = null, liabilityType = null, createdAt = 0,
            ),
        )
        val budget = OfflineBudgetsRepository(db.budgetDao()).saveBudget(Budget(name = "Spesa", amount = 40_000, currencyCode = "EUR"))
        val bill = OfflineBillsRepository(db.billDao()).saveBill(
            Bill(name = "Affitto", amountMin = 80_000, amountMax = 80_000, currencyCode = "EUR",
                rule = RecurrenceRule(RecurrenceFrequency.MONTHLY, startDate = LocalDate.of(2026, 1, 5))),
        )
        transactions.saveTransaction(
            TransactionDraft(
                type = TransactionType.WITHDRAWAL, description = "Spesa, \"grande\"", date = LocalDate.of(2026, 10, 3), amount = 4_250,
                currencyCode = "EUR", source = AccountRef.Existing(checking),
                destination = AccountRef.New("Supermercato", AccountType.EXPENSE, "EUR"),
                categoryName = "Cibo", tagNames = listOf("Casa", "Settimana"), budgetId = budget, notes = "riga 1\nriga 2",
            ),
        )
        transactions.saveTransaction(
            TransactionDraft(
                type = TransactionType.WITHDRAWAL, description = "Affitto", date = LocalDate.of(2026, 10, 5), amount = 80_000,
                currencyCode = "EUR", source = AccountRef.Existing(checking),
                destination = AccountRef.New("Padrone", AccountType.EXPENSE, "EUR"), billId = bill,
            ),
        )
        OfflineRecurringTransactionsRepository(db.recurringTransactionDao(), transactions, runner).saveRecurringTransaction(
            RecurringTransaction(
                template = TransactionDraft(
                    type = TransactionType.DEPOSIT, description = "Stipendio", date = LocalDate.of(2026, 11, 27), amount = 200_000,
                    currencyCode = "EUR", source = AccountRef.New("Azienda", AccountType.REVENUE, "EUR"), destination = AccountRef.Existing(checking),
                ),
                rule = RecurrenceRule(RecurrenceFrequency.MONTHLY, startDate = LocalDate.of(2026, 11, 27)),
            ),
        )
        val piggyBanks = OfflinePiggyBanksRepository(db.piggyBankDao(), db.accountDao(), runner)
        val jar = piggyBanks.savePiggyBank(PiggyBank(name = "Vacanze", accountId = checking, targetAmount = 50_000, startDate = LocalDate.of(2026, 1, 1)))
        piggyBanks.moveMoney(jar, 10_000, LocalDate.of(2026, 10, 1))
        OfflineRulesRepository(db.ruleDao(), transactions, RuleApplier(db.ruleDao(), db.accountDao())).saveRule(
            Rule(
                name = "Bar",
                triggers = listOf(RuleTrigger(TriggerField.COUNTERPARTY, TriggerOperator.IS, "Bar")),
                actions = listOf(RuleAction(ActionType.SET_CATEGORY, "Svago")),
            ),
        )
    }

    /** Every table as raw rows, to compare the database before and after a restore. */
    private suspend fun snapshot() = with(db.backupDao()) {
        listOf(
            accounts(), categories(), tags(), budgets(), bills(), transactions(), transactionTags().sortedBy { it.transactionId * 1000 + it.tagId },
            recurringTransactions(), piggyBanks(), piggyBankEvents(), rules(), ruleTriggers(), ruleActions(),
        )
    }

    @Test
    fun backupRestoresExactlyTheSameData() = runTest {
        populate()
        val before = snapshot()
        val bytes = ByteArrayOutputStream().also { backup.writeBackup(it) }.toByteArray()

        // Change the data, then restore.
        db.backupDao().clearAccounts()
        transactions.saveTransaction(
            TransactionDraft(
                type = TransactionType.DEPOSIT, description = "Altro", date = LocalDate.of(2026, 10, 9), amount = 1,
                currencyCode = "EUR", source = AccountRef.New("X", AccountType.REVENUE, "EUR"),
                destination = AccountRef.New("Y", AccountType.EXPENSE, "EUR"),
            ),
        )
        val parsed = backup.readBackup(bytes.inputStream())
        assertEquals(2, parsed.summary.transactions)
        assertEquals(1, parsed.summary.rules)
        backup.restoreBackup(parsed)

        assertEquals(before, snapshot())
        val restored = transactions.searchTransactions(TransactionFilter()).first()
        assertEquals(setOf("Casa", "Settimana"), restored.single { it.description.startsWith("Spesa") }.tags.map { it.name }.toSet())
    }

    @Test
    fun invalidFilesAreRejectedWithoutTouchingData() = runTest {
        populate()
        val before = snapshot()
        listOf("not json", """{"format":"other","database_version":1,"exported_at":"2026-10-18T10:00:00Z"}""",
            """{"format":"dragonfly-backup","database_version":999,"exported_at":"2026-10-18T10:00:00Z"}""").forEach { content ->
            try {
                backup.readBackup(content.byteInputStream())
                fail("Expected InvalidBackupException for $content")
            } catch (_: InvalidBackupException) {
            }
        }
        assertEquals(before, snapshot())
    }

    @Test
    fun csvQuotesSpecialCharacters() = runTest {
        populate()
        val csv = ByteArrayOutputStream().also { backup.writeTransactionsCsv(it) }.toString(Charsets.UTF_8.name()).removePrefix("\uFEFF")
        val lines = csv.split("\r\n").filter { it.isNotEmpty() }
        assertEquals("date,type,description,amount,currency,foreign_amount,source,destination,category,tags,budget,bill,notes,split_group", lines.first())
        assertTrue(csv.contains("2026-10-03,withdrawal,\"Spesa, \"\"grande\"\"\",42.50,EUR,,Conto,Supermercato,Cibo,Casa Settimana,Spesa,,\"riga 1\nriga 2\","))
        assertTrue(csv.contains("2026-10-05,withdrawal,Affitto,800.00,EUR,,Conto,Padrone,,,,Affitto,"))
    }
}
