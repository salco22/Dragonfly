package com.salco.dragonfly.feature.transactions.edit

import androidx.lifecycle.ViewModel
import com.salco.dragonfly.core.data.repository.AccountsRepository
import com.salco.dragonfly.core.data.repository.BillsRepository
import com.salco.dragonfly.core.data.repository.BudgetsRepository
import com.salco.dragonfly.core.data.repository.CategoriesRepository
import com.salco.dragonfly.core.data.repository.TagsRepository
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.TransactionDraft
import com.salco.dragonfly.core.model.TransactionGroupDraft
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.parseAmountToMinorUnits
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/**
 * Shared state and validation of the transaction form, used both for single transactions and
 * for recurring transaction templates.
 */
abstract class TransactionFormViewModel(
    initial: TransactionEditUiState,
    private val accountsRepository: AccountsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val tagsRepository: TagsRepository,
    private val budgetsRepository: BudgetsRepository,
    private val billsRepository: BillsRepository,
) : ViewModel() {

    protected val _uiState = MutableStateFlow(initial)
    val uiState: StateFlow<TransactionEditUiState> = _uiState.asStateFlow()

    protected var allAccounts: List<Account> = emptyList()
        private set

    /** Loads accounts and suggestions; returns the form state ready to be filled. */
    protected suspend fun loadOptions(state: TransactionEditUiState): TransactionEditUiState {
        allAccounts = accountsRepository.observeAccounts().first()
        val ownAccounts = allAccounts.filter { it.active && it.type.isOwn }
        return state.copy(
            isLoading = false,
            categoryNames = categoriesRepository.observeCategories().first().map { it.name },
            tagSuggestions = tagsRepository.observeTags().first().map { it.name },
            budgets = budgetsRepository.observeBudgets().first(),
            bills = billsRepository.observeBills().first(),
            ownAccounts = ownAccounts,
            expenseNames = allAccounts.namesOf(AccountType.EXPENSE),
            revenueNames = allAccounts.namesOf(AccountType.REVENUE),
            accountId = state.accountId ?: defaultAccountId(ownAccounts),
        )
    }

    /** Keeps inactive budgets and archived accounts selectable when they are already in use. */
    protected fun TransactionEditUiState.keepingSelected(): TransactionEditUiState = copy(
        budgets = budgets.filter { budget -> budget.active || budget.id == budgetId || splits.any { it.budgetId == budget.id } },
        bills = bills.filter { it.active || it.id == billId },
        ownAccounts = (ownAccounts + allAccounts.filter { (it.id == accountId || it.id == toAccountId) && it.type.isOwn })
            .distinctBy { it.id },
    )

    fun onTypeChange(value: TransactionType) = _uiState.update { it.copy(type = value).clearErrors() }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amount = value, amountError = false) }
    fun onForeignAmountChange(value: String) = _uiState.update { it.copy(foreignAmount = value, foreignAmountError = false) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value, descriptionError = false) }
    fun onDateChange(value: LocalDate) = _uiState.update { it.copy(date = value) }
    fun onAccountChange(id: Long) = _uiState.update { it.copy(accountId = id).clearErrors() }
    fun onToAccountChange(id: Long) = _uiState.update { it.copy(toAccountId = id).clearErrors() }
    fun onCounterpartyChange(value: String) = _uiState.update { it.copy(counterparty = value) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }
    fun onCategoryChange(value: String) = _uiState.update { it.copy(category = value) }
    fun onBudgetChange(id: Long?) = _uiState.update { it.copy(budgetId = id) }
    fun onBillChange(id: Long?) = _uiState.update { it.copy(billId = id) }
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

    fun onGroupTitleChange(value: String) = _uiState.update { it.copy(groupTitle = value, groupTitleError = false) }

    /** Adds an empty part; the first time, the title starts from the description of part 1. */
    fun addSplit() = _uiState.update { state ->
        state.copy(
            splits = state.splits + SplitForm(),
            groupTitle = state.groupTitle.ifBlank { state.description },
        )
    }

    /** Removes a part after the first; with no parts left the transaction is an ordinary one again. */
    fun removeSplit(index: Int) = _uiState.update { state ->
        state.copy(splits = state.splits.filterIndexed { i, _ -> i != index })
    }

    /** Changes the part at [index] (0 is part 2), clearing the errors of the fields being edited. */
    fun updateSplit(index: Int, change: (SplitForm) -> SplitForm) = _uiState.update { state ->
        state.copy(splits = state.splits.mapIndexed { i, split -> if (i == index) change(split) else split })
    }

    fun onSplitDescriptionChange(index: Int, value: String) = updateSplit(index) { it.copy(description = value, descriptionError = false) }
    fun onSplitAmountChange(index: Int, value: String) = updateSplit(index) { it.copy(amount = value, amountError = false) }
    fun onSplitCounterpartyChange(index: Int, value: String) = updateSplit(index) { it.copy(counterparty = value) }
    fun onSplitCategoryChange(index: Int, value: String) = updateSplit(index) { it.copy(category = value) }
    fun onSplitBudgetChange(index: Int, id: Long?) = updateSplit(index) { it.copy(budgetId = id) }
    fun onSplitTagInputChange(index: Int, value: String) = updateSplit(index) { it.copy(tagInput = value) }

    fun addSplitTag(index: Int, name: String) = updateSplit(index) { split ->
        val tag = name.trim()
        if (tag.isEmpty() || split.tags.any { it.equals(tag, ignoreCase = true) }) split.copy(tagInput = "")
        else split.copy(tags = split.tags + tag, tagInput = "")
    }

    fun removeSplitTag(index: Int, name: String) = updateSplit(index) { it.copy(tags = it.tags - name) }

    /**
     * Validates a split transaction: part 1 through [validatedDraft], every other part with the
     * same type, date and own account. Returns null and shows errors when something is missing.
     */
    protected fun validatedGroup(firstId: Long, unnamedCounterparty: String): TransactionGroupDraft? {
        val first = validatedDraft(firstId, unnamedCounterparty)
        val state = _uiState.value
        val currency = state.currencyCode
        val checked = state.splits.map { split ->
            split.copy(
                amountError = currency == null || parseAmountToMinorUnits(split.amount, currency)?.takeIf { it > 0 } == null,
                descriptionError = split.description.isBlank(),
            )
        }
        val titleError = state.groupTitle.isBlank()
        if (first == null || titleError || checked.any { it.hasErrors }) {
            _uiState.update { it.copy(splits = checked, groupTitleError = titleError) }
            return null
        }
        val others = checked.map { split ->
            val counterparty = if (state.type == TransactionType.DEPOSIT) {
                counterpartyRef(split.counterparty.ifBlank { unnamedCounterparty }, AccountType.REVENUE, first.currencyCode)
            } else {
                counterpartyRef(split.counterparty.ifBlank { unnamedCounterparty }, AccountType.EXPENSE, first.currencyCode)
            }
            first.copy(
                id = split.id,
                description = split.description.trim(),
                amount = parseAmountToMinorUnits(split.amount, first.currencyCode)!!,
                source = if (state.type == TransactionType.DEPOSIT) counterparty else first.source,
                destination = if (state.type == TransactionType.DEPOSIT) first.destination else counterparty,
                notes = null,
                categoryName = split.category.trim().ifEmpty { null },
                tagNames = (split.tags + split.tagInput.trim()).filter { it.isNotEmpty() },
                budgetId = split.budgetId.takeIf { state.type == TransactionType.WITHDRAWAL },
                billId = null,
            )
        }
        return TransactionGroupDraft(id = state.groupId, title = state.groupTitle.trim(), splits = listOf(first) + others)
    }

    /**
     * Validates the form, showing errors, and returns the transaction it describes or null. The
     * payee or payer is optional: without one the money goes to (or comes from) [unnamedCounterparty].
     */
    protected fun validatedDraft(id: Long = 0, unnamedCounterparty: String = DEFAULT_UNNAMED): TransactionDraft? {
        val state = _uiState.value
        val account = state.account
        val toAccount = state.toAccount
        val currency = account?.currencyCode ?: run {
            _uiState.update { it.copy(accountError = true) }
            return null
        }
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
        )
        if (errors.hasErrors || amount == null) {
            _uiState.value = errors
            return null
        }

        val own = AccountRef.Existing(account.id)
        val (source, destination) = when (state.type) {
            TransactionType.WITHDRAWAL -> own to counterpartyRef(state.counterparty.ifBlank { unnamedCounterparty }, AccountType.EXPENSE, currency)
            TransactionType.DEPOSIT -> counterpartyRef(state.counterparty.ifBlank { unnamedCounterparty }, AccountType.REVENUE, currency) to own
            TransactionType.TRANSFER -> own to AccountRef.Existing(toAccount!!.id)
        }
        return TransactionDraft(
            id = id,
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
            billId = state.billId.takeIf { state.type == TransactionType.WITHDRAWAL },
        )
    }

    companion object {
        /** Payee/payer used without one when the screen does not pass its translated name (tests). */
        const val DEFAULT_UNNAMED = "Senza nome"
    }

    protected fun counterpartyRef(name: String, type: AccountType, currency: String): AccountRef {
        val trimmed = name.trim()
        val existing = allAccounts.firstOrNull { it.type == type && it.name.equals(trimmed, ignoreCase = true) }
        return existing?.let { AccountRef.Existing(it.id) } ?: AccountRef.New(trimmed, type, currency)
    }
}

internal val AccountType.isOwn: Boolean
    get() = this == AccountType.ASSET || this == AccountType.LIABILITY

private fun List<Account>.namesOf(type: AccountType) = filter { it.type == type }.map { it.name }.sorted()

private fun defaultAccountId(ownAccounts: List<Account>): Long? =
    ownAccounts.firstOrNull { it.type == AccountType.ASSET }?.id ?: ownAccounts.firstOrNull()?.id
