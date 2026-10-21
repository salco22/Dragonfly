package com.salco.dragonfly.feature.transactions.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.AccountsRepository
import com.salco.dragonfly.core.data.repository.BillsRepository
import com.salco.dragonfly.core.data.repository.BudgetsRepository
import com.salco.dragonfly.core.data.repository.CategoriesRepository
import com.salco.dragonfly.core.data.repository.TagsRepository
import com.salco.dragonfly.core.data.repository.TransactionsRepository
import com.salco.dragonfly.core.model.Account
import com.salco.dragonfly.core.model.AccountType
import com.salco.dragonfly.core.model.Bill
import com.salco.dragonfly.core.model.Budget
import com.salco.dragonfly.core.model.Transaction
import com.salco.dragonfly.core.model.TransactionGroupDraft
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.minorUnitsToInput
import com.salco.dragonfly.feature.transactions.navigation.TransactionEditRoute
import dagger.hilt.android.lifecycle.HiltViewModel
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
    billsRepository: BillsRepository,
    clock: Clock,
) : TransactionFormViewModel(
    initial = TransactionEditUiState(
        isNew = savedStateHandle.get<Long>(TransactionEditRoute::transactionId.name).let { it == null || it == 0L },
        isLoading = true,
        date = LocalDate.now(clock),
    ),
    accountsRepository, categoriesRepository, tagsRepository, budgetsRepository, billsRepository,
) {

    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        transactionsRepository: TransactionsRepository,
        accountsRepository: AccountsRepository,
        categoriesRepository: CategoriesRepository,
        tagsRepository: TagsRepository,
        budgetsRepository: BudgetsRepository,
        billsRepository: BillsRepository,
    ) : this(
        savedStateHandle, transactionsRepository, accountsRepository, categoriesRepository, tagsRepository,
        budgetsRepository, billsRepository, Clock.systemDefaultZone(),
    )

    // Read the argument by name so the ViewModel stays testable without Android's Bundle.
    private val transactionId: Long = savedStateHandle[TransactionEditRoute::transactionId.name] ?: 0L
    private val prefillAccountId: Long = savedStateHandle[TransactionEditRoute::accountId.name] ?: 0L
    private val prefillBillId: Long = savedStateHandle[TransactionEditRoute::billId.name] ?: 0L

    init {
        viewModelScope.launch {
            val base = loadOptions(_uiState.value).let { options ->
                // A new transaction starts on the account used most recently, so a quick expense
                // needs no account choice.
                val recent = lastUsedAccountId()?.takeIf { id -> options.ownAccounts.any { it.id == id } }
                if (transactionId == 0L && recent != null) options.copy(accountId = recent) else options
            }
            val transaction = if (transactionId != 0L) transactionsRepository.observeTransaction(transactionId).first() else null
            _uiState.value = if (transaction == null) {
                val prefill = allAccounts.firstOrNull { it.id == prefillAccountId }
                when (prefill?.type) {
                    AccountType.ASSET, AccountType.LIABILITY ->
                        base.copy(accountId = prefill.id.takeIf { id -> base.ownAccounts.any { it.id == id } } ?: base.accountId)
                    AccountType.EXPENSE -> base.copy(counterparty = prefill.name)
                    AccountType.REVENUE -> base.copy(type = TransactionType.DEPOSIT, counterparty = prefill.name)
                    null -> base
                }.copy(budgets = base.budgets.filter { it.active }).let { state ->
                    // Paying a bill: a withdrawal to a payee named like the bill, for its usual amount.
                    val bill = state.bills.firstOrNull { it.id == prefillBillId }
                    if (bill == null) {
                        state.copy(bills = state.bills.filter { it.active })
                    } else {
                        state.copy(
                            type = TransactionType.WITHDRAWAL,
                            billId = bill.id,
                            description = bill.name,
                            counterparty = bill.name,
                            amount = minorUnitsToInput(bill.amountMax, bill.currencyCode),
                            accountId = state.ownAccounts.firstOrNull { it.currencyCode == bill.currencyCode }?.id ?: state.accountId,
                        ).keepingSelected()
                    }
                }
            } else {
                val parts = transaction.groupId?.let { transactionsRepository.observeSplitTransaction(it).first() }.orEmpty()
                val first = parts.firstOrNull() ?: transaction
                base.copy(
                    transactionId = first.id,
                    groupId = transaction.groupId ?: 0,
                    groupTitle = transaction.groupTitle.orEmpty(),
                    splits = parts.drop(1).map { it.asSplitForm() },
                    type = first.type,
                    amount = minorUnitsToInput(first.amount, first.currencyCode),
                    foreignAmount = first.foreignAmount
                        ?.let { minorUnitsToInput(it, first.destination.currencyCode) }
                        .orEmpty(),
                    description = first.description,
                    date = first.date,
                    accountId = if (first.type == TransactionType.DEPOSIT) first.destination.id else first.source.id,
                    toAccountId = first.destination.id.takeIf { first.type == TransactionType.TRANSFER },
                    counterparty = when (first.type) {
                        TransactionType.WITHDRAWAL -> first.destination.name
                        TransactionType.DEPOSIT -> first.source.name
                        TransactionType.TRANSFER -> ""
                    },
                    notes = first.notes.orEmpty(),
                    category = first.category?.name.orEmpty(),
                    tags = first.tags.map { it.name },
                    budgetId = first.budget?.id,
                    billId = first.bill?.id,
                ).keepingSelected()
            }
        }
    }

    private suspend fun lastUsedAccountId(): Long? {
        val today = _uiState.value.date
        return transactionsRepository.observeTransactions(today.minusMonths(RECENT_MONTHS), today.plusDays(1)).first()
            .filter { it.type != TransactionType.TRANSFER }
            .maxByOrNull { it.id }
            ?.let { if (it.type == TransactionType.DEPOSIT) it.destination.id else it.source.id }
    }

    /** Saves the transaction; a missing payee or payer becomes [unnamedCounterparty]. */
    fun save(unnamedCounterparty: String = DEFAULT_UNNAMED) {
        val state = _uiState.value
        if (state.isSplit && state.canSplit) {
            val group = validatedGroup(state.transactionId, unnamedCounterparty) ?: return
            viewModelScope.launch {
                transactionsRepository.saveSplitTransaction(group)
                _uiState.update { it.copy(isFinished = true) }
            }
            return
        }
        val draft = validatedDraft(id = state.transactionId, unnamedCounterparty = unnamedCounterparty) ?: return
        viewModelScope.launch {
            if (state.groupId != 0L) {
                // The other parts were removed: saving a single part dissolves the group.
                transactionsRepository.saveSplitTransaction(TransactionGroupDraft(state.groupId, state.groupTitle, listOf(draft)))
            } else {
                transactionsRepository.saveTransaction(draft)
            }
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    /** Deletes the transaction, or every part of a split transaction. */
    private companion object {
        const val RECENT_MONTHS = 3L
    }

    fun delete() {
        val state = _uiState.value
        if (state.transactionId == 0L) return
        viewModelScope.launch {
            if (state.groupId != 0L) {
                transactionsRepository.deleteSplitTransaction(state.groupId)
            } else {
                transactionsRepository.deleteTransaction(state.transactionId)
            }
            _uiState.update { it.copy(isFinished = true) }
        }
    }
}

private fun Transaction.asSplitForm() = SplitForm(
    id = id,
    description = description,
    amount = minorUnitsToInput(amount, currencyCode),
    counterparty = if (type == TransactionType.DEPOSIT) source.name else destination.name,
    category = category?.name.orEmpty(),
    budgetId = budget?.id,
    tags = tags.map { it.name },
)

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
    val billId: Long? = null,
    /** Bills a withdrawal can pay. */
    val bills: List<Bill> = emptyList(),
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
    /** Split transaction being edited; 0 when it is not one yet. */
    val groupId: Long = 0,
    /** Title of a split transaction, shown in lists instead of the parts' descriptions. */
    val groupTitle: String = "",
    val groupTitleError: Boolean = false,
    /** Parts after the first: the fields above are part 1. Empty for an ordinary transaction. */
    val splits: List<SplitForm> = emptyList(),
    /** Id of part 1 when editing an existing transaction. */
    val transactionId: Long = 0,
    /** True once the transaction was saved or deleted and the screen should close. */
    val isFinished: Boolean = false,
) {
    val isSplit: Boolean get() = splits.isNotEmpty()

    /** Transfers move money between own accounts and are not split. */
    val canSplit: Boolean get() = type != TransactionType.TRANSFER

    /** Sum of every part that has a valid amount, in the account's currency. */
    val splitTotal: Long?
        get() {
            val currency = currencyCode ?: return null
            return (listOf(amount) + splits.map { it.amount })
                .sumOf { com.salco.dragonfly.core.model.parseAmountToMinorUnits(it, currency)?.coerceAtLeast(0) ?: 0 }
        }

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
        get() = amountError || foreignAmountError || descriptionError || accountError || sameAccountError ||
            groupTitleError || splits.any { it.hasErrors }

    internal fun clearErrors() = copy(accountError = false, sameAccountError = false, foreignAmountError = false)
}

/** A part of a split transaction after the first. */
data class SplitForm(
    val id: Long = 0,
    val description: String = "",
    val amount: String = "",
    val counterparty: String = "",
    val category: String = "",
    val budgetId: Long? = null,
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val amountError: Boolean = false,
    val descriptionError: Boolean = false,
) {
    internal val hasErrors: Boolean get() = amountError || descriptionError
}
