package com.chemecador.passwordmemory.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * How long the app may stay unlocked after it leaves the foreground. [IMMEDIATE] is the default:
 * anything longer trades a little safety for not having to authenticate every time the user steps
 * out to read the code they were sent.
 */
enum class AutoLockTimeout(val seconds: Int) {
    IMMEDIATE(0),
    SECONDS_30(30),
    MINUTE_1(60),
    MINUTES_5(300);

    val duration: Duration get() = seconds.seconds

    companion object {
        val DEFAULT = IMMEDIATE

        fun fromSeconds(seconds: Int): AutoLockTimeout =
            entries.firstOrNull { it.seconds == seconds } ?: DEFAULT
    }
}

/**
 * How long a copied password is left on the clipboard before the app wipes it. [NEVER] keeps the
 * platform behaviour of leaving it there until something else replaces it.
 */
enum class ClipboardClearDelay(val seconds: Int) {
    NEVER(0),
    SECONDS_30(30),
    MINUTE_1(60),
    MINUTES_2(120);

    val duration: Duration get() = seconds.seconds

    companion object {
        val DEFAULT = SECONDS_30

        fun fromSeconds(seconds: Int): ClipboardClearDelay =
            entries.firstOrNull { it.seconds == seconds } ?: DEFAULT
    }
}

/** Everything the user can tune. Nothing here is a secret, so it lives in plain preferences. */
data class AppSettings(
    val autoLock: AutoLockTimeout = AutoLockTimeout.DEFAULT,
    val clipboardClear: ClipboardClearDelay = ClipboardClearDelay.DEFAULT
)
