package com.chemecador.passwordmemory.domain.model

/**
 * How an entry's password is protected. Chosen at creation time and irreversible afterwards.
 */
enum class ProtectionMode {
    /** AES-256-GCM with a Keystore key: the password can be decrypted and copied. */
    ENCRYPTED,

    /** SHA-256 + random salt: the password cannot be recovered, only guessed. */
    HASHED
}

/**
 * An entry as shown by the UI. It deliberately carries no cryptographic material: ciphertext,
 * hashes and salts never leave the data layer.
 */
data class PasswordEntry(
    val id: Long = 0,
    val serviceName: String,
    val username: String? = null,
    val hint: String? = null,
    val category: String? = null,
    val isFavorite: Boolean = false,
    val mode: ProtectionMode,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)
