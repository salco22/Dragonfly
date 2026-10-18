package com.salco.dragonfly.feature.csvimport

import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.ColumnRole
import com.salco.dragonfly.core.model.ImportRow
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.testing.FakeAccountsRepository
import com.salco.dragonfly.core.testing.FakeImportRepository
import com.salco.dragonfly.core.testing.FakeTransactionsRepository
import com.salco.dragonfly.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CsvImportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val accounts = FakeAccountsRepository()
    private val transactions = FakeTransactionsRepository(accounts)

    private class FakeFiles(val files: Map<String, ByteArray>) : FileSource {
        override fun read(uri: String) = files[uri]
        override fun displayName(uri: String) = uri.substringAfterLast('/')
    }

    private val bankCsv = """
        Data contabile;Data valuta;Descrizione operazione;Dare;Avere
        01/10/2026;01/10/2026;POS ESSELUNGA MILANO;42,50;
        02/10/2026;02/10/2026;BONIFICO STIPENDIO;;2.000,00
        data sbagliata;;???;1,00;
    """.trimIndent()

    private fun viewModel(vararg files: Pair<String, String>) = CsvImportViewModel(
        FakeFiles(files.associate { (k, v) -> k to v.toByteArray(Charsets.UTF_8) }),
        FakeImportRepository(transactions, accounts),
        accounts,
    )

    @Before
    fun setUp() = runTest {
        accounts.saveAccount(Account(name = "Conto", type = AccountType.ASSET, currencyCode = "EUR"))
        accounts.saveAccount(Account(name = "Supermercato", type = AccountType.EXPENSE, currencyCode = "EUR"))
    }

    @Test
    fun bankFile_isRecognisedPreviewedAndImportedOnce() = runTest {
        val vm = viewModel("content://estratto.csv" to bankCsv)
        assertEquals(listOf("Conto"), vm.uiState.value.accounts.map { it.name })
        vm.onFileChosen("content://estratto.csv")
        with(vm.uiState.value) {
            assertEquals("estratto.csv", fileName)
            assertEquals(
                listOf(ColumnRole.DATE, ColumnRole.IGNORE, ColumnRole.DESCRIPTION, ColumnRole.EXPENSE, ColumnRole.INCOME),
                roles,
            )
            assertEquals("dd/MM/yyyy", datePattern)
            assertEquals(2, validCount)
            assertEquals(1, rows.count { it is ImportRow.Invalid })
            assertEquals(2, toImport)
        }

        vm.import("Senza nome")
        assertEquals(2, vm.uiState.value.result?.imported)
        assertEquals(
            setOf(TransactionType.WITHDRAWAL to "Senza nome", TransactionType.DEPOSIT to "Senza nome"),
            transactions.transactions.value.map { it.type to if (it.type == TransactionType.WITHDRAWAL) it.destination.name else it.source.name }.toSet(),
        )

        // The same file again: both rows are duplicates.
        val again = viewModel("content://estratto.csv" to bankCsv)
        again.onFileChosen("content://estratto.csv")
        assertEquals(setOf(2, 3), again.uiState.value.duplicateLines)
        assertEquals(0, again.uiState.value.toImport)
    }

    @Test
    fun rolesCanBeCorrected() = runTest {
        val vm = viewModel("content://a.csv" to "01/10/2026,Caffè,-1.20\n02/10/2026,Pranzo,-12.00\n")
        vm.onFileChosen("content://a.csv")
        // No header names: nothing is guessed and the first row counts as data.
        assertFalse(vm.uiState.value.hasHeader)
        assertFalse(vm.uiState.value.hasRequiredRoles)

        vm.onRoleChange(0, ColumnRole.DATE)
        vm.onRoleChange(1, ColumnRole.DESCRIPTION)
        vm.onRoleChange(2, ColumnRole.AMOUNT)
        assertTrue(vm.uiState.value.hasRequiredRoles)
        assertEquals(2, vm.uiState.value.validCount)

        // Picking the date for another column moves it there.
        vm.onRoleChange(1, ColumnRole.DATE)
        assertEquals(listOf(ColumnRole.IGNORE, ColumnRole.DATE, ColumnRole.AMOUNT), vm.uiState.value.roles)
    }

    @Test
    fun unreadableOrEmptyFiles_showErrors() = runTest {
        val vm = viewModel("content://empty.csv" to "\n\n")
        vm.onFileChosen("content://missing.csv")
        assertEquals(ImportError.READ, vm.uiState.value.error)
        vm.onFileChosen("content://empty.csv")
        assertEquals(ImportError.EMPTY, vm.uiState.value.error)
    }
}
