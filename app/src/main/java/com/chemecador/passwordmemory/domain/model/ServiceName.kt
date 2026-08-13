package com.chemecador.passwordmemory.domain.model

/**
 * The rules that turn whatever was typed, pasted or imported into the name of a service, and that
 * decide when two of those names mean the same thing.
 *
 * There are two different questions here, and they are deliberately not answered by the same rule:
 *
 * - **Is this written differently, or is it a different name?** A pasted URL carries prefixes that
 *   say nothing about whose service it is, and they used to decide where the entry landed:
 *   `https://netflix.com` sorted under H, wore an H in its avatar, and stood apart from the
 *   `netflix.com` typed by hand. [clean] removes them on the way into the database.
 * - **Are these two names the same service?** `Gmail` and `gmail.com` are, and no amount of
 *   normalising one string will turn it into the other — the domain suffix has to be folded away to
 *   see it. [key] does that, for comparison only: the suffix is part of the name and stays in what
 *   is stored and shown.
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

    /**
     * What makes two entries the same service: the [clean] name, folded for case, with any domain
     * suffix dropped. `Gmail`, `gmail.com` and `GMAIL.COM` all come out as `gmail`.
     *
     * This is what the list groups by and what an import calls a duplicate. It is a judgement about
     * two names rather than a fact about one, so it is kept away from [exactKey]'s job: folding too
     * eagerly here costs a row that reads oddly, which is visible and undoable.
     */
    fun key(raw: String): String {
        val exact = exactKey(raw)
        val labels = exact.split('.')
        if (labels.size < 2) return exact
        // The first label always survives, or `x.com` would fold to nothing at all: the rule that
        // reads `.uk` as a suffix reads a one-letter name the same way.
        val kept = labels.dropLastWhile(::isDomainSuffix).ifEmpty { labels.take(1) }
        return kept.joinToString(".")
    }

    /**
     * The same name, ignoring case — nothing more. Two entries share it only when they really were
     * given the same name, which is what the autofill save asks before replacing the password of an
     * entry it did not create. That decision is silent and it destroys a password, so it is the one
     * place that gets no help from a guess.
     */
    fun exactKey(raw: String): String = clean(raw).lowercase()

    /**
     * Whether a trailing label is the domain's suffix rather than its name. Two characters or fewer
     * covers every country code without listing them, which is also what saves `amazon.co.uk` from
     * being read as a service called `amazon.co`.
     *
     * [com.chemecador.passwordmemory.autofill.Sites] answers a looser version of this question for
     * autofill, where a false match only lengthens a list. It is not reused here: it also splits on
     * spaces and hyphens and drops words like `login`, which would make `The Guardian` and
     * `Guardian Seguros` one service.
     */
    private fun isDomainSuffix(label: String): Boolean =
        label.length <= SUFFIX_LENGTH_WITHOUT_MEANING || label in DOMAIN_SUFFIXES

    /**
     * Stripped in this order, so `https://www.x.com` loses both: the scheme first, and the `www.`
     * it uncovers second. The migration that cleaned the entries written before this rule existed
     * strips the same prefixes in the same order.
     */
    private val PREFIXES = listOf("https://", "http://", "www.")

    private const val SUFFIX_LENGTH_WITHOUT_MEANING = 2

    /** The suffixes long enough to be mistaken for a name, so they have to be named. */
    private val DOMAIN_SUFFIXES = setOf(
        "com", "org", "net", "edu", "gov", "mil", "int", "info", "biz", "name", "pro",
        "app", "dev", "online", "site", "shop", "store", "cloud", "xyz", "tech", "blog",
        "page", "live", "email", "eus", "cat", "gal"
    )
}
