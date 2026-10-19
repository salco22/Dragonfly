package com.salco.dragonfly.core.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Whether the app may post notifications, and how to ask for it. */
@Stable
class NotificationPermissionState internal constructor(
    granted: Boolean,
    private val onRequest: () -> Unit,
) {
    var granted by mutableStateOf(granted)
        internal set

    /**
     * Shows the system prompt (Android 13+). After the user said no once, or when notifications
     * are turned off for the app, opens the app's notification settings instead.
     */
    fun request() = onRequest()
}

@Composable
fun rememberNotificationPermission(onResult: (Boolean) -> Unit = {}): NotificationPermissionState {
    val context = LocalContext.current
    var denied by rememberSaveable { mutableStateOf(false) }
    lateinit var state: NotificationPermissionState
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        state.granted = notificationsAllowed(context)
        denied = !granted
        onResult(state.granted)
    }
    state = remember {
        NotificationPermissionState(notificationsAllowed(context)) {
            val canPrompt = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !denied && !hasPermission(context)
            if (canPrompt) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
    // The user may change it in the system settings and come back.
    LifecycleResumeEffect(state) {
        state.granted = notificationsAllowed(context)
        onPauseOrDispose { }
    }
    return state
}

private fun hasPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun notificationsAllowed(context: Context): Boolean =
    hasPermission(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()
