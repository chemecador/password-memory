package com.chemecador.passwordmemory.data.backup

import com.chemecador.passwordmemory.domain.model.ProtectionMode
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/** The backup file could not be read: wrong passphrase, wrong file, or corrupted content. */
class BackupFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Serialises entries into a self-contained encrypted backup file and back.
 *
 * File layout: `magic | salt (16 B) | iv (12 B) | AES-256-GCM ciphertext`. The key is derived from
 * a one-time passphrase with PBKDF2-HMAC-SHA256; the passphrase is never stored anywhere.
 *
 * Fields are Base64-encoded inside the payload so no value ever needs escaping.
 */
@Singleton
class BackupCodec @Inject constructor(
    private val secureRandom: SecureRandom
) {

    fun encode(entries: List<BackupEntry>, passphrase: CharArray): ByteArray {
        val salt = ByteArray(SALT_SIZE_BYTES).also(secureRandom::nextBytes)
        val iv = ByteArray(IV_SIZE_BYTES).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(TAG_BITS, iv))
        }
        val cipherText = cipher.doFinal(serialise(entries).toByteArray(Charsets.UTF_8))
        return MAGIC + salt + iv + cipherText
    }

    fun decode(bytes: ByteArray, passphrase: CharArray): List<BackupEntry> {
        val headerSize = MAGIC.size + SALT_SIZE_BYTES + IV_SIZE_BYTES
        if (bytes.size <= headerSize || !bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw BackupFormatException("Not a Password Memory backup file")
        }
        val salt = bytes.copyOfRange(MAGIC.size, MAGIC.size + SALT_SIZE_BYTES)
        val iv = bytes.copyOfRange(MAGIC.size + SALT_SIZE_BYTES, headerSize)
        val cipherText = bytes.copyOfRange(headerSize, bytes.size)

        val plain = try {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    deriveKey(passphrase, salt),
                    GCMParameterSpec(TAG_BITS, iv)
                )
            }
            cipher.doFinal(cipherText).toString(Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            throw BackupFormatException("Wrong passphrase or corrupted file", e)
        }
        return deserialise(plain)
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        val spec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEY_SIZE_BITS)
        return try {
            SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun serialise(entries: List<BackupEntry>): String = buildString {
        append(HEADER_LINE).append('\n')
        entries.forEach { entry ->
            val secondary = if (entry.mode == ProtectionMode.HASHED) entry.salt else null
            val primary = when (entry.mode) {
                ProtectionMode.ENCRYPTED -> entry.plainPassword?.toByteArray(Charsets.UTF_8)
                ProtectionMode.HASHED -> entry.hash
            }
            append(
                listOf(
                    encodeField(entry.serviceName.toByteArray(Charsets.UTF_8)),
                    encodeField(entry.username?.toByteArray(Charsets.UTF_8)),
                    encodeField(entry.hint?.toByteArray(Charsets.UTF_8)),
                    encodeField(entry.category?.toByteArray(Charsets.UTF_8)),
                    entry.isFavorite.toString(),
                    entry.mode.name,
                    encodeField(primary),
                    encodeField(secondary),
                    entry.createdAt.toString(),
                    entry.updatedAt.toString()
                ).joinToString(separator = FIELD_SEPARATOR)
            )
            append('\n')
        }
    }

    private fun deserialise(payload: String): List<BackupEntry> {
        val lines = payload.lines().filter(String::isNotBlank)
        if (lines.firstOrNull() != HEADER_LINE) {
            throw BackupFormatException("Unsupported backup version")
        }
        return lines.drop(1).map { line ->
            val fields = line.split(FIELD_SEPARATOR)
            if (fields.size != FIELD_COUNT) {
                throw BackupFormatException("Malformed backup entry")
            }
            try {
                val mode = ProtectionMode.valueOf(fields[5])
                val primary = decodeField(fields[6])
                BackupEntry(
                    serviceName = decodeField(fields[0])!!.toString(Charsets.UTF_8),
                    username = decodeField(fields[1])?.toString(Charsets.UTF_8),
                    hint = decodeField(fields[2])?.toString(Charsets.UTF_8),
                    category = decodeField(fields[3])?.toString(Charsets.UTF_8),
                    isFavorite = fields[4].toBooleanStrict(),
                    mode = mode,
                    plainPassword = primary
                        ?.takeIf { mode == ProtectionMode.ENCRYPTED }
                        ?.toString(Charsets.UTF_8),
                    hash = primary?.takeIf { mode == ProtectionMode.HASHED },
                    salt = decodeField(fields[7]),
                    createdAt = fields[8].toLong(),
                    updatedAt = fields[9].toLong()
                )
            } catch (e: IllegalArgumentException) {
                throw BackupFormatException("Malformed backup entry", e)
            } catch (e: NullPointerException) {
                throw BackupFormatException("Malformed backup entry", e)
            }
        }
    }

    private fun encodeField(value: ByteArray?): String =
        value?.let { Base64.getEncoder().encodeToString(it) } ?: ""

    private fun decodeField(value: String): ByteArray? =
        value.takeIf(String::isNotEmpty)?.let { Base64.getDecoder().decode(it) }

    private companion object {
        val MAGIC = "PMBK1".toByteArray(Charsets.US_ASCII)
        const val HEADER_LINE = "password-memory-backup;v1"
        const val FIELD_SEPARATOR = "|"
        const val FIELD_COUNT = 10
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
        const val PBKDF2_ITERATIONS = 210_000
        const val KEY_SIZE_BITS = 256
        const val SALT_SIZE_BYTES = 16
        const val IV_SIZE_BYTES = 12
        const val TAG_BITS = 128
    }
}
