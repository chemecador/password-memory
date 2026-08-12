package com.chemecador.passwordmemory.data.repository

import com.chemecador.passwordmemory.data.backup.BackupEntry
import com.chemecador.passwordmemory.data.db.PasswordEntryDao
import com.chemecador.passwordmemory.data.db.PasswordEntryEntity
import com.chemecador.passwordmemory.data.importer.EntryKey
import com.chemecador.passwordmemory.domain.model.PasswordEntry
import com.chemecador.passwordmemory.domain.model.ProtectionMode
import com.chemecador.passwordmemory.domain.model.ServiceName
import com.chemecador.passwordmemory.security.AuthenticationRequiredException
import com.chemecador.passwordmemory.security.CryptoManager
import com.chemecador.passwordmemory.security.EncryptedPayload
import com.chemecador.passwordmemory.security.HashedPassword
import com.chemecador.passwordmemory.security.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

/** What came of trying to read back an encrypted password. */
sealed interface RevealResult {

    data class Success(val password: String) : RevealResult

    /** The Keystore window has closed. Authenticate and ask again; nothing is lost. */
    data object AuthenticationRequired : RevealResult

    /**
     * The entry is hashed or missing, or the Keystore key is gone (e.g. after a reinstall), in
     * which case only a backup can bring the password back.
     */
    data object Unavailable : RevealResult
}

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

    /** One-shot snapshot, for callers that cannot collect a flow (the autofill service). */
    suspend fun getEntries(): List<PasswordEntry> = dao.getAll().map(PasswordEntryEntity::toDomain)

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
            serviceName = ServiceName.clean(draft.serviceName),
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

    /**
     * Stores a credential the autofill service caught being typed somewhere else. If an encrypted
     * entry for the same service and username is already here, its password is replaced instead of
     * piling up a second copy; [origin] is the site or app it came from, kept in the hint so the
     * entry can be recognised the next time that form shows up.
     */
    suspend fun saveCaptured(
        serviceName: String,
        username: String?,
        password: String,
        origin: String?
    ): Long {
        val key = EntryKey.of(serviceName, username)
        val existing = dao.getAll().firstOrNull {
            it.mode == ProtectionMode.ENCRYPTED && EntryKey.of(it.serviceName, it.username) == key
        }
        if (existing == null) {
            return createEncrypted(
                EntryDraft(serviceName = serviceName, username = username, hint = origin),
                password
            )
        }
        val payload = cryptoManager.encrypt(password)
        dao.update(
            existing.copy(
                cipherText = payload.cipherText,
                iv = payload.iv,
                hint = existing.hint ?: origin,
                updatedAt = System.currentTimeMillis()
            )
        )
        return existing.id
    }

    suspend fun setFavorite(id: Long, isFavorite: Boolean) {
        dao.setFavorite(id, isFavorite, System.currentTimeMillis())
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    /** Decrypts the password of an [ProtectionMode.ENCRYPTED] entry. */
    suspend fun revealPassword(id: Long): RevealResult {
        val entity = dao.getById(id) ?: return RevealResult.Unavailable
        if (entity.mode != ProtectionMode.ENCRYPTED) return RevealResult.Unavailable
        val cipherText = entity.cipherText ?: return RevealResult.Unavailable
        val iv = entity.iv ?: return RevealResult.Unavailable
        return try {
            RevealResult.Success(
                cryptoManager.decrypt(EncryptedPayload(cipherText = cipherText, iv = iv))
            )
        } catch (_: AuthenticationRequiredException) {
            RevealResult.AuthenticationRequired
        } catch (_: GeneralSecurityException) {
            RevealResult.Unavailable
        }
    }

    /**
     * Moves every encrypted entry onto the current Keystore key and drops the superseded one.
     *
     * Runs after an unlock, so the key is inside its authentication window. Interrupting it is
     * harmless: decryption falls back to the old key, and whatever is left is picked up next time.
     *
     * @return true when nothing is on the old key any more.
     */
    suspend fun rotateEncryptionKey(): Boolean {
        if (!cryptoManager.hasLegacyKey()) return true
        var complete = true
        dao.getAll()
            .filter { it.mode == ProtectionMode.ENCRYPTED }
            .forEach { entity ->
                val cipherText = entity.cipherText
                val iv = entity.iv
                if (cipherText == null || iv == null) return@forEach
                val plain = try {
                    cryptoManager.decrypt(EncryptedPayload(cipherText = cipherText, iv = iv))
                } catch (authRequired: AuthenticationRequiredException) {
                    // Out of window: stop rather than churn through every entry failing. What has
                    // already moved stays moved, and the rest goes on the next unlock.
                    throw authRequired
                } catch (_: GeneralSecurityException) {
                    // Unreadable with either key: the old key is keeping no secrets for this one.
                    complete = false
                    return@forEach
                }
                val payload = cryptoManager.encrypt(plain)
                // updatedAt is untouched: re-encrypting is not an edit the user made.
                dao.update(entity.copy(cipherText = payload.cipherText, iv = payload.iv))
            }
        if (complete) cryptoManager.discardLegacyKey()
        return complete
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

    /**
     * Snapshot of every entry for a backup. Encrypted entries are decrypted here, since the
     * Keystore key cannot be exported.
     *
     * This operation is all-or-nothing. Missing cryptographic fields and every decryption failure
     * abort the snapshot, including [AuthenticationRequiredException]. Silently writing a backup
     * that is missing entries is how someone finds out their safety net had holes only when they
     * need it.
     */
    suspend fun exportEntries(): List<BackupEntry> = dao.getAll().map { entity ->
        when (entity.mode) {
            ProtectionMode.ENCRYPTED -> {
                val cipherText = checkNotNull(entity.cipherText) {
                    "Encrypted entry ${entity.id} has no ciphertext"
                }
                val iv = checkNotNull(entity.iv) {
                    "Encrypted entry ${entity.id} has no IV"
                }
                val plain = cryptoManager.decrypt(
                    EncryptedPayload(cipherText = cipherText, iv = iv)
                )
                entity.toBackupEntry(plainPassword = plain)
            }

            ProtectionMode.HASHED -> {
                checkNotNull(entity.hash) { "Hashed entry ${entity.id} has no hash" }
                checkNotNull(entity.salt) { "Hashed entry ${entity.id} has no salt" }
                entity.toBackupEntry()
            }
        }
    }

    /** Identity of every stored entry, so an import can tell apart what is already here. */
    suspend fun existingKeys(): Set<EntryKey> =
        dao.getAll().mapTo(mutableSetOf()) { EntryKey.of(it.serviceName, it.username) }

    /**
     * Restores backed up entries, re-encrypting the recoverable ones with this device's key.
     *
     * Every entry is validated and transformed before Room receives anything. A malformed record
     * therefore aborts the whole import instead of quietly disappearing from the restored vault.
     */
    suspend fun importEntries(entries: List<BackupEntry>): Int {
        val restored = entries.map { entry ->
            require(entry.serviceName.isNotBlank()) { "Backup entry has no service name" }
            val base = PasswordEntryEntity(
                serviceName = ServiceName.clean(entry.serviceName),
                username = entry.username,
                hint = entry.hint,
                category = entry.category,
                isFavorite = entry.isFavorite,
                mode = entry.mode,
                createdAt = entry.createdAt,
                updatedAt = entry.updatedAt
            )
            when (entry.mode) {
                ProtectionMode.ENCRYPTED -> {
                    val plain = requireNotNull(entry.plainPassword) {
                        "Encrypted backup entry has no password"
                    }
                    require(entry.hash == null && entry.salt == null) {
                        "Encrypted backup entry contains hashed material"
                    }
                    val payload = cryptoManager.encrypt(plain)
                    base.copy(cipherText = payload.cipherText, iv = payload.iv)
                }

                ProtectionMode.HASHED -> {
                    require(entry.plainPassword == null) {
                        "Hashed backup entry contains a plaintext password"
                    }
                    val hash = requireNotNull(entry.hash) {
                        "Hashed backup entry has no hash"
                    }
                    val salt = requireNotNull(entry.salt) {
                        "Hashed backup entry has no salt"
                    }
                    require(hash.size == SHA_256_HASH_SIZE_BYTES) {
                        "Hashed backup entry has an invalid hash"
                    }
                    require(salt.size >= MIN_HASH_SALT_SIZE_BYTES) {
                        "Hashed backup entry has an invalid salt"
                    }
                    base.copy(hash = hash, salt = salt)
                }
            }
        }
        dao.insertAll(restored)
        return restored.size
    }

    private companion object {
        const val SHA_256_HASH_SIZE_BYTES = 32
        const val MIN_HASH_SALT_SIZE_BYTES = 16
    }
}

private fun PasswordEntryEntity.toBackupEntry(plainPassword: String? = null) = BackupEntry(
    serviceName = serviceName,
    username = username,
    hint = hint,
    category = category,
    isFavorite = isFavorite,
    mode = mode,
    plainPassword = plainPassword,
    hash = hash,
    salt = salt,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/** Optional fields arrive from the form as possibly blank strings; store them as null instead. */
private fun String?.normalise(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private fun EntryDraft.toEntity(
    mode: ProtectionMode,
    createdAt: Long,
    updatedAt: Long
) = PasswordEntryEntity(
    serviceName = ServiceName.clean(serviceName),
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
