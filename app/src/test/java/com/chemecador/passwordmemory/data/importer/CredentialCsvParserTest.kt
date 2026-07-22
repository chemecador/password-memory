package com.chemecador.passwordmemory.data.importer

import com.chemecador.passwordmemory.domain.model.ProtectionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialCsvParserTest {

    private val parser = CredentialCsvParser()

    private fun parse(csv: String) = parser.parse(csv.toByteArray(Charsets.UTF_8), now = 1_000)

    @Test
    fun `a Google Password Manager export is mapped field by field`() {
        val parsed = parse(
            """
            name,url,username,password,note
            Gmail,https://accounts.google.com/,someone@example.com,s3cr3t,my note
            """.trimIndent()
        )

        val entry = parsed.entries.single()
        assertEquals("Gmail", entry.serviceName)
        assertEquals("someone@example.com", entry.username)
        assertEquals("s3cr3t", entry.plainPassword)
        assertEquals("https://accounts.google.com/\nmy note", entry.hint)
        assertEquals(ProtectionMode.ENCRYPTED, entry.mode)
        assertEquals(1_000L, entry.createdAt)
        assertNull(entry.category)
    }

    @Test
    fun `columns are located by name, not by position`() {
        val parsed = parse(
            """
            password,note,name,username,url
            s3cr3t,,Netflix,me@example.com,https://netflix.com
            """.trimIndent()
        )

        val entry = parsed.entries.single()
        assertEquals("Netflix", entry.serviceName)
        assertEquals("me@example.com", entry.username)
        assertEquals("s3cr3t", entry.plainPassword)
    }

    @Test
    fun `an export without the note column still works`() {
        val parsed = parse(
            """
            name,url,username,password
            Netflix,https://netflix.com,me@example.com,s3cr3t
            """.trimIndent()
        )

        assertEquals("https://netflix.com", parsed.entries.single().hint)
    }

    @Test
    fun `a UTF-8 BOM does not hide the first column`() {
        val parsed = parse("﻿name,url,username,password\nNetflix,,me,s3cr3t")

        assertEquals("Netflix", parsed.entries.single().serviceName)
    }

    @Test
    fun `a missing name falls back to the host of the url`() {
        val parsed = parse("name,url,username,password\n,https://www.example.com/login?x=1,me,s3cr3t")

        assertEquals("example.com", parsed.entries.single().serviceName)
    }

    @Test
    fun `android credentials fall back to the package name`() {
        val parsed = parse("name,url,username,password\n,android://a-hash@com.example.app/,me,s3cr3t")

        assertEquals("com.example.app", parsed.entries.single().serviceName)
    }

    @Test
    fun `rows without a password or a service are skipped, not imported`() {
        val parsed = parse(
            """
            name,url,username,password
            Netflix,https://netflix.com,me,s3cr3t
            Empty,https://empty.com,me,
            ,,me,orphan
            """.trimIndent()
        )

        assertEquals(1, parsed.entries.size)
        assertEquals(2, parsed.skipped)
    }

    @Test
    fun `passwords keep commas, quotes and line breaks`() {
        val parsed = parse(
            "name,url,username,password\nBank,,me,\"a,\"\"weird\"\"\npassword\""
        )

        assertEquals("a,\"weird\"\npassword", parsed.entries.single().plainPassword)
    }

    @Test
    fun `an empty username becomes null`() {
        val parsed = parse("name,url,username,password\nNetflix,,,s3cr3t")

        assertNull(parsed.entries.single().username)
        assertNull(parsed.entries.single().hint)
    }

    @Test
    fun `a header-only file yields nothing`() {
        val parsed = parse("name,url,username,password\n")

        assertTrue(parsed.entries.isEmpty())
        assertEquals(0, parsed.skipped)
    }

    @Test(expected = CsvFormatException::class)
    fun `a file with no password column is rejected`() {
        parse("first name,last name\nAda,Lovelace")
    }

    @Test
    fun `an entirely empty file yields nothing`() {
        val parsed = parse("")

        assertTrue(parsed.entries.isEmpty())
    }
}
