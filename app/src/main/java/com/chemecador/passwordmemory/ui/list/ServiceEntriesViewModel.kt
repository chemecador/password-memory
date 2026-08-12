package com.chemecador.passwordmemory.ui.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemecador.passwordmemory.data.repository.PasswordRepository
import com.chemecador.passwordmemory.domain.model.PasswordEntry
import com.chemecador.passwordmemory.domain.model.groupByService
import com.chemecador.passwordmemory.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServiceEntriesUiState(
    val serviceName: String = "",
    val entries: List<PasswordEntry> = emptyList(),
    val isLoading: Boolean = true
) {
    /** Nothing left under this service: every account was deleted or renamed away from it. */
    val isEmpty: Boolean get() = !isLoading && entries.isEmpty()
}

/** The accounts saved under one service, reached from its row in the list. */
@HiltViewModel
class ServiceEntriesViewModel @Inject constructor(
    private val repository: PasswordRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val serviceKey: String = checkNotNull(savedStateHandle[Destinations.ARG_SERVICE_KEY])

    val uiState: StateFlow<ServiceEntriesUiState> = repository.observeEntries()
        .map { entries ->
            // Grouping the whole list keeps the order of the accounts identical to the one the
            // list screen counted, instead of a second, subtly different rule for the same data.
            val group = entries.groupByService().firstOrNull { it.key == serviceKey }
            ServiceEntriesUiState(
                serviceName = group?.serviceName.orEmpty(),
                entries = group?.entries.orEmpty(),
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ServiceEntriesUiState()
        )

    fun onFavoriteToggle(entry: PasswordEntry) {
        viewModelScope.launch { repository.setFavorite(entry.id, !entry.isFavorite) }
    }
}
