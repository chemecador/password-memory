package com.chemecador.passwordmemory.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class PasswordHasherTest {

    private val hasher = PasswordHasher(SecureRandom())

    @Test
    fun `verify accepts the original password`() {
        val hashed = hasher.hash("correct horse battery staple")

        assertTrue(hasher.verify("correct horse battery staple", hashed))
    }

    @Test
    fun `verify rejects a wrong password`() {
        val hashed = hasher.hash("correct horse battery staple")

        assertFalse(hasher.verify("correct horse battery stapl", hashed))
        assertFalse(hasher.verify("", hashed))
        assertFalse(hasher.verify("CORRECT HORSE BATTERY STAPLE", hashed))
    }

    @Test
    fun `salt is random, so the same password hashes differently every time`() {
        val first = hasher.hash("same-password")
        val second = hasher.hash("same-password")

        assertNotEquals(first.salt.toList(), second.salt.toList())
        assertNotEquals(first.hash.toList(), second.hash.toList())
    }

    @Test
    fun `salt is at least 16 bytes and the digest is SHA-256 sized`() {
        val hashed = hasher.hash("whatever")

        assertTrue(hashed.salt.size >= 16)
        assertEquals(32, hashed.hash.size)
    }

    @Test
    fun `unicode passwords round-trip`() {
        val password = "contraseña-ñÑ-日本語-🔐"
        val hashed = hasher.hash(password)

        assertTrue(hasher.verify(password, hashed))
    }
}
