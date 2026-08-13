package com.chemecador.passwordmemory.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemecador.passwordmemory.data.repository.EntryDraft
import com.chemecador.passwordmemory.data.repository.PasswordRepository
import com.chemecador.passwordmemory.domain.model.ProtectionMode
import com.chemecador.passwordmemory.security.AuthenticationRequiredException
import com.chemecador.passwordmemory.security.PasswordGenerator
import com.chemecador.passwordmemory.security.PasswordGeneratorOptions
import com.chemecador.passwordmemory.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditEntryUiState(
    val entryId: Long? = null,
    val serviceName: String = "",
    val username: String = "",
    val password: String = "",
    val hint: String = "",
    val category: String = "",
    val isFavorite: Boolean = false,
    val mode: ProtectionMode = ProtectionMode.ENCRYPTED,
    val categories: List<String> = emptyList(),
    val generatorOptions: PasswordGeneratorOptions = PasswordGeneratorOptions(),
    val isGeneratorVisible: Boolean = false,
    val isSaved: Boolean = false,
    /** The Keystore window closed mid-form: the screen prompts and the save resumes. */
    val needsAuthentication: Boolean = false
) {
    val isEditing: Boolean get() = entryId != null

    /** A hashed password is irrecoverable, so it can never be edited: only the metadata can. */
    val isPasswordEditable: Boolean get() = !isEditing || mode == ProtectionMode.ENCRYPTED

    /** The mode is chosen once, at creation time. */
    val isModeEditable: Boolean get() = !isEditing

    /** When editing, an empty password simply means "keep the current one". */
    val canSave: Boolean
        get() = serviceName.isNotBlank() && (isEditing || password.isNotEmpty())
}

@HiltViewModel
class EditEntryViewModel @Inject constructor(
    private val repository: PasswordRepository,
    private val generator: PasswordGenerator,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val entryId: Long? =
        savedStateHandle.get<Long>(Destinations.ARG_ENTRY_ID)?.takeIf { it > 0 }

    /**
     * The service this account is being added to, when the form was opened from inside one. It is
     * a starting value and not a fixed one: the field stays editable, so a name typed there by
     * mistake is corrected the same way as any other.
     */
    private val presetServiceName: String =
        savedStateHandle.get<String>(Destinations.ARG_SERVICE_NAME).orEmpty()

    private val _uiState =
        MutableStateFlow(EditEntryUiState(entryId = entryId, serviceName = presetServiceName))
    val uiState: StateFlow<EditEntryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            entryId?.let { id ->
                repository.getEntry(id)?.let { entry ->
                    _uiState.update {
                        it.copy(
                            serviceName = entry.serviceName,
                            username = entry.username.orEmpty(),
                            hint = entry.hint.orEmpty(),
                            category = entry.category.orEmpty(),
                            isFavorite = entry.isFavorite,
                            mode = entry.mode
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.observeCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun onServiceNameChange(value: String) = _uiState.update { it.copy(serviceName = value) }

    fun onUsernameChange(value: String) = _uiState.update { it.copy(username = value) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value) }

    fun onHintChange(value: String) = _uiState.update { it.copy(hint = value) }

    fun onCategoryChange(value: String) = _uiState.update { it.copy(category = value) }

    fun onFavoriteToggle() = _uiState.update { it.copy(isFavorite = !it.isFavorite) }

    fun onModeChange(mode: ProtectionMode) = _uiState.update { it.copy(mode = mode) }

    fun onGeneratorVisibilityToggle() =
        _uiState.update { it.copy(isGeneratorVisible = !it.isGeneratorVisible) }

    fun onGeneratorOptionsChange(options: PasswordGeneratorOptions) =
        _uiState.update { it.copy(generatorOptions = options) }

    fun onGenerateClick() {
        val options = _uiState.value.generatorOptions
        if (!options.hasAnyCharacterSet) return
        _uiState.update { it.copy(password = generator.generate(options)) }
    }

    fun onSaveClick() {
        val state = _uiState.value
        if (state.serviceName.isBlank()) return
        if (!state.isEditing && state.password.isEmpty()) return

        val draft = EntryDraft(
            serviceName = state.serviceName,
            username = state.username,
            hint = state.hint,
            category = state.category,
            isFavorite = state.isFavorite
        )
        viewModelScope.launch {
            try {
                val id = state.entryId
                if (id == null) {
                    when (state.mode) {
                        ProtectionMode.ENCRYPTED -> repository.createEncrypted(draft, state.password)
                        ProtectionMode.HASHED -> repository.createHashed(draft, state.password)
                    }
                } else {
                    repository.update(
                        id = id,
                        draft = draft,
                        newPassword = state.password.takeIf { it.isNotEmpty() }
                    )
                }
                _uiState.update { it.copy(isSaved = true) }
            } catch (_: AuthenticationRequiredException) {
                // Storing a password encrypts it, and filling in this form easily takes longer than
                // the Keystore window lasts. Ask again instead of losing what the user typed.
                _uiState.update { it.copy(needsAuthentication = true) }
            }
        }
    }

    /** Answer to the prompt a closed Keystore window asked for; the save picks up where it left. */
    fun onAuthenticationResult(granted: Boolean) {
        _uiState.update { it.copy(needsAuthentication = false) }
        if (granted) onSaveClick()
    }
}
