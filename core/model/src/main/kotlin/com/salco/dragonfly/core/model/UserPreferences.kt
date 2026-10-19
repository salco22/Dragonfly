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
)
