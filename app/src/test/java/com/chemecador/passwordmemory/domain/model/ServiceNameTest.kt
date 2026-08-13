package com.chemecador.passwordmemory.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A pasted URL used to sort by its scheme: `https://netflix.com` sat under H, next to Hotmail,
 * with an H in its avatar. What the prefixes are stripped from is the identity of the entry, so
 * these cases are also what decides whether two spellings share a row.
 */
class ServiceNameTest {

    @Test
    fun `the url prefixes are stripped`() {
        assertEquals("netflix.com", ServiceName.clean("https://netflix.com"))
        assertEquals("netflix.com", ServiceName.clean("http://netflix.com"))
        assertEquals("netflix.com", ServiceName.clean("www.netflix.com"))
        assertEquals("netflix.com", ServiceName.clean("https://www.netflix.com"))
    }

    @Test
    fun `stripping ignores the case of the prefix but keeps the case of the name`() {
        assertEquals("Netflix.com", ServiceName.clean("HTTPS://WWW.Netflix.com"))
        assertEquals("Netflix", ServiceName.clean("Www.Netflix"))
    }

    @Test
    fun `surrounding and uncovered whitespace goes`() {
        assertEquals("netflix.com", ServiceName.clean("   https://netflix.com  "))
        assertEquals("netflix.com", ServiceName.clean("https:// netflix.com"))
    }

    @Test
    fun `everything after the prefix is left as it was`() {
        assertEquals("netflix.com/browse", ServiceName.clean("https://netflix.com/browse"))
        assertEquals("netflix.com:8080", ServiceName.clean("https://netflix.com:8080"))
    }

    @Test
    fun `a prefix is only a prefix when the whole of it is there`() {
        assertEquals("httpsomething", ServiceName.clean("httpsomething"))
        assertEquals("wwwgrande", ServiceName.clean("wwwgrande"))
        assertEquals("my www.site", ServiceName.clean("my www.site"))
    }

    @Test
    fun `a name made of nothing but a prefix survives`() {
        // The service name is required, and an entry called "https://" is far easier to find in
        // the list than one called "".
        assertEquals("https://", ServiceName.clean("https://"))
        assertEquals("www.", ServiceName.clean("  www.  "))
    }

    @Test
    fun `the key folds case, so every spelling of a service is the same service`() {
        assertEquals(ServiceName.key("netflix.com"), ServiceName.key("https://www.Netflix.com"))
        assertEquals(ServiceName.exactKey("netflix.com"), ServiceName.exactKey("NETFLIX.com"))
    }

    @Test
    fun `the key sees past the domain suffix, so a brand and its domain are one service`() {
        assertEquals(ServiceName.key("Gmail"), ServiceName.key("gmail.com"))
        assertEquals(ServiceName.key("Netflix"), ServiceName.key("https://www.netflix.com"))
        assertEquals("gmail", ServiceName.key("GMAIL.COM"))
    }

    @Test
    fun `a country code is not mistaken for part of the name`() {
        // Without this, `amazon.co.uk` would be a service called `amazon.co`, next to no one.
        assertEquals(ServiceName.key("Amazon"), ServiceName.key("amazon.co.uk"))
        assertEquals(ServiceName.key("Amazon"), ServiceName.key("amazon.es"))
    }

    @Test
    fun `a subdomain still names its own service`() {
        // Whoever saved `mail.google.com` meant that, not everything Google runs.
        assertEquals("mail.google", ServiceName.key("mail.google.com"))
        assertTrue(ServiceName.key("Google") != ServiceName.key("mail.google.com"))
    }

    @Test
    fun `a name is never folded away entirely`() {
        // A service someone chose to call `com` is called com, and a one-letter name is a name:
        // `x.com` folds to `x` rather than to nothing, which is what makes it meet `X`.
        assertEquals("com", ServiceName.key("com"))
        assertEquals("co", ServiceName.key("co.uk"))
        assertEquals(ServiceName.key("X"), ServiceName.key("https://x.com"))
    }

    @Test
    fun `the exact key keeps the suffix the service key drops`() {
        // The autofill save replaces a password on this one, so it never guesses at a resemblance.
        assertEquals("gmail.com", ServiceName.exactKey("Gmail.com"))
        assertTrue(ServiceName.exactKey("Gmail") != ServiceName.exactKey("gmail.com"))
    }
}
