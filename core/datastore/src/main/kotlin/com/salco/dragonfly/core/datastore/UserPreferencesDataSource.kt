package com.salco.dragonfly.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.salco.dragonfly.core.model.SupportedCurrencyCodes
import com.salco.dragonfly.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val userPreferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            appLockEnabled = prefs[APP_LOCK_ENABLED] ?: false,
            defaultCurrencyCode = prefs[DEFAULT_CURRENCY] ?: SupportedCurrencyCodes.first(),
            autoBackupFolder = prefs[AUTO_BACKUP_FOLDER],
            lastBackupAt = prefs[LAST_BACKUP_AT],
            recurringNotificationsEnabled = prefs[RECURRING_NOTIFICATIONS] ?: true,
            hideInRecents = prefs[HIDE_IN_RECENTS] ?: true,
            lastSyncAt = prefs[LAST_SYNC_AT],
            syncedUserId = prefs[SYNCED_USER_ID],
            loginSkipped = prefs[LOGIN_SKIPPED] ?: false,
        )
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { it[APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setDefaultCurrencyCode(currencyCode: String) {
        dataStore.edit { it[DEFAULT_CURRENCY] = currencyCode }
    }

    suspend fun setAutoBackupFolder(uri: String?) {
        dataStore.edit { if (uri == null) it.remove(AUTO_BACKUP_FOLDER) else it[AUTO_BACKUP_FOLDER] = uri }
    }

    suspend fun setLastBackupAt(epochMillis: Long) {
        dataStore.edit { it[LAST_BACKUP_AT] = epochMillis }
    }

    suspend fun setRecurringNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[RECURRING_NOTIFICATIONS] = enabled }
    }

    suspend fun setHideInRecents(hide: Boolean) {
        dataStore.edit { it[HIDE_IN_RECENTS] = hide }
    }

    suspend fun setSyncState(lastSyncAt: Long?, syncedUserId: String?) {
        dataStore.edit { prefs ->
            if (lastSyncAt == null) prefs.remove(LAST_SYNC_AT) else prefs[LAST_SYNC_AT] = lastSyncAt
            if (syncedUserId == null) prefs.remove(SYNCED_USER_ID) else prefs[SYNCED_USER_ID] = syncedUserId
        }
    }

    suspend fun setLoginSkipped(skipped: Boolean) {
        dataStore.edit { it[LOGIN_SKIPPED] = skipped }
    }

    private companion object {
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val AUTO_BACKUP_FOLDER = stringPreferencesKey("auto_backup_folder")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val RECURRING_NOTIFICATIONS = booleanPreferencesKey("recurring_notifications")
        val HIDE_IN_RECENTS = booleanPreferencesKey("hide_in_recents")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val SYNCED_USER_ID = stringPreferencesKey("synced_user_id")
        val LOGIN_SKIPPED = booleanPreferencesKey("login_skipped")
    }
}
