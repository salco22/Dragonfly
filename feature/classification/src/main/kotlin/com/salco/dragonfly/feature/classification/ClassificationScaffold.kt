package com.salco.dragonfly.feature.classification

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.salco.dragonfly.core.designsystem.component.DfEmptyState
import com.salco.dragonfly.core.designsystem.component.DfFab
import com.salco.dragonfly.core.designsystem.component.DfLoading
import com.salco.dragonfly.core.designsystem.component.DfScaffold

/** Shared layout of the categories and tags lists. */
@Composable
internal fun ClassificationScaffold(
    title: String,
    isLoading: Boolean,
    isEmpty: Boolean,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptyMessage: String,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    content: LazyListScope.() -> Unit,
) {
    DfScaffold(
        title = title,
        onBack = onBack,
        floatingAction = { DfFab(stringResource(R.string.feature_classification_add), Icons.Filled.Add, onAdd) },
    ) { padding ->
        when {
            isLoading -> DfLoading(Modifier.padding(padding))
            isEmpty -> DfEmptyState(emptyIcon, emptyTitle, emptyMessage, Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 96.dp),
                content = content,
            )
        }
    }
}
