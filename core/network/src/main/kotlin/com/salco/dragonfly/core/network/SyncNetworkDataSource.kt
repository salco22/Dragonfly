package com.salco.dragonfly.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** A synced row as stored in the `sync_records` table (see supabase/migrations). */
@Serializable
data class NetworkRecord(
    @SerialName("table_name") val table: String,
    val id: String,
    /** Null for a deletion. */
    val data: JsonObject?,
    val deleted: Boolean,
    @SerialName("modified_at") val modifiedAt: Long,
    /** Set by the server; null when sending. */
    @SerialName("updated_at") val updatedAt: String? = null,
)

/** The signed-in user, or how far signing in has got. */
sealed interface NetworkAccount {
    data object Loading : NetworkAccount
    data object SignedOut : NetworkAccount
    data class SignedIn(val userId: String, val email: String?) : NetworkAccount
}

/** Why a call to the server failed, in terms the app can explain. */
class SyncNetworkException(val reason: Reason, cause: Throwable? = null) : Exception(reason.name, cause) {
    enum class Reason {
        /** No connection, or the server could not be reached. */
        Offline,

        /** The email code is wrong or expired. */
        InvalidCode,

        /** Too many codes requested; wait before asking again. */
        RateLimited,

        /** The session is no longer valid; sign in again. */
        SignedOut,

        /** Anything else the server refused. */
        Server,
    }
}

/** Sign in and exchange synced rows with the server. Failures are [SyncNetworkException]s. */
interface SyncNetworkDataSource {
    /** False when the build has no server address or key (see secrets.properties). */
    val isConfigured: Boolean

    val account: Flow<NetworkAccount>

    /** Emails a one-time code; a new account is created for an unknown address. */
    suspend fun sendCode(email: String)

    /** Signs in with the code received by email. */
    suspend fun verifyCode(email: String, code: String)

    suspend fun signOut()

    /** Stores [records] for the signed-in user; the server keeps the newest change of each row. */
    suspend fun push(records: List<NetworkRecord>)

    /** Records changed on the server at or after [since] (server time), oldest first, at most [limit]. */
    suspend fun pull(since: String?, limit: Int): List<NetworkRecord>
}
