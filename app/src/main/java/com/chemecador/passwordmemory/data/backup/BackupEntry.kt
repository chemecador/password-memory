package com.chemecador.passwordmemory.data.backup

import com.chemecador.passwordmemory.domain.model.ProtectionMode

/**
 * One entry as it travels through a backup file.
 *
 * Encrypted entries carry their [plainPassword] because the Keystore key is not exportable: the
 * whole backup file is what protects it. Hashed entries travel as-is ([hash] + [salt]), they are
 * irrecoverable by design.
 */
data class BackupEntry(
    val serviceName: String,
    val username: String?,
    val hint: String?,
    val category: String?,
    val isFavorite: Boolean,
    val mode: ProtectionMode,
    val plainPassword: String? = null,
    val hash: ByteArray? = null,
    val salt: ByteArray? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BackupEntry) return false
        return serviceName == other.serviceName &&
            username == other.username &&
            hint == other.hint &&
            category == other.category &&
            isFavorite == other.isFavorite &&
            mode == other.mode &&
            plainPassword == other.plainPassword &&
            hash.contentEquals(other.hash) &&
            salt.contentEquals(other.salt) &&
            createdAt == other.createdAt &&
            updatedAt == other.updatedAt
    }

    override fun hashCode(): Int {
        var result = serviceName.hashCode()
        result = 31 * result + (username?.hashCode() ?: 0)
        result = 31 * result + (hint?.hashCode() ?: 0)
        result = 31 * result + (category?.hashCode() ?: 0)
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + mode.hashCode()
        result = 31 * result + (plainPassword?.hashCode() ?: 0)
        result = 31 * result + hash.contentHashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
