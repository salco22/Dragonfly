package com.salco.dragonfly.feature.classification

/** State of the dialog that creates or renames a category or tag. */
data class NameEditorState(
    /** 0 when creating a new item. */
    val id: Long = 0,
    val name: String = "",
    val error: NameError? = null,
) {
    val isNew: Boolean get() = id == 0L
}

enum class NameError { BLANK, DUPLICATE }
