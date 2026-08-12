package com.chemecador.passwordmemory.domain.model

import org.junit.Assert.assertEquals
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
        assertEquals("netflix.com", ServiceName.key(" HTTP://NETFLIX.COM "))
    }
}
