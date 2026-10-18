package com.salco.dragonfly.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.salco.dragonfly.core.designsystem.R
import com.salco.dragonfly.core.designsystem.theme.DfTheme

/**
 * The frame of every screen: a flat top bar (a large title on main screens, a back arrow and a
 * smaller title on inner ones), an optional floating action and one-off [message] snackbars.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DfScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingAction: (@Composable () -> Unit)? = null,
    /** Shown once as a snackbar; [onMessageShown] is called afterwards. */
    message: String? = null,
    onMessageShown: () -> Unit = {},
    titleContent: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            onMessageShown()
        }
    }
    Scaffold(
        modifier = modifier,
        containerColor = DfTheme.colors.background,
        contentColor = DfTheme.colors.textPrimary,
        topBar = {
            TopAppBar(
                title = {
                    titleContent?.invoke() ?: DfText(
                        title,
                        style = if (onBack == null) DfTheme.typography.headline else DfTheme.typography.title,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        DfIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.core_designsystem_back), onBack)
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DfTheme.colors.background,
                    scrolledContainerColor = DfTheme.colors.background,
                ),
            )
        },
        floatingActionButton = { floatingAction?.invoke() },
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(data, containerColor = DfTheme.colors.textPrimary, contentColor = DfTheme.colors.background)
            }
        },
        content = content,
    )
}

/** A plain full-screen background, for screens without a top bar. */
@Composable
fun DfSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier.fillMaxSize(), color = DfTheme.colors.background, contentColor = DfTheme.colors.textPrimary) {
        Box { content() }
    }
}
