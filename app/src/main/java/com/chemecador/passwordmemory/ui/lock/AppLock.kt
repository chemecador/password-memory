package com.chemecador.passwordmemory.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat

/**
 * Biometric with fallback to the device PIN/pattern/password. There is no master password.
 *
 * It has to be `BIOMETRIC_STRONG`: the Keystore key entries are encrypted with only becomes usable
 * after an authentication the keyguard trusts, and a weak biometric is not one. On a device whose
 * only biometric is weak this means the user gets the PIN screen instead — the unlock still works,
 * it just cannot be a face scan the platform does not vouch for.
 */
const val ALLOWED_AUTHENTICATORS = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

/**
 * Lets a screen warn the app lock that it is about to hand control to another app, typically the
 * system file picker. Without it the app would re-lock on the way out, tear down the navigation
 * graph, and drop the result the picker comes back with.
 */
fun interface AppLockSuppressor {
    fun onLeavingForPicker()
}

/** No-op by default, so previews and tests can render a screen without the lock around it. */
val LocalAppLockSuppressor = staticCompositionLocalOf { AppLockSuppressor {} }

sealed interface LockResult {
    data object Unlocked : LockResult

    /** The user cancelled or failed: the app stays locked and can retry. */
    data object Cancelled : LockResult

    /** The device has no biometrics and no screen lock configured. */
    data object Unavailable : LockResult
}

fun FragmentActivity.canAuthenticate(): Boolean =
    BiometricManager.from(this).canAuthenticate(ALLOWED_AUTHENTICATORS) ==
        BiometricManager.BIOMETRIC_SUCCESS

fun FragmentActivity.promptUnlock(
    title: String,
    subtitle: String,
    onResult: (LockResult) -> Unit
) {
    if (!canAuthenticate()) {
        onResult(LockResult.Unavailable)
        return
    }
    val prompt = BiometricPrompt(
        this,
        ContextCompat.getMainExecutor(this),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(LockResult.Unlocked)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onResult(LockResult.Cancelled)
            }
        }
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .build()
    )
}
