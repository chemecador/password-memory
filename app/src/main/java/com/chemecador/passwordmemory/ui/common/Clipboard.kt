package com.chemecador.passwordmemory.ui.common

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.core.content.getSystemService

/**
 * Copies a secret to the clipboard, flagged as sensitive so the system preview (Android 13+) shows
 * a placeholder instead of the password itself.
 */
fun Context.copySensitiveToClipboard(label: String, value: String) {
    val clipboard = getSystemService<ClipboardManager>() ?: return
    val clip = ClipData.newPlainText(label, value).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
    }
    clipboard.setPrimaryClip(clip)
}
