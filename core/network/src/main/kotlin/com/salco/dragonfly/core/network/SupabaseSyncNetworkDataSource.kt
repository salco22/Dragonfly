package com.salco.dragonfly.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SupabaseSyncNetworkDataSource @Inject constructor() : SyncNetworkDataSource {

    override val isConfigured: Boolean = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank()

    private val client: SupabaseClient by lazy {
        check(isConfigured) { "Supabase is not configured: add SUPABASE_URL and SUPABASE_KEY to secrets.properties" }
        createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY) {
            install(Auth)
            install(Postgrest)
        }
    }

    override val account: Flow<NetworkAccount> =
        if (!isConfigured) {
            flowOf(NetworkAccount.SignedOut)
        } else {
            client.auth.sessionStatus.map { status ->
                when (status) {
                    is SessionStatus.Authenticated -> status.session.user
                        ?.let { NetworkAccount.SignedIn(it.id, it.email) }
                        ?: NetworkAccount.SignedOut
                    SessionStatus.Initializing -> NetworkAccount.Loading
                    else -> NetworkAccount.SignedOut
                }
            }
        }

    override suspend fun sendCode(email: String) = translatingErrors {
        client.auth.signInWith(OTP) {
            this.email = email
            createUser = true
        }
    }

    override suspend fun verifyCode(email: String, code: String) {
        translatingErrors(invalidInput = SyncNetworkException.Reason.InvalidCode) {
            client.auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = email, token = code)
        }
    }

    // Signing out locally must work offline too.
    override suspend fun signOut() = client.auth.signOut(SignOutScope.LOCAL)

    override suspend fun push(records: List<NetworkRecord>) = translatingErrors {
        if (records.isEmpty()) return@translatingErrors
        val userId = client.auth.currentUserOrNull()?.id ?: throw SyncNetworkException(SyncNetworkException.Reason.SignedOut)
        records.chunked(PUSH_BATCH).forEach { batch ->
            client.from(TABLE).upsert(batch.map { it.toRow(userId) }) {
                onConflict = "user_id,table_name,id"
            }
        }
    }

    override suspend fun pull(since: String?, limit: Int): List<NetworkRecord> = translatingErrors {
        client.from(TABLE).select {
            if (since != null) filter { gte("updated_at", since) }
            order("updated_at", Order.ASCENDING)
            limit(limit.toLong())
        }.decodeList<NetworkRecord>()
    }

    private suspend fun <T> translatingErrors(
        invalidInput: SyncNetworkException.Reason = SyncNetworkException.Reason.Server,
        block: suspend () -> T,
    ): T = try {
        block()
    } catch (e: SyncNetworkException) {
        throw e
    } catch (e: HttpRequestException) {
        throw SyncNetworkException(SyncNetworkException.Reason.Offline, e)
    } catch (e: IOException) {
        throw SyncNetworkException(SyncNetworkException.Reason.Offline, e)
    } catch (e: RestException) {
        val reason = when (e.statusCode) {
            401 -> SyncNetworkException.Reason.SignedOut
            429 -> SyncNetworkException.Reason.RateLimited
            400, 403, 422 -> invalidInput
            else -> SyncNetworkException.Reason.Server
        }
        throw SyncNetworkException(reason, e)
    }

    private companion object {
        const val TABLE = "sync_records"
        const val PUSH_BATCH = 500
    }
}

@Serializable
private data class Row(
    @SerialName("user_id") val userId: String,
    @SerialName("table_name") val table: String,
    val id: String,
    val data: JsonObject?,
    val deleted: Boolean,
    @SerialName("modified_at") val modifiedAt: Long,
)

private fun NetworkRecord.toRow(userId: String) = Row(userId, table, id, data, deleted, modifiedAt)
