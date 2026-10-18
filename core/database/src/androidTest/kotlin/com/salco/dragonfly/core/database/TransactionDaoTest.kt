package com.salco.dragonfly.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.salco.dragonfly.core.database.dao.AccountDao
import com.salco.dragonfly.core.database.dao.SplitPart
import com.salco.dragonfly.core.database.dao.TransactionDao
import com.salco.dragonfly.core.database.model.AccountEntity
import com.salco.dragonfly.core.database.model.TransactionEntity
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var db: DragonflyDatabase
    private lateinit var accountDao: AccountDao
    private lateinit var transactionDao: TransactionDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DragonflyDatabase::class.java).build()
        accountDao = db.accountDao()
        transactionDao = db.transactionDao()
    }

    @After
    fun tearDown() = db.close()

    private fun account(name: String, type: AccountType, currency: String = "EUR", opening: Long = 0) = AccountEntity(
        name = name, type = type, currencyCode = currency, openingBalance = opening, iban = null, notes = null,
        active = true, includeInNetWorth = true, assetRole = null, liabilityType = null, createdAt = 0,
    )

    private fun transaction(type: TransactionType, amount: Long, source: Long, destination: Long, foreign: Long? = null) =
        TransactionEntity(
            type = type, description = "t", date = 20_000, amount = amount, currencyCode = "EUR",
            foreignAmount = foreign, sourceAccountId = source, destinationAccountId = destination, notes = null,
        )

    @Test
    fun balances_followTransactions() = runTest {
        val checking = accountDao.upsert(account("Conto", AccountType.ASSET, opening = 100_000))
        val usd = accountDao.upsert(account("USD", AccountType.ASSET, currency = "USD"))
        val loan = accountDao.upsert(account("Prestito", AccountType.LIABILITY, opening = -50_000))
        val shop = accountDao.upsert(account("Supermercato", AccountType.EXPENSE))
        val salary = accountDao.upsert(account("Stipendio", AccountType.REVENUE))

        transactionDao.upsert(transaction(TransactionType.WITHDRAWAL, 4_000, checking, shop))
        transactionDao.upsert(transaction(TransactionType.DEPOSIT, 150_000, salary, checking))
        transactionDao.upsert(transaction(TransactionType.TRANSFER, 10_000, checking, loan))
        transactionDao.upsert(transaction(TransactionType.TRANSFER, 20_000, checking, usd, foreign = 21_600))

        val balances = accountDao.observeAll().first().associate { it.account.id to it.balance }
        assertEquals(100_000L - 4_000 + 150_000 - 10_000 - 20_000, balances[checking])
        assertEquals(21_600L, balances[usd])
        assertEquals(-40_000L, balances[loan])
    }

    @Test
    fun upsertResolvingAccounts_reusesOrCreatesCounterparties() = runTest {
        val checking = accountDao.upsert(account("Conto", AccountType.ASSET))
        val shop = accountDao.upsert(account("Supermercato", AccountType.EXPENSE))

        transactionDao.save(
            transaction(TransactionType.WITHDRAWAL, 1_000, checking, 0),
            newSource = null,
            newDestination = account("supermercato", AccountType.EXPENSE),
        )
        transactionDao.save(
            transaction(TransactionType.WITHDRAWAL, 2_000, checking, 0),
            newSource = null,
            newDestination = account("Farmacia", AccountType.EXPENSE),
        )

        val saved = transactionDao.observeForAccount(checking).first()
        assertEquals(setOf("Supermercato", "Farmacia"), saved.map { it.destinationName }.toSet())
        assertTrue(saved.any { it.transaction.destinationAccountId == shop })
        assertEquals(3, accountDao.observeAll().first().size)
    }

    @Test
    fun deletingAccount_deletesItsTransactions() = runTest {
        val checking = accountDao.upsert(account("Conto", AccountType.ASSET))
        val shop = accountDao.upsert(account("Supermercato", AccountType.EXPENSE))
        transactionDao.upsert(transaction(TransactionType.WITHDRAWAL, 1_000, checking, shop))

        accountDao.deleteById(shop)

        assertTrue(transactionDao.observeForAccount(checking).first().isEmpty())
    }

    @Test
    fun save_resolvesCategoryAndTagsAndTotals() = runTest {
        val checking = accountDao.upsert(account("Conto", AccountType.ASSET))
        val shop = accountDao.upsert(account("Supermercato", AccountType.EXPENSE))

        val id = transactionDao.save(
            transaction(TransactionType.WITHDRAWAL, 3_000, checking, shop),
            categoryName = "Spesa",
            tagNames = listOf("Casa", "casa", "Vacanze"),
        )
        transactionDao.save(
            transaction(TransactionType.WITHDRAWAL, 2_000, checking, shop),
            categoryName = "spesa ",
            tagNames = listOf("Casa"),
        )

        val saved = transactionDao.observeById(id).first()!!
        assertEquals("Spesa", saved.categoryName)
        assertEquals(setOf("Casa", "Vacanze"), saved.tags.map { it.name }.toSet())

        val totals = db.categoryDao().observeTotals(0, 100_000).first().single()
        assertEquals(5_000L, totals.spent)
        assertEquals(mapOf("Casa" to 2, "Vacanze" to 1), db.tagDao().observeUsage().first().associate { it.name to it.transactionCount })

        // Editing replaces the tags instead of adding to them.
        transactionDao.save(saved.transaction, tagNames = listOf("Lavoro"))
        assertEquals(listOf("Lavoro"), transactionDao.observeById(id).first()!!.tags.map { it.name })
    }

    @Test
    fun deletingCategory_keepsTransactionsUncategorized() = runTest {
        val checking = accountDao.upsert(account("Conto", AccountType.ASSET))
        val shop = accountDao.upsert(account("Supermercato", AccountType.EXPENSE))
        val id = transactionDao.save(transaction(TransactionType.WITHDRAWAL, 1_000, checking, shop), categoryName = "Spesa")

        db.categoryDao().deleteById(transactionDao.observeById(id).first()!!.transaction.categoryId!!)

        val after = transactionDao.observeById(id).first()!!
        assertEquals(null, after.transaction.categoryId)
    }

    @Test
    fun search_matchesTextAndFilters() = runTest {
        val checking = accountDao.upsert(account("Conto", AccountType.ASSET))
        val shop = accountDao.upsert(account("Supermercato", AccountType.EXPENSE))
        val bar = accountDao.upsert(account("Bar", AccountType.EXPENSE))
        transactionDao.save(transaction(TransactionType.WITHDRAWAL, 3_000, checking, shop).copy(description = "Spesa", date = 100), categoryName = "Cibo", tagNames = listOf("Casa"))
        transactionDao.save(transaction(TransactionType.WITHDRAWAL, 500, checking, bar).copy(description = "Caffè", date = 200), tagNames = listOf("Lavoro"))

        suspend fun search(
            query: String? = null, type: String? = null, tagId: Long? = null, accountId: Long? = null,
            from: Long? = null, to: Long? = null,
        ) = transactionDao.search(query, type, null, tagId, null, null, accountId, from, to).first().map { it.transaction.description }

        assertEquals(listOf("Spesa"), search(query = "SUPERMERC"))
        assertEquals(listOf("Spesa"), search(query = "cibo"))
        assertEquals(listOf("Caffè"), search(query = "lavoro"))
        assertEquals(listOf("Caffè"), search(accountId = bar))
        assertEquals(listOf("Caffè", "Spesa"), search(type = "WITHDRAWAL"))
        assertEquals(emptyList<String>(), search(type = "DEPOSIT"))
        assertEquals(listOf("Spesa"), search(from = 50, to = 150))
        val casa = db.tagDao().findByName("casa")!!.id
        assertEquals(listOf("Spesa"), search(tagId = casa))
    }

    @Test
    fun budgetSpending_countsOnlyItsWithdrawalsInPeriodAndCurrency() = runTest {
        val checking = accountDao.upsert(account("Conto", AccountType.ASSET))
        val shop = accountDao.upsert(account("Supermercato", AccountType.EXPENSE))
        val budget = db.budgetDao().upsert(com.salco.dragonfly.core.database.model.BudgetEntity(name = "Spesa", amount = 40_000, currencyCode = "EUR", active = true))
        transactionDao.save(transaction(TransactionType.WITHDRAWAL, 3_000, checking, shop).copy(budgetId = budget, date = 100))
        transactionDao.save(transaction(TransactionType.WITHDRAWAL, 9_000, checking, shop).copy(budgetId = budget, date = 500))
        transactionDao.save(transaction(TransactionType.WITHDRAWAL, 700, checking, shop).copy(budgetId = budget, date = 100, currencyCode = "USD"))
        transactionDao.save(transaction(TransactionType.WITHDRAWAL, 2_000, checking, shop).copy(date = 100))

        assertEquals(3_000L, db.budgetDao().observeSpending(0, 200).first().single().spent)
        assertEquals(mapOf("EUR" to 2_000L), db.budgetDao().observeUnbudgeted(0, 200).first().associate { it.currencyCode to it.total })
        assertEquals(listOf("Spesa", "Spesa"), transactionDao.search(null, null, null, null, budget, null, null, 0, 200).first().map { it.budgetName })

        db.budgetDao().deleteById(budget)
        assertTrue(transactionDao.observeForAccount(checking).first().all { it.transaction.budgetId == null })
    }

    @Test
    fun billPayments_areLinkedSearchableAndSurviveBillDeletion() = runTest {
        val checking = accountDao.upsert(account("Conto", AccountType.ASSET))
        val landlord = accountDao.upsert(account("Padrone", AccountType.EXPENSE))
        val bill = db.billDao().upsert(
            com.salco.dragonfly.core.database.model.BillEntity(
                name = "Affitto", amountMin = 80_000, amountMax = 80_000, currencyCode = "EUR",
                frequency = com.salco.dragonfly.core.model.RecurrenceFrequency.MONTHLY, interval = 1, startDate = 0,
                endDate = null, active = true, notes = null,
            ),
        )
        val paid = transactionDao.save(transaction(TransactionType.WITHDRAWAL, 80_000, checking, landlord).copy(billId = bill, date = 300))
        transactionDao.save(transaction(TransactionType.WITHDRAWAL, 1_000, checking, landlord))

        assertEquals(listOf(paid), db.billDao().observePayments().first().map { it.id })
        assertEquals("Affitto", transactionDao.observeById(paid).first()!!.billName)
        assertEquals(listOf(paid), transactionDao.search(null, null, null, null, null, bill, null, null, null).first().map { it.transaction.id })

        db.billDao().deleteById(bill)
        assertEquals(null, transactionDao.observeById(paid).first()!!.transaction.billId)
    }

    @Test
    fun saveGroup_createsUpdatesAndDissolvesSplitTransactions() = runTest {
        val checking = accountDao.upsert(account("Conto", AccountType.ASSET, opening = 10_000))
        val shop = accountDao.upsert(account("Supermercato", AccountType.EXPENSE))
        val pharmacy = accountDao.upsert(account("Farmacia", AccountType.EXPENSE))

        val groupId = transactionDao.saveGroup(
            groupId = 0,
            title = "Spesa al centro commerciale",
            parts = listOf(
                SplitPart(transaction(TransactionType.WITHDRAWAL, 3_000, checking, shop), categoryName = "Alimentari"),
                SplitPart(transaction(TransactionType.WITHDRAWAL, 1_200, checking, pharmacy), categoryName = "Salute"),
            ),
        )
        assertTrue(groupId > 0)
        val parts = transactionDao.observeGroup(groupId).first()
        assertEquals(listOf(3_000L, 1_200L), parts.map { it.transaction.amount })
        assertEquals(listOf("Alimentari", "Salute"), parts.map { it.categoryName })
        assertEquals("Spesa al centro commerciale", parts.first().groupTitle)
        // Every part moves money on its own, so the balance sees the whole total.
        assertEquals(10_000L - 4_200, accountDao.observeAll().first().first { it.account.id == checking }.balance)

        // Edit part 1, drop part 2, add a new part 3.
        val (first, _) = parts
        transactionDao.saveGroup(
            groupId = groupId,
            title = "Centro commerciale",
            parts = listOf(
                SplitPart(first.transaction.copy(amount = 2_500)),
                SplitPart(transaction(TransactionType.WITHDRAWAL, 800, checking, pharmacy)),
            ),
        )
        val edited = transactionDao.observeGroup(groupId).first()
        assertEquals(listOf(2_500L, 800L), edited.map { it.transaction.amount })
        assertEquals(first.transaction.id, edited.first().transaction.id)
        assertEquals("Centro commerciale", edited.first().groupTitle)

        // A single part left becomes an ordinary transaction.
        assertEquals(0L, transactionDao.saveGroup(groupId, "x", listOf(SplitPart(edited.first().transaction))))
        val remaining = transactionDao.observeBetween(0, 100_000).first()
        assertEquals(1, remaining.size)
        assertEquals(null, remaining.single().transaction.groupId)
    }

    @Test
    fun deleteGroup_deletesEveryPart() = runTest {
        val checking = accountDao.upsert(account("Conto", AccountType.ASSET))
        val shop = accountDao.upsert(account("Supermercato", AccountType.EXPENSE))
        val other = transactionDao.upsert(transaction(TransactionType.WITHDRAWAL, 100, checking, shop))
        val groupId = transactionDao.saveGroup(
            0,
            "Spesa",
            listOf(
                SplitPart(transaction(TransactionType.WITHDRAWAL, 1_000, checking, shop)),
                SplitPart(transaction(TransactionType.WITHDRAWAL, 2_000, checking, shop)),
            ),
        )
        transactionDao.deleteGroup(groupId)
        assertEquals(listOf(other), transactionDao.observeBetween(0, 100_000).first().map { it.transaction.id })
    }
}
