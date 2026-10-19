package com.salco.dragonfly.core.data.repository

import com.salco.dragonfly.core.datastore.UserPreferencesDataSource
import com.salco.dragonfly.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface UserDataRepository {
    val userPreferences: Flow<UserPreferences>
    suspend fun setAppLockEnabled(enabled: Boolean)
    suspend fun setDefaultCurrencyCode(currencyCode: String)
    suspend fun setAutoBackupFolder(uri: String?)
    suspend fun setLastBackupAt(epochMillis: Long)
    suspend fun setRecurringNotificationsEnabled(enabled: Boolean)
    suspend fun setHideInRecents(hide: Boolean)
    suspend fun setSyncState(lastSyncAt: Long?, syncedUserId: String?)
    suspend fun setLoginSkipped(skipped: Boolean)
}

internal class OfflineUserDataRepository @Inject constructor(
    private val dataSource: UserPreferencesDataSource,
) : UserDataRepository {
    override val userPreferences: Flow<UserPreferences> = dataSource.userPreferences
    override suspend fun setAppLockEnabled(enabled: Boolean) = dataSource.setAppLockEnabled(enabled)
    override suspend fun setDefaultCurrencyCode(currencyCode: String) =
        dataSource.setDefaultCurrencyCode(currencyCode)
    override suspend fun setAutoBackupFolder(uri: String?) = dataSource.setAutoBackupFolder(uri)
    override suspend fun setLastBackupAt(epochMillis: Long) = dataSource.setLastBackupAt(epochMillis)
    override suspend fun setRecurringNotificationsEnabled(enabled: Boolean) = dataSource.setRecurringNotificationsEnabled(enabled)
    override suspend fun setHideInRecents(hide: Boolean) = dataSource.setHideInRecents(hide)
    override suspend fun setSyncState(lastSyncAt: Long?, syncedUserId: String?) = dataSource.setSyncState(lastSyncAt, syncedUserId)
    override suspend fun setLoginSkipped(skipped: Boolean) = dataSource.setLoginSkipped(skipped)
}
