package com.chemecador.passwordmemory.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.chemecador.passwordmemory.domain.model.AppSettings
import com.chemecador.passwordmemory.domain.model.AutoLockTimeout
import com.chemecador.passwordmemory.domain.model.ClipboardClearDelay
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The user's preferences. Plain [SharedPreferences] rather than DataStore: none of this is secret,
 * there are only a handful of values, and both the app lock and the autofill service need to read
 * them synchronously from callbacks where suspending is not an option.
 *
 * Values are keyed by their duration in seconds, so renaming an enum constant cannot orphan a
 * setting the user already chose.
 */
@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    /** Emits the current settings straight away and again on every change. */
    val settings: Flow<AppSettings> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(current())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        send(current())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.conflate().distinctUntilChanged()

    fun current(): AppSettings = AppSettings(
        autoLock = AutoLockTimeout.fromSeconds(
            prefs.getInt(KEY_AUTO_LOCK, AutoLockTimeout.DEFAULT.seconds)
        ),
        clipboardClear = ClipboardClearDelay.fromSeconds(
            prefs.getInt(KEY_CLIPBOARD_CLEAR, ClipboardClearDelay.DEFAULT.seconds)
        )
    )

    fun setAutoLock(timeout: AutoLockTimeout) {
        prefs.edit { putInt(KEY_AUTO_LOCK, timeout.seconds) }
    }

    fun setClipboardClear(delay: ClipboardClearDelay) {
        prefs.edit { putInt(KEY_CLIPBOARD_CLEAR, delay.seconds) }
    }

    private companion object {
        const val NAME = "settings"
        const val KEY_AUTO_LOCK = "auto_lock_seconds"
        const val KEY_CLIPBOARD_CLEAR = "clipboard_clear_seconds"
    }
}
