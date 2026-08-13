package com.chemecador.passwordmemory.data.repository

import com.chemecador.passwordmemory.data.backup.BackupEntry
import com.chemecador.passwordmemory.data.importer.EntryKey
import com.chemecador.passwordmemory.domain.model.ProtectionMode
import com.chemecador.passwordmemory.security.AuthenticationRequiredException
import com.chemecador.passwordmemory.security.PasswordHasher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.GeneralSecurityException
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
    fun `an import recognises a saved brand behind its domain`() = runTest {
        repository.createEncrypted(
            EntryDraft(serviceName = "Gmail", username = "someone@example.com"),
            "s3cr3t"
        )

        // What a Google export calls the same login, spelled the way Chrome writes it.
        assertTrue(
            EntryKey.of("https://gmail.com", "someone@example.com") in repository.existingKeys()
        )
    }

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

        assertEquals("s3cr3t", repository.revealed(id))
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

        assertNull(repository.revealed(id))
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

        assertEquals("new", repository.revealed(id))
        assertEquals("Gmail work", repository.getEntry(id)!!.serviceName)
    }

    @Test
    fun `updating with a null password keeps the current one`() = runTest {
        val id = repository.createEncrypted(draft, "old")

        repository.update(id, draft.copy(category = "Work"), newPassword = null)

        assertEquals("old", repository.revealed(id))
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
            assertEquals("s3cr3t", repository.revealed(id))
        }

    @Test
    fun `saving the same login twice replaces the password instead of duplicating it`() = runTest {
        val first = repository.saveCaptured("github.com", "SomeOne@example.com", "old", null)

        val second = repository.saveCaptured("GitHub.com", "someone@example.com", "new", null)

        assertEquals(first, second)
        assertEquals("new", repository.revealed(first))
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
        assertEquals("s3cr3t", repository.revealed(captured))
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
    fun `a pasted url is stored under the name of the service`() = runTest {
        val id = repository.createEncrypted(
            draft.copy(serviceName = " https://www.netflix.com "),
            "pw"
        )

        assertEquals("netflix.com", repository.getEntry(id)!!.serviceName)

        repository.update(id, draft.copy(serviceName = "HTTP://netflix.com"))

        assertEquals("netflix.com", repository.getEntry(id)!!.serviceName)
    }

    @Test
    fun `a captured credential finds the entry it belongs to through the prefix`() = runTest {
        val first = repository.saveCaptured("github.com", "someone", "old", null)

        val second = repository.saveCaptured("https://www.github.com", "someone", "new", null)

        assertEquals(first, second)
        assertEquals(1, repository.getEntries().size)
        assertEquals("new", repository.revealed(first))
    }

    @Test
    fun `a captured credential does not replace the password of a merely similar entry`() = runTest {
        // The list shows `GitHub` and `github.com` as one service, but replacing a password is
        // silent and irreversible, so autofill wants the same name and not just the same service.
        val byBrand = repository.createEncrypted(
            EntryDraft(serviceName = "GitHub", username = "someone"),
            "the one that must survive"
        )

        val captured = repository.saveCaptured("github.com", "someone", "s3cr3t", null)

        assertNotEquals(byBrand, captured)
        assertEquals("the one that must survive", repository.revealed(byBrand))
        assertEquals("s3cr3t", repository.revealed(captured))
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
    fun `export fails instead of skipping entries whose key is gone`() = runTest {
        repository.createEncrypted(draft, "s3cr3t")
        repository.createHashed(draft.copy(serviceName = "Bank"), "other")
        crypto.simulateLostKey()

        assertThrows(GeneralSecurityException::class.java) {
            runBlocking { repository.exportEntries() }
        }
    }

    @Test
    fun `export fails when an encrypted entry has incomplete cryptographic material`() = runTest {
        val id = repository.createEncrypted(draft, "s3cr3t")
        val valid = dao.getById(id)!!

        listOf(valid.copy(cipherText = null), valid.copy(iv = null)).forEach { incomplete ->
            dao.update(incomplete)
            assertThrows(IllegalStateException::class.java) {
                runBlocking { repository.exportEntries() }
            }
        }
    }

    @Test
    fun `export fails when a hashed entry has incomplete cryptographic material`() = runTest {
        val id = repository.createHashed(draft, "s3cr3t")
        val valid = dao.getById(id)!!

        listOf(valid.copy(hash = null), valid.copy(salt = null)).forEach { incomplete ->
            dao.update(incomplete)
            assertThrows(IllegalStateException::class.java) {
                runBlocking { repository.exportEntries() }
            }
        }
    }

    @Test
    fun `revealing fails gracefully when the key is gone`() = runTest {
        val id = repository.createEncrypted(draft, "s3cr3t")
        crypto.simulateLostKey()

        assertEquals(RevealResult.Unavailable, repository.revealPassword(id))
    }

    @Test
    fun `a closed authentication window is told apart from a lost key`() = runTest {
        val id = repository.createEncrypted(draft, "s3cr3t")
        crypto.simulateClosedAuthWindow()

        assertEquals(RevealResult.AuthenticationRequired, repository.revealPassword(id))
    }

    @Test
    fun `export refuses to write a backup that would be missing entries`() = runTest {
        repository.createEncrypted(draft, "s3cr3t")
        crypto.simulateClosedAuthWindow()

        assertThrows(AuthenticationRequiredException::class.java) {
            runBlocking { repository.exportEntries() }
        }
    }

    @Test
    fun `rotation re-encrypts every entry and drops the old key`() = runTest {
        repository.createEncrypted(draft, "s3cr3t")
        repository.createEncrypted(draft.copy(serviceName = "Bank"), "other")
        repository.createHashed(draft.copy(serviceName = "Wifi"), "irrelevant")
        crypto.simulateLegacyKey()
        val before = crypto.encryptions

        assertTrue(repository.rotateEncryptionKey())

        // Only the two encrypted entries are touched; the hashed one has no key to move.
        assertEquals(2, crypto.encryptions - before)
        assertTrue(crypto.legacyKeyDiscarded)
        assertEquals("s3cr3t", repository.revealed(1))
    }

    @Test
    fun `rotation without an old key does nothing at all`() = runTest {
        repository.createEncrypted(draft, "s3cr3t")
        val before = crypto.encryptions

        assertTrue(repository.rotateEncryptionKey())

        assertEquals(before, crypto.encryptions)
        assertFalse(crypto.legacyKeyDiscarded)
    }

    @Test
    fun `rotation keeps the old key when an entry cannot be moved`() = runTest {
        repository.createEncrypted(draft, "s3cr3t")
        crypto.simulateLegacyKey()
        crypto.simulateLostKey()

        assertFalse(repository.rotateEncryptionKey())

        assertFalse(crypto.legacyKeyDiscarded)
    }

    /** The reveal of a decryptable entry, or null for every other outcome. */
    private suspend fun PasswordRepository.revealed(id: Long): String? =
        (revealPassword(id) as? RevealResult.Success)?.password

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
        assertEquals("s3cr3t", repository.revealed(encrypted.id))
        val hashed = entries.single { it.mode == ProtectionMode.HASHED }
        assertTrue(repository.verifyGuess(hashed.id, "other"))
    }

    @Test
    fun `imported entries are stored under the same cleaned name as typed ones`() = runTest {
        val imported = BackupEntry(
            serviceName = "  https://www.netflix.com  ",
            username = "someone@example.com",
            hint = null,
            category = null,
            isFavorite = false,
            mode = ProtectionMode.ENCRYPTED,
            plainPassword = "s3cr3t",
            createdAt = 1,
            updatedAt = 1
        )

        repository.importEntries(listOf(imported))

        assertEquals("netflix.com", dao.getAll().single().serviceName)
        // And so the row it would have duplicated is now recognised as its own.
        assertTrue(EntryKey.of("Netflix.com", "someone@example.com") in repository.existingKeys())
    }

    @Test
    fun `import is atomic when an encrypted backup entry is malformed`() = runTest {
        val valid = BackupEntry(
            serviceName = "Mail",
            username = "someone@example.com",
            hint = null,
            category = "Personal",
            isFavorite = false,
            mode = ProtectionMode.ENCRYPTED,
            plainPassword = "correct horse battery staple",
            createdAt = 1,
            updatedAt = 1
        )
        val malformed = valid.copy(serviceName = "Broken", plainPassword = null)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.importEntries(listOf(valid, malformed)) }
        }
        assertTrue(dao.getAll().isEmpty())
    }

    @Test
    fun `import is atomic when hashed material has an invalid size`() = runTest {
        val valid = BackupEntry(
            serviceName = "Archive",
            username = null,
            hint = null,
            category = null,
            isFavorite = false,
            mode = ProtectionMode.HASHED,
            hash = ByteArray(32),
            salt = ByteArray(16),
            createdAt = 1,
            updatedAt = 1
        )
        val malformed = valid.copy(serviceName = "Broken", hash = ByteArray(31))

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.importEntries(listOf(valid, malformed)) }
        }
        assertTrue(dao.getAll().isEmpty())
    }
}
