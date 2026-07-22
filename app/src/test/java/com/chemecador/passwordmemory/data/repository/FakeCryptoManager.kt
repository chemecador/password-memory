package com.chemecador.passwordmemory.data.repository

import com.chemecador.passwordmemory.security.CryptoManager
import com.chemecador.passwordmemory.security.EncryptedPayload

/**
 * Reversible stand-in for the Keystore-backed manager, which cannot run on the JVM. It is not
 * encryption: it only has to be reversible and to keep the ciphertext different from the plaintext.
 */
class FakeCryptoManager(private var failing: Boolean = false) : CryptoManager {

    fun simulateLostKey() {
        failing = true
    }

    override fun encrypt(plainText: String): EncryptedPayload {
        val iv = ByteArray(12) { it.toByte() }
        val cipherText = plainText.toByteArray(Charsets.UTF_8)
            .mapIndexed { index, byte -> (byte.toInt() xor iv[index % iv.size].toInt()).toByte() }
            .toByteArray()
        return EncryptedPayload(cipherText = cipherText, iv = iv)
    }

    override fun decrypt(payload: EncryptedPayload): String {
        if (failing) throw IllegalStateException("Key no longer available")
        return payload.cipherText
            .mapIndexed { index, byte ->
                (byte.toInt() xor payload.iv[index % payload.iv.size].toInt()).toByte()
            }
            .toByteArray()
            .toString(Charsets.UTF_8)
    }
}
