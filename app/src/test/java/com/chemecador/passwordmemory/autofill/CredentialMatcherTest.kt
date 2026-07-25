package com.chemecador.passwordmemory.autofill

import com.chemecador.passwordmemory.domain.model.PasswordEntry
import com.chemecador.passwordmemory.domain.model.ProtectionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialMatcherTest {

    private var nextId = 1L

    private fun entry(serviceName: String, hint: String? = null) = PasswordEntry(
        id = nextId++,
        serviceName = serviceName,
        hint = hint,
        mode = ProtectionMode.ENCRYPTED
    )

    private fun webForm(domain: String) = LoginForm(
        packageName = "com.android.chrome",
        webDomain = domain,
        usernameId = null,
        passwordIds = emptyList(),
        username = null,
        password = null
    )

    private fun appForm(packageName: String) = LoginForm(
        packageName = packageName,
        webDomain = null,
        usernameId = null,
        passwordIds = emptyList(),
        username = null,
        password = null
    )

    @Test
    fun `an entry named after the site matches it`() {
        val entries = listOf(entry("GitHub"), entry("Netflix"))

        val matches = CredentialMatcher.matching(entries, webForm("github.com"))

        assertEquals(listOf("GitHub"), matches.map(PasswordEntry::serviceName))
    }

    @Test
    fun `subdomains match the entry of the site`() {
        val entries = listOf(entry("Google"))

        val matches = CredentialMatcher.matching(entries, webForm("accounts.google.com"))

        assertEquals(1, matches.size)
    }

    @Test
    fun `the URL kept in the hint wins over a similar name`() {
        val byName = entry("Amazon")
        val byUrl = entry("Compras", hint = "https://www.amazon.es/ap/signin")

        val matches = CredentialMatcher.matching(listOf(byName, byUrl), webForm("amazon.es"))

        assertEquals(listOf("Compras", "Amazon"), matches.map(PasswordEntry::serviceName))
    }

    @Test
    fun `an app is matched through its package name`() {
        val entries = listOf(entry("Twitter"), entry("Instagram"))

        val matches = CredentialMatcher.matching(entries, appForm("com.twitter.android"))

        assertEquals(listOf("Twitter"), matches.map(PasswordEntry::serviceName))
    }

    @Test
    fun `an entry saved by autofill is found again on the same site`() {
        val saved = entry("github.com", hint = "https://github.com")

        val matches = CredentialMatcher.matching(listOf(saved), webForm("github.com"))

        assertEquals(1, matches.size)
    }

    @Test
    fun `words every site shares do not match everything`() {
        val entries = listOf(entry("Login"), entry("Mi cuenta"), entry("App"))

        val matches = CredentialMatcher.matching(entries, webForm("accounts.google.com"))

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `an unrelated site matches nothing`() {
        val entries = listOf(entry("Netflix", hint = "https://netflix.com"))

        val matches = CredentialMatcher.matching(entries, webForm("github.com"))

        assertTrue(matches.isEmpty())
    }

    @Test
    fun `a form with no origin at all is not guessed at`() {
        val entries = listOf(entry("GitHub"))

        val matches = CredentialMatcher.matching(entries, appForm(""))

        assertTrue(matches.isEmpty())
    }
}
