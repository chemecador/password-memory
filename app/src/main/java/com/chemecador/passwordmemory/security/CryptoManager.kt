package com.chemecador.passwordmemory.security

import java.security.GeneralSecurityException

/**
 * Ciphertext produced by [CryptoManager], along with the IV needed to decrypt it.
 */
data class EncryptedPayload(
    val cipherText: ByteArray,
    val iv: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedPayload) return false
        return cipherText.contentEquals(other.cipherText) && iv.contentEquals(other.iv)
    }

    override fun hashCode(): Int = 31 * cipherText.contentHashCode() + iv.contentHashCode()
}

/**
 * The key is bound to the user's authentication and the window since the last one has closed. The
 * caller is expected to prompt and try again rather than to treat the entry as lost.
 */
class AuthenticationRequiredException(cause: Throwable? = null) :
    GeneralSecurityException("The Keystore key needs a fresh authentication", cause)

/**
 * Symmetric encryption for entries stored in "encrypted" (recoverable) mode.
 *
 * The production implementation keeps the AES-256 key inside the Android Keystore, so it never
 * leaves the secure hardware and cannot be exported (see the backup feature for the consequences).
 * Using it requires a recent authentication, so both operations can throw
 * [AuthenticationRequiredException].
 */
interface CryptoManager {

    fun encrypt(plainText: String): EncryptedPayload

    fun decrypt(payload: EncryptedPayload): String

    /** True while entries encrypted with a superseded key are still around. */
    fun hasLegacyKey(): Boolean

    /** Drops the superseded key, once nothing is encrypted with it any more. */
    fun discardLegacyKey()
}
