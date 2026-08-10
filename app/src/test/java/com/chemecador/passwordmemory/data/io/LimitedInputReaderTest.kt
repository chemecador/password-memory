package com.chemecador.passwordmemory.data.io

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class LimitedInputReaderTest {

    @Test
    fun `input below the limit is returned unchanged`() {
        val input = "password-memory".toByteArray()

        val result = ByteArrayInputStream(input).readBytesLimited(input.size + 1)

        assertArrayEquals(input, result)
    }

    @Test
    fun `input exactly at the limit is accepted`() {
        val input = ByteArray(32) { it.toByte() }

        val result = ByteArrayInputStream(input).readBytesLimited(input.size)

        assertArrayEquals(input, result)
    }

    @Test
    fun `input over the limit is rejected after only one extra byte`() {
        val input = CountingInputStream(size = 1_000_000)

        val exception = assertThrows(InputSizeLimitExceededException::class.java) {
            input.readBytesLimited(maxBytes = 32)
        }

        assertEquals(32, exception.maxBytes)
        assertEquals(33, input.bytesRead)
    }

    @Test
    fun `negative limits are rejected before reading`() {
        val input = CountingInputStream(size = 1)

        assertThrows(IllegalArgumentException::class.java) {
            input.readBytesLimited(maxBytes = -1)
        }
        assertEquals(0, input.bytesRead)
    }

    private class CountingInputStream(
        private val size: Long
    ) : InputStream() {

        var bytesRead: Long = 0
            private set

        override fun read(): Int =
            if (bytesRead >= size) {
                -1
            } else {
                bytesRead++
                0
            }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (bytesRead >= size) return -1
            val count = minOf(length.toLong(), size - bytesRead).toInt()
            buffer.fill(0, offset, offset + count)
            bytesRead += count
            return count
        }
    }
}
