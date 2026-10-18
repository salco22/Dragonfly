package com.salco.dragonfly.feature.piggybanks

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.PiggyBank
import com.salco.dragonfly.core.testing.FakeAccountsRepository
import com.salco.dragonfly.core.testing.FakePiggyBanksRepository
import com.salco.dragonfly.core.testing.MainDispatcherRule
import com.salco.dragonfly.feature.piggybanks.detail.MoveMoneyError
import com.salco.dragonfly.feature.piggybanks.detail.PiggyBankDetailUiState
import com.salco.dragonfly.feature.piggybanks.detail.PiggyBankDetailViewModel
import com.salco.dragonfly.feature.piggybanks.edit.PiggyBankEditViewModel
import com.salco.dragonfly.feature.piggybanks.list.PiggyBanksUiState
import com.salco.dragonfly.feature.piggybanks.list.PiggyBanksViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

class PiggyBanksViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accounts = FakeAccountsRepository()
    private val piggyBanks = FakePiggyBanksRepository(accounts)
    private val clock = Clock.fixed(LocalDate.of(2026, 10, 18).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private var savings = 0L

    @Before
    fun setUp() = runTest {
        savings = accounts.saveAccount(Account(name = "Risparmi", type = AccountType.ASSET, currencyCode = "EUR", openingBalance = 100_000))
        accounts.saveAccount(Account(name = "Supermercato", type = AccountType.EXPENSE, currencyCode = "EUR"))
    }

    private suspend fun jar(name: String, target: Long? = 50_000) = piggyBanks.savePiggyBank(
        PiggyBank(name = name, accountId = savings, targetAmount = target, startDate = LocalDate.of(2026, 1, 1)),
    )

    @Test
    fun edit_offersOnlyAssetAccountsAndSaves() = runTest {
        val vm = PiggyBankEditViewModel(SavedStateHandle(mapOf("piggyBankId" to 0L)), piggyBanks, accounts, clock)
        assertEquals(listOf("Risparmi"), vm.uiState.value.accounts.map { it.name })
        vm.onNameChange("Vacanze")
        vm.onTargetChange("1.500")
        vm.onTargetDateChange(LocalDate.of(2027, 6, 30))
        vm.save()
        assertTrue(vm.uiState.value.isFinished)
        with(piggyBanks.piggyBanks.value.single()) {
            assertEquals(150_000L, targetAmount)
            assertEquals(savings, accountId)
            assertEquals(LocalDate.of(2026, 10, 18), startDate)
        }
    }

    @Test
    fun detail_movesMoneyWithinWhatIsAvailable() = runTest {
        val holiday = jar("Vacanze")
        val emergency = jar("Emergenze", target = null)
        piggyBanks.moveMoney(emergency, 70_000, LocalDate.of(2026, 10, 1))

        val vm = PiggyBankDetailViewModel(SavedStateHandle(mapOf("piggyBankId" to holiday)), piggyBanks, accounts, clock)
        vm.uiState.test {
            assertEquals(30_000L, (awaitItem() as PiggyBankDetailUiState.Success).availableInAccount)

            vm.startAdding()
            vm.onAmountChange("400")
            vm.confirm()
            assertEquals(MoveMoneyError.Insufficient(30_000), vm.dialog.value?.error)

            vm.onAmountChange("250")
            vm.confirm()
            assertNull(vm.dialog.value)
            // Several sources change at once; check the settled state.
            with(expectMostRecentItem() as PiggyBankDetailUiState.Success) {
                assertEquals(25_000L, summary.saved)
                assertEquals(5_000L, availableInAccount)
                assertEquals(listOf(25_000L), events.map { it.amount })
            }

            vm.startRemoving()
            vm.onAmountChange("300")
            vm.confirm()
            assertEquals(MoveMoneyError.Insufficient(25_000), vm.dialog.value?.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun list_sumsSavedMoney() = runTest {
        piggyBanks.moveMoney(jar("Vacanze"), 20_000, LocalDate.of(2026, 10, 1))
        piggyBanks.moveMoney(jar("Auto"), 5_000, LocalDate.of(2026, 10, 2))
        PiggyBanksViewModel(piggyBanks, clock).uiState.test {
            with(awaitItem() as PiggyBanksUiState.Success) {
                assertEquals(listOf("Auto", "Vacanze"), items.map { it.piggyBank.name })
                assertEquals(mapOf("EUR" to 25_000L), totalSaved)
            }
        }
    }
}
