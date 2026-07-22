package com.chemecador.passwordmemory.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat

/** Biometric with fallback to the device PIN/pattern/password. There is no master password. */
const val ALLOWED_AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

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
