package com.salco.dragonfly.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
        )
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { it[APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setDefaultCurrencyCode(currencyCode: String) {
        dataStore.edit { it[DEFAULT_CURRENCY] = currencyCode }
    }

    private companion object {
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
    }
}
