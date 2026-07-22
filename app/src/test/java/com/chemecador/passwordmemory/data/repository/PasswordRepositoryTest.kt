package com.chemecador.passwordmemory.data.repository

import com.chemecador.passwordmemory.domain.model.ProtectionMode
import com.chemecador.passwordmemory.security.PasswordHasher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class PasswordRepositoryTest {

    private val dao = FakePasswordEntryDao()
    private val crypto = FakeCryptoManager()
    private val repository = PasswordRepository(dao, crypto, PasswordHasher(SecureRandom()))

    private val draft = EntryDraft(
        serviceName = "Gmail",
        username = "someone@example.com",
        hint = "the usual one",
        category = "Email"
    )

    @Test
    fun `an encrypted entry can be revealed`() = runTest {
        val id = repository.createEncrypted(draft, "s3cr3t")

        assertEquals("s3cr3t", repository.revealPassword(id))
    }

    @Test
    fun `the plaintext of an encrypted entry is never stored`() = runTest {
        val id = repository.createEncrypted(draft, "s3cr3t")

        val stored = dao.getById(id)!!
        assertFalse(stored.cipherText!!.toString(Charsets.UTF_8).contains("s3cr3t"))
        assertNull(stored.hash)
    }

    @Test
    fun `a hashed entry cannot be revealed, only guessed`() = runTest {
        val id = repository.createHashed(draft, "s3cr3t")

        assertNull(repository.revealPassword(id))
        assertTrue(repository.verifyGuess(id, "s3cr3t"))
        assertFalse(repository.verifyGuess(id, "wrong"))
    }

    @Test
    fun `a hashed entry stores neither ciphertext nor plaintext`() = runTest {
        val id = repository.createHashed(draft, "s3cr3t")

        val stored = dao.getById(id)!!
        assertNull(stored.cipherText)
        assertNull(stored.iv)
        assertEquals(32, stored.hash!!.size)
        assertEquals(16, stored.salt!!.size)
    }

    @Test
    fun `guessing an encrypted entry is not supported`() = runTest {
        val id = repository.createEncrypted(draft, "s3cr3t")

        assertFalse(repository.verifyGuess(id, "s3cr3t"))
    }

    @Test
    fun `updating an encrypted entry replaces its password`() = runTest {
        val id = repository.createEncrypted(draft, "old")

        repository.update(id, draft.copy(serviceName = "Gmail work"), newPassword = "new")

        assertEquals("new", repository.revealPassword(id))
        assertEquals("Gmail work", repository.getEntry(id)!!.serviceName)
    }

    @Test
    fun `updating with a null password keeps the current one`() = runTest {
        val id = repository.createEncrypted(draft, "old")

        repository.update(id, draft.copy(category = "Work"), newPassword = null)

        assertEquals("old", repository.revealPassword(id))
        assertEquals("Work", repository.getEntry(id)!!.category)
    }

    @Test
    fun `a hashed password cannot be replaced by an update`() = runTest {
        val id = repository.createHashed(draft, "original")

        repository.update(id, draft, newPassword = "replacement")

        assertTrue(repository.verifyGuess(id, "original"))
        assertFalse(repository.verifyGuess(id, "replacement"))
    }

    @Test
    fun `blank optional fields are normalised to null`() = runTest {
        val id = repository.createEncrypted(
            EntryDraft(serviceName = "  Netflix  ", username = "   ", hint = "", category = null),
            "pw"
        )

        val entry = repository.getEntry(id)!!
        assertEquals("Netflix", entry.serviceName)
        assertNull(entry.username)
        assertNull(entry.hint)
        assertNull(entry.category)
    }

    @Test
    fun `entries are exposed without any cryptographic material`() = runTest {
        repository.createEncrypted(draft, "pw")
        repository.createHashed(draft.copy(serviceName = "Bank"), "pw")

        val entries = repository.observeEntries().first()

        assertEquals(setOf("Bank", "Gmail"), entries.map { it.serviceName }.toSet())
        assertEquals(
            setOf(ProtectionMode.ENCRYPTED, ProtectionMode.HASHED),
            entries.map { it.mode }.toSet()
        )
    }

    @Test
    fun `favorites are listed first`() = runTest {
        repository.createEncrypted(draft.copy(serviceName = "Aaa"), "pw")
        val favoriteId = repository.createEncrypted(draft.copy(serviceName = "Zzz"), "pw")

        repository.setFavorite(favoriteId, true)

        assertEquals("Zzz", repository.observeEntries().first().first().serviceName)
    }

    @Test
    fun `deleting removes the entry and its secrets`() = runTest {
        val id = repository.createEncrypted(draft, "pw")

        repository.delete(id)

        assertNull(repository.getEntry(id))
        assertNull(dao.getById(id))
    }

    @Test
    fun `revealing fails gracefully when the key is gone`() = runTest {
        val id = repository.createEncrypted(draft, "s3cr3t")
        crypto.simulateLostKey()

        assertNull(repository.revealPassword(id))
    }
}
