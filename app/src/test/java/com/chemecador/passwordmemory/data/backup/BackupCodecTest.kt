package com.chemecador.passwordmemory.data.backup

import com.chemecador.passwordmemory.domain.model.ProtectionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class BackupCodecTest {

    private val codec = BackupCodec(SecureRandom())

    private val encrypted = BackupEntry(
        serviceName = "Gmail",
        username = "someone@example.com",
        hint = "the usual one | with a separator",
        category = "Email",
        isFavorite = true,
        mode = ProtectionMode.ENCRYPTED,
        plainPassword = "s3cr3t-ñ-🔐",
        createdAt = 1_000,
        updatedAt = 2_000
    )

    private val hashed = BackupEntry(
        serviceName = "Old bank",
        username = null,
        hint = null,
        category = null,
        isFavorite = false,
        mode = ProtectionMode.HASHED,
        hash = ByteArray(32) { it.toByte() },
        salt = ByteArray(16) { (it * 3).toByte() },
        createdAt = 3_000,
        updatedAt = 4_000
    )

    @Test
    fun `entries survive a full encode-decode round trip`() {
        val bytes = codec.encode(listOf(encrypted, hashed), "passphrase".toCharArray())

        val restored = codec.decode(bytes, "passphrase".toCharArray())

        assertEquals(listOf(encrypted, hashed), restored)
    }

    @Test
    fun `an empty backup is still a valid file`() {
        val bytes = codec.encode(emptyList(), "pp".toCharArray())

        assertTrue(codec.decode(bytes, "pp".toCharArray()).isEmpty())
    }

    @Test
    fun `the password never appears in clear in the encoded file`() {
        val bytes = codec.encode(listOf(encrypted), "passphrase".toCharArray())

        val asText = bytes.toString(Charsets.ISO_8859_1)
        assertTrue(!asText.contains("s3cr3t"))
        assertTrue(!asText.contains("Gmail"))
    }

    @Test
    fun `each export uses a fresh salt and iv`() {
        val first = codec.encode(listOf(encrypted), "pp".toCharArray())
        val second = codec.encode(listOf(encrypted), "pp".toCharArray())

        assertTrue(!first.contentEquals(second))
    }

    @Test(expected = BackupFormatException::class)
    fun `decoding with the wrong passphrase fails`() {
        val bytes = codec.encode(listOf(encrypted), "right".toCharArray())

        codec.decode(bytes, "wrong".toCharArray())
    }

    @Test(expected = BackupFormatException::class)
    fun `decoding a foreign file fails`() {
        codec.decode("just some other file".toByteArray(), "pp".toCharArray())
    }

    @Test(expected = BackupFormatException::class)
    fun `decoding a truncated file fails`() {
        val bytes = codec.encode(listOf(encrypted), "pp".toCharArray())

        codec.decode(bytes.copyOfRange(0, bytes.size - 4), "pp".toCharArray())
    }

    @Test
    fun `hashed entries never carry a plaintext password`() {
        val bytes = codec.encode(listOf(hashed), "pp".toCharArray())

        assertNull(codec.decode(bytes, "pp".toCharArray()).single().plainPassword)
    }
}
