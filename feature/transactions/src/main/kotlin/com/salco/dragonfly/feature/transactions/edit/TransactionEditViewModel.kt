package com.salco.dragonfly.feature.transactions.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.AccountsRepository
import com.salco.dragonfly.core.data.repository.BudgetsRepository
import com.salco.dragonfly.core.data.repository.CategoriesRepository
import com.salco.dragonfly.core.data.repository.TagsRepository
import com.salco.dragonfly.core.data.repository.TransactionsRepository
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.Budget
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.minorUnitsToInput
import com.salco.dragonfly.core.model.parseAmountToMinorUnits
import com.salco.dragonfly.feature.transactions.navigation.TransactionEditRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TransactionEditViewModel internal constructor(
    savedStateHandle: SavedStateHandle,
    private val transactionsRepository: TransactionsRepository,
    accountsRepository: AccountsRepository,
    categoriesRepository: CategoriesRepository,
    tagsRepository: TagsRepository,
    budgetsRepository: BudgetsRepository,
    clock: Clock,
) : ViewModel() {

    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        transactionsRepository: TransactionsRepository,
        accountsRepository: AccountsRepository,
        categoriesRepository: CategoriesRepository,
        tagsRepository: TagsRepository,
        budgetsRepository: BudgetsRepository,
    ) : this(
        savedStateHandle, transactionsRepository, accountsRepository, categoriesRepository, tagsRepository,
        budgetsRepository, Clock.systemDefaultZone(),
    )

    // Read the argument by name so the ViewModel stays testable without Android's Bundle.
    private val transactionId: Long = savedStateHandle[TransactionEditRoute::transactionId.name] ?: 0L
    private val prefillAccountId: Long = savedStateHandle[TransactionEditRoute::accountId.name] ?: 0L

    private val _uiState = MutableStateFlow(
        TransactionEditUiState(isNew = transactionId == 0L, isLoading = true, date = LocalDate.now(clock)),
    )
    val uiState: StateFlow<TransactionEditUiState> = _uiState.asStateFlow()

    private var allAccounts: List<Account> = emptyList()

    init {
        viewModelScope.launch {
            allAccounts = accountsRepository.observeAccounts().first()
            val ownAccounts = allAccounts.filter { it.active && it.type.isOwn }
            val categoryNames = categoriesRepository.observeCategories().first().map { it.name }
            val tagNames = tagsRepository.observeTags().first().map { it.name }
            val budgets = budgetsRepository.observeBudgets().first()
            val transaction = if (transactionId != 0L) transactionsRepository.observeTransaction(transactionId).first() else null
            _uiState.update { state ->
                val base = state.copy(
                    isLoading = false,
                    categoryNames = categoryNames,
                    tagSuggestions = tagNames,
                    budgets = budgets.filter { it.active },
                    ownAccounts = ownAccounts,
                    expenseNames = allAccounts.namesOf(AccountType.EXPENSE),
                    revenueNames = allAccounts.namesOf(AccountType.REVENUE),
                )
                if (transaction == null) {
                    val defaultAccountId = ownAccounts.firstOrNull { it.type == AccountType.ASSET }?.id ?: ownAccounts.firstOrNull()?.id
                    val prefill = allAccounts.firstOrNull { it.id == prefillAccountId }
                    when (prefill?.type) {
                        AccountType.ASSET, AccountType.LIABILITY ->
                            base.copy(accountId = prefill.id.takeIf { id -> ownAccounts.any { it.id == id } } ?: defaultAccountId)
                        AccountType.EXPENSE -> base.copy(accountId = defaultAccountId, counterparty = prefill.name)
                        AccountType.REVENUE -> base.copy(
                            type = TransactionType.DEPOSIT,
                            accountId = defaultAccountId,
                            counterparty = prefill.name,
                        )
                        null -> base.copy(accountId = defaultAccountId)
                    }
                } else {
                    val ownId = if (transaction.type == TransactionType.DEPOSIT) transaction.destination.id else transaction.source.id
                    base.copy(
                        type = transaction.type,
                        amount = minorUnitsToInput(transaction.amount, transaction.currencyCode),
                        foreignAmount = transaction.foreignAmount
                            ?.let { minorUnitsToInput(it, transaction.destination.currencyCode) }
                            .orEmpty(),
                        description = transaction.description,
                        date = transaction.date,
                        accountId = ownId,
                        toAccountId = transaction.destination.id.takeIf { transaction.type == TransactionType.TRANSFER },
                        counterparty = when (transaction.type) {
                            TransactionType.WITHDRAWAL -> transaction.destination.name
                            TransactionType.DEPOSIT -> transaction.source.name
                            TransactionType.TRANSFER -> ""
                        },
                        notes = transaction.notes.orEmpty(),
                        category = transaction.category?.name.orEmpty(),
                        tags = transaction.tags.map { it.name },
                        budgetId = transaction.budget?.id,
                        // An inactive budget stays selectable for the transactions already using it.
                        budgets = budgets.filter { it.active || it.id == transaction.budget?.id },
                        // An archived account still has to be selectable for its old transactions.
                        ownAccounts = (ownAccounts + allAccounts.filter { it.id == ownId || it.id == transaction.destination.id && it.type.isOwn })
                            .distinctBy { it.id },
                    )
                }
            }
        }
    }

    fun onTypeChange(value: TransactionType) = _uiState.update { it.copy(type = value).clearErrors() }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amount = value, amountError = false) }
    fun onForeignAmountChange(value: String) = _uiState.update { it.copy(foreignAmount = value, foreignAmountError = false) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value, descriptionError = false) }
    fun onDateChange(value: LocalDate) = _uiState.update { it.copy(date = value) }
    fun onAccountChange(id: Long) = _uiState.update { it.copy(accountId = id).clearErrors() }
    fun onToAccountChange(id: Long) = _uiState.update { it.copy(toAccountId = id).clearErrors() }
    fun onCounterpartyChange(value: String) = _uiState.update { it.copy(counterparty = value, counterpartyError = false) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }
    fun onCategoryChange(value: String) = _uiState.update { it.copy(category = value) }
    fun onBudgetChange(id: Long?) = _uiState.update { it.copy(budgetId = id) }
    fun onTagInputChange(value: String) = _uiState.update { it.copy(tagInput = value) }

    /** Adds [name] (or the text being typed) as a tag, ignoring blanks and duplicates. */
    fun addTag(name: String = _uiState.value.tagInput) = _uiState.update { state ->
        val tag = name.trim()
        if (tag.isEmpty() || state.tags.any { it.equals(tag, ignoreCase = true) }) {
            state.copy(tagInput = "")
        } else {
            state.copy(tags = state.tags + tag, tagInput = "")
        }
    }

    fun removeTag(name: String) = _uiState.update { it.copy(tags = it.tags - name) }

    fun save() {
        val state = _uiState.value
        val account = state.account
        val toAccount = state.toAccount
        val currency = account?.currencyCode ?: return run { _uiState.update { it.copy(accountError = true) } }
        val amount = parseAmountToMinorUnits(state.amount, currency)?.takeIf { it > 0 }
        val foreignAmount = if (state.needsForeignAmount && toAccount != null) {
            parseAmountToMinorUnits(state.foreignAmount, toAccount.currencyCode)?.takeIf { it > 0 }
        } else {
            null
        }
        val errors = state.copy(
            amountError = amount == null,
            foreignAmountError = state.needsForeignAmount && foreignAmount == null,
            descriptionError = state.description.isBlank(),
            accountError = state.type == TransactionType.TRANSFER && toAccount == null,
            sameAccountError = state.type == TransactionType.TRANSFER && toAccount?.id == account.id,
            counterpartyError = state.type != TransactionType.TRANSFER && state.counterparty.isBlank(),
        )
        if (errors.hasErrors || amount == null) {
            _uiState.value = errors
            return
        }

        val own = AccountRef.Existing(account.id)
        val (source, destination) = when (state.type) {
            TransactionType.WITHDRAWAL -> own to counterpartyRef(state.counterparty, AccountType.EXPENSE, currency)
            TransactionType.DEPOSIT -> counterpartyRef(state.counterparty, AccountType.REVENUE, currency) to own
            TransactionType.TRANSFER -> own to AccountRef.Existing(toAccount!!.id)
        }
        val draft = TransactionDraft(
            id = transactionId,
            type = state.type,
            description = state.description.trim(),
            date = state.date,
            amount = amount,
            currencyCode = currency,
            foreignAmount = foreignAmount,
            source = source,
            destination = destination,
            notes = state.notes.trim().ifEmpty { null },
            categoryName = state.category.trim().ifEmpty { null },
            // A tag still being typed when saving is kept too.
            tagNames = (state.tags + state.tagInput.trim()).filter { it.isNotEmpty() },
            budgetId = state.budgetId.takeIf { state.type == TransactionType.WITHDRAWAL },
        )
        viewModelScope.launch {
            transactionsRepository.saveTransaction(draft)
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    fun delete() {
        if (transactionId == 0L) return
        viewModelScope.launch {
            transactionsRepository.deleteTransaction(transactionId)
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    private fun counterpartyRef(name: String, type: AccountType, currency: String): AccountRef {
        val trimmed = name.trim()
        val existing = allAccounts.firstOrNull { it.type == type && it.name.equals(trimmed, ignoreCase = true) }
        return existing?.let { AccountRef.Existing(it.id) } ?: AccountRef.New(trimmed, type, currency)
    }
}

private val AccountType.isOwn: Boolean
    get() = this == AccountType.ASSET || this == AccountType.LIABILITY

private fun List<Account>.namesOf(type: AccountType) = filter { it.type == type }.map { it.name }.sorted()

data class TransactionEditUiState(
    val isNew: Boolean,
    val isLoading: Boolean = false,
    val type: TransactionType = TransactionType.WITHDRAWAL,
    val amount: String = "",
    val foreignAmount: String = "",
    val description: String = "",
    val date: LocalDate,
    /** The user's own account: source of withdrawals and transfers, destination of deposits. */
    val accountId: Long? = null,
    /** Destination of a transfer. */
    val toAccountId: Long? = null,
    /** Payee of a withdrawal or payer of a deposit, matched or created by name. */
    val counterparty: String = "",
    val notes: String = "",
    val category: String = "",
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val budgetId: Long? = null,
    /** Budgets a withdrawal can be assigned to. */
    val budgets: List<Budget> = emptyList(),
    val ownAccounts: List<Account> = emptyList(),
    val expenseNames: List<String> = emptyList(),
    val revenueNames: List<String> = emptyList(),
    val categoryNames: List<String> = emptyList(),
    val tagSuggestions: List<String> = emptyList(),
    val amountError: Boolean = false,
    val foreignAmountError: Boolean = false,
    val descriptionError: Boolean = false,
    val accountError: Boolean = false,
    val sameAccountError: Boolean = false,
    val counterpartyError: Boolean = false,
    /** True once the transaction was saved or deleted and the screen should close. */
    val isFinished: Boolean = false,
) {
    val account: Account? get() = ownAccounts.firstOrNull { it.id == accountId }
    val toAccount: Account? get() = ownAccounts.firstOrNull { it.id == toAccountId }
    val currencyCode: String? get() = account?.currencyCode

    /** A transfer between currencies needs the amount the destination receives. */
    val needsForeignAmount: Boolean
        get() = type == TransactionType.TRANSFER && toAccount != null && account != null &&
            toAccount?.currencyCode != account?.currencyCode

    val counterpartySuggestions: List<String>
        get() = if (type == TransactionType.DEPOSIT) revenueNames else expenseNames

    internal val hasErrors: Boolean
        get() = amountError || foreignAmountError || descriptionError || accountError || sameAccountError || counterpartyError

    internal fun clearErrors() = copy(accountError = false, sameAccountError = false, foreignAmountError = false)
}
