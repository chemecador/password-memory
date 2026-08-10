package com.chemecador.passwordmemory.ui.common

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.core.content.getSystemService
import com.chemecador.passwordmemory.data.settings.SettingsRepository
import com.chemecador.passwordmemory.domain.model.ClipboardClearDelay
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only way a password reaches the clipboard. On top of flagging the clip as sensitive it starts
 * a timer that wipes it again, so a password copied into a login form does not sit there for the
 * rest of the day.
 *
 * The timer lives in the application process: if Android kills the app before it fires, the clip
 * survives. That is the same limit every offline password manager has without a background service,
 * and it is why the copy is flagged sensitive in the first place.
 */
@Singleton
class SecureClipboard @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settings: SettingsRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var pendingClear: Job? = null

    private companion object {
        const val UNKNOWN_STAMP = -1L
    }

    /** Copies [value] under [label] and schedules the wipe configured by the user. */
    fun copy(label: String, value: String) {
        val clipboard = context.getSystemService<ClipboardManager>() ?: return
        clipboard.setPrimaryClip(sensitiveClip(label, value))

        // A second copy replaces the first, so only the newest one gets a timer.
        pendingClear?.cancel()
        val after = settings.current().clipboardClear
        if (after == ClipboardClearDelay.NEVER) return
        // What the system recorded for our clip, so the timer can tell later whether it is still
        // the one on the clipboard. UNKNOWN_STAMP means we could not find out and will wipe anyway.
        val stamp = clipboard.primaryClipDescription?.timestamp ?: UNKNOWN_STAMP
        pendingClear = scope.launch {
            delay(after.duration)
            clear(clipboard, stamp)
        }
    }

    /**
     * Wipes the clipboard, unless something has written to it since.
     *
     * The check is on [ClipDescription.getTimestamp] rather than on the label we set: a label is
     * rewritten by anything that re-posts the clip — a clipboard manager, a keyboard, the emulator's
     * bridge to the host — and comparing it would quietly skip the wipe exactly when some other tool
     * is holding the password. A different timestamp means the primary clip is no longer ours, so
     * the password is already gone from it and wiping would only destroy what replaced it.
     *
     * Reading the description does not count as reading the clipboard, so this raises no
     * "pasted from your clipboard" toast.
     */
    private fun clear(clipboard: ClipboardManager, stamp: Long) {
        val current = clipboard.primaryClipDescription
        if (stamp != UNKNOWN_STAMP && current != null && current.timestamp != stamp) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            clipboard.setPrimaryClip(sensitiveClip("", ""))
        }
    }

    /**
     * Flagged as sensitive so the system preview (Android 13+) shows a placeholder instead of the
     * password itself.
     */
    @SuppressLint("InlinedApi")
    private fun sensitiveClip(label: String, value: String): ClipData =
        ClipData.newPlainText(label, value).apply {
            description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
}
