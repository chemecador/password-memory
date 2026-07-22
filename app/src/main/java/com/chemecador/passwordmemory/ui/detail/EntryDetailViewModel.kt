package com.chemecador.passwordmemory.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemecador.passwordmemory.data.repository.PasswordRepository
import com.chemecador.passwordmemory.domain.model.PasswordEntry
import com.chemecador.passwordmemory.ui.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GuessResult { NONE, WRONG, CORRECT }

data class EntryDetailUiState(
    val entry: PasswordEntry? = null,
    /** Decrypted password, or the guess the user just got right. Never persisted. */
    val revealedPassword: String? = null,
    val revealFailed: Boolean = false,
    val guess: String = "",
    val guessResult: GuessResult = GuessResult.NONE,
    val isDeleted: Boolean = false
)

@HiltViewModel
class EntryDetailViewModel @Inject constructor(
    private val repository: PasswordRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val entryId: Long = checkNotNull(savedStateHandle[Destinations.ARG_ENTRY_ID])

    private val _uiState = MutableStateFlow(EntryDetailUiState())
    val uiState: StateFlow<EntryDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeEntry(entryId).collect { entry ->
                _uiState.update { it.copy(entry = entry) }
            }
        }
    }

    fun onRevealClick() {
        viewModelScope.launch {
            val password = repository.revealPassword(entryId)
            _uiState.update {
                it.copy(revealedPassword = password, revealFailed = password == null)
            }
        }
    }

    /** Drops the plaintext from memory as soon as the user is done with it. */
    fun onHideClick() = _uiState.update { it.copy(revealedPassword = null, revealFailed = false) }

    fun onGuessChange(value: String) =
        _uiState.update { it.copy(guess = value, guessResult = GuessResult.NONE) }

    /**
     * Verifies a guess. Attempts are unlimited on purpose; on success we show back the very text
     * the user just typed, since the real value was never stored.
     */
    fun onGuessSubmit() {
        val guess = _uiState.value.guess
        if (guess.isEmpty()) return
        viewModelScope.launch {
            val correct = repository.verifyGuess(entryId, guess)
            _uiState.update {
                it.copy(
                    guessResult = if (correct) GuessResult.CORRECT else GuessResult.WRONG,
                    revealedPassword = guess.takeIf { correct }
                )
            }
        }
    }

    fun onFavoriteToggle() {
        val entry = _uiState.value.entry ?: return
        viewModelScope.launch { repository.setFavorite(entry.id, !entry.isFavorite) }
    }

    fun onDeleteConfirm() {
        viewModelScope.launch {
            repository.delete(entryId)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }
}
