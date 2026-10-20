package com.salco.dragonfly.core.data

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.salco.dragonfly.core.data.repository.OfflineFirstSyncRepository
import com.salco.dragonfly.core.database.DragonflyDatabase
import com.salco.dragonfly.core.database.model.AccountEntity
import com.salco.dragonfly.core.database.model.CategoryEntity
import com.salco.dragonfly.core.database.sync.SyncStore
import com.salco.dragonfly.core.database.sync.SyncTriggers
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.SyncError
import com.salco.dragonfly.core.model.SyncException
import com.salco.dragonfly.core.network.NetworkAccount
import com.salco.dragonfly.core.network.NetworkRecord
import com.salco.dragonfly.core.network.SyncNetworkDataSource
import com.salco.dragonfly.core.network.SyncNetworkException
import com.salco.dragonfly.core.testing.FakeUserDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** In-memory stand-in for the sync_records table, with the same last-write-wins rule. */
private class FakeServer {
    val rows = mutableMapOf<Pair<String, String>, NetworkRecord>()
    private var clock = Instant.parse("2026-10-19T08:00:00Z")
    var offline = false

    fun upsert(userId: String, records: List<NetworkRecord>) {
        if (offline) throw SyncNetworkException(SyncNetworkException.Reason.Offline)
        records.forEach { record ->
            val key = userId to "${record.table}/${record.id}"
            val existing = rows[key]
            if (existing == null || record.modifiedAt >= existing.modifiedAt) {
                clock = clock.plusMillis(1)
                rows[key] = record.copy(updatedAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(clock.atOffset(ZoneOffset.UTC)))
            }
        }
    }

    fun select(userId: String, since: String?, limit: Int): List<NetworkRecord> {
        if (offline) throw SyncNetworkException(SyncNetworkException.Reason.Offline)
        return rows.filterKeys { it.first == userId }.values
            .filter { since == null || Instant.parse(it.updatedAt!!) >= Instant.parse(since) }
            .sortedBy { Instant.parse(it.updatedAt!!) }
            .take(limit)
    }
}

private class FakeNetwork(private val server: FakeServer, userId: String?) : SyncNetworkDataSource {
    override val isConfigured = true
    override val account = MutableStateFlow<NetworkAccount>(
        userId?.let { NetworkAccount.SignedIn(it, "$it@example.com") } ?: NetworkAccount.SignedOut,
    )
    private val userId get() = (account.value as NetworkAccount.SignedIn).userId

    override val googleClientId: String? = null
    override suspend fun signInWithGoogle(idToken: String, rawNonce: String) = Unit
    override suspend fun sendCode(email: String) = Unit
    override suspend fun verifyCode(email: String, code: String) {
        if (code != "123456") throw SyncNetworkException(SyncNetworkException.Reason.InvalidCode)
        account.value = NetworkAccount.SignedIn(email.substringBefore("@"), email)
    }
    override suspend fun signOut() { account.value = NetworkAccount.SignedOut }
    override suspend fun push(records: List<NetworkRecord>) = server.upsert(userId, records)
    override suspend fun pull(since: String?, limit: Int) = server.select(userId, since, limit)
}

@RunWith(AndroidJUnit4::class)
class SyncRepositoryTest {

    private val server = FakeServer()
    private val databases = mutableListOf<DragonflyDatabase>()

    private fun database() = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DragonflyDatabase::class.java)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) = SyncTriggers.install(db)
        })
        .build()
        .also { databases += it }

    private class Device(val db: DragonflyDatabase, val network: FakeNetwork, val repository: OfflineFirstSyncRepository)

    private fun device(userId: String? = "luca"): Device {
        val db = database()
        val network = FakeNetwork(server, userId)
        val repository = OfflineFirstSyncRepository(
            network, SyncStore(db), db.syncMetaDao(), FakeUserDataRepository(), Clock.fixed(Instant.parse("2026-10-19T09:00:00Z"), ZoneOffset.UTC),
        )
        return Device(db, network, repository)
    }

    @After
    fun tearDown() = databases.forEach { it.close() }

    private fun account(name: String) = AccountEntity(
        name = name, type = AccountType.ASSET, currencyCode = "EUR", openingBalance = 0, iban = null, notes = null,
        active = true, includeInNetWorth = true, assetRole = null, liabilityType = null, createdAt = 0,
    )

    @Test
    fun twoDevicesConvergeThroughTheServer() = runTest {
        val phone = device()
        val tablet = device()
        phone.db.accountDao().upsert(account("Conto"))
        tablet.db.categoryDao().upsert(CategoryEntity(name = "Viaggi"))

        assertEquals(1, phone.repository.sync()!!.sent)
        val tabletResult = tablet.repository.sync()!!
        assertEquals(1, tabletResult.sent)
        assertEquals(1, tabletResult.received)
        phone.repository.sync()

        listOf(phone, tablet).forEach { device ->
            assertEquals(listOf("Conto"), device.db.accountDao().observeAll().first().map { it.account.name })
            assertEquals(listOf("Viaggi"), device.db.categoryDao().observeAll().first().map { it.name })
            with(device.repository.status.first()) {
                assertEquals(0, pendingChanges)
                assertEquals(Instant.parse("2026-10-19T09:00:00Z").toEpochMilli(), lastSyncAt)
                assertNull(lastError)
            }
        }
    }

    @Test
    fun signedOut_doesNothing_andOfflineIsReported() = runTest {
        val guest = device(userId = null)
        assertNull(guest.repository.sync())

        val phone = device()
        phone.db.accountDao().upsert(account("Conto"))
        server.offline = true
        try {
            phone.repository.sync()
            fail("Expected SyncException")
        } catch (e: SyncException) {
            assertEquals(SyncError.Offline, e.error)
        }
        with(phone.repository.status.first()) {
            assertEquals(SyncError.Offline, lastError)
            assertEquals(1, pendingChanges)
        }
        server.offline = false
        phone.repository.sync()
        assertEquals(0, phone.repository.status.first().pendingChanges)
    }

    @Test
    fun wrongCode_isRejected_andAnotherAccountGetsTheLocalData() = runTest {
        val phone = device(userId = null)
        phone.db.accountDao().upsert(account("Conto"))
        try {
            phone.repository.verifyCode("anna@example.com", "000000")
            fail("Expected SyncException")
        } catch (e: SyncException) {
            assertEquals(SyncError.InvalidCode, e.error)
        }
        phone.repository.verifyCode("anna@example.com", "123456")
        phone.repository.sync()
        assertEquals(1, server.rows.keys.count { it.first == "anna" })

        // Signing in as someone else sends everything again to that account.
        phone.repository.signOut()
        phone.repository.verifyCode("marco@example.com", "123456")
        phone.repository.sync()
        assertTrue(server.rows.keys.any { it.first == "marco" })
    }
}
