package com.salco.dragonfly.core.work

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.formatMoney

/** Tells the user which recurring transactions were created while the app was in the background. */
internal class RecurringNotifier(private val context: Context) {

    fun notify(created: List<TransactionDraft>) {
        if (created.isEmpty() || isAppInForeground() || !canPostNotifications(context)) return
        createChannel()

        val title = context.resources.getQuantityString(R.plurals.core_work_recurring_title, created.size, created.size)
        val lines = recurringNotificationLines(created)
        val style = NotificationCompat.InboxStyle().setBigContentTitle(title)
        lines.take(MAX_LINES).forEach(style::addLine)
        if (lines.size > MAX_LINES) style.setSummaryText(context.getString(R.string.core_work_recurring_more, lines.size - MAX_LINES))

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_recurring)
            .setContentTitle(title)
            .setContentText(lines.joinToString(", "))
            .setStyle(style)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            // Amounts stay hidden on the lock screen, like the rest of the app behind its lock.
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_recurring)
                    .setContentTitle(title)
                    .build(),
            )
            .apply {
                if (launch != null) {
                    setContentIntent(PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                }
            }
            .build()
        @Suppress("MissingPermission") // Checked by canPostNotifications.
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.core_work_recurring_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.core_work_recurring_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    // Opening the app already shows the new transactions, so a notification would only repeat them.
    private fun isAppInForeground(): Boolean {
        val state = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(state)
        return state.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    private companion object {
        const val CHANNEL_ID = "recurring_transactions"
        const val NOTIFICATION_ID = 1001
        const val MAX_LINES = 5
    }
}

/** True when the app may show notifications: always before Android 13, then only once granted. */
internal fun canPostNotifications(context: Context): Boolean =
    (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled()

/** "Affitto · −€800.00", one line per created transaction, newest last. */
internal fun recurringNotificationLines(created: List<TransactionDraft>): List<String> =
    created.sortedBy { it.date }.map { draft ->
        val sign = when (draft.type) {
            TransactionType.WITHDRAWAL -> "−"
            TransactionType.DEPOSIT -> "+"
            TransactionType.TRANSFER -> ""
        }
        "${draft.description} · $sign${formatMoney(draft.amount, draft.currencyCode)}"
    }
