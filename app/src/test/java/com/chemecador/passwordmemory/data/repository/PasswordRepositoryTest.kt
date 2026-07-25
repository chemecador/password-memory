package com.chemecador.passwordmemory.data.repository

import com.chemecador.passwordmemory.data.importer.EntryKey
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
    fun `existing keys ignore case and blank usernames`() = runTest {
        repository.createEncrypted(draft.copy(serviceName = "GMail"), "s3cr3t")
        repository.createEncrypted(draft.copy(serviceName = "Bank", username = " "), "s3cr3t")

        val keys = repository.existingKeys()

        assertTrue(EntryKey.of("gmail", "SOMEONE@example.com") in keys)
        assertTrue(EntryKey.of("Bank", null) in keys)
    }

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
    fun `a credential caught by autofill is stored encrypted, with its origin as the hint`() =
        runTest {
            val id = repository.saveCaptured(
                serviceName = "github.com",
                username = "someone@example.com",
                password = "s3cr3t",
                origin = "https://github.com"
            )

            val entry = repository.getEntry(id)!!
            assertEquals(ProtectionMode.ENCRYPTED, entry.mode)
            assertEquals("https://github.com", entry.hint)
            assertEquals("s3cr3t", repository.revealPassword(id))
        }

    @Test
    fun `saving the same login twice replaces the password instead of duplicating it`() = runTest {
        val first = repository.saveCaptured("github.com", "SomeOne@example.com", "old", null)

        val second = repository.saveCaptured("GitHub.com", "someone@example.com", "new", null)

        assertEquals(first, second)
        assertEquals("new", repository.revealPassword(first))
        assertEquals(1, repository.getEntries().size)
    }

    @Test
    fun `a captured credential never overwrites a hashed entry`() = runTest {
        val hashed = repository.createHashed(
            EntryDraft(serviceName = "Bank", username = "me"),
            "original"
        )

        val captured = repository.saveCaptured("Bank", "me", "s3cr3t", null)

        assertTrue(repository.verifyGuess(hashed, "original"))
        assertEquals("s3cr3t", repository.revealPassword(captured))
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
    fun `export decrypts encrypted entries and passes hashed ones through`() = runTest {
        repository.createEncrypted(draft, "s3cr3t")
        repository.createHashed(draft.copy(serviceName = "Bank"), "other")

        val exported = repository.exportEntries()

        val encrypted = exported.single { it.mode == ProtectionMode.ENCRYPTED }
        assertEquals("s3cr3t", encrypted.plainPassword)
        val hashed = exported.single { it.mode == ProtectionMode.HASHED }
        assertNull(hashed.plainPassword)
        assertEquals(32, hashed.hash!!.size)
    }

    @Test
    fun `entries whose key is gone are skipped on export`() = runTest {
        repository.createEncrypted(draft, "s3cr3t")
        repository.createHashed(draft.copy(serviceName = "Bank"), "other")
        crypto.simulateLostKey()

        val exported = repository.exportEntries()

        assertEquals(listOf(ProtectionMode.HASHED), exported.map { it.mode })
    }

    @Test
    fun `revealing fails gracefully when the key is gone`() = runTest {
        val id = repository.createEncrypted(draft, "s3cr3t")
        crypto.simulateLostKey()

        assertNull(repository.revealPassword(id))
    }

    @Test
    fun `import restores both modes`() = runTest {
        repository.createEncrypted(draft, "s3cr3t")
        val hashedId = repository.createHashed(draft.copy(serviceName = "Bank"), "other")
        val exported = repository.exportEntries()
        repository.delete(hashedId)
        repository.observeEntries().first().forEach { repository.delete(it.id) }

        val restored = repository.importEntries(exported)

        assertEquals(2, restored)
        val entries = repository.observeEntries().first()
        val encrypted = entries.single { it.mode == ProtectionMode.ENCRYPTED }
        assertEquals("s3cr3t", repository.revealPassword(encrypted.id))
        val hashed = entries.single { it.mode == ProtectionMode.HASHED }
        assertTrue(repository.verifyGuess(hashed.id, "other"))
    }
}
