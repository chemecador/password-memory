package com.chemecador.passwordmemory

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.chemecador.passwordmemory.ui.lock.LockResult
import com.chemecador.passwordmemory.ui.lock.LockScreen
import com.chemecador.passwordmemory.ui.lock.canAuthenticate
import com.chemecador.passwordmemory.ui.lock.promptUnlock
import com.chemecador.passwordmemory.ui.navigation.PasswordMemoryNavHost
import com.chemecador.passwordmemory.ui.theme.PasswordMemoryTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * [FragmentActivity] rather than `ComponentActivity` because `BiometricPrompt` needs a fragment
 * host to survive configuration changes.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PasswordMemoryTheme {
                AppRoot(activity = this)
            }
        }
    }
}

/**
 * Nothing is rendered until the user authenticates, and the app locks itself again as soon as it
 * leaves the foreground.
 */
@Composable
private fun AppRoot(activity: FragmentActivity) {
    var unlocked by remember { mutableStateOf(false) }
    var unavailable by remember { mutableStateOf(false) }
    var promptShown by remember { mutableStateOf(false) }
    val title = stringResource(R.string.lock_prompt_title)
    val subtitle = stringResource(R.string.lock_prompt_subtitle)
    val lifecycleOwner = LocalLifecycleOwner.current

    fun authenticate() {
        promptShown = true
        activity.promptUnlock(title, subtitle) { result ->
            when (result) {
                LockResult.Unlocked -> unlocked = true
                LockResult.Unavailable -> unavailable = true
                LockResult.Cancelled -> promptShown = false
            }
        }
    }

    LaunchedEffect(lifecycleOwner) {
        unavailable = !activity.canAuthenticate()
    }

    // Re-lock on background so a password left on screen is not visible in the recents list.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                unlocked = false
                promptShown = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(unlocked, promptShown, unavailable) {
        if (!unlocked && !promptShown && !unavailable) authenticate()
    }

    if (unlocked) {
        PasswordMemoryNavHost(modifier = Modifier.fillMaxSize())
    } else {
        LockScreen(
            unavailable = unavailable,
            onUnlockClick = ::authenticate,
            modifier = Modifier.fillMaxSize()
        )
    }
}
