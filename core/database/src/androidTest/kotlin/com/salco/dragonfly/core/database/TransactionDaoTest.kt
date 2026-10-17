package com.salco.dragonfly.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.salco.dragonfly.core.database.dao.AccountDao
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
}
