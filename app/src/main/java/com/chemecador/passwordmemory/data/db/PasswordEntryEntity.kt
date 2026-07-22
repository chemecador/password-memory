package com.chemecador.passwordmemory.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chemecador.passwordmemory.domain.model.ProtectionMode

/**
 * Stored entry. Exactly one of the two secret groups is populated depending on [mode]:
 * ([cipherText], [iv]) for [ProtectionMode.ENCRYPTED], ([hash], [salt]) for
 * [ProtectionMode.HASHED]. The plaintext password is never stored in either case.
 */
@Entity(tableName = "entries")
data class PasswordEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "service_name") val serviceName: String,
    val username: String? = null,
    val hint: String? = null,
    val category: String? = null,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    val mode: ProtectionMode,
    @ColumnInfo(name = "cipher_text", typeAffinity = ColumnInfo.BLOB) val cipherText: ByteArray? = null,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val iv: ByteArray? = null,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val hash: ByteArray? = null,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val salt: ByteArray? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PasswordEntryEntity) return false
        return id == other.id &&
            serviceName == other.serviceName &&
            username == other.username &&
            hint == other.hint &&
            category == other.category &&
            isFavorite == other.isFavorite &&
            mode == other.mode &&
            cipherText.contentEquals(other.cipherText) &&
            iv.contentEquals(other.iv) &&
            hash.contentEquals(other.hash) &&
            salt.contentEquals(other.salt) &&
            createdAt == other.createdAt &&
            updatedAt == other.updatedAt
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + serviceName.hashCode()
        result = 31 * result + (username?.hashCode() ?: 0)
        result = 31 * result + (hint?.hashCode() ?: 0)
        result = 31 * result + (category?.hashCode() ?: 0)
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + mode.hashCode()
        result = 31 * result + cipherText.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + hash.contentHashCode()
        result = 31 * result + salt.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        return result
    }
}
