package com.salco.dragonfly.feature.accounts

import androidx.lifecycle.SavedStateHandle
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.feature.accounts.edit.AccountEditViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import com.salco.dragonfly.core.testing.FakeAccountsRepository
import com.salco.dragonfly.core.testing.FakeUserDataRepository
import com.salco.dragonfly.core.testing.MainDispatcherRule
import org.junit.Rule
import org.junit.Test

class AccountEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accounts = FakeAccountsRepository()

    private fun viewModel(accountId: Long = 0L) = AccountEditViewModel(
        savedStateHandle = SavedStateHandle(mapOf("accountId" to accountId)),
        accountsRepository = accounts,
        userDataRepository = FakeUserDataRepository(currency = "USD"),
    )

    @Test
    fun newAccount_usesDefaultCurrency() = runTest {
        assertEquals("USD", viewModel().uiState.value.currencyCode)
    }

    @Test
    fun save_withBlankName_showsError() = runTest {
        val vm = viewModel()
        vm.save()
        assertTrue(vm.uiState.value.nameError)
        assertFalse(vm.uiState.value.isFinished)
        assertTrue(accounts.accounts.value.isEmpty())
    }

    @Test
    fun save_withInvalidAmount_showsError() = runTest {
        val vm = viewModel()
        vm.onNameChange("Conto")
        vm.onOpeningBalanceChange("12a")
        vm.save()
        assertTrue(vm.uiState.value.amountError)
    }

    @Test
    fun save_liability_storesNegativeBalance() = runTest {
        val vm = viewModel()
        vm.onNameChange(" Mutuo ")
        vm.onTypeChange(AccountType.LIABILITY)
        vm.onOpeningBalanceChange("95.000,00")
        vm.save()

        val saved = accounts.accounts.value.single()
        assertEquals("Mutuo", saved.name)
        assertEquals(-9_500_000L, saved.openingBalance)
        assertNull(saved.assetRole)
        assertTrue(vm.uiState.value.isFinished)
    }

    @Test
    fun existingAccount_isLoadedAndDeleted() = runTest {
        accounts.accounts.value = listOf(
            Account(id = 7, name = "Mutuo", type = AccountType.LIABILITY, currencyCode = "EUR", openingBalance = -150_000),
        )
        val vm = viewModel(accountId = 7)
        assertEquals("Mutuo", vm.uiState.value.name)
        assertEquals("1500.00", vm.uiState.value.openingBalance)

        vm.delete()
        assertTrue(accounts.accounts.value.isEmpty())
        assertTrue(vm.uiState.value.isFinished)
    }
}
