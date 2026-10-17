package com.salco.dragonfly.feature.transactions.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.AccountsRepository
import com.salco.dragonfly.core.data.repository.CategoriesRepository
import com.salco.dragonfly.core.data.repository.TagsRepository
import com.salco.dragonfly.core.data.repository.TransactionsRepository
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.Category
import com.salco.dragonfly.core.model.Tag
import com.salco.dragonfly.core.model.Transaction
import com.salco.dragonfly.core.model.TransactionFilter
import com.salco.dragonfly.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel internal constructor(
    transactionsRepository: TransactionsRepository,
    accountsRepository: AccountsRepository,
    categoriesRepository: CategoriesRepository,
    tagsRepository: TagsRepository,
    clock: Clock,
) : ViewModel() {

    @Inject
    constructor(
        transactionsRepository: TransactionsRepository,
        accountsRepository: AccountsRepository,
        categoriesRepository: CategoriesRepository,
        tagsRepository: TagsRepository,
    ) : this(transactionsRepository, accountsRepository, categoriesRepository, tagsRepository, Clock.systemDefaultZone())

    private val month = MutableStateFlow(YearMonth.now(clock))

    /** Search criteria; dates are ignored here because they come from [month] when not searching. */
    private val criteria = MutableStateFlow(TransactionFilter())

    private val options = combine(
        accountsRepository.observeAccounts(),
        categoriesRepository.observeCategories(),
        tagsRepository.observeTags(),
    ) { accounts, categories, tags -> FilterOptions(accounts, categories, tags) }

    val uiState: StateFlow<TransactionsUiState> = combine(month, criteria, ::Pair)
        .flatMapLatest { (month, criteria) ->
            // Searching spans every date; browsing shows one month at a time.
            val filter = if (criteria.hasCriteria) criteria else TransactionFilter(from = month.atDay(1), to = month.atEndOfMonth())
            combine(transactionsRepository.searchTransactions(filter), options) { transactions, options ->
                transactions.toUiState(month, criteria, options)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState.Loading)

    fun previousMonth() = month.update { it.minusMonths(1) }
    fun nextMonth() = month.update { it.plusMonths(1) }

    // The search field reads this directly: going through the database-backed uiState would lag
    // behind fast typing and drop characters.
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    fun onQueryChange(query: String) {
        _query.value = query
        criteria.update { it.copy(query = query) }
    }
    fun onTypeFilter(type: TransactionType?) = criteria.update { it.copy(type = type) }
    fun onCategoryFilter(categoryId: Long?) = criteria.update { it.copy(categoryId = categoryId) }
    fun onTagFilter(tagId: Long?) = criteria.update { it.copy(tagId = tagId) }
    fun onAccountFilter(accountId: Long?) = criteria.update { it.copy(accountId = accountId) }
    fun clearFilters() {
        _query.value = ""
        criteria.value = TransactionFilter()
    }
}

private fun List<Transaction>.toUiState(
    month: YearMonth,
    criteria: TransactionFilter,
    options: FilterOptions,
): TransactionsUiState.Success {
    // Transfers move money between own accounts, so they count as neither income nor expenses.
    val totals = filter { it.type != TransactionType.TRANSFER }
        .groupBy { it.currencyCode }
        .mapValues { (_, list) ->
            MonthTotals(
                income = list.filter { it.type == TransactionType.DEPOSIT }.sumOf { it.amount },
                expenses = list.filter { it.type == TransactionType.WITHDRAWAL }.sumOf { it.amount },
            )
        }
    return TransactionsUiState.Success(
        month = month,
        days = groupBy { it.date }.map { (date, list) -> DaySection(date, list) },
        totals = totals,
        filter = criteria,
        options = options,
        resultCount = size,
    )
}

data class MonthTotals(val income: Long, val expenses: Long)

data class DaySection(val date: LocalDate, val transactions: List<Transaction>)

data class FilterOptions(
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val tags: List<Tag> = emptyList(),
)

sealed interface TransactionsUiState {
    data object Loading : TransactionsUiState
    data class Success(
        val month: YearMonth,
        /** Days with at least one transaction, newest first. */
        val days: List<DaySection>,
        /** Income and expenses per currency, in minor units, of the month or of the search results. */
        val totals: Map<String, MonthTotals>,
        val filter: TransactionFilter = TransactionFilter(),
        val options: FilterOptions = FilterOptions(),
        val resultCount: Int = days.sumOf { it.transactions.size },
    ) : TransactionsUiState {
        val isSearching: Boolean get() = filter.hasCriteria
    }
}
