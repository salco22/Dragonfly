package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.datastore.UserPreferencesDataSource
import com.salco.dragonfly.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface UserDataRepository {
    val userPreferences: Flow<UserPreferences>
    suspend fun setAppLockEnabled(enabled: Boolean)
    suspend fun setDefaultCurrencyCode(currencyCode: String)
}

internal class OfflineUserDataRepository @Inject constructor(
    private val dataSource: UserPreferencesDataSource,
) : UserDataRepository {
    override val userPreferences: Flow<UserPreferences> = dataSource.userPreferences
    override suspend fun setAppLockEnabled(enabled: Boolean) = dataSource.setAppLockEnabled(enabled)
    override suspend fun setDefaultCurrencyCode(currencyCode: String) =
        dataSource.setDefaultCurrencyCode(currencyCode)
}
