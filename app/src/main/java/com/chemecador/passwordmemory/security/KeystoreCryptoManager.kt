package com.chemecador.passwordmemory.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AES-256-GCM encryption backed by a key that lives in the Android Keystore.
 *
 * The key is created lazily the first time an entry is encrypted and is never exportable: it does
 * not survive a reinstall and does not migrate between devices.
 *
 * Using it also requires the user to have authenticated in the last [AUTH_WINDOW_SECONDS] seconds,
 * which is what makes the biometric prompt a cryptographic gate rather than a screen in front of
 * data that is readable anyway. Two consequences worth knowing:
 *
 * - A timeout-based key is unlocked by the keyguard, so `BIOMETRIC_WEAK` will not do — see
 *   `ALLOWED_AUTHENTICATORS`.
 * - Being timeout-based also means enrolling a new fingerprint does not invalidate it. That is the
 *   behaviour we want: losing every entry because the user added a finger would be far worse than
 *   the alternative.
 *
 * [LEGACY_KEY_ALIAS] is the unauthenticated key earlier versions used. It is kept only so entries
 * written back then can still be read while [CryptoKeyRotation] moves them across, and is deleted
 * as soon as nothing needs it.
 */
@Singleton
class KeystoreCryptoManager @Inject constructor() : CryptoManager {

    override fun encrypt(plainText: String): EncryptedPayload {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        authenticating { cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey()) }
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(cipherText = cipherText, iv = cipher.iv)
    }

    /**
     * Decrypts with the current key, falling back to the superseded one. GCM authenticates the
     * ciphertext, so a payload written with the other key fails its tag check rather than coming
     * back as plausible nonsense — which is what makes trying both safe.
     */
    override fun decrypt(payload: EncryptedPayload): String = try {
        decryptWith(getOrCreateKey(), payload)
    } catch (authRequired: AuthenticationRequiredException) {
        // Never read around a closed window: the caller is meant to authenticate and come back.
        throw authRequired
    } catch (currentKeyFailed: Exception) {
        val legacy = existingKey(LEGACY_KEY_ALIAS) ?: throw currentKeyFailed
        decryptWith(legacy, payload)
    }

    override fun hasLegacyKey(): Boolean = existingKey(LEGACY_KEY_ALIAS) != null

    override fun discardLegacyKey() {
        keyStore().deleteEntry(LEGACY_KEY_ALIAS)
    }

    private fun decryptWith(key: SecretKey, payload: EncryptedPayload): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        authenticating {
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, payload.iv))
        }
        return cipher.doFinal(payload.cipherText).toString(Charsets.UTF_8)
    }

    /** Restates the platform's "you have not authenticated recently" as something callers can act on. */
    private inline fun <T> authenticating(block: () -> T): T = try {
        block()
    } catch (notAuthenticated: UserNotAuthenticatedException) {
        throw AuthenticationRequiredException(notAuthenticated)
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun existingKey(alias: String): SecretKey? =
        (keyStore().getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey

    private fun getOrCreateKey(): SecretKey =
        existingKey(KEY_ALIAS) ?: synchronized(KEY_CREATION_LOCK) {
            existingKey(KEY_ALIAS) ?: KeyGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                .apply { init(keySpec()) }
                .generateKey()
        }

    private fun keySpec(): KeyGenParameterSpec {
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                AUTH_WINDOW_SECONDS,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION") // The parameters overload needs API 30, this app starts at 26.
            builder.setUserAuthenticationValidityDurationSeconds(AUTH_WINDOW_SECONDS)
        }
        return builder.build()
    }

    companion object {
        /** How long the key stays usable after an authentication. */
        const val AUTH_WINDOW_SECONDS = 30

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "password_memory_entry_key_v2"
        private const val LEGACY_KEY_ALIAS = "password_memory_entry_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val TAG_LENGTH_BITS = 128
        private val KEY_CREATION_LOCK = Any()
    }
}
