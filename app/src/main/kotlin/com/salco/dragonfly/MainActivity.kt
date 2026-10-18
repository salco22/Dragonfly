package com.salco.dragonfly

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.salco.dragonfly.core.designsystem.theme.DragonflyTheme
import com.salco.dragonfly.ui.DragonflyAppRoot
import dagger.hilt.android.AndroidEntryPoint

// FragmentActivity is required by BiometricPrompt.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            DragonflyTheme {
                DragonflyAppRoot(uiState = uiState, onUnlocked = viewModel::unlock)
            }
        }
    }
}
