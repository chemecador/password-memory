package com.chemecador.passwordmemory.security

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
 * Symmetric encryption for entries stored in "encrypted" (recoverable) mode.
 *
 * The production implementation keeps the AES-256 key inside the Android Keystore, so it never
 * leaves the secure hardware and cannot be exported (see the backup feature for the consequences).
 */
interface CryptoManager {

    fun encrypt(plainText: String): EncryptedPayload

    fun decrypt(payload: EncryptedPayload): String
}
