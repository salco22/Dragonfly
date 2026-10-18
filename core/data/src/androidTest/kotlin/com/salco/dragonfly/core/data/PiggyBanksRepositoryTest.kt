package com.salco.dragonfly.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.salco.dragonfly.core.data.repository.InsufficientFundsException
import com.salco.dragonfly.core.data.repository.OfflinePiggyBanksRepository
import com.salco.dragonfly.core.database.DragonflyDatabase
import com.salco.dragonfly.core.database.RoomTransactionRunner
import com.salco.dragonfly.core.database.model.AccountEntity
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.PiggyBank
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class PiggyBanksRepositoryTest {

    private lateinit var db: DragonflyDatabase
    private lateinit var repository: OfflinePiggyBanksRepository
    private var account = 0L

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DragonflyDatabase::class.java).build()
        repository = OfflinePiggyBanksRepository(db.piggyBankDao(), db.accountDao(), RoomTransactionRunner(db))
        account = db.accountDao().upsert(
            AccountEntity(
                name = "Risparmi", type = AccountType.ASSET, currencyCode = "EUR", openingBalance = 50_000, iban = null, notes = null,
                active = true, includeInNetWorth = true, assetRole = null, liabilityType = null, createdAt = 0,
            ),
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun moneyCannotExceedTheAccountAndDeletionCascades() = runTest {
        val date = LocalDate.of(2026, 10, 18)
        val a = repository.savePiggyBank(PiggyBank(name = "A", accountId = account, targetAmount = 40_000, startDate = date))
        val b = repository.savePiggyBank(PiggyBank(name = "B", accountId = account, startDate = date))
        repository.moveMoney(a, 30_000, date)
        try {
            repository.moveMoney(b, 30_000, date)
            fail("Expected InsufficientFundsException")
        } catch (e: InsufficientFundsException) {
            assertEquals(20_000L, e.available)
        }
        repository.moveMoney(b, 20_000, date)
        repository.moveMoney(a, -10_000, date)

        assertEquals(mapOf("A" to 20_000L, "B" to 20_000L), repository.observePiggyBanks().first().associate { it.piggyBank.name to it.saved })
        assertEquals(listOf(-10_000L, 30_000L), repository.observeEvents(a).first().map { it.amount }.sorted())

        db.accountDao().deleteById(account)
        assertTrue(repository.observePiggyBanks().first().isEmpty())
    }
}
