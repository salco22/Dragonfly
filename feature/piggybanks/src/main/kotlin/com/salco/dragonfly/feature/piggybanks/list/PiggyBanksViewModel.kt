package com.salco.dragonfly.feature.piggybanks.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.PiggyBanksRepository
import com.salco.dragonfly.core.model.PiggyBankSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PiggyBanksViewModel internal constructor(
    repository: PiggyBanksRepository,
    clock: Clock,
) : ViewModel() {

    @Inject
    constructor(repository: PiggyBanksRepository) : this(repository, Clock.systemDefaultZone())

    val uiState: StateFlow<PiggyBanksUiState> = repository.observePiggyBanks()
        .map { list ->
            PiggyBanksUiState.Success(
                items = list,
                totalSaved = list.groupBy { it.currencyCode }.mapValues { (_, l) -> l.sumOf { it.saved } },
                today = LocalDate.now(clock),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PiggyBanksUiState.Loading)
}

sealed interface PiggyBanksUiState {
    data object Loading : PiggyBanksUiState
    data class Success(
        val items: List<PiggyBankSummary>,
        /** Money saved in all piggy banks, per currency. */
        val totalSaved: Map<String, Long>,
        val today: LocalDate,
    ) : PiggyBanksUiState
}
