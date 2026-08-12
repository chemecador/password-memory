package com.chemecador.passwordmemory.domain.model

/**
 * The rule that turns whatever was typed, pasted or imported into the name of a service.
 *
 * A pasted URL carries prefixes that say nothing about whose service it is, and they used to decide
 * where the entry landed: `https://netflix.com` sorted under H, wore an H in its avatar, and stood
 * apart from the `netflix.com` that was typed by hand. They are stripped on the way into the
 * database, so a service has one spelling instead of one per way of writing it down.
 */
object ServiceName {

    /** The name as it is stored, shown and sorted. */
    fun clean(raw: String): String {
        var name = raw.trim()
        PREFIXES.forEach { prefix ->
            // A name that is nothing but a prefix keeps it: the field is required, and an entry
            // called "https://" is a great deal easier to find than one called "".
            if (name.length > prefix.length && name.startsWith(prefix, ignoreCase = true)) {
                name = name.substring(prefix.length).trim()
            }
        }
        return name
    }

    /** [clean] folded for comparison: what decides that two entries are the same service. */
    fun key(raw: String): String = clean(raw).lowercase()

    /**
     * Stripped in this order, so `https://www.x.com` loses both: the scheme first, and the `www.`
     * it uncovers second. The migration that cleaned the entries written before this rule existed
     * strips the same prefixes in the same order.
     */
    private val PREFIXES = listOf("https://", "http://", "www.")
}
