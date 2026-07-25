package com.chemecador.passwordmemory.ui.settings

import androidx.annotation.StringRes
import com.chemecador.passwordmemory.R
import com.chemecador.passwordmemory.domain.model.AutoLockTimeout
import com.chemecador.passwordmemory.domain.model.ClipboardClearDelay

/**
 * The durations share their wording with anywhere else that names them — the "copied" confirmation
 * in the detail screen reads the clipboard label from here too.
 */
@StringRes
fun AutoLockTimeout.labelRes(): Int = when (this) {
    AutoLockTimeout.IMMEDIATE -> R.string.settings_autolock_immediate
    AutoLockTimeout.SECONDS_30 -> R.string.duration_seconds_30
    AutoLockTimeout.MINUTE_1 -> R.string.duration_minutes_1
    AutoLockTimeout.MINUTES_5 -> R.string.duration_minutes_5
}

@StringRes
fun ClipboardClearDelay.labelRes(): Int = when (this) {
    ClipboardClearDelay.NEVER -> R.string.settings_clipboard_never
    ClipboardClearDelay.SECONDS_30 -> R.string.duration_seconds_30
    ClipboardClearDelay.MINUTE_1 -> R.string.duration_minutes_1
    ClipboardClearDelay.MINUTES_2 -> R.string.duration_minutes_2
}
