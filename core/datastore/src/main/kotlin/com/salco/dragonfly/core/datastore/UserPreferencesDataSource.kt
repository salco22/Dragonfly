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

    private companion object {
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val AUTO_BACKUP_FOLDER = stringPreferencesKey("auto_backup_folder")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
    }
}
