package com.salco.dragonfly.feature.csvimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.AccountsRepository
import com.salco.dragonfly.core.data.repository.ImportRepository
import com.salco.dragonfly.core.data.repository.FireflyImportPreview
import com.salco.dragonfly.core.data.repository.ImportResult
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.ColumnRole
import com.salco.dragonfly.core.model.FireflyExport
import com.salco.dragonfly.core.model.FireflyType
import com.salco.dragonfly.core.model.ImportRow
import com.salco.dragonfly.core.model.SupportedDatePatterns
import com.salco.dragonfly.core.model.decodeCsvBytes
import com.salco.dragonfly.core.model.detectDatePattern
import com.salco.dragonfly.core.model.guessColumnRoles
import com.salco.dragonfly.core.model.isFireflyExport
import com.salco.dragonfly.core.model.mapImportRows
import com.salco.dragonfly.core.model.parseCsv
import com.salco.dragonfly.core.model.parseFireflyExport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CsvImportViewModel @Inject constructor(
    private val fileSource: FileSource,
    private val importRepository: ImportRepository,
    accountsRepository: AccountsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CsvImportUiState())
    val uiState: StateFlow<CsvImportUiState> = _uiState.asStateFlow()

    /** Every row of the file, header included. */
    private var allRows: List<List<String>> = emptyList()
    private var fireflyExport: FireflyExport? = null
    private var previewJob: Job? = null

    init {
        viewModelScope.launch {
            val accounts = accountsRepository.observeAccounts().first()
                .filter { it.active && (it.type == AccountType.ASSET || it.type == AccountType.LIABILITY) }
            _uiState.update { it.copy(accounts = accounts, accountId = accounts.firstOrNull { a -> a.type == AccountType.ASSET }?.id ?: accounts.firstOrNull()?.id) }
        }
    }

    fun onFileChosen(uri: String) {
        val bytes = runCatching { fileSource.read(uri) }.getOrNull()
        if (bytes == null) {
            _uiState.update { it.copy(error = ImportError.READ) }
            return
        }
        allRows = parseCsv(decodeCsvBytes(bytes))
        if (allRows.isEmpty()) {
            _uiState.update { it.copy(error = ImportError.EMPTY) }
            return
        }
        // A Firefly III export carries accounts, transfers and types: it needs no column mapping.
        if (isFireflyExport(allRows.first())) {
            val export = parseFireflyExport(allRows)
            fireflyExport = export
            _uiState.update {
                CsvImportUiState(
                    accounts = it.accounts,
                    accountId = it.accountId,
                    fileName = fileSource.displayName(uri) ?: uri.substringAfterLast('/'),
                    firefly = FireflySummary(export),
                )
            }
            viewModelScope.launch {
                val preview = importRepository.previewFirefly(export)
                _uiState.update { state -> state.copy(firefly = state.firefly?.copy(preview = preview)) }
            }
            return
        }
        fireflyExport = null
        val columns = allRows.maxOf { it.size }
        val guessed = guessColumnRoles(allRows.first()).let { it + List(columns - it.size) { ColumnRole.IGNORE } }
        // Without any recognised header name, the first row is probably data.
        val hasHeader = guessed.any { it != ColumnRole.IGNORE }
        _uiState.update {
            it.copy(
                fileName = fileSource.displayName(uri) ?: uri.substringAfterLast('/'),
                error = null,
                header = if (hasHeader) allRows.first() else List(columns) { "" },
                hasHeader = hasHeader,
                roles = guessed,
                result = null,
                firefly = null,
            )
        }
        detectDateFormat()
        refreshPreview()
    }

    fun onHasHeaderChange(value: Boolean) {
        _uiState.update { it.copy(hasHeader = value, header = if (value) allRows.first() else List(it.roles.size) { "" }) }
        detectDateFormat()
        refreshPreview()
    }

    fun onRoleChange(column: Int, role: ColumnRole) {
        _uiState.update { state ->
            // A role belongs to one column, and a signed amount excludes separate in/out columns.
            val conflicting = when (role) {
                ColumnRole.AMOUNT -> setOf(ColumnRole.AMOUNT, ColumnRole.INCOME, ColumnRole.EXPENSE)
                ColumnRole.INCOME, ColumnRole.EXPENSE -> setOf(role, ColumnRole.AMOUNT)
                ColumnRole.IGNORE -> emptySet()
                else -> setOf(role)
            }
            state.copy(roles = state.roles.mapIndexed { i, r -> if (i == column) role else if (r in conflicting) ColumnRole.IGNORE else r })
        }
        if (role == ColumnRole.DATE) detectDateFormat()
        refreshPreview()
    }

    fun onDatePatternChange(pattern: String) {
        _uiState.update { it.copy(datePattern = pattern) }
        refreshPreview()
    }

    fun onAccountChange(id: Long) {
        _uiState.update { it.copy(accountId = id) }
        refreshPreview()
    }

    fun onSkipDuplicatesChange(value: Boolean) = _uiState.update { it.copy(skipDuplicates = value) }
    fun onUnnamedChange(value: String) = _uiState.update { it.copy(unnamedCounterparty = value) }

    fun import(defaultUnnamed: String, reconciliationName: String = defaultUnnamed) {
        val state = _uiState.value
        val export = fireflyExport
        if (state.firefly != null && export != null) {
            viewModelScope.launch {
                _uiState.update { it.copy(importing = true) }
                val result = importRepository.importFirefly(
                    export = export,
                    skipDuplicates = state.skipDuplicates,
                    unnamedCounterparty = state.unnamedCounterparty.trim().ifEmpty { defaultUnnamed },
                    reconciliationName = reconciliationName,
                )
                _uiState.update { it.copy(importing = false, result = result) }
            }
            return
        }
        val accountId = state.accountId ?: return
        val rows = state.rows.filterIsInstance<ImportRow.Valid>()
        if (rows.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(importing = true) }
            val result = importRepository.importRows(
                accountId = accountId,
                rows = rows,
                skipDuplicates = state.skipDuplicates,
                unnamedCounterparty = state.unnamedCounterparty.trim().ifEmpty { defaultUnnamed },
            )
            _uiState.update { it.copy(importing = false, result = result) }
        }
    }

    /** First non-empty value of [column] among the data rows, shown as an example. */
    fun sample(column: Int): String? =
        dataRows(_uiState.value).firstNotNullOfOrNull { row -> row.getOrNull(column)?.takeIf { it.isNotBlank() } }?.take(30)

    private fun dataRows(state: CsvImportUiState) = if (state.hasHeader) allRows.drop(1) else allRows

    private fun detectDateFormat() = _uiState.update { state ->
        val dateColumn = state.roles.indexOf(ColumnRole.DATE)
        val detected = if (dateColumn < 0) null else detectDatePattern(dataRows(state).mapNotNull { it.getOrNull(dateColumn) })
        state.copy(datePattern = detected ?: state.datePattern)
    }

    private fun refreshPreview() {
        val state = _uiState.value
        val currency = state.account?.currencyCode ?: "EUR"
        val rows = if (state.hasRequiredRoles) {
            mapImportRows(dataRows(state), state.roles, state.datePattern, currency, firstLine = if (state.hasHeader) 2 else 1)
        } else {
            emptyList()
        }
        _uiState.update { it.copy(rows = rows, duplicateLines = emptySet()) }
        val accountId = state.accountId ?: return
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            val duplicates = importRepository.findDuplicates(accountId, rows.filterIsInstance<ImportRow.Valid>())
            _uiState.update { it.copy(duplicateLines = duplicates) }
        }
    }
}

enum class ImportError { READ, EMPTY }

/** What a Firefly III export contains, and what importing it would change here. */
data class FireflySummary(
    val withdrawals: Int,
    val deposits: Int,
    val transfers: Int,
    val reconciliations: Int,
    val openingBalances: Int,
    val categories: Int,
    val budgets: Int,
    val tags: Int,
    val invalidLines: Int,
    val movements: Int,
    /** Null until compared with the data already here. */
    val preview: FireflyImportPreview? = null,
) {
    constructor(export: FireflyExport) : this(
        withdrawals = export.count(FireflyType.WITHDRAWAL),
        deposits = export.count(FireflyType.DEPOSIT),
        transfers = export.count(FireflyType.TRANSFER),
        reconciliations = export.count(FireflyType.RECONCILIATION),
        openingBalances = export.count(FireflyType.OPENING_BALANCE),
        categories = export.categories.size,
        budgets = export.budgets.size,
        tags = export.tags.size,
        invalidLines = export.invalidLines.size,
        movements = export.movements.size,
    )
}

data class CsvImportUiState(
    val accounts: List<Account> = emptyList(),
    val accountId: Long? = null,
    val fileName: String? = null,
    val error: ImportError? = null,
    /** Column names, or blanks when the file has no header row. */
    val header: List<String> = emptyList(),
    val hasHeader: Boolean = true,
    val roles: List<ColumnRole> = emptyList(),
    val datePattern: String = SupportedDatePatterns.first(),
    val skipDuplicates: Boolean = true,
    val unnamedCounterparty: String = "",
    /** Data rows mapped with the current settings. */
    val rows: List<ImportRow> = emptyList(),
    /** Lines of valid rows already present on the account. */
    val duplicateLines: Set<Int> = emptySet(),
    val importing: Boolean = false,
    val result: ImportResult? = null,
    /** Set when the file is a Firefly III export, imported as a whole. */
    val firefly: FireflySummary? = null,
) {
    val account: Account? get() = accounts.firstOrNull { it.id == accountId }
    val hasRequiredRoles: Boolean
        get() = ColumnRole.DATE in roles && (ColumnRole.AMOUNT in roles || ColumnRole.INCOME in roles || ColumnRole.EXPENSE in roles)
    val validCount: Int get() = rows.count { it is ImportRow.Valid }
    val toImport: Int get() = validCount - if (skipDuplicates) duplicateLines.size else 0
}
