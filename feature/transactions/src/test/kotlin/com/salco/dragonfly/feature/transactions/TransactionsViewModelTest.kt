package com.salco.dragonfly.feature.transactions

import app.cash.turbine.test
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.testing.FakeAccountsRepository
import com.salco.dragonfly.core.testing.FakeCategoriesRepository
import com.salco.dragonfly.core.testing.FakeTagsRepository
import com.salco.dragonfly.core.testing.FakeTransactionsRepository
import com.salco.dragonfly.core.testing.MainDispatcherRule
import com.salco.dragonfly.feature.transactions.list.MonthTotals
import com.salco.dragonfly.feature.transactions.list.TransactionsUiState
import com.salco.dragonfly.feature.transactions.list.TransactionsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class TransactionsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accounts = FakeAccountsRepository()
    private val categories = FakeCategoriesRepository()
    private val tags = FakeTagsRepository()
    private val transactions = FakeTransactionsRepository(accounts, categories, tags)
    private val clock = Clock.fixed(LocalDate.of(2026, 10, 17).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

    private suspend fun add(
        type: TransactionType,
        date: LocalDate,
        amount: Long,
        counterparty: String,
        category: String? = null,
        tagNames: List<String> = emptyList(),
    ) {
        val own = AccountRef.Existing(1)
        val other = AccountRef.New(counterparty, if (type == TransactionType.DEPOSIT) AccountType.REVENUE else AccountType.EXPENSE, "EUR")
        transactions.saveTransaction(
            TransactionDraft(
                type = type,
                description = counterparty,
                date = date,
                amount = amount,
                currencyCode = "EUR",
                source = if (type == TransactionType.DEPOSIT) other else own,
                destination = if (type == TransactionType.DEPOSIT) own else other,
                categoryName = category,
                tagNames = tagNames,
            ),
        )
    }

    @Test
    fun groupsCurrentMonthByDayAndSumsTotals() = runTest {
        accounts.saveAccount(Account(name = "Conto", type = AccountType.ASSET, currencyCode = "EUR"))
        add(TransactionType.WITHDRAWAL, LocalDate.of(2026, 10, 3), 4_000, "Spesa")
        add(TransactionType.WITHDRAWAL, LocalDate.of(2026, 10, 3), 1_000, "Bar")
        add(TransactionType.DEPOSIT, LocalDate.of(2026, 10, 10), 200_000, "Stipendio")
        add(TransactionType.WITHDRAWAL, LocalDate.of(2026, 9, 30), 9_999, "Settembre")

        TransactionsViewModel(transactions, accounts, categories, tags, clock).uiState.test {
            val state = awaitItem() as TransactionsUiState.Success
            assertEquals(YearMonth.of(2026, 10), state.month)
            assertEquals(listOf(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 3)), state.days.map { it.date })
            assertEquals(2, state.days[1].transactions.size)
            assertEquals(mapOf("EUR" to MonthTotals(income = 200_000, expenses = 5_000)), state.totals)
        }
    }

    @Test
    fun previousMonth_showsOlderTransactions() = runTest {
        accounts.saveAccount(Account(name = "Conto", type = AccountType.ASSET, currencyCode = "EUR"))
        add(TransactionType.WITHDRAWAL, LocalDate.of(2026, 9, 30), 9_999, "Settembre")
        val viewModel = TransactionsViewModel(transactions, accounts, categories, tags, clock)

        viewModel.uiState.test {
            assertEquals(0, (awaitItem() as TransactionsUiState.Success).days.size)
            viewModel.previousMonth()
            val september = awaitItem() as TransactionsUiState.Success
            assertEquals(YearMonth.of(2026, 9), september.month)
            assertEquals(9_999L, september.totals.getValue("EUR").expenses)
        }
    }

    @Test
    fun search_spansAllMonthsAndCombinesFilters() = runTest {
        accounts.saveAccount(Account(name = "Conto", type = AccountType.ASSET, currencyCode = "EUR"))
        add(TransactionType.WITHDRAWAL, LocalDate.of(2026, 10, 3), 4_000, "Supermercato", category = "Spesa", tagNames = listOf("Casa"))
        add(TransactionType.WITHDRAWAL, LocalDate.of(2025, 1, 15), 2_500, "Supermercato", category = "Spesa")
        add(TransactionType.WITHDRAWAL, LocalDate.of(2026, 10, 4), 900, "Bar", category = "Svago", tagNames = listOf("Casa"))
        add(TransactionType.DEPOSIT, LocalDate.of(2026, 10, 10), 200_000, "Stipendio")
        val viewModel = TransactionsViewModel(transactions, accounts, categories, tags, clock)

        viewModel.uiState.test {
            awaitItem()
            viewModel.onQueryChange("supermerc")
            with(awaitItem() as TransactionsUiState.Success) {
                assertEquals(true, isSearching)
                assertEquals(2, resultCount)
                assertEquals(6_500L, totals.getValue("EUR").expenses)
            }

            viewModel.onQueryChange("")
            awaitItem()
            viewModel.onTagFilter(tags.tags.value.single { it.name == "Casa" }.id)
            assertEquals(listOf("Bar", "Supermercato"), (awaitItem() as TransactionsUiState.Success).allDescriptions())

            viewModel.onCategoryFilter(categories.categories.value.single { it.name == "Spesa" }.id)
            assertEquals(listOf("Supermercato"), (awaitItem() as TransactionsUiState.Success).allDescriptions())

            viewModel.clearFilters()
            with(awaitItem() as TransactionsUiState.Success) {
                assertEquals(false, isSearching)
                assertEquals(3, resultCount)
            }
        }
    }

    private fun TransactionsUiState.Success.allDescriptions() = days.flatMap { it.transactions }.map { it.description }
}
