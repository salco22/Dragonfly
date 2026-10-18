package com.salco.dragonfly.feature.lock

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.salco.dragonfly.core.designsystem.component.DfButton
import com.salco.dragonfly.core.designsystem.component.DfIcon
import com.salco.dragonfly.core.designsystem.component.DfSurface
import com.salco.dragonfly.core.designsystem.component.DfText
import com.salco.dragonfly.core.designsystem.component.DfTone
import com.salco.dragonfly.core.designsystem.theme.DfTheme
import com.salco.dragonfly.core.designsystem.theme.DragonflyTheme

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = context.findFragmentActivity()
    var error by remember { mutableStateOf<String?>(null) }
    val authenticate = {
        if (activity != null) {
            error = null
            showAppLockPrompt(activity, onSuccess = onUnlocked, onError = { error = it.toString() })
        }
    }
    LaunchedEffect(Unit) {
        // If the device lock was removed after enabling the app lock, the prompt can never
        // succeed: let the user in rather than locking them out of their data.
        if (isAppLockAvailable(context)) authenticate() else onUnlocked()
    }

    LockContent(error = error, onUnlockClick = authenticate)
}

@Composable
private fun LockContent(error: String?, onUnlockClick: () -> Unit) {
    DfSurface {
        Column(
            modifier = Modifier.fillMaxSize().padding(40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DfIcon(Icons.Outlined.Lock, contentDescription = null, tone = DfTone.Primary, modifier = Modifier.padding(bottom = 8.dp))
            DfText(stringResource(R.string.feature_lock_title), style = DfTheme.typography.headline, textAlign = TextAlign.Center)
            DfText(
                stringResource(R.string.feature_lock_message),
                tone = DfTone.Secondary,
                textAlign = TextAlign.Center,
            )
            if (error != null) DfText(error, tone = DfTone.Negative, textAlign = TextAlign.Center, style = DfTheme.typography.bodySmall)
            DfButton(stringResource(R.string.feature_lock_unlock), onUnlockClick, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Preview
@Composable
private fun LockContentPreview() {
    DragonflyTheme { LockContent(error = null, onUnlockClick = {}) }
}
