package com.chemecador.passwordmemory.ui.common

import android.app.Activity
import android.view.WindowManager

/**
 * Keeps the window out of screenshots, screen recordings and the recents thumbnail. Every screen in
 * this app either shows a password or is one tap away from one, so the flag is set for the whole
 * activity instead of being toggled per screen.
 */
fun Activity.blockScreenCapture() {
    window.setFlags(
        WindowManager.LayoutParams.FLAG_SECURE,
        WindowManager.LayoutParams.FLAG_SECURE
    )
}
