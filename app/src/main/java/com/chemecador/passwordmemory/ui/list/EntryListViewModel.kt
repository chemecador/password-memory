package com.chemecador.passwordmemory.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemecador.passwordmemory.data.repository.PasswordRepository
import com.chemecador.passwordmemory.domain.model.PasswordEntry
import com.chemecador.passwordmemory.domain.model.ServiceGroup
import com.chemecador.passwordmemory.domain.model.groupByService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EntryListUiState(
    /** Every entry the filters let through, ungrouped: what the autofill picker chooses from. */
    val entries: List<PasswordEntry> = emptyList(),
    /** The same entries as the list shows them: one row per service. */
    val groups: List<ServiceGroup> = emptyList(),
    val categories: List<String> = emptyList(),
    val query: String = "",
    val selectedCategory: String? = null,
    val favoritesOnly: Boolean = false,
    val isLoading: Boolean = true
) {
    val isEmpty: Boolean get() = !isLoading && entries.isEmpty()
    val hasFilters: Boolean get() = query.isNotBlank() || selectedCategory != null || favoritesOnly
}

private data class Filters(
    val query: String = "",
    val category: String? = null,
    val favoritesOnly: Boolean = false
)

@HiltViewModel
class EntryListViewModel @Inject constructor(
    private val repository: PasswordRepository
) : ViewModel() {

    private val filters = MutableStateFlow(Filters())

    val uiState: StateFlow<EntryListUiState> = combine(
        repository.observeEntries(),
        repository.observeCategories(),
        filters
    ) { entries, categories, current ->
        val matching = entries.filter { it.matches(current) }
        EntryListUiState(
            entries = matching,
            groups = matching.groupByService(),
            categories = categories,
            query = current.query,
            selectedCategory = current.category,
            favoritesOnly = current.favoritesOnly,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EntryListUiState()
    )

    fun onQueryChange(query: String) {
        filters.value = filters.value.copy(query = query)
    }

    fun onCategorySelected(category: String?) {
        filters.value = filters.value.copy(category = category)
    }

    fun onFavoritesOnlyToggle() {
        filters.value = filters.value.copy(favoritesOnly = !filters.value.favoritesOnly)
    }

    fun onFavoriteToggle(entry: PasswordEntry) {
        viewModelScope.launch { repository.setFavorite(entry.id, !entry.isFavorite) }
    }
}

private fun PasswordEntry.matches(filters: Filters): Boolean {
    val matchesQuery = filters.query.isBlank() ||
        serviceName.contains(filters.query.trim(), ignoreCase = true)
    val matchesCategory = filters.category == null || category == filters.category
    val matchesFavorite = !filters.favoritesOnly || isFavorite
    return matchesQuery && matchesCategory && matchesFavorite
}
