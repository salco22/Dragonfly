package com.salco.dragonfly.core.model

/** The account used to sync data between devices. */
sealed interface SyncAccount {
    /** This build has no server to sync with. */
    data object NotConfigured : SyncAccount
    data object Loading : SyncAccount
    data object SignedOut : SyncAccount
    data class SignedIn(val email: String?) : SyncAccount
}

/** Why syncing or signing in failed. */
enum class SyncError {
    Offline,
    InvalidCode,
    RateLimited,
    SignedOut,
    Server,
}

class SyncException(val error: SyncError, cause: Throwable? = null) : Exception(error.name, cause)

data class SyncStatus(
    val running: Boolean = false,
    /** Local changes not sent yet. */
    val pendingChanges: Int = 0,
    /** End of the last successful sync, in epoch milliseconds. */
    val lastSyncAt: Long? = null,
    /** Error of the last attempt, cleared by the next successful one. */
    val lastError: SyncError? = null,
)

/** What one sync did. */
data class SyncResult(val sent: Int, val received: Int)
