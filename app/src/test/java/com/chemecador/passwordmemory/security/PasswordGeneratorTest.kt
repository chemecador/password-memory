package com.chemecador.passwordmemory.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class PasswordGeneratorTest {

    private val generator = PasswordGenerator(SecureRandom())

    @Test
    fun `respects the requested length`() {
        val password = generator.generate(PasswordGeneratorOptions(length = 24))

        assertEquals(24, password.length)
    }

    @Test
    fun `clamps lengths outside the supported range`() {
        assertEquals(
            PasswordGeneratorOptions.MIN_LENGTH,
            generator.generate(PasswordGeneratorOptions(length = 1)).length
        )
        assertEquals(
            PasswordGeneratorOptions.MAX_LENGTH,
            generator.generate(PasswordGeneratorOptions(length = 5_000)).length
        )
    }

    @Test
    fun `uses only the selected character sets`() {
        val password = generator.generate(
            PasswordGeneratorOptions(
                length = 40,
                useUppercase = false,
                useLowercase = false,
                useDigits = true,
                useSymbols = false
            )
        )

        assertTrue(password.all(Char::isDigit))
    }

    @Test
    fun `includes at least one character of every selected set`() {
        repeat(50) {
            val password = generator.generate(PasswordGeneratorOptions(length = 8))

            assertTrue(password.any(Char::isUpperCase))
            assertTrue(password.any(Char::isLowerCase))
            assertTrue(password.any(Char::isDigit))
            assertTrue(password.any { !it.isLetterOrDigit() })
        }
    }

    @Test
    fun `consecutive passwords differ`() {
        val first = generator.generate(PasswordGeneratorOptions(length = 32))
        val second = generator.generate(PasswordGeneratorOptions(length = 32))

        assertNotEquals(first, second)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects an empty character set selection`() {
        generator.generate(
            PasswordGeneratorOptions(
                useUppercase = false,
                useLowercase = false,
                useDigits = false,
                useSymbols = false
            )
        )
    }
}
