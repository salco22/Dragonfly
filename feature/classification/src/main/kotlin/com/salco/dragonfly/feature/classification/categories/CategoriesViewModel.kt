package com.salco.dragonfly.feature.classification.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.CategoriesRepository
import com.salco.dragonfly.core.data.repository.DuplicateNameException
import com.salco.dragonfly.core.model.Category
import com.salco.dragonfly.core.model.CategorySummary
import com.salco.dragonfly.feature.classification.NameEditorState
import com.salco.dragonfly.feature.classification.NameError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel internal constructor(
    private val categoriesRepository: CategoriesRepository,
    clock: Clock,
) : ViewModel() {

    @Inject
    constructor(categoriesRepository: CategoriesRepository) : this(categoriesRepository, Clock.systemDefaultZone())

    private val month = YearMonth.now(clock)

    val uiState: StateFlow<CategoriesUiState> = categoriesRepository
        .observeCategorySummaries(month.atDay(1), month.atEndOfMonth())
        .map<List<CategorySummary>, CategoriesUiState>(CategoriesUiState::Success)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState.Loading)

    private val _editor = MutableStateFlow<NameEditorState?>(null)
    val editor: StateFlow<NameEditorState?> = _editor.asStateFlow()

    fun startAdding() {
        _editor.value = NameEditorState()
    }

    fun startEditing(category: Category) {
        _editor.value = NameEditorState(id = category.id, name = category.name)
    }

    fun onNameChange(name: String) = _editor.update { it?.copy(name = name, error = null) }

    fun dismiss() {
        _editor.value = null
    }

    fun save() {
        val state = _editor.value ?: return
        if (state.name.isBlank()) {
            _editor.value = state.copy(error = NameError.BLANK)
            return
        }
        viewModelScope.launch {
            try {
                categoriesRepository.saveCategory(Category(id = state.id, name = state.name))
                _editor.value = null
            } catch (_: DuplicateNameException) {
                _editor.value = state.copy(error = NameError.DUPLICATE)
            }
        }
    }

    fun delete() {
        val id = _editor.value?.id?.takeIf { it != 0L } ?: return
        viewModelScope.launch {
            categoriesRepository.deleteCategory(id)
            _editor.value = null
        }
    }
}

sealed interface CategoriesUiState {
    data object Loading : CategoriesUiState
    data class Success(val categories: List<CategorySummary>) : CategoriesUiState
}
