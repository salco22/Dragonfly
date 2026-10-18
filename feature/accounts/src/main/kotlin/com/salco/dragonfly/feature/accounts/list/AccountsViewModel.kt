package com.salco.dragonfly.feature.accounts.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.AccountsRepository
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.feature.accounts.hasBalance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    accountsRepository: AccountsRepository,
) : ViewModel() {

    val uiState: StateFlow<AccountsUiState> = accountsRepository.observeAccounts()
        .map(::toUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState.Loading)

    private fun toUiState(accounts: List<Account>): AccountsUiState {
        if (accounts.isEmpty()) return AccountsUiState.Empty
        val netWorth = accounts
            .filter { it.active && it.includeInNetWorth && it.type.hasBalance }
            .groupBy { it.currencyCode }
            .mapValues { (_, list) -> list.sumOf { it.balance } }
        val sections = AccountType.entries.mapNotNull { type ->
            accounts.filter { it.type == type }.takeIf { it.isNotEmpty() }?.let { type to it }
        }.toMap()
        return AccountsUiState.Success(netWorthByCurrency = netWorth, sections = sections)
    }
}

sealed interface AccountsUiState {
    data object Loading : AccountsUiState
    data object Empty : AccountsUiState
    data class Success(
        /** Net worth per currency, in minor units. */
        val netWorthByCurrency: Map<String, Long>,
        /** Accounts grouped by type, in [AccountType] order. */
        val sections: Map<AccountType, List<Account>>,
    ) : AccountsUiState
}
