package com.salco.dragonfly.feature.sync

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

/** What the account picker returned. */
internal sealed interface GoogleSignInResult {
    /** [rawNonce] goes to Supabase, which checks it against the SHA-256 inside the token. */
    data class Token(val idToken: String, val rawNonce: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data object NoAccount : GoogleSignInResult
    data object Failed : GoogleSignInResult
}

/** Shows the system's Google account picker and returns an ID token for [clientId]. */
internal suspend fun requestGoogleIdToken(activityContext: Context, clientId: String): GoogleSignInResult {
    val rawNonce = UUID.randomUUID().toString()
    val hashedNonce = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray()).joinToString("") { "%02x".format(it) }
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(serverClientId = clientId).setNonce(hashedNonce).build())
        .build()
    return try {
        val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            GoogleSignInResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken, rawNonce)
        } else {
            Log.w(TAG, "Unexpected credential type ${credential.type}")
            GoogleSignInResult.Failed
        }
    } catch (e: GetCredentialCancellationException) {
        GoogleSignInResult.Cancelled
    } catch (e: NoCredentialException) {
        GoogleSignInResult.NoAccount
    } catch (e: GetCredentialException) {
        // A setup problem (e.g. "[28444] Developer console is not set up correctly") shows up here.
        Log.w(TAG, "Google sign-in failed: ${e.type} ${e.errorMessage}", e)
        GoogleSignInResult.Failed
    }
}

private const val TAG = "GoogleSignIn"
