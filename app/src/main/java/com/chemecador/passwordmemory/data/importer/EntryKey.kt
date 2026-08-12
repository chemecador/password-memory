package com.chemecador.passwordmemory.data.importer

import com.chemecador.passwordmemory.domain.model.ServiceName

/** Identity of an entry for the purposes of spotting duplicates while importing. */
data class EntryKey(val serviceName: String, val username: String?) {

    companion object {
        fun of(serviceName: String, username: String?) = EntryKey(
            // The same name the entry will be stored under, so a CSV row spelled
            // `https://x.com` is recognised as the `x.com` already here rather than added twice.
            serviceName = ServiceName.key(serviceName),
            username = username?.trim()?.lowercase()?.takeIf(String::isNotEmpty)
        )
    }
}
