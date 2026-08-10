package com.chemecador.passwordmemory.autofill

import java.net.IDN
import java.util.Locale

/**
 * The two identifiers Android exposes for an autofill request.
 *
 * Neither value proves that the app owns the claimed web domain. They are deliberately kept
 * separate so the UI never presents an unverified association as trusted.
 */
internal class AutofillRequestOrigin private constructor(
    val claimedWebDomain: String?,
    val requestingPackage: String?
) {

    companion object {

        fun from(claimedWebDomain: String?, requestingPackage: String?): AutofillRequestOrigin =
            AutofillRequestOrigin(
                claimedWebDomain = claimedWebDomain.safeDomain(),
                requestingPackage = requestingPackage.visibleIdentifier()
            )
    }
}

internal fun LoginForm.requestOrigin(): AutofillRequestOrigin =
    AutofillRequestOrigin.from(
        claimedWebDomain = webDomain,
        requestingPackage = packageName
    )

/**
 * Request metadata is untrusted display text. Controls and Unicode formatting characters become a
 * visible replacement marker: deleting them could turn a hostile value such as `pay\npal.com` into
 * the innocent-looking `paypal.com`. The middle of an exceptionally long value is collapsed while
 * retaining both identifying ends.
 */
private fun String?.visibleIdentifier(): String? {
    val cleaned = this?.let { source ->
        buildString(source.length) {
            var previousWasUnsafe = false
            source.forEach { character ->
                val isUnsafe = character.isISOControl() ||
                    Character.getType(character) == Character.FORMAT.toInt()
                when {
                    isUnsafe && !previousWasUnsafe -> append(VISIBLE_REPLACEMENT)
                    !isUnsafe -> append(character)
                }
                previousWasUnsafe = isUnsafe
            }
        }
    }
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?: return null
    return cleaned.abbreviate()
}

/**
 * Clean domains are rendered as lowercase ASCII/punycode to make Unicode homographs visible.
 * Malformed claims keep their visible representation and gain a warning marker.
 */
private fun String?.safeDomain(): String? {
    val source = this ?: return null
    val hadOuterWhitespace = source != source.trim()
    val visible = visibleIdentifier() ?: return null
    if (VISIBLE_REPLACEMENT in visible) return visible
    if (hadOuterWhitespace) return "$INVALID_MARKER$visible".abbreviate()

    val ascii = runCatching {
        IDN.toASCII(visible, IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT)
    }.getOrNull()
    return if (ascii != null && ascii.isValidAsciiDomain()) {
        ascii
    } else {
        "$INVALID_MARKER$visible".abbreviate()
    }
}

private fun String.isValidAsciiDomain(): Boolean =
    length <= MAX_DOMAIN_LENGTH &&
        split('.').all { label ->
            label.isNotEmpty() &&
                label.length <= MAX_DOMAIN_LABEL_LENGTH &&
                !label.startsWith('-') &&
                !label.endsWith('-')
        }

private fun String.abbreviate(): String {
    if (length <= MAX_IDENTIFIER_LENGTH) return this
    return take(IDENTIFIER_EDGE_LENGTH) +
        ELLIPSIS +
        takeLast(IDENTIFIER_EDGE_LENGTH)
}

private const val MAX_IDENTIFIER_LENGTH = 257
private const val IDENTIFIER_EDGE_LENGTH = 128
private const val MAX_DOMAIN_LENGTH = 253
private const val MAX_DOMAIN_LABEL_LENGTH = 63
private const val VISIBLE_REPLACEMENT = '\uFFFD'
private const val INVALID_MARKER = "⚠ "
private const val ELLIPSIS = "…"
