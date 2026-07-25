package com.chemecador.passwordmemory.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Settings are persisted as a number of seconds so renaming a constant cannot orphan the user's
 * choice. These check that the round trip holds and that anything unrecognised — a value written by
 * a future version, or a corrupt preferences file — falls back to the safe default instead of
 * throwing on startup.
 */
class AppSettingsTest {

    @Test
    fun `auto lock survives the round trip through seconds`() {
        AutoLockTimeout.entries.forEach { timeout ->
            assertEquals(timeout, AutoLockTimeout.fromSeconds(timeout.seconds))
        }
    }

    @Test
    fun `clipboard delay survives the round trip through seconds`() {
        ClipboardClearDelay.entries.forEach { delay ->
            assertEquals(delay, ClipboardClearDelay.fromSeconds(delay.seconds))
        }
    }

    @Test
    fun `unknown values fall back to the default`() {
        assertEquals(AutoLockTimeout.DEFAULT, AutoLockTimeout.fromSeconds(-1))
        assertEquals(AutoLockTimeout.DEFAULT, AutoLockTimeout.fromSeconds(9_999))
        assertEquals(ClipboardClearDelay.DEFAULT, ClipboardClearDelay.fromSeconds(-1))
        assertEquals(ClipboardClearDelay.DEFAULT, ClipboardClearDelay.fromSeconds(9_999))
    }

    @Test
    fun `the app locks itself as soon as it leaves the foreground unless told otherwise`() {
        assertEquals(AutoLockTimeout.IMMEDIATE, AppSettings().autoLock)
    }

    @Test
    fun `a copied password does not sit on the clipboard forever by default`() {
        assertEquals(ClipboardClearDelay.SECONDS_30, AppSettings().clipboardClear)
    }
}
