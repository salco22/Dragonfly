package com.salco.dragonfly.feature.lock

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.feature_lock_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.feature_lock_message),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (error != null) {
                Text(text = error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            Button(onClick = onUnlockClick) {
                Text(stringResource(R.string.feature_lock_unlock))
            }
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
