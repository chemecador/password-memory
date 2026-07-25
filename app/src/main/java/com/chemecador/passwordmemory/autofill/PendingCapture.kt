package com.chemecador.passwordmemory.autofill

import javax.inject.Inject
import javax.inject.Singleton

/** A credential the autofill service caught, waiting for the user to authorise storing it. */
data class Capture(
    val serviceName: String,
    val username: String?,
    val password: String,
    val origin: String?
)

/**
 * Hands a captured credential from [PasswordMemoryAutofillService] to [AutofillUnlockActivity].
 *
 * Storing it encrypts it, and the Keystore key needs a recent authentication that a background
 * service has no way to ask for — so the service parks the credential here and launches the
 * activity, which prompts and then commits. It stays in memory, and deliberately never travels as
 * an `Intent` extra: a password has no business in an object the system copies around.
 */
@Singleton
class PendingCapture @Inject constructor() {

    @Volatile
    private var capture: Capture? = null

    fun put(capture: Capture) {
        this.capture = capture
    }

    /** Returns the parked credential and forgets it, so it is never stored twice. */
    fun take(): Capture? {
        val current = capture
        capture = null
        return current
    }
}
