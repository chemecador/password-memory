package com.chemecador.passwordmemory.autofill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillRequestOriginTest {

    @Test
    fun `keeps the claimed domain separate from the requesting package`() {
        val origin = AutofillRequestOrigin.from(
            claimedWebDomain = "accounts.example.com",
            requestingPackage = "com.example.browser"
        )

        assertEquals("accounts.example.com", origin.claimedWebDomain)
        assertEquals("com.example.browser", origin.requestingPackage)
    }

    @Test
    fun `blank identifiers remain explicitly unavailable`() {
        val origin = AutofillRequestOrigin.from(
            claimedWebDomain = "  ",
            requestingPackage = null
        )

        assertNull(origin.claimedWebDomain)
        assertNull(origin.requestingPackage)
    }

    @Test
    fun `control and bidi formatting characters remain visibly marked`() {
        val origin = AutofillRequestOrigin.from(
            claimedWebDomain = "accounts.\n\u202Eexample.com",
            requestingPackage = "com.example.\u2066browser\u2069"
        )

        assertEquals("accounts.\uFFFDexample.com", origin.claimedWebDomain)
        assertEquals("com.example.\uFFFDbrowser\uFFFD", origin.requestingPackage)
    }

    @Test
    fun `unicode domain is rendered as ascii punycode`() {
        val origin = AutofillRequestOrigin.from(
            claimedWebDomain = "раypal.com",
            requestingPackage = "com.example.browser"
        )

        assertTrue(requireNotNull(origin.claimedWebDomain).startsWith("xn--"))
    }

    @Test
    fun `malformed domain receives a visible warning marker`() {
        val origin = AutofillRequestOrigin.from(
            claimedWebDomain = "pay pal.com",
            requestingPackage = "com.example.browser"
        )

        assertTrue(requireNotNull(origin.claimedWebDomain).startsWith("⚠ "))
    }

    @Test
    fun `outer domain whitespace is not silently normalized away`() {
        val origin = AutofillRequestOrigin.from(
            claimedWebDomain = " paypal.com ",
            requestingPackage = "com.example.browser"
        )

        assertEquals("⚠ paypal.com", origin.claimedWebDomain)
    }

    @Test
    fun `very long identifiers retain both ends`() {
        val origin = AutofillRequestOrigin.from(
            claimedWebDomain = "start." + "x".repeat(400) + ".example.com",
            requestingPackage = "com.example.browser"
        )

        val domain = requireNotNull(origin.claimedWebDomain)
        assertEquals(257, domain.length)
        assertTrue(domain.startsWith("⚠ start."))
        assertTrue(domain.endsWith(".example.com"))
        assertTrue("…" in domain)
    }
}
