package com.salco.dragonfly.feature.sync

import com.salco.dragonfly.core.model.SyncAccount
import com.salco.dragonfly.core.model.SyncError
import com.salco.dragonfly.core.testing.FakeSyncRepository
import com.salco.dragonfly.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeSyncRepository()

    private fun kotlinx.coroutines.test.TestScope.viewModel() = SyncViewModel(repository).also { vm ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    @Test
    fun signIn_withEmailCode_thenSyncsRightAway() = runTest {
        val vm = viewModel()
        vm.onEmailChange("non-una-email")
        vm.sendCode()
        assertTrue(vm.uiState.value.form.emailError)
        assertEquals(0, repository.codesSent)

        vm.onEmailChange("luca@example.com")
        vm.sendCode()
        assertTrue(vm.uiState.value.form.codeSent)

        vm.onCodeChange("12 34 56")
        assertEquals("123456", vm.uiState.value.form.code)
        vm.verifyCode()
        assertEquals(SyncAccount.SignedIn("luca@example.com"), vm.uiState.value.account)
        assertEquals(1, repository.syncs)
        assertFalse(vm.uiState.value.form.busy)
    }

    @Test
    fun wrongCode_andOffline_areShown() = runTest {
        val vm = viewModel()
        vm.onEmailChange("luca@example.com")
        vm.sendCode()
        vm.onCodeChange("000000")
        vm.verifyCode()
        assertEquals(SyncError.InvalidCode, vm.uiState.value.form.error)
        assertTrue(vm.uiState.value.account is SyncAccount.SignedOut)

        vm.onCodeChange("123456")
        vm.verifyCode()
        repository.failNextWith = SyncError.Offline
        vm.syncNow()
        assertEquals(SyncError.Offline, vm.uiState.value.form.error)
        vm.messageShown()
        vm.signOut()
        assertEquals(SyncAccount.SignedOut, vm.uiState.value.account)
    }
}
