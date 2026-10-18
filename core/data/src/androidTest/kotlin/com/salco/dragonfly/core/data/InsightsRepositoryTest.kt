package com.salco.dragonfly.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.salco.dragonfly.core.data.repository.OfflineInsightsRepository
import com.salco.dragonfly.core.data.repository.OfflineTransactionsRepository
import com.salco.dragonfly.core.data.repository.RuleApplier
import com.salco.dragonfly.core.database.DragonflyDatabase
import com.salco.dragonfly.core.database.model.AccountEntity
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.MonthlySummary
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class InsightsRepositoryTest {

    private lateinit var db: DragonflyDatabase
    private lateinit var transactions: OfflineTransactionsRepository
    private lateinit var insights: OfflineInsightsRepository
    private var checking = 0L
    private var savings = 0L

    private fun account(name: String, type: AccountType, opening: Long = 0, netWorth: Boolean = true) = AccountEntity(
        name = name, type = type, currencyCode = "EUR", openingBalance = opening, iban = null, notes = null,
        active = true, includeInNetWorth = netWorth, assetRole = null, liabilityType = null, createdAt = 0,
    )

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DragonflyDatabase::class.java).build()
        transactions = OfflineTransactionsRepository(db.transactionDao(), RuleApplier(db.ruleDao(), db.accountDao()))
        insights = OfflineInsightsRepository(db.insightsDao())
        checking = db.accountDao().upsert(account("Conto", AccountType.ASSET, opening = 100_000))
        savings = db.accountDao().upsert(account("Risparmi", AccountType.ASSET, opening = 50_000))
        db.accountDao().upsert(account("Escluso", AccountType.ASSET, opening = 999_999, netWorth = false))
    }

    @After
    fun tearDown() = db.close()

    private suspend fun add(type: TransactionType, date: LocalDate, amount: Long, source: AccountRef, destination: AccountRef, category: String? = null) =
        transactions.saveTransaction(
            TransactionDraft(type = type, description = "t", date = date, amount = amount, currencyCode = "EUR",
                source = source, destination = destination, categoryName = category),
        )

    @Test
    fun aggregatesByMonthCategoryAndNetWorth() = runTest {
        val own = AccountRef.Existing(checking)
        add(TransactionType.DEPOSIT, LocalDate.of(2026, 9, 27), 200_000, AccountRef.New("Azienda", AccountType.REVENUE, "EUR"), own)
        add(TransactionType.WITHDRAWAL, LocalDate.of(2026, 10, 3), 30_000, own, AccountRef.New("Supermercato", AccountType.EXPENSE, "EUR"), "Spesa")
        add(TransactionType.WITHDRAWAL, LocalDate.of(2026, 10, 9), 5_000, own, AccountRef.New("Bar", AccountType.EXPENSE, "EUR"))
        // Transfers between own accounts change neither cash flow nor net worth.
        add(TransactionType.TRANSFER, LocalDate.of(2026, 10, 10), 50_000, own, AccountRef.Existing(savings))

        assertEquals(
            listOf(
                MonthlySummary(YearMonth.of(2026, 9), "EUR", 200_000, 0),
                MonthlySummary(YearMonth.of(2026, 10), "EUR", 0, 35_000),
            ),
            insights.observeMonthlySummaries(YearMonth.of(2026, 5), YearMonth.of(2026, 10)).first(),
        )
        assertEquals(
            listOf("Spesa" to 30_000L, null to 5_000L),
            insights.observeCategorySpending(YearMonth.of(2026, 10)).first().map { it.categoryName to it.amount },
        )
        assertEquals(
            listOf(150_000L, 350_000L, 315_000L),
            insights.observeNetWorthHistory(YearMonth.of(2026, 8), YearMonth.of(2026, 10)).first().map { it.amount },
        )
    }
}
