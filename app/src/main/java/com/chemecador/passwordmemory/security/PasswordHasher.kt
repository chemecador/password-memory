package com.chemecador.passwordmemory.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hash of a password stored in "hashed" (irrecoverable) mode.
 */
data class HashedPassword(
    val hash: ByteArray,
    val salt: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HashedPassword) return false
        return hash.contentEquals(other.hash) && salt.contentEquals(other.salt)
    }

    override fun hashCode(): Int = 31 * hash.contentHashCode() + salt.contentHashCode()
}

/**
 * SHA-256 with a per-entry random salt. The plaintext is never stored, so the password cannot be
 * recovered: the user can only verify a guess against the stored hash.
 */
@Singleton
class PasswordHasher @Inject constructor(
    private val secureRandom: SecureRandom
) {

    fun hash(password: String): HashedPassword {
        val salt = ByteArray(SALT_SIZE_BYTES).also(secureRandom::nextBytes)
        return HashedPassword(hash = digest(password, salt), salt = salt)
    }

    /** Constant-time comparison, so a guess never leaks how much of the hash matched. */
    fun verify(guess: String, hashed: HashedPassword): Boolean =
        MessageDigest.isEqual(digest(guess, hashed.salt), hashed.hash)

    private fun digest(password: String, salt: ByteArray): ByteArray =
        MessageDigest.getInstance(ALGORITHM).run {
            update(salt)
            digest(password.toByteArray(Charsets.UTF_8))
        }

    private companion object {
        const val ALGORITHM = "SHA-256"
        const val SALT_SIZE_BYTES = 16
    }
}
