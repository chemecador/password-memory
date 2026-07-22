package com.chemecador.passwordmemory.data.importer

/** Identity of an entry for the purposes of spotting duplicates while importing. */
data class EntryKey(val serviceName: String, val username: String?) {

    companion object {
        fun of(serviceName: String, username: String?) = EntryKey(
            serviceName = serviceName.trim().lowercase(),
            username = username?.trim()?.lowercase()?.takeIf(String::isNotEmpty)
        )
    }
}
