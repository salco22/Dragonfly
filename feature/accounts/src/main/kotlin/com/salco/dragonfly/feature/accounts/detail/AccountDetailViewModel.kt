package com.salco.dragonfly.feature.accounts.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.AccountsRepository
import com.salco.dragonfly.core.data.repository.TransactionsRepository
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.Transaction
import com.salco.dragonfly.core.model.TransactionFilter
import com.salco.dragonfly.feature.accounts.navigation.AccountDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    accountsRepository: AccountsRepository,
    transactionsRepository: TransactionsRepository,
) : ViewModel() {

    // Read the argument by name so the ViewModel stays testable without Android's Bundle.
    val accountId: Long = savedStateHandle[AccountDetailRoute::accountId.name] ?: 0L

    /** Read directly by the search field so fast typing never waits for the database. */
    val query = MutableStateFlow("")

    val uiState: StateFlow<AccountDetailUiState> = query
        .flatMapLatest { query ->
            combine(
                accountsRepository.observeAccount(accountId),
                transactionsRepository.searchTransactions(TransactionFilter(query = query, accountId = accountId)),
            ) { account, transactions ->
                if (account == null) {
                    AccountDetailUiState.NotFound
                } else {
                    AccountDetailUiState.Success(
                        account = account,
                        query = query,
                        days = transactions.groupBy { it.date }.toList(),
                        totals = transactions.totalsFor(account),
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountDetailUiState.Loading)

    fun onQueryChange(value: String) {
        query.value = value
    }
}

/** Money in and out of [account] across [this], in the account's currency. */
private fun List<Transaction>.totalsFor(account: Account): FlowTotals {
    var incoming = 0L
    var outgoing = 0L
    forEach { t ->
        if (t.destination.id == account.id) incoming += t.foreignAmount ?: t.amount
        if (t.source.id == account.id) outgoing += t.amount
    }
    return FlowTotals(incoming = incoming, outgoing = outgoing)
}

data class FlowTotals(val incoming: Long, val outgoing: Long)

sealed interface AccountDetailUiState {
    data object Loading : AccountDetailUiState

    /** The account was deleted while on screen. */
    data object NotFound : AccountDetailUiState

    data class Success(
        val account: Account,
        val query: String,
        val days: List<Pair<LocalDate, List<Transaction>>>,
        val totals: FlowTotals,
    ) : AccountDetailUiState
}
