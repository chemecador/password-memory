package com.chemecador.passwordmemory.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemecador.passwordmemory.data.backup.BackupFormatException
import com.chemecador.passwordmemory.data.backup.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BackupMessage {
    data class Exported(val count: Int) : BackupMessage
    data class Imported(val count: Int) : BackupMessage
    data object WrongPassphrase : BackupMessage
    data object Failed : BackupMessage
}

data class BackupUiState(
    val isBusy: Boolean = false,
    val message: BackupMessage? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun onExport(target: Uri, passphrase: String) = run(passphrase) {
        BackupMessage.Exported(backupManager.export(target, passphrase.toCharArray()))
    }

    fun onImport(source: Uri, passphrase: String) = run(passphrase) {
        BackupMessage.Imported(backupManager.import(source, passphrase.toCharArray()))
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    private fun run(passphrase: String, block: suspend () -> BackupMessage) {
        if (passphrase.isEmpty()) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val message = try {
                block()
            } catch (e: BackupFormatException) {
                BackupMessage.WrongPassphrase
            } catch (e: Exception) {
                BackupMessage.Failed
            }
            _uiState.update { it.copy(isBusy = false, message = message) }
        }
    }
}
