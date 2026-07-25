package com.chemecador.passwordmemory.autofill

import com.chemecador.passwordmemory.domain.model.PasswordEntry

/**
 * Decides which stored entries belong to a login form.
 *
 * Entries do not record the site they were created for, so matching works on names: the meaningful
 * labels of the domain or of the package name (`github` out of `github.com`, `twitter` out of
 * `com.twitter.android`) are compared against the service name and against any URL left in the
 * hint — which is where the CSV import, and the autofill save flow, put the origin.
 *
 * False positives are cheap here: the user still picks the entry from the list. Missing an entry is
 * the expensive outcome, so the comparison stays generous.
 */
object CredentialMatcher {

    fun matching(entries: List<PasswordEntry>, form: LoginForm): List<PasswordEntry> {
        val host = Sites.hostOf(form.webDomain)
        val targets = buildSet {
            host?.let { addAll(Sites.labelsOf(it)) }
            form.packageName?.let { addAll(Sites.labelsOf(it)) }
        }
        if (targets.isEmpty()) return emptyList()
        return entries
            .map { entry -> entry to score(entry, host, targets) }
            .filter { (_, score) -> score > 0 }
            .sortedWith(compareByDescending<Pair<PasswordEntry, Int>> { it.second }
                .thenBy { it.first.serviceName.lowercase() })
            .map { it.first }
    }

    private fun score(entry: PasswordEntry, host: String?, targets: Set<String>): Int {
        val hintHosts = Sites.hostsIn(entry.hint)
        return when {
            host != null && hintHosts.any { it == host || host.endsWith(".$it") } -> 3
            Sites.labelsOf(entry.serviceName).any { it in targets } -> 2
            hintHosts.flatMap(Sites::labelsOf).any { it in targets } -> 1
            else -> 0
        }
    }
}

/** Small helpers to turn URLs, domains and package names into something comparable. */
object Sites {

    /** `https://accounts.google.com/signin` and `www.google.com` both become `accounts.google.com`. */
    fun hostOf(url: String?): String? = url
        ?.trim()
        ?.lowercase()
        ?.substringAfter("://")
        ?.substringBefore('/')
        ?.substringBefore('?')
        ?.substringAfterLast('@')
        ?.substringBefore(':')
        ?.removePrefix("www.")
        ?.takeIf(String::isNotEmpty)

    /** The parts of a name worth comparing, with the noise every domain and package shares removed. */
    fun labelsOf(value: String): List<String> = value
        .lowercase()
        .split(*SEPARATORS)
        .filter { it.length >= MIN_LABEL_LENGTH && it !in NOISE }

    /** Hosts mentioned anywhere in a free text field, typically the URL kept in an entry's hint. */
    fun hostsIn(text: String?): List<String> = text
        ?.split(*WHITESPACE)
        ?.filter { it.contains("://") || it.contains('.') }
        ?.mapNotNull(::hostOf)
        .orEmpty()

    /** How an entry saved from a login form records where it came from. */
    fun originOf(form: LoginForm): String? {
        hostOf(form.webDomain)?.let { return "https://$it" }
        return form.packageName?.let { "android://$it" }
    }

    private val SEPARATORS = arrayOf(".", "-", "_", " ", "/", "+")
    private val WHITESPACE = arrayOf(" ", "\n", "\t", ",", ";")
    private const val MIN_LABEL_LENGTH = 3

    /**
     * Labels that say nothing about who the site belongs to. Country and generic top level domains
     * live here together with the words every Android package name is padded with.
     */
    private val NOISE = setOf(
        "com", "org", "net", "www", "edu", "gov", "int", "info", "biz", "app", "apps", "dev",
        "online", "site", "web", "shop", "store", "cloud", "android", "mobile", "client", "lite",
        "free", "beta", "prod", "release", "login", "signin", "auth", "account", "accounts",
        "http", "https", "the", "and", "www2", "www3"
    )
}
