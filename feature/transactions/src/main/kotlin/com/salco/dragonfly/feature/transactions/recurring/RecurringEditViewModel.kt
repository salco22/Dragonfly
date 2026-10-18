package com.salco.dragonfly.feature.transactions.recurring

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.AccountsRepository
import com.salco.dragonfly.core.data.repository.BillsRepository
import com.salco.dragonfly.core.data.repository.BudgetsRepository
import com.salco.dragonfly.core.data.repository.CategoriesRepository
import com.salco.dragonfly.core.data.repository.RecurringTransactionsRepository
import com.salco.dragonfly.core.data.repository.TagsRepository
import com.salco.dragonfly.core.model.AccountRef
import com.salco.dragonfly.core.model.RecurrenceFrequency
import com.salco.dragonfly.core.model.RecurrenceRule
import com.salco.dragonfly.core.model.RecurringTransaction
import com.salco.dragonfly.core.model.TransactionType
import com.salco.dragonfly.core.model.minorUnitsToInput
import com.salco.dragonfly.feature.transactions.edit.TransactionEditUiState
import com.salco.dragonfly.feature.transactions.edit.TransactionFormViewModel
import com.salco.dragonfly.feature.transactions.navigation.RecurringEditRoute
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
class RecurringEditViewModel internal constructor(
    savedStateHandle: SavedStateHandle,
    private val recurringRepository: RecurringTransactionsRepository,
    accountsRepository: AccountsRepository,
    categoriesRepository: CategoriesRepository,
    tagsRepository: TagsRepository,
    budgetsRepository: BudgetsRepository,
    billsRepository: BillsRepository,
    private val clock: Clock,
) : TransactionFormViewModel(
    initial = TransactionEditUiState(
        isNew = savedStateHandle.get<Long>(RecurringEditRoute::recurringId.name).let { it == null || it == 0L },
        isLoading = true,
        date = LocalDate.now(clock),
    ),
    accountsRepository, categoriesRepository, tagsRepository, budgetsRepository, billsRepository,
) {

    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        recurringRepository: RecurringTransactionsRepository,
        accountsRepository: AccountsRepository,
        categoriesRepository: CategoriesRepository,
        tagsRepository: TagsRepository,
        budgetsRepository: BudgetsRepository,
        billsRepository: BillsRepository,
    ) : this(
        savedStateHandle, recurringRepository, accountsRepository, categoriesRepository, tagsRepository,
        budgetsRepository, billsRepository, Clock.systemDefaultZone(),
    )

    // Read the argument by name so the ViewModel stays testable without Android's Bundle.
    private val recurringId: Long = savedStateHandle[RecurringEditRoute::recurringId.name] ?: 0L

    private val _schedule = MutableStateFlow(ScheduleState())
    val schedule: StateFlow<ScheduleState> = _schedule.asStateFlow()

    init {
        viewModelScope.launch {
            val base = loadOptions(_uiState.value)
            val recurring = if (recurringId != 0L) recurringRepository.observeRecurringTransaction(recurringId).first() else null
            if (recurring == null) {
                _uiState.value = base.copy(budgets = base.budgets.filter { it.active }, bills = base.bills.filter { it.active })
                return@launch
            }
            val template = recurring.template
            val ownRef = (if (template.type == TransactionType.DEPOSIT) template.destination else template.source) as? AccountRef.Existing
            val toAccount = allAccounts.firstOrNull { it.id == (template.destination as? AccountRef.Existing)?.id }
            _uiState.value = base.copy(
                type = template.type,
                amount = minorUnitsToInput(template.amount, template.currencyCode),
                foreignAmount = template.foreignAmount?.let { minorUnitsToInput(it, toAccount?.currencyCode ?: template.currencyCode) }.orEmpty(),
                description = template.description,
                date = recurring.rule.startDate,
                accountId = ownRef?.id,
                toAccountId = toAccount?.id.takeIf { template.type == TransactionType.TRANSFER },
                counterparty = when (template.type) {
                    TransactionType.WITHDRAWAL -> recurring.destinationName
                    TransactionType.DEPOSIT -> recurring.sourceName
                    TransactionType.TRANSFER -> ""
                },
                notes = template.notes.orEmpty(),
                category = template.categoryName.orEmpty(),
                tags = template.tagNames,
                budgetId = template.budgetId,
                billId = template.billId,
            ).keepingSelected()
            val rule = recurring.rule
            _schedule.value = ScheduleState(
                frequency = rule.frequency,
                interval = rule.interval.toString(),
                end = when {
                    rule.endDate != null -> EndMode.UNTIL
                    rule.maxOccurrences != null -> EndMode.TIMES
                    else -> EndMode.NEVER
                },
                endDate = rule.endDate,
                maxOccurrences = rule.maxOccurrences?.toString().orEmpty(),
                active = recurring.active,
                occurrencesCreated = recurring.occurrencesCreated,
            )
        }
    }

    fun onFrequencyChange(value: RecurrenceFrequency) = _schedule.update { it.copy(frequency = value) }
    fun onIntervalChange(value: String) = _schedule.update { it.copy(interval = value.filter(Char::isDigit), intervalError = false) }
    fun onEndModeChange(value: EndMode) = _schedule.update { it.copy(end = value, endError = false) }
    fun onEndDateChange(value: LocalDate) = _schedule.update { it.copy(endDate = value, endError = false) }
    fun onMaxOccurrencesChange(value: String) = _schedule.update { it.copy(maxOccurrences = value.filter(Char::isDigit), endError = false) }
    fun onActiveChange(value: Boolean) = _schedule.update { it.copy(active = value) }

    fun save() {
        val draft = validatedDraft()
        val schedule = _schedule.value
        val startDate = _uiState.value.date
        val interval = schedule.interval.toIntOrNull()?.takeIf { it >= 1 }
        val endDate = schedule.endDate.takeIf { schedule.end == EndMode.UNTIL }
        val maxOccurrences = schedule.maxOccurrences.toIntOrNull().takeIf { schedule.end == EndMode.TIMES }
        val endError = when (schedule.end) {
            EndMode.NEVER -> false
            EndMode.UNTIL -> endDate == null || endDate.isBefore(startDate)
            EndMode.TIMES -> maxOccurrences == null || maxOccurrences < 1
        }
        if (interval == null || endError) {
            _schedule.update { it.copy(intervalError = interval == null, endError = endError) }
            return
        }
        if (draft == null) return

        val recurring = RecurringTransaction(
            id = recurringId,
            template = draft,
            rule = RecurrenceRule(schedule.frequency, interval, startDate, endDate, maxOccurrences),
            active = schedule.active,
        )
        viewModelScope.launch {
            recurringRepository.saveRecurringTransaction(recurring)
            // Create right away whatever is already due, e.g. a schedule starting today.
            recurringRepository.generateDueTransactions(LocalDate.now(clock))
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    fun delete() {
        if (recurringId == 0L) return
        viewModelScope.launch {
            recurringRepository.deleteRecurringTransaction(recurringId)
            _uiState.update { it.copy(isFinished = true) }
        }
    }
}

enum class EndMode { NEVER, UNTIL, TIMES }

data class ScheduleState(
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val interval: String = "1",
    val end: EndMode = EndMode.NEVER,
    val endDate: LocalDate? = null,
    val maxOccurrences: String = "",
    val active: Boolean = true,
    val occurrencesCreated: Int = 0,
    val intervalError: Boolean = false,
    val endError: Boolean = false,
)
