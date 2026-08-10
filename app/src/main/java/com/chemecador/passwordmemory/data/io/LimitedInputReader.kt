package com.chemecador.passwordmemory.data.io

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** The selected input is larger than the bounded in-memory import allows. */
class InputSizeLimitExceededException(
    val maxBytes: Int
) : IOException("Selected file exceeds the $maxBytes byte limit")

/**
 * Reads this stream into memory while enforcing a hard upper bound.
 *
 * At most [maxBytes] are retained and only one additional byte is requested to detect an
 * oversized input. This keeps a malicious or accidentally huge SAF document from exhausting the
 * app process before its format can be validated.
 */
fun InputStream.readBytesLimited(maxBytes: Int): ByteArray {
    require(maxBytes >= 0) { "maxBytes must not be negative" }

    val output = ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var totalBytes = 0
    while (true) {
        val remaining = maxBytes - totalBytes
        val requested = minOf(buffer.size.toLong(), remaining.toLong() + 1L).toInt()
        val read = read(buffer, 0, requested)
        if (read < 0) return output.toByteArray()
        if (read == 0) {
            // InputStream promises progress for a non-empty buffer, but avoid looping forever if a
            // provider-backed implementation violates that contract.
            val byte = read()
            if (byte < 0) return output.toByteArray()
            if (totalBytes == maxBytes) throw InputSizeLimitExceededException(maxBytes)
            output.write(byte)
            totalBytes++
            continue
        }
        if (read > remaining) throw InputSizeLimitExceededException(maxBytes)
        output.write(buffer, 0, read)
        totalBytes += read
    }
}
