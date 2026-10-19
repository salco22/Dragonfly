package com.salco.dragonfly.core.model

data class UserPreferences(
    val appLockEnabled: Boolean,
    val defaultCurrencyCode: String,
    /** Folder (a document tree URI) where automatic backups are written; null when they are off. */
    val autoBackupFolder: String? = null,
    /** When the last backup was written, in epoch milliseconds. */
    val lastBackupAt: Long? = null,
    /** Notify when recurring transactions are created in the background. */
    val recurringNotificationsEnabled: Boolean = true,
    /** Keep the app's content out of the recent apps preview. */
    val hideInRecents: Boolean = true,
    /** End of the last successful sync, in epoch milliseconds. */
    val lastSyncAt: Long? = null,
    /** Account the local data was last synced with; another account starts from scratch. */
    val syncedUserId: String? = null,
    /** The user chose to use the app without an account; the login screen is not shown again. */
    val loginSkipped: Boolean = false,
)
