package com.chemecador.passwordmemory.security

import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

data class PasswordGeneratorOptions(
    val length: Int = DEFAULT_LENGTH,
    val useUppercase: Boolean = true,
    val useLowercase: Boolean = true,
    val useDigits: Boolean = true,
    val useSymbols: Boolean = true
) {
    val hasAnyCharacterSet: Boolean
        get() = useUppercase || useLowercase || useDigits || useSymbols

    companion object {
        const val MIN_LENGTH = 4
        const val MAX_LENGTH = 64
        const val DEFAULT_LENGTH = 16
    }
}

/**
 * Random password generator. Every selected character set is guaranteed to appear at least once,
 * and the result is shuffled with [SecureRandom] so that guarantee does not bias character
 * positions.
 */
@Singleton
class PasswordGenerator @Inject constructor(
    private val secureRandom: SecureRandom
) {

    fun generate(options: PasswordGeneratorOptions): String {
        require(options.hasAnyCharacterSet) { "At least one character set must be selected" }
        val length = options.length.coerceIn(
            PasswordGeneratorOptions.MIN_LENGTH,
            PasswordGeneratorOptions.MAX_LENGTH
        )

        val pools = buildList {
            if (options.useUppercase) add(UPPERCASE)
            if (options.useLowercase) add(LOWERCASE)
            if (options.useDigits) add(DIGITS)
            if (options.useSymbols) add(SYMBOLS)
        }
        val allChars = pools.joinToString(separator = "")

        val chars = ArrayList<Char>(length)
        pools.take(length).forEach { pool -> chars.add(randomChar(pool)) }
        while (chars.size < length) chars.add(randomChar(allChars))

        // Fisher-Yates with SecureRandom: Collections.shuffle() would need a seeded Random.
        for (i in chars.lastIndex downTo 1) {
            val j = secureRandom.nextInt(i + 1)
            val tmp = chars[i]
            chars[i] = chars[j]
            chars[j] = tmp
        }
        return chars.joinToString(separator = "")
    }

    private fun randomChar(pool: String): Char = pool[secureRandom.nextInt(pool.length)]

    private companion object {
        const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
        const val DIGITS = "0123456789"
        const val SYMBOLS = "!@#$%&*()-_=+[]{}<>?"
    }
}
