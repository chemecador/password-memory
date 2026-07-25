package com.chemecador.passwordmemory.ui.lock

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Reopens the Keystore's authentication window from inside a screen.
 *
 * The app lock is what normally authorises decryption, but its window is far shorter than a session
 * — a user can be reading the list long after unlocking. When an operation comes back asking for
 * authentication, the screen asks here instead of reporting a failure, and retries once the user is
 * through.
 */
fun interface CryptoGate {
    fun authorize(onResult: (granted: Boolean) -> Unit)
}

/** Denies by default, so previews and tests can render a screen without an activity behind it. */
val LocalCryptoGate = staticCompositionLocalOf { CryptoGate { it(false) } }
