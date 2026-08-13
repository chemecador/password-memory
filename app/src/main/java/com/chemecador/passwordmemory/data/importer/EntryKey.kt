package com.chemecador.passwordmemory.data.importer

import com.chemecador.passwordmemory.domain.model.ServiceName

/**
 * Identity of an entry, in the two strengths the app needs.
 *
 * [of] asks whether this is the same *service*, which is what an import means by a duplicate: a
 * row spelled `https://gmail.com` is the `Gmail` already saved, and offering to add it again is
 * offering to make a mess. Being wrong here is shown before anything is written — the import
 * screen counts the duplicates and lets them through on a checkbox.
 *
 * [exact] asks whether this is the same *name*, and nothing looser. The autofill save uses it to
 * decide whether to replace the password of an entry it did not create, which is silent and
 * destroys a password, so it is not a decision to take on a resemblance.
 */
data class EntryKey(val serviceName: String, val username: String?) {

    companion object {
        fun of(serviceName: String, username: String?) =
            EntryKey(ServiceName.key(serviceName), username.fold())

        fun exact(serviceName: String, username: String?) =
            EntryKey(ServiceName.exactKey(serviceName), username.fold())

        private fun String?.fold(): String? =
            this?.trim()?.lowercase()?.takeIf(String::isNotEmpty)
    }
}
