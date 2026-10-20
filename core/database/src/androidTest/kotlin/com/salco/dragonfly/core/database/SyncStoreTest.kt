package com.salco.dragonfly.core.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.salco.dragonfly.core.database.dao.SplitPart
import com.salco.dragonfly.core.database.model.AccountEntity
import com.salco.dragonfly.core.database.model.TransactionEntity
import com.salco.dragonfly.core.database.sync.SyncRecord
import com.salco.dragonfly.core.database.sync.SyncStore
import com.salco.dragonfly.core.database.sync.SyncTriggers
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Two in-memory databases play two devices exchanging records through a fake server. */
@RunWith(AndroidJUnit4::class)
class SyncStoreTest {

    private lateinit var phone: DragonflyDatabase
    private lateinit var tablet: DragonflyDatabase
    private lateinit var phoneSync: SyncStore
    private lateinit var tabletSync: SyncStore

    private fun device(): DragonflyDatabase =
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DragonflyDatabase::class.java)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) = SyncTriggers.install(db)
            })
            .build()

    @Before
    fun setUp() {
        phone = device()
        tablet = device()
        phoneSync = SyncStore(phone)
        tabletSync = SyncStore(tablet)
    }

    @After
    fun tearDown() {
        phone.close()
        tablet.close()
    }

    private fun account(name: String, type: AccountType) = AccountEntity(
        name = name, type = type, currencyCode = "EUR", openingBalance = 0, iban = null, notes = null,
        active = true, includeInNetWorth = type == AccountType.ASSET, assetRole = null, liabilityType = null, createdAt = 0,
    )

    private fun withdrawal(amount: Long, source: Long, destination: Long, description: String = "Spesa") = TransactionEntity(
        type = TransactionType.WITHDRAWAL, description = description, date = 20_000, amount = amount, currencyCode = "EUR",
        foreignAmount = null, sourceAccountId = source, destinationAccountId = destination, notes = null,
    )

    /** Sends the phone's changes to the tablet, as a push followed by a pull would. */
    private suspend fun phoneToTablet(): List<SyncRecord> {
        val changes = phoneSync.pendingChanges()
        phoneSync.markSent(changes)
        tabletSync.applyRemote(changes)
        return changes
    }

    @Test
    fun everyWriteIsTracked_withReferencesAsSyncIds() = runTest {
        val checking = phone.accountDao().upsert(account("Conto", AccountType.ASSET))
        val shop = phone.accountDao().upsert(account("Supermercato", AccountType.EXPENSE))
        phone.transactionDao().save(withdrawal(4_250, checking, shop), categoryName = "Cibo", tagNames = listOf("casa"))

        val changes = phoneSync.pendingChanges()
        assertEquals(
            listOf("accounts", "accounts", "categories", "tags", "transactions", "transaction_tags"),
            changes.map { it.table },
        )
        val accountIds = changes.filter { it.table == "accounts" }.map { it.syncId }
        val transaction = changes.single { it.table == "transactions" }.data!!
        assertFalse("id" in transaction)
        assertEquals(accountIds[0], transaction.getValue("source_account_id").jsonPrimitive.content)
        assertEquals(accountIds[1], transaction.getValue("destination_account_id").jsonPrimitive.content)

        phoneSync.markSent(changes)
        assertEquals(0, phoneSync.pendingCount())
    }

    @Test
    fun anotherDeviceRebuildsTheSameData() = runTest {
        val checking = phone.accountDao().upsert(account("Conto", AccountType.ASSET))
        val shop = phone.accountDao().upsert(account("Supermercato", AccountType.EXPENSE))
        phone.transactionDao().save(withdrawal(4_250, checking, shop), categoryName = "Cibo", tagNames = listOf("casa"))
        phone.transactionDao().saveGroup(
            0,
            "Centro commerciale",
            listOf(SplitPart(withdrawal(1_000, checking, shop, "Pane")), SplitPart(withdrawal(2_000, checking, shop, "Latte"))),
        )

        phoneToTablet()

        val transactions = tablet.transactionDao().observeBetween(0, 100_000).first()
        assertEquals(3, transactions.size)
        val single = transactions.single { it.transaction.groupId == null }
        assertEquals("Conto", single.sourceName)
        assertEquals("Supermercato", single.destinationName)
        assertEquals("Cibo", single.categoryName)
        assertEquals(listOf("casa"), single.tags.map { it.name })
        assertEquals(setOf("Centro commerciale"), transactions.mapNotNull { it.groupTitle }.toSet())
        // Applying remote changes must not queue them to be sent back.
        assertEquals(0, tabletSync.pendingCount())
        val balance = tablet.accountDao().observeAll().first().single { it.account.name == "Conto" }.balance
        assertEquals(-7_250L, balance)
    }

    @Test
    fun editsAndDeletionsTravel_andTheNewestChangeWins() = runTest {
        val checking = phone.accountDao().upsert(account("Conto", AccountType.ASSET))
        val shop = phone.accountDao().upsert(account("Supermercato", AccountType.EXPENSE))
        val id = phone.transactionDao().upsert(withdrawal(1_000, checking, shop))
        phoneToTablet()

        // The tablet edits first, the phone later: the phone's change is newer and wins on both.
        val onTablet = tablet.transactionDao().observeBetween(0, 100_000).first().single().transaction
        tablet.transactionDao().upsert(onTablet.copy(description = "Dal tablet"))
        Thread.sleep(5)
        phone.transactionDao().upsert(withdrawal(1_000, checking, shop, "Dal telefono").copy(id = id))

        val fromTablet = tabletSync.pendingChanges()
        tabletSync.markSent(fromTablet)
        phoneSync.applyRemote(fromTablet)
        phoneToTablet()
        assertEquals("Dal telefono", phone.transactionDao().observeById(id).first()!!.transaction.description)
        assertEquals("Dal telefono", tablet.transactionDao().observeBetween(0, 100_000).first().single().transaction.description)

        // Deleting an account cascades to its transactions; every deletion is sent.
        phone.accountDao().deleteById(shop)
        val deletions = phoneToTablet()
        assertTrue(deletions.all { it.deleted })
        assertEquals(setOf("accounts", "transactions"), deletions.map { it.table }.toSet())
        assertTrue(tablet.transactionDao().observeBetween(0, 100_000).first().isEmpty())
        assertEquals(listOf("Conto"), tablet.accountDao().observeAll().first().map { it.account.name })
    }

    @Test
    fun aRecordWaitsForTheRowItRefersTo() = runTest {
        val checking = phone.accountDao().upsert(account("Conto", AccountType.ASSET))
        val shop = phone.accountDao().upsert(account("Supermercato", AccountType.EXPENSE))
        phone.transactionDao().upsert(withdrawal(1_000, checking, shop))
        val changes = phoneSync.pendingChanges()

        // Only the transaction arrives: it cannot be placed yet.
        assertEquals(0, tabletSync.applyRemote(changes.filter { it.table == "transactions" }))
        assertTrue(tablet.transactionDao().observeBetween(0, 100_000).first().isEmpty())
        assertEquals(3, tabletSync.applyRemote(changes))
        assertEquals(1, tablet.transactionDao().observeBetween(0, 100_000).first().size)
    }

    @Test
    fun unknownColumnsFromANewerVersionAreIgnored() = runTest {
        val record = SyncRecord(
            table = "categories",
            syncId = "00000000-0000-4000-8000-000000000001",
            data = JsonObject(mapOf("name" to JsonPrimitive("Viaggi"), "color" to JsonPrimitive("#ff0000"))),
            modifiedAt = 1,
        )
        assertEquals(1, tabletSync.applyRemote(listOf(record)))
        assertEquals(listOf("Viaggi"), tablet.categoryDao().observeAll().first().map { it.name })
        assertNull(tabletSync.lastPulledAt())
    }
}
