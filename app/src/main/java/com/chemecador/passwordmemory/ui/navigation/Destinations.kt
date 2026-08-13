package com.chemecador.passwordmemory.ui.navigation

import android.net.Uri

object Destinations {
    const val ARG_ENTRY_ID = "entryId"
    const val ARG_SERVICE_KEY = "serviceKey"
    const val ARG_SERVICE_NAME = "serviceName"

    const val LIST = "list"
    const val BACKUP = "backup"
    const val AUTOFILL = "autofill"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val DETAIL = "detail/{$ARG_ENTRY_ID}"
    const val EDIT = "edit/{$ARG_ENTRY_ID}?$ARG_SERVICE_NAME={$ARG_SERVICE_NAME}"
    const val SERVICE = "service/{$ARG_SERVICE_KEY}"

    /** [NEW_ENTRY_ID] means "create", since navigation arguments cannot be nullable longs. */
    const val NEW_ENTRY_ID = 0L

    fun detail(entryId: Long) = "detail/$entryId"

    fun edit(entryId: Long = NEW_ENTRY_ID) = "edit/$entryId"

    /**
     * Creating an account from inside a service, which already answers what the form would ask
     * first: the name is carried over so the entry lands in the group the user came from instead
     * of starting one of its own next to it.
     */
    fun createForService(serviceName: String) =
        "edit/$NEW_ENTRY_ID?$ARG_SERVICE_NAME=${Uri.encode(serviceName)}"

    /**
     * A service is identified by its own name, which is free text: it has to be encoded or a
     * slash in it would look like another segment of the route.
     */
    fun service(serviceKey: String) = "service/${Uri.encode(serviceKey)}"
}
