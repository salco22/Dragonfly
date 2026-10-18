package com.salco.dragonfly.feature.lock

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

// BIOMETRIC_WEAK | DEVICE_CREDENTIAL is the only combination supported from API 26 up,
// and lets users without enrolled biometrics fall back to the device PIN or pattern.
private const val ALLOWED_AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

/** True when the device has a biometric or a screen lock that can protect the app. */
fun isAppLockAvailable(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(ALLOWED_AUTHENTICATORS) ==
        BiometricManager.BIOMETRIC_SUCCESS

fun showAppLockPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: (CharSequence) -> Unit = {},
) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString)
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(activity.getString(R.string.feature_lock_prompt_title))
        .setSubtitle(activity.getString(R.string.feature_lock_prompt_subtitle))
        .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
        .build()
    prompt.authenticate(info)
}
