package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.database.sync.SyncMetaDao
import com.salco.dragonfly.core.database.sync.SyncRecord
import com.salco.dragonfly.core.database.sync.SyncStore
import com.salco.dragonfly.core.model.SyncAccount
import com.salco.dragonfly.core.model.SyncError
import com.salco.dragonfly.core.model.SyncException
import com.salco.dragonfly.core.model.SyncResult
import com.salco.dragonfly.core.model.SyncStatus
import com.salco.dragonfly.core.network.NetworkAccount
import com.salco.dragonfly.core.network.NetworkRecord
import com.salco.dragonfly.core.network.SyncNetworkDataSource
import com.salco.dragonfly.core.network.SyncNetworkException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** Signs in to the sync server and keeps local data and the server in step. */
interface SyncRepository {
    val account: Flow<SyncAccount>
    val status: Flow<SyncStatus>

    /** Google client id for the sign-in request, or null when Google sign-in is not available. */
    val googleClientId: String?

    /** Emails a one-time code; throws [SyncException]. */
    suspend fun sendCode(email: String)

    /** Signs in with the emailed code; throws [SyncException]. */
    suspend fun verifyCode(email: String, code: String)

    /** Signs in with a Google ID token obtained with the SHA-256 of [rawNonce]; throws [SyncException]. */
    suspend fun signInWithGoogle(idToken: String, rawNonce: String)

    /** Signs out; local data stays and is sent again to the next account. */
    suspend fun signOut()

    /**
     * Sends local changes, then receives remote ones. Returns null when not signed in; throws
     * [SyncException] when it fails (the error is also kept in [status]).
     */
    suspend fun sync(): SyncResult?
}

@Singleton
internal class OfflineFirstSyncRepository(
    private val network: SyncNetworkDataSource,
    private val store: SyncStore,
    private val syncMetaDao: SyncMetaDao,
    private val userDataRepository: UserDataRepository,
    private val clock: Clock,
) : SyncRepository {

    @Inject
    constructor(
        network: SyncNetworkDataSource,
        store: SyncStore,
        syncMetaDao: SyncMetaDao,
        userDataRepository: UserDataRepository,
    ) : this(network, store, syncMetaDao, userDataRepository, Clock.systemUTC())

    private val mutex = Mutex()
    private val running = MutableStateFlow(false)
    private val lastError = MutableStateFlow<SyncError?>(null)

    override val account: Flow<SyncAccount> =
        if (!network.isConfigured) {
            flowOf(SyncAccount.NotConfigured)
        } else {
            network.account.map { account ->
                when (account) {
                    NetworkAccount.Loading -> SyncAccount.Loading
                    NetworkAccount.SignedOut -> SyncAccount.SignedOut
                    is NetworkAccount.SignedIn -> SyncAccount.SignedIn(account.email)
                }
            }
        }

    override val status: Flow<SyncStatus> = combine(
        running,
        lastError,
        syncMetaDao.observePendingCount(),
        userDataRepository.userPreferences,
    ) { running, error, pending, preferences ->
        SyncStatus(running = running, pendingChanges = pending, lastSyncAt = preferences.lastSyncAt, lastError = error)
    }

    override val googleClientId: String? get() = network.googleClientId

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String) = translating { network.signInWithGoogle(idToken, rawNonce) }

    override suspend fun sendCode(email: String) = translating { network.sendCode(email.trim()) }

    override suspend fun verifyCode(email: String, code: String) = translating { network.verifyCode(email.trim(), code.trim()) }

    override suspend fun signOut() = mutex.withLock {
        translating { network.signOut() }
        store.reset()
        userDataRepository.setSyncState(lastSyncAt = null, syncedUserId = null)
        // Back to the login screen, as after a fresh install.
        userDataRepository.setLoginSkipped(false)
        lastError.value = null
    }

    override suspend fun sync(): SyncResult? = mutex.withLock {
        if (!network.isConfigured) return@withLock null
        val account = network.account.first { it != NetworkAccount.Loading } as? NetworkAccount.SignedIn
            ?: return@withLock null
        running.value = true
        try {
            // Data synced with another account is sent to this one as new.
            if (userDataRepository.userPreferences.first().syncedUserId != account.userId) {
                store.reset()
                userDataRepository.setSyncState(lastSyncAt = null, syncedUserId = account.userId)
            }
            val sent = push()
            val received = pull()
            userDataRepository.setSyncState(lastSyncAt = clock.millis(), syncedUserId = account.userId)
            lastError.value = null
            SyncResult(sent, received)
        } catch (e: SyncException) {
            lastError.value = e.error
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            lastError.value = SyncError.Server
            throw SyncException(SyncError.Server, e)
        } finally {
            running.value = false
        }
    }

    private suspend fun push(): Int {
        val changes = store.pendingChanges()
        translating { network.push(changes.map { it.asNetworkRecord() }) }
        store.markSent(changes)
        return changes.size
    }

    private suspend fun pull(): Int {
        val previous = store.lastPulledAt()
        // Rows committed late can carry a slightly older server time: look back a little.
        var since = previous?.let { OffsetDateTime.parse(it).minusMinutes(OVERLAP_MINUTES).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) }
        var newest = previous
        var received = 0
        while (true) {
            val page = translating { network.pull(since, PAGE_SIZE) }
            received += store.applyRemote(page.map { it.asSyncRecord() })
            page.lastOrNull()?.updatedAt?.let { newest = maxOf(newest, it) }
            val next = page.lastOrNull()?.updatedAt
            if (page.size < PAGE_SIZE || next == null || next == since) break
            since = next
        }
        store.setLastPulledAt(newest)
        return received
    }

    private suspend fun <T> translating(block: suspend () -> T): T = try {
        block()
    } catch (e: SyncNetworkException) {
        throw SyncException(
            when (e.reason) {
                SyncNetworkException.Reason.Offline -> SyncError.Offline
                SyncNetworkException.Reason.InvalidCode -> SyncError.InvalidCode
                SyncNetworkException.Reason.RateLimited -> SyncError.RateLimited
                SyncNetworkException.Reason.SignedOut -> SyncError.SignedOut
                SyncNetworkException.Reason.Server -> SyncError.Server
            },
            e,
        )
    }

    private companion object {
        const val PAGE_SIZE = 500
        const val OVERLAP_MINUTES = 2L
    }
}

private fun maxOf(a: String?, b: String): String =
    if (a == null || OffsetDateTime.parse(b).isAfter(OffsetDateTime.parse(a))) b else a

private fun SyncRecord.asNetworkRecord() = NetworkRecord(table, syncId, data, deleted, modifiedAt)

private fun NetworkRecord.asSyncRecord() = SyncRecord(table, id, if (deleted) null else data, modifiedAt)
