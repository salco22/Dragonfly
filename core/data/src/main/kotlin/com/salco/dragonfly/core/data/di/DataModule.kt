package com.salco.dragonfly.core.data.di

import com.salco.dragonfly.core.data.repository.AccountsRepository
import com.salco.dragonfly.core.data.repository.BillsRepository
import com.salco.dragonfly.core.data.repository.BudgetsRepository
import com.salco.dragonfly.core.data.repository.OfflineBillsRepository
import com.salco.dragonfly.core.data.repository.OfflineBudgetsRepository
import com.salco.dragonfly.core.data.repository.CategoriesRepository
import com.salco.dragonfly.core.data.repository.OfflineCategoriesRepository
import com.salco.dragonfly.core.data.repository.InsightsRepository
import com.salco.dragonfly.core.data.repository.OfflineInsightsRepository
import com.salco.dragonfly.core.data.repository.OfflinePiggyBanksRepository
import com.salco.dragonfly.core.data.repository.OfflineRecurringTransactionsRepository
import com.salco.dragonfly.core.data.repository.PiggyBanksRepository
import com.salco.dragonfly.core.data.repository.OfflineTagsRepository
import com.salco.dragonfly.core.data.repository.RecurringTransactionsRepository
import com.salco.dragonfly.core.data.repository.TagsRepository
import com.salco.dragonfly.core.data.repository.OfflineAccountsRepository
import com.salco.dragonfly.core.data.repository.OfflineTransactionsRepository
import com.salco.dragonfly.core.data.repository.OfflineUserDataRepository
import com.salco.dragonfly.core.data.repository.TransactionsRepository
import com.salco.dragonfly.core.data.repository.UserDataRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    abstract fun bindsAccountsRepository(impl: OfflineAccountsRepository): AccountsRepository

    @Binds
    abstract fun bindsTransactionsRepository(impl: OfflineTransactionsRepository): TransactionsRepository

    @Binds
    abstract fun bindsBillsRepository(impl: OfflineBillsRepository): BillsRepository

    @Binds
    abstract fun bindsBudgetsRepository(impl: OfflineBudgetsRepository): BudgetsRepository

    @Binds
    abstract fun bindsCategoriesRepository(impl: OfflineCategoriesRepository): CategoriesRepository

    @Binds
    abstract fun bindsInsightsRepository(impl: OfflineInsightsRepository): InsightsRepository

    @Binds
    abstract fun bindsPiggyBanksRepository(impl: OfflinePiggyBanksRepository): PiggyBanksRepository

    @Binds
    abstract fun bindsRecurringTransactionsRepository(
        impl: OfflineRecurringTransactionsRepository,
    ): RecurringTransactionsRepository

    @Binds
    abstract fun bindsTagsRepository(impl: OfflineTagsRepository): TagsRepository

    @Binds
    abstract fun bindsUserDataRepository(impl: OfflineUserDataRepository): UserDataRepository
}
