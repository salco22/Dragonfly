package com.salco.dragonfly.feature.classification.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salco.dragonfly.core.data.repository.DuplicateNameException
import com.salco.dragonfly.core.data.repository.TagsRepository
import com.salco.dragonfly.core.model.Tag
import com.salco.dragonfly.core.model.TagSummary
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
import javax.inject.Inject

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val tagsRepository: TagsRepository,
) : ViewModel() {

    val uiState: StateFlow<TagsUiState> = tagsRepository.observeTagSummaries()
        .map<List<TagSummary>, TagsUiState>(TagsUiState::Success)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TagsUiState.Loading)

    private val _editor = MutableStateFlow<NameEditorState?>(null)
    val editor: StateFlow<NameEditorState?> = _editor.asStateFlow()

    fun startAdding() {
        _editor.value = NameEditorState()
    }

    fun startEditing(tag: Tag) {
        _editor.value = NameEditorState(id = tag.id, name = tag.name)
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
                tagsRepository.saveTag(Tag(id = state.id, name = state.name))
                _editor.value = null
            } catch (_: DuplicateNameException) {
                _editor.value = state.copy(error = NameError.DUPLICATE)
            }
        }
    }

    fun delete() {
        val id = _editor.value?.id?.takeIf { it != 0L } ?: return
        viewModelScope.launch {
            tagsRepository.deleteTag(id)
            _editor.value = null
        }
    }
}

sealed interface TagsUiState {
    data object Loading : TagsUiState
    data class Success(val tags: List<TagSummary>) : TagsUiState
}
