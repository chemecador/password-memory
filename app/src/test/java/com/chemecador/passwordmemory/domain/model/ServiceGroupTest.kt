package com.chemecador.passwordmemory.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The list shows services, not entries, and nothing but the name says two entries belong to the
 * same one. Whatever the name is spelled like, both accounts have to end up behind a single row:
 * a second "x.com" row is the exact thing this grouping exists to avoid.
 */
class ServiceGroupTest {

    @Test
    fun `entries of the same service collapse into one group`() {
        val groups = listOf(
            entry(id = 1, service = "x.com", username = "first"),
            entry(id = 2, service = "  X.com ", username = "second"),
            entry(id = 3, service = "Gmail", username = "someone")
        ).groupByService()

        assertEquals(listOf("gmail", "x.com"), groups.map(ServiceGroup::key))
        assertEquals(listOf(1L, 2L), groups.last().entries.map(PasswordEntry::id))
    }

    @Test
    fun `the label keeps the spelling of the first account in order`() {
        val groups = listOf(
            entry(id = 1, service = " X.COM ", username = "zoe"),
            entry(id = 2, service = "x.com", username = "adam")
        ).groupByService()

        assertEquals("x.com", groups.single().serviceName)
    }

    @Test
    fun `a service with one account exposes it, one with several does not`() {
        val alone = listOf(entry(id = 1, service = "Gmail")).groupByService().single()
        val shared = listOf(
            entry(id = 1, service = "x.com", username = "a"),
            entry(id = 2, service = "x.com", username = "b")
        ).groupByService().single()

        assertEquals(1L, alone.single?.id)
        assertEquals(1, alone.size)
        assertNull(shared.single)
        assertEquals(2, shared.size)
    }

    @Test
    fun `a favourite account pulls its service to the top`() {
        val groups = listOf(
            entry(id = 1, service = "Amazon"),
            entry(id = 2, service = "x.com", username = "work"),
            entry(id = 3, service = "x.com", username = "personal", favorite = true)
        ).groupByService()

        assertEquals(listOf("x.com", "amazon"), groups.map(ServiceGroup::key))
        assertTrue(groups.first().isFavorite)
        // Favourites first inside the service too, so the starred account leads the accounts screen.
        assertEquals(listOf(3L, 2L), groups.first().entries.map(PasswordEntry::id))
    }

    @Test
    fun `accounts without a username sort last`() {
        val group = listOf(
            entry(id = 1, service = "x.com", username = null),
            entry(id = 2, service = "x.com", username = "beta"),
            entry(id = 3, service = "x.com", username = "Alpha")
        ).groupByService().single()

        assertEquals(listOf(3L, 2L, 1L), group.entries.map(PasswordEntry::id))
    }

    @Test
    fun `the row only claims a mode when every account shares it`() {
        val agreeing = listOf(
            entry(id = 1, service = "x.com", username = "a"),
            entry(id = 2, service = "x.com", username = "b")
        ).groupByService().single()
        val mixed = listOf(
            entry(id = 1, service = "x.com", username = "a"),
            entry(id = 2, service = "x.com", username = "b", mode = ProtectionMode.HASHED)
        ).groupByService().single()

        assertEquals(ProtectionMode.ENCRYPTED, agreeing.mode)
        assertNull(mixed.mode)
    }

    private fun entry(
        id: Long,
        service: String,
        username: String? = null,
        favorite: Boolean = false,
        mode: ProtectionMode = ProtectionMode.ENCRYPTED
    ) = PasswordEntry(
        id = id,
        serviceName = service,
        username = username,
        isFavorite = favorite,
        mode = mode
    )
}
