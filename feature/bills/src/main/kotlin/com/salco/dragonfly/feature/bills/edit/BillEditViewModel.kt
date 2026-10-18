package com.salco.dragonfly.feature.bills.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.BillsRepository
import com.salco.dragonfly.core.data.repository.DuplicateNameException
import com.salco.dragonfly.core.data.repository.UserDataRepository
import com.salco.dragonfly.core.model.Bill
import com.salco.dragonfly.core.model.RecurrenceFrequency
import com.salco.dragonfly.core.model.RecurrenceRule
import com.salco.dragonfly.core.model.minorUnitsToInput
import com.salco.dragonfly.core.model.parseAmountToMinorUnits
import com.salco.dragonfly.feature.bills.navigation.BillEditRoute
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
class BillEditViewModel internal constructor(
    savedStateHandle: SavedStateHandle,
    private val billsRepository: BillsRepository,
    userDataRepository: UserDataRepository,
    clock: Clock,
) : ViewModel() {

    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        billsRepository: BillsRepository,
        userDataRepository: UserDataRepository,
    ) : this(savedStateHandle, billsRepository, userDataRepository, Clock.systemDefaultZone())

    // Read the argument by name so the ViewModel stays testable without Android's Bundle.
    private val billId: Long = savedStateHandle[BillEditRoute::billId.name] ?: 0L

    private val _uiState = MutableStateFlow(BillEditUiState(isNew = billId == 0L, isLoading = true, firstDate = LocalDate.now(clock)))
    val uiState: StateFlow<BillEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val bill = if (billId != 0L) billsRepository.observeBill(billId).first() else null
            val currency = userDataRepository.userPreferences.first().defaultCurrencyCode
            _uiState.update { state ->
                if (bill == null) {
                    state.copy(isLoading = false, currencyCode = currency)
                } else {
                    state.copy(
                        isLoading = false,
                        name = bill.name,
                        amountMin = minorUnitsToInput(bill.amountMin, bill.currencyCode),
                        amountMax = if (bill.amountMax == bill.amountMin) "" else minorUnitsToInput(bill.amountMax, bill.currencyCode),
                        currencyCode = bill.currencyCode,
                        firstDate = bill.rule.startDate,
                        frequency = bill.rule.frequency,
                        interval = bill.rule.interval.toString(),
                        endDate = bill.rule.endDate,
                        active = bill.active,
                        notes = bill.notes.orEmpty(),
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameError = null) }
    fun onAmountMinChange(value: String) = _uiState.update { it.copy(amountMin = value, amountError = null) }
    fun onAmountMaxChange(value: String) = _uiState.update { it.copy(amountMax = value, amountError = null) }
    fun onCurrencyChange(value: String) = _uiState.update { it.copy(currencyCode = value) }
    fun onFirstDateChange(value: LocalDate) = _uiState.update { it.copy(firstDate = value) }
    fun onFrequencyChange(value: RecurrenceFrequency) = _uiState.update { it.copy(frequency = value) }
    fun onIntervalChange(value: String) = _uiState.update { it.copy(interval = value.filter(Char::isDigit), intervalError = false) }
    fun onEndDateChange(value: LocalDate?) = _uiState.update { it.copy(endDate = value) }
    fun onActiveChange(value: Boolean) = _uiState.update { it.copy(active = value) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }

    fun save() {
        val state = _uiState.value
        val min = parseAmountToMinorUnits(state.amountMin, state.currencyCode)?.takeIf { it > 0 }
        val max = if (state.amountMax.isBlank()) min else parseAmountToMinorUnits(state.amountMax, state.currencyCode)
        val interval = state.interval.toIntOrNull()?.takeIf { it >= 1 }
        val amountError = when {
            min == null || max == null -> AmountError.INVALID
            max < min -> AmountError.RANGE
            else -> null
        }
        val nameError = if (state.name.isBlank()) NameError.BLANK else null
        if (amountError != null || nameError != null || interval == null) {
            _uiState.update { it.copy(amountError = amountError, nameError = nameError, intervalError = interval == null) }
            return
        }
        val bill = Bill(
            id = billId,
            name = state.name,
            amountMin = min!!,
            amountMax = max!!,
            currencyCode = state.currencyCode,
            rule = RecurrenceRule(state.frequency, interval, state.firstDate, state.endDate?.takeIf { !it.isBefore(state.firstDate) }),
            active = state.active,
            notes = state.notes.trim().ifEmpty { null },
        )
        viewModelScope.launch {
            try {
                billsRepository.saveBill(bill)
                _uiState.update { it.copy(isFinished = true) }
            } catch (_: DuplicateNameException) {
                _uiState.update { it.copy(nameError = NameError.DUPLICATE) }
            }
        }
    }

    fun delete() {
        if (billId == 0L) return
        viewModelScope.launch {
            billsRepository.deleteBill(billId)
            _uiState.update { it.copy(isFinished = true) }
        }
    }
}

enum class NameError { BLANK, DUPLICATE }
enum class AmountError { INVALID, RANGE }

data class BillEditUiState(
    val isNew: Boolean,
    val isLoading: Boolean = false,
    val name: String = "",
    val amountMin: String = "",
    /** Empty when the amount is always the same. */
    val amountMax: String = "",
    val currencyCode: String = "EUR",
    val firstDate: LocalDate,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val interval: String = "1",
    val endDate: LocalDate? = null,
    val active: Boolean = true,
    val notes: String = "",
    val nameError: NameError? = null,
    val amountError: AmountError? = null,
    val intervalError: Boolean = false,
    /** True once the bill was saved or deleted and the screen should close. */
    val isFinished: Boolean = false,
)
