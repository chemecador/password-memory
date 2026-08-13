package com.chemecador.passwordmemory

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.withResumed
import com.chemecador.passwordmemory.data.settings.SettingsRepository
import com.chemecador.passwordmemory.domain.model.AutoLockTimeout
import com.chemecador.passwordmemory.ui.common.blockScreenCapture
import com.chemecador.passwordmemory.ui.lock.AppLockSuppressor
import com.chemecador.passwordmemory.ui.lock.AppLockViewModel
import com.chemecador.passwordmemory.ui.lock.CryptoGate
import com.chemecador.passwordmemory.ui.lock.LocalAppLockSuppressor
import com.chemecador.passwordmemory.ui.lock.LocalCryptoGate
import com.chemecador.passwordmemory.ui.lock.LockResult
import com.chemecador.passwordmemory.ui.lock.LockScreen
import com.chemecador.passwordmemory.ui.lock.canAuthenticate
import com.chemecador.passwordmemory.ui.lock.promptUnlock
import com.chemecador.passwordmemory.ui.navigation.PasswordMemoryNavHost
import com.chemecador.passwordmemory.ui.theme.PasswordMemoryTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * [FragmentActivity] rather than `ComponentActivity` because `BiometricPrompt` needs a fragment
 * host to survive configuration changes.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        blockScreenCapture()
        enableEdgeToEdge()
        setContent {
            PasswordMemoryTheme {
                AppRoot(activity = this, settings = settings)
            }
        }
    }
}

/**
 * Nothing is rendered until the user authenticates, and the app locks itself again once it has been
 * away from the foreground for longer than the user's auto-lock timeout.
 */
@Composable
private fun AppRoot(
    activity: FragmentActivity,
    settings: SettingsRepository,
    viewModel: AppLockViewModel = hiltViewModel()
) {
    var unlocked by remember { mutableStateOf(false) }
    // Kept apart from [unlocked] so the lock screen stays composed on top while it plays its
    // unlock animation over the content that has already appeared underneath.
    var revealed by remember { mutableStateOf(false) }
    var unavailable by remember { mutableStateOf(false) }
    var promptShown by remember { mutableStateOf(false) }
    // Set when the user turns the prompt away. Without it the prompt would go straight back up and
    // the only way out of the app would be to kill it.
    var promptDismissed by remember { mutableStateOf(false) }
    var pickerPending by remember { mutableStateOf(false) }
    // When the app went to the background, or null while it is in front. Monotonic, so changing the
    // device clock cannot buy extra unlocked time.
    var leftAt by remember { mutableStateOf<Long?>(null) }
    val title = stringResource(R.string.lock_prompt_title)
    val subtitle = stringResource(R.string.lock_prompt_subtitle)
    val reauthSubtitle = stringResource(R.string.lock_reauth_subtitle)
    val lifecycleOwner = LocalLifecycleOwner.current

    fun authenticate() {
        promptShown = true
        activity.promptUnlock(title, subtitle) { result ->
            when (result) {
                LockResult.Unlocked -> {
                    unlocked = true
                    viewModel.onUnlocked()
                }

                LockResult.Unavailable -> unavailable = true
                LockResult.Cancelled -> {
                    promptShown = false
                    promptDismissed = true
                }
            }
        }
    }

    LaunchedEffect(lifecycleOwner) {
        unavailable = !activity.canAuthenticate()
    }

    fun lock() {
        unlocked = false
        revealed = false
        promptShown = false
    }

    // Re-lock after a trip to the background. With an immediate timeout that happens on the way out;
    // with a longer one the app stays unlocked for a short errand — reading the code that was just
    // texted, say — and locks on the way back in if the user took too long. Either way FLAG_SECURE
    // keeps the recents thumbnail blank in the meantime.
    //
    // The system file picker is the one exception: re-locking there would destroy the screen waiting
    // for its result. The exemption is spent on the first stop, so a later trip to the background
    // locks the app as usual.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> when {
                    pickerPending -> pickerPending = false
                    settings.current().autoLock == AutoLockTimeout.IMMEDIATE -> lock()
                    else -> leftAt = SystemClock.elapsedRealtime()
                }

                Lifecycle.Event.ON_START -> {
                    // Coming back is a fresh attempt at getting in, so a prompt turned away earlier
                    // is not held against the user.
                    promptDismissed = false
                    val away = leftAt?.let { SystemClock.elapsedRealtime() - it }
                    leftAt = null
                    val timeout = settings.current().autoLock.duration.inWholeMilliseconds
                    if (away != null && away >= timeout) lock()
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Being asked is the point of opening a locked app, so the prompt comes up by itself and the
    // lock screen behind it is only a backdrop. It has to wait for the activity to be resumed:
    // `BiometricPrompt` refuses to show anything once its host has saved its state, and it refuses
    // silently, without ever calling back. That is what an immediate auto-lock ran into — the lock
    // on the way out to the background asked for a prompt nobody could see, and the request that
    // never came back left the app waiting for a tap on returning.
    LaunchedEffect(unlocked, promptShown, unavailable, promptDismissed) {
        if (unlocked || promptShown || unavailable || promptDismissed) return@LaunchedEffect
        lifecycleOwner.withResumed { authenticate() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (unlocked) {
            val suppressor = remember { AppLockSuppressor { pickerPending = true } }
            // Screens reach for this when the Keystore window has closed under them.
            val cryptoGate = remember(reauthSubtitle) {
                CryptoGate { onResult ->
                    activity.promptUnlock(title, reauthSubtitle) { result ->
                        onResult(result == LockResult.Unlocked)
                    }
                }
            }
            CompositionLocalProvider(
                LocalAppLockSuppressor provides suppressor,
                LocalCryptoGate provides cryptoGate
            ) {
                PasswordMemoryNavHost(modifier = Modifier.fillMaxSize())
            }
        }
        if (!revealed) {
            LockScreen(
                unavailable = unavailable,
                onUnlockClick = ::authenticate,
                modifier = Modifier.fillMaxSize(),
                canRetry = promptDismissed,
                unlocking = unlocked,
                onUnlockAnimationEnd = { revealed = true }
            )
        }
    }
}
