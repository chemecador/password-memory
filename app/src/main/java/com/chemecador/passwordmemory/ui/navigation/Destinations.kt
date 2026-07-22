package com.chemecador.passwordmemory.ui.navigation

object Destinations {
    const val ARG_ENTRY_ID = "entryId"

    const val LIST = "list"
    const val BACKUP = "backup"
    const val DETAIL = "detail/{$ARG_ENTRY_ID}"
    const val EDIT = "edit/{$ARG_ENTRY_ID}"

    /** [NEW_ENTRY_ID] means "create", since navigation arguments cannot be nullable longs. */
    const val NEW_ENTRY_ID = 0L

    fun detail(entryId: Long) = "detail/$entryId"

    fun edit(entryId: Long = NEW_ENTRY_ID) = "edit/$entryId"
}
