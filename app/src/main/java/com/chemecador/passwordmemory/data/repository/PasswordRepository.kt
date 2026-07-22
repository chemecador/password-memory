package com.chemecador.passwordmemory.data.repository

import com.chemecador.passwordmemory.data.db.PasswordEntryDao
import com.chemecador.passwordmemory.data.db.PasswordEntryEntity
import com.chemecador.passwordmemory.domain.model.PasswordEntry
import com.chemecador.passwordmemory.domain.model.ProtectionMode
import com.chemecador.passwordmemory.security.CryptoManager
import com.chemecador.passwordmemory.security.EncryptedPayload
import com.chemecador.passwordmemory.security.HashedPassword
import com.chemecador.passwordmemory.security.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Editable fields of an entry. The password is passed separately and never round-trips. */
data class EntryDraft(
    val serviceName: String,
    val username: String? = null,
    val hint: String? = null,
    val category: String? = null,
    val isFavorite: Boolean = false
)

/**
 * Single access point to the entries. Cryptographic material stays inside this class: callers only
 * ever see [PasswordEntry] metadata, a decrypted password they explicitly asked for, or the boolean
 * result of a guess.
 */
@Singleton
class PasswordRepository @Inject constructor(
    private val dao: PasswordEntryDao,
    private val cryptoManager: CryptoManager,
    private val hasher: PasswordHasher
) {

    fun observeEntries(): Flow<List<PasswordEntry>> =
        dao.observeAll().map { entities -> entities.map(PasswordEntryEntity::toDomain) }

    fun observeCategories(): Flow<List<String>> = dao.observeCategories()

    fun observeEntry(id: Long): Flow<PasswordEntry?> =
        dao.observeById(id).map { it?.toDomain() }

    suspend fun getEntry(id: Long): PasswordEntry? = dao.getById(id)?.toDomain()

    /** Creates an entry whose password can be decrypted later. */
    suspend fun createEncrypted(draft: EntryDraft, password: String): Long {
        val payload = cryptoManager.encrypt(password)
        val now = System.currentTimeMillis()
        return dao.insert(
            draft.toEntity(
                mode = ProtectionMode.ENCRYPTED,
                createdAt = now,
                updatedAt = now
            ).copy(cipherText = payload.cipherText, iv = payload.iv)
        )
    }

    /** Creates an entry whose password is irrecoverable: only guesses can be verified. */
    suspend fun createHashed(draft: EntryDraft, password: String): Long {
        val hashed = hasher.hash(password)
        val now = System.currentTimeMillis()
        return dao.insert(
            draft.toEntity(
                mode = ProtectionMode.HASHED,
                createdAt = now,
                updatedAt = now
            ).copy(hash = hashed.hash, salt = hashed.salt)
        )
    }

    /**
     * Updates the metadata of an entry, and optionally its password.
     *
     * [newPassword] is only honoured for [ProtectionMode.ENCRYPTED] entries; a hashed password can
     * never be replaced, the entry has to be deleted and created again.
     */
    suspend fun update(id: Long, draft: EntryDraft, newPassword: String? = null) {
        val current = dao.getById(id) ?: return
        var updated = current.copy(
            serviceName = draft.serviceName.trim(),
            username = draft.username.normalise(),
            hint = draft.hint.normalise(),
            category = draft.category.normalise(),
            isFavorite = draft.isFavorite,
            updatedAt = System.currentTimeMillis()
        )
        if (newPassword != null && current.mode == ProtectionMode.ENCRYPTED) {
            val payload = cryptoManager.encrypt(newPassword)
            updated = updated.copy(cipherText = payload.cipherText, iv = payload.iv)
        }
        dao.update(updated)
    }

    suspend fun setFavorite(id: Long, isFavorite: Boolean) {
        dao.setFavorite(id, isFavorite, System.currentTimeMillis())
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    /**
     * Decrypts the password of an [ProtectionMode.ENCRYPTED] entry.
     *
     * @return null if the entry is hashed, missing, or the Keystore key is gone (e.g. after a
     * reinstall), in which case only a backup can restore it.
     */
    suspend fun revealPassword(id: Long): String? {
        val entity = dao.getById(id) ?: return null
        if (entity.mode != ProtectionMode.ENCRYPTED) return null
        val cipherText = entity.cipherText ?: return null
        val iv = entity.iv ?: return null
        return runCatching {
            cryptoManager.decrypt(EncryptedPayload(cipherText = cipherText, iv = iv))
        }.getOrNull()
    }

    /**
     * Checks a guess against a [ProtectionMode.HASHED] entry. Attempts are unlimited by design:
     * it is the user's own device and their own password.
     */
    suspend fun verifyGuess(id: Long, guess: String): Boolean {
        val entity = dao.getById(id) ?: return false
        if (entity.mode != ProtectionMode.HASHED) return false
        val hash = entity.hash ?: return false
        val salt = entity.salt ?: return false
        return hasher.verify(guess, HashedPassword(hash = hash, salt = salt))
    }
}

/** Optional fields arrive from the form as possibly blank strings; store them as null instead. */
private fun String?.normalise(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private fun EntryDraft.toEntity(
    mode: ProtectionMode,
    createdAt: Long,
    updatedAt: Long
) = PasswordEntryEntity(
    serviceName = serviceName.trim(),
    username = username.normalise(),
    hint = hint.normalise(),
    category = category.normalise(),
    isFavorite = isFavorite,
    mode = mode,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun PasswordEntryEntity.toDomain() = PasswordEntry(
    id = id,
    serviceName = serviceName,
    username = username,
    hint = hint,
    category = category,
    isFavorite = isFavorite,
    mode = mode,
    createdAt = createdAt,
    updatedAt = updatedAt
)
