package com.salco.dragonfly.feature.transactions

import androidx.lifecycle.SavedStateHandle
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.testing.FakeAccountsRepository
import com.salco.dragonfly.core.testing.FakeCategoriesRepository
import com.salco.dragonfly.core.testing.FakeTagsRepository
import com.salco.dragonfly.core.testing.FakeTransactionsRepository
import com.salco.dragonfly.core.testing.MainDispatcherRule
import com.salco.dragonfly.feature.transactions.edit.TransactionEditViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

class TransactionEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accounts = FakeAccountsRepository()
    private val categories = FakeCategoriesRepository()
    private val tags = FakeTagsRepository()
    private val transactions = FakeTransactionsRepository(accounts, categories, tags)
    private val clock = Clock.fixed(LocalDate.of(2026, 10, 17).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private var checkingId = 0L
    private var savingsUsdId = 0L
    private var shopId = 0L

    @Before
    fun setUp() = runTest {
        checkingId = accounts.saveAccount(Account(name = "Conto", type = AccountType.ASSET, currencyCode = "EUR"))
        savingsUsdId = accounts.saveAccount(Account(name = "Dollari", type = AccountType.ASSET, currencyCode = "USD"))
        shopId = accounts.saveAccount(Account(name = "Supermercato", type = AccountType.EXPENSE, currencyCode = "EUR"))
    }

    private fun viewModel(id: Long = 0L) = TransactionEditViewModel(
        SavedStateHandle(mapOf("transactionId" to id)), transactions, accounts, categories, tags, clock,
    )

    @Test
    fun newTransaction_defaultsToTodayAndFirstAsset() = runTest {
        val state = viewModel().uiState.value
        assertEquals(LocalDate.of(2026, 10, 17), state.date)
        assertEquals(checkingId, state.accountId)
        assertEquals(listOf("Supermercato"), state.counterpartySuggestions)
        assertFalse(state.ownAccounts.any { it.type == AccountType.EXPENSE })
    }

    @Test
    fun save_withMissingFields_showsErrors() = runTest {
        val vm = viewModel()
        vm.save()
        with(vm.uiState.value) {
            assertTrue(amountError)
            assertTrue(descriptionError)
            assertTrue(counterpartyError)
            assertFalse(isFinished)
        }
    }

    @Test
    fun withdrawal_reusesExistingPayeeIgnoringCase() = runTest {
        val vm = viewModel()
        vm.onAmountChange("12,50")
        vm.onDescriptionChange("Spesa")
        vm.onCounterpartyChange("supermercato ")
        vm.save()

        val saved = transactions.transactions.value.single()
        assertEquals(1_250L, saved.amount)
        assertEquals(checkingId, saved.source.id)
        assertEquals(shopId, saved.destination.id)
        assertTrue(vm.uiState.value.isFinished)
    }

    @Test
    fun deposit_createsNewRevenueAccount() = runTest {
        val vm = viewModel()
        vm.onTypeChange(TransactionType.DEPOSIT)
        vm.onAmountChange("2000")
        vm.onDescriptionChange("Stipendio ottobre")
        vm.onCounterpartyChange("Datore di lavoro")
        vm.save()

        val saved = transactions.transactions.value.single()
        assertEquals(AccountType.REVENUE, saved.source.type)
        assertEquals("Datore di lavoro", saved.source.name)
        assertEquals(checkingId, saved.destination.id)
    }

    @Test
    fun transfer_toSameAccount_isRejected() = runTest {
        val vm = viewModel()
        vm.onTypeChange(TransactionType.TRANSFER)
        vm.onAmountChange("10")
        vm.onDescriptionChange("Giroconto")
        vm.onToAccountChange(checkingId)
        vm.save()
        assertTrue(vm.uiState.value.sameAccountError)
        assertTrue(transactions.transactions.value.isEmpty())
    }

    @Test
    fun transfer_betweenCurrencies_requiresForeignAmount() = runTest {
        val vm = viewModel()
        vm.onTypeChange(TransactionType.TRANSFER)
        vm.onAmountChange("100")
        vm.onDescriptionChange("Cambio")
        vm.onToAccountChange(savingsUsdId)
        assertTrue(vm.uiState.value.needsForeignAmount)
        vm.save()
        assertTrue(vm.uiState.value.foreignAmountError)

        vm.onForeignAmountChange("108.20")
        vm.save()
        val saved = transactions.transactions.value.single()
        assertEquals(10_000L, saved.amount)
        assertEquals(10_820L, saved.foreignAmount)
    }

    @Test
    fun existingTransaction_isLoadedAndDeleted() = runTest {
        val first = viewModel()
        first.onAmountChange("5")
        first.onDescriptionChange("Caffè")
        first.onCounterpartyChange("Bar")
        first.save()
        val id = transactions.transactions.value.single().id

        val vm = viewModel(id)
        with(vm.uiState.value) {
            assertFalse(isNew)
            assertEquals("5.00", amount)
            assertEquals("Bar", counterparty)
            assertNull(toAccountId)
        }
        vm.delete()
        assertTrue(transactions.transactions.value.isEmpty())
    }

    @Test
    fun categoryAndTags_areSavedAndReloaded() = runTest {
        categories.saveCategory(com.salco.dragonfly.core.model.Category(name = "Spesa alimentare"))
        val vm = viewModel()
        assertEquals(listOf("Spesa alimentare"), vm.uiState.value.categoryNames)
        vm.onAmountChange("30")
        vm.onDescriptionChange("Spesa")
        vm.onCounterpartyChange("Supermercato")
        vm.onCategoryChange("spesa alimentare")
        vm.addTag("Casa")
        vm.addTag("casa")
        vm.onTagInputChange("Vacanze ")
        vm.save()

        val saved = transactions.transactions.value.single()
        assertEquals("Spesa alimentare", saved.category?.name)
        assertEquals(1, categories.categories.value.size)
        assertEquals(listOf("Casa", "Vacanze"), saved.tags.map { it.name })

        val reloaded = viewModel(saved.id).uiState.value
        assertEquals("Spesa alimentare", reloaded.category)
        assertEquals(listOf("Casa", "Vacanze"), reloaded.tags)
    }

    @Test
    fun removeTag_dropsIt() = runTest {
        val vm = viewModel()
        vm.addTag("Lavoro")
        vm.addTag("Casa")
        vm.removeTag("Lavoro")
        assertEquals(listOf("Casa"), vm.uiState.value.tags)
    }
}
