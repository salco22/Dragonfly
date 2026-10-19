package com.salco.dragonfly.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.salco.dragonfly.core.data.repository.OfflineRecurringTransactionsRepository
import com.salco.dragonfly.core.data.repository.OfflineTransactionsRepository
import com.salco.dragonfly.core.data.repository.RuleApplier
import com.salco.dragonfly.core.database.DragonflyDatabase
import com.salco.dragonfly.core.database.RoomTransactionRunner
import com.salco.dragonfly.core.database.model.AccountEntity
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.RecurrenceFrequency
import com.salco.dragonfly.core.model.RecurrenceRule
import com.salco.dragonfly.core.model.RecurringTransaction
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionFilter
import com.salco.dragonfly.core.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RecurringTransactionsRepositoryTest {

    private lateinit var db: DragonflyDatabase
    private lateinit var transactions: OfflineTransactionsRepository
    private lateinit var repository: OfflineRecurringTransactionsRepository
    private var checking = 0L

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DragonflyDatabase::class.java).build()
        transactions = OfflineTransactionsRepository(db.transactionDao(), RuleApplier(db.ruleDao(), db.accountDao()))
        repository = OfflineRecurringTransactionsRepository(db.recurringTransactionDao(), transactions, RoomTransactionRunner(db))
        checking = db.accountDao().upsert(
            AccountEntity(
                name = "Conto", type = AccountType.ASSET, currencyCode = "EUR", openingBalance = 0, iban = null, notes = null,
                active = true, includeInNetWorth = true, assetRole = null, liabilityType = null, createdAt = 0,
            ),
        )
    }

    @After
    fun tearDown() = db.close()

    private fun rent(rule: RecurrenceRule) = RecurringTransaction(
        template = TransactionDraft(
            type = TransactionType.WITHDRAWAL, description = "Affitto", date = rule.startDate, amount = 80_000,
            currencyCode = "EUR", source = AccountRef.Existing(checking),
            destination = AccountRef.New("Padrone di casa", AccountType.EXPENSE, "EUR"),
            categoryName = "Casa", tagNames = listOf("Fisso"),
        ),
        rule = rule,
    )

    private suspend fun all() = transactions.searchTransactions(TransactionFilter()).first()

    @Test
    fun generatesDueOccurrencesOnceAndResolvesNames() = runTest {
        val id = repository.saveRecurringTransaction(
            rent(RecurrenceRule(RecurrenceFrequency.MONTHLY, startDate = LocalDate.of(2026, 8, 31), maxOccurrences = 4)),
        )

        // 31 August, then the last day of September and 31 October again.
        assertEquals(3, repository.generateDueTransactions(LocalDate.of(2026, 10, 31)).size)
        // Running again on the same day creates nothing new.
        assertEquals(0, repository.generateDueTransactions(LocalDate.of(2026, 10, 31)).size)

        val created = all()
        assertEquals(
            listOf(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 9, 30), LocalDate.of(2026, 8, 31)),
            created.map { it.date },
        )
        assertEquals(setOf("Padrone di casa"), created.map { it.destination.name }.toSet())
        assertEquals(setOf("Casa"), created.mapNotNull { it.category?.name }.toSet())
        assertEquals(1, db.accountDao().observeAll().first().count { it.account.type == AccountType.EXPENSE })

        val saved = repository.observeRecurringTransaction(id).first()!!
        assertEquals(3, saved.occurrencesCreated)
        assertEquals("Padrone di casa", saved.destinationName)

        // The fourth and last occurrence ends the schedule.
        assertEquals(1, repository.generateDueTransactions(LocalDate.of(2026, 12, 31)).size)
        assertNull(repository.observeRecurringTransaction(id).first()!!.nextDate)
    }

    @Test
    fun editingTheSchedule_doesNotRepeatGeneratedDates() = runTest {
        val start = LocalDate.of(2026, 10, 1)
        val id = repository.saveRecurringTransaction(rent(RecurrenceRule(RecurrenceFrequency.WEEKLY, startDate = start)))
        assertEquals(3, repository.generateDueTransactions(LocalDate.of(2026, 10, 15)).size)

        // Switching to daily from the same start continues after the last generated date.
        val saved = repository.observeRecurringTransaction(id).first()!!
        repository.saveRecurringTransaction(saved.copy(rule = saved.rule.copy(frequency = RecurrenceFrequency.DAILY)))
        assertEquals(LocalDate.of(2026, 10, 16), repository.observeRecurringTransaction(id).first()!!.nextDate)
    }

    @Test
    fun pausedSchedule_createsNothing() = runTest {
        repository.saveRecurringTransaction(
            rent(RecurrenceRule(RecurrenceFrequency.DAILY, startDate = LocalDate.of(2026, 10, 1))).copy(active = false),
        )
        assertEquals(0, repository.generateDueTransactions(LocalDate.of(2026, 10, 10)).size)
        assertEquals(0, all().size)
    }
}
