package com.chemecador.passwordmemory.domain.model

/**
 * Every account saved under the same service, as one row of the list.
 *
 * Entries carry no notion of a service beyond their name, so the name is the identity: it is
 * matched through [ServiceName.key], the same way
 * [com.chemecador.passwordmemory.data.importer.EntryKey] decides what counts as a duplicate.
 */
data class ServiceGroup(
    val key: String,
    val serviceName: String,
    val entries: List<PasswordEntry>
) {

    val size: Int get() = entries.size

    /** The only account of this service, or null when there are several to choose from. */
    val single: PasswordEntry? get() = entries.singleOrNull()

    /** A service is a favourite as soon as one of its accounts is, so it sorts to the top. */
    val isFavorite: Boolean get() = entries.any(PasswordEntry::isFavorite)

    /** The mode of the whole service, or null when its accounts do not agree on one. */
    val mode: ProtectionMode?
        get() = entries.map(PasswordEntry::mode).distinct().singleOrNull()

    companion object {
        fun keyOf(serviceName: String): String = ServiceName.key(serviceName)
    }
}

/**
 * Collapses entries into one group per service: favourites first, then by name, with the accounts
 * inside each group ordered the same way.
 *
 * The ordering is decided here rather than left to the query, because a service is a favourite when
 * any of its accounts is.
 */
fun List<PasswordEntry>.groupByService(): List<ServiceGroup> =
    groupBy { ServiceGroup.keyOf(it.serviceName) }
        .map { (key, entries) ->
            val accounts = entries.sortedWith(ACCOUNT_ORDER)
            ServiceGroup(key = key, serviceName = accounts.label(), entries = accounts)
        }
        .sortedWith(SERVICE_ORDER)

/**
 * What the row calls a service whose accounts spell it differently. The shortest spelling wins:
 * `Gmail` and `gmail.com` are one service, and which of the two names the row should not depend on
 * whichever of them happens to hold the username that sorts first.
 */
private fun List<PasswordEntry>.label(): String = map { ServiceName.clean(it.serviceName) }
    .minWith(compareBy<String>(String::length).thenBy(String.CASE_INSENSITIVE_ORDER) { it })

private val ACCOUNT_ORDER = compareByDescending<PasswordEntry> { it.isFavorite }
    // Accounts without a username last: there is no name to sort them among the others by.
    .thenBy { it.username == null }
    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.username.orEmpty() }

private val SERVICE_ORDER = compareByDescending<ServiceGroup> { it.isFavorite }
    .thenBy(String.CASE_INSENSITIVE_ORDER, ServiceGroup::serviceName)
