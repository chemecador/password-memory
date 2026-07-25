package com.chemecador.passwordmemory.data.repository

import com.chemecador.passwordmemory.security.AuthenticationRequiredException
import com.chemecador.passwordmemory.security.CryptoManager
import com.chemecador.passwordmemory.security.EncryptedPayload
import java.security.GeneralSecurityException

/**
 * Reversible stand-in for the Keystore-backed manager, which cannot run on the JVM. It is not
 * encryption: it only has to be reversible and to keep the ciphertext different from the plaintext.
 *
 * It also plays the two states the real key can be in — outside its authentication window, and
 * superseded by a rotation — so the repository's handling of both can be tested off-device.
 */
class FakeCryptoManager(
    private var failing: Boolean = false,
    private var legacyKey: Boolean = false,
    private var authRequired: Boolean = false
) : CryptoManager {

    var legacyKeyDiscarded: Boolean = false
        private set

    /** Every encryption performed so far, in order, so a rotation can be seen to have happened. */
    var encryptions: Int = 0
        private set

    fun simulateLostKey() {
        failing = true
    }

    fun simulateLegacyKey() {
        legacyKey = true
    }

    fun simulateClosedAuthWindow() {
        authRequired = true
    }

    /** What authenticating again does to the key: it becomes usable once more. */
    fun simulateOpenAuthWindow() {
        authRequired = false
    }

    override fun encrypt(plainText: String): EncryptedPayload {
        requireAuthentication()
        encryptions++
        val iv = ByteArray(12) { it.toByte() }
        val cipherText = plainText.toByteArray(Charsets.UTF_8)
            .mapIndexed { index, byte -> (byte.toInt() xor iv[index % iv.size].toInt()).toByte() }
            .toByteArray()
        return EncryptedPayload(cipherText = cipherText, iv = iv)
    }

    override fun decrypt(payload: EncryptedPayload): String {
        requireAuthentication()
        if (failing) throw GeneralSecurityException("Key no longer available")
        return payload.cipherText
            .mapIndexed { index, byte ->
                (byte.toInt() xor payload.iv[index % payload.iv.size].toInt()).toByte()
            }
            .toByteArray()
            .toString(Charsets.UTF_8)
    }

    override fun hasLegacyKey(): Boolean = legacyKey && !legacyKeyDiscarded

    override fun discardLegacyKey() {
        legacyKeyDiscarded = true
    }

    private fun requireAuthentication() {
        if (authRequired) throw AuthenticationRequiredException()
    }
}
