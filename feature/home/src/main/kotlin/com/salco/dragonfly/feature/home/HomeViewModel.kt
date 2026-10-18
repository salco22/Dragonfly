package com.salco.dragonfly.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.AccountsRepository
import com.salco.dragonfly.core.data.repository.BillsRepository
import com.salco.dragonfly.core.data.repository.BudgetsRepository
import com.salco.dragonfly.core.data.repository.InsightsRepository
import com.salco.dragonfly.core.data.repository.PiggyBanksRepository
import com.salco.dragonfly.core.data.repository.UserDataRepository
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.Bill
import com.salco.dragonfly.core.model.BillStatus
import com.salco.dragonfly.core.model.BudgetSpending
import com.salco.dragonfly.core.model.CategorySpending
import com.salco.dragonfly.core.model.MonthlySummary
import com.salco.dragonfly.core.model.NetWorthPoint
import com.salco.dragonfly.core.model.PiggyBankSummary
import com.salco.dragonfly.core.model.statusOn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** Months shown in the net worth chart and in the income/expenses chart. */
internal const val NET_WORTH_MONTHS = 12
internal const val CASH_FLOW_MONTHS = 6

/** Bills shown on the dashboard: overdue ones and those due within this many days. */
internal const val BILLS_HORIZON_DAYS = 14L

@HiltViewModel
class HomeViewModel internal constructor(
    userDataRepository: UserDataRepository,
    accountsRepository: AccountsRepository,
    private val insightsRepository: InsightsRepository,
    private val budgetsRepository: BudgetsRepository,
    billsRepository: BillsRepository,
    piggyBanksRepository: PiggyBanksRepository,
    private val clock: Clock,
) : ViewModel() {

    @Inject
    constructor(
        userDataRepository: UserDataRepository,
        accountsRepository: AccountsRepository,
        insightsRepository: InsightsRepository,
        budgetsRepository: BudgetsRepository,
        billsRepository: BillsRepository,
        piggyBanksRepository: PiggyBanksRepository,
    ) : this(
        userDataRepository, accountsRepository, insightsRepository, budgetsRepository, billsRepository,
        piggyBanksRepository, Clock.systemDefaultZone(),
    )

    private val today get() = LocalDate.now(clock)

    private val cashFlow: Flow<CashFlow> = run {
        val month = YearMonth.now(clock)
        combine(
            insightsRepository.observeNetWorthHistory(month.minusMonths(NET_WORTH_MONTHS - 1L), month),
            insightsRepository.observeMonthlySummaries(month.minusMonths(CASH_FLOW_MONTHS - 1L), month),
            insightsRepository.observeCategorySpending(month),
        ) { netWorth, summaries, categories -> CashFlow(netWorth, summaries, categories) }
    }

    private val planning: Flow<Planning> = run {
        val month = YearMonth.now(clock)
        combine(
            budgetsRepository.observeBudgetSpending(month.atDay(1), month.atEndOfMonth()),
            billsRepository.observeBills(),
            billsRepository.observePayments(),
            piggyBanksRepository.observePiggyBanks(),
        ) { budgets, bills, payments, piggyBanks ->
            val horizon = today.plusDays(BILLS_HORIZON_DAYS)
            Planning(
                budgets = budgets.filter { it.budget.active },
                bills = bills.map { it to it.statusOn(today, payments) }.filter { (_, status) ->
                    status is BillStatus.Overdue || (status is BillStatus.Due && !status.dueDate.isAfter(horizon))
                },
                piggyBanks = piggyBanks,
            )
        }
    }

    val uiState: StateFlow<HomeUiState> = userDataRepository.userPreferences
        .map { it.defaultCurrencyCode }
        .flatMapLatest { currency ->
            combine(accountsRepository.observeAccounts(), cashFlow, planning) { accounts, cash, plan ->
                val month = YearMonth.now(clock)
                val own = accounts.filter { it.type == AccountType.ASSET || it.type == AccountType.LIABILITY }
                HomeUiState.Success(
                    currencyCode = currency,
                    month = month,
                    hasAccounts = own.isNotEmpty(),
                    netWorth = own.filter { it.active && it.includeInNetWorth }
                        .groupBy { it.currencyCode }.mapValues { (_, l) -> l.sumOf { it.balance } },
                    netWorthHistory = cash.netWorth.filter { it.currencyCode == currency },
                    cashFlow = (CASH_FLOW_MONTHS - 1 downTo 0).map { back ->
                        val m = month.minusMonths(back.toLong())
                        cash.summaries.firstOrNull { it.month == m && it.currencyCode == currency }
                            ?: MonthlySummary(m, currency, 0, 0)
                    },
                    categorySpending = cash.categories.filter { it.currencyCode == currency },
                    budgets = plan.budgets.sortedByDescending { it.progress }.take(3),
                    bills = plan.bills.sortedBy { (_, status) -> (status as? BillStatus.Overdue)?.dueDate ?: (status as BillStatus.Due).dueDate },
                    piggyBanks = plan.piggyBanks,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    private data class CashFlow(
        val netWorth: List<NetWorthPoint>,
        val summaries: List<MonthlySummary>,
        val categories: List<CategorySpending>,
    )

    private data class Planning(
        val budgets: List<BudgetSpending>,
        val bills: List<Pair<Bill, BillStatus>>,
        val piggyBanks: List<PiggyBankSummary>,
    )
}

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        /** Default currency: charts show amounts in this currency only. */
        val currencyCode: String,
        val month: YearMonth,
        val hasAccounts: Boolean,
        /** Current net worth per currency. */
        val netWorth: Map<String, Long>,
        /** Net worth at the end of each of the last months, in [currencyCode]. */
        val netWorthHistory: List<NetWorthPoint>,
        /** Income and expenses of the last months, oldest first, in [currencyCode]. */
        val cashFlow: List<MonthlySummary>,
        /** Spending of the current month per category, largest first, in [currencyCode]. */
        val categorySpending: List<CategorySpending>,
        /** The most used active budgets of the month. */
        val budgets: List<BudgetSpending>,
        /** Overdue bills and bills due soon. */
        val bills: List<Pair<Bill, BillStatus>>,
        val piggyBanks: List<PiggyBankSummary>,
    ) : HomeUiState {
        val thisMonth: MonthlySummary get() = cashFlow.last()
    }
}
