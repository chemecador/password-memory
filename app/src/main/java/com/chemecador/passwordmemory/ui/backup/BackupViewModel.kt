package com.chemecador.passwordmemory.ui.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemecador.passwordmemory.data.backup.BackupFormatException
import com.chemecador.passwordmemory.data.backup.BackupManager
import com.chemecador.passwordmemory.data.importer.CredentialImportManager
import com.chemecador.passwordmemory.data.importer.CredentialImportPreview
import com.chemecador.passwordmemory.data.importer.CsvFormatException
import com.chemecador.passwordmemory.security.AuthenticationRequiredException
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

    /** Not an error: the Keystore window closed, and the work is waiting for a fresh unlock. */
    data object AuthenticationRequired : BackupMessage

    data object CsvUnreadable : BackupMessage
    data object CsvEmpty : BackupMessage
    data object CsvSourceDeleted : BackupMessage
    data object CsvSourceKept : BackupMessage
}

/** What a selected CSV holds, as counts only: the parsed passwords never reach the UI layer. */
data class CsvPreviewUi(
    val fresh: Int,
    val duplicates: Int,
    val skipped: Int
)

data class BackupUiState(
    val isBusy: Boolean = false,
    val message: BackupMessage? = null,
    val csvPreview: CsvPreviewUi? = null,
    val askToDeleteCsv: Boolean = false
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val importManager: CredentialImportManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    /** Held between the preview and the confirmation, and dropped as soon as either ends. */
    private var pendingCsv: CredentialImportPreview? = null
    private var pendingSource: Uri? = null

    /** Work that stopped for want of a fresh authentication. Never holds a passphrase of its own. */
    private var pendingRetry: (suspend () -> BackupMessage?)? = null

    fun onExport(target: Uri, passphrase: String) = runWithPassphrase(passphrase) {
        BackupMessage.Exported(backupManager.export(target, passphrase.toCharArray()))
    }

    fun onImport(source: Uri, passphrase: String) = runWithPassphrase(passphrase) {
        BackupMessage.Imported(backupManager.import(source, passphrase.toCharArray()))
    }

    /** Reads the CSV and reports what it would add; nothing is stored yet. */
    fun onCsvSelected(source: Uri) = runBusy {
        val preview = importManager.preview(source)
        if (preview.isEmpty) {
            BackupMessage.CsvEmpty
        } else {
            pendingCsv = preview
            pendingSource = source
            _uiState.update {
                it.copy(
                    csvPreview = CsvPreviewUi(
                        fresh = preview.fresh.size,
                        duplicates = preview.duplicates.size,
                        skipped = preview.skipped
                    )
                )
            }
            null
        }
    }

    fun onCsvConfirm(includeDuplicates: Boolean) {
        val preview = pendingCsv ?: return
        pendingCsv = null
        _uiState.update { it.copy(csvPreview = null) }
        runBusy {
            val entries = preview.fresh + if (includeDuplicates) preview.duplicates else emptyList()
            val count = importManager.import(entries)
            _uiState.update { it.copy(askToDeleteCsv = pendingSource != null) }
            BackupMessage.Imported(count)
        }
    }

    fun onCsvCancel() {
        pendingCsv = null
        pendingSource = null
        _uiState.update { it.copy(csvPreview = null) }
    }

    /** Offered right after importing: the CSV holds every password in clear text. */
    fun onDeleteCsvSource() {
        val source = pendingSource ?: return
        pendingSource = null
        _uiState.update { it.copy(askToDeleteCsv = false) }
        runBusy {
            if (importManager.deleteSource(source)) {
                BackupMessage.CsvSourceDeleted
            } else {
                BackupMessage.CsvSourceKept
            }
        }
    }

    fun onKeepCsvSource() {
        pendingSource = null
        _uiState.update { it.copy(askToDeleteCsv = false, message = BackupMessage.CsvSourceKept) }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    private fun runWithPassphrase(passphrase: String, block: suspend () -> BackupMessage) {
        if (passphrase.isEmpty()) return
        runBusy(block)
    }

    /**
     * Resumes the operation a closed Keystore window interrupted. Exporting decrypts every entry,
     * and both imports encrypt what they store, so any of them can land out of window — the file
     * picker alone takes longer than the key stays open.
     */
    fun onAuthenticated() {
        val retry = pendingRetry ?: return
        pendingRetry = null
        runBusy(retry)
    }

    fun onAuthenticationDeclined() {
        pendingRetry = null
    }

    private fun runBusy(block: suspend () -> BackupMessage?) {
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val message = try {
                block()
            } catch (e: AuthenticationRequiredException) {
                pendingRetry = block
                BackupMessage.AuthenticationRequired
            } catch (e: BackupFormatException) {
                BackupMessage.WrongPassphrase
            } catch (e: CsvFormatException) {
                BackupMessage.CsvUnreadable
            } catch (e: Exception) {
                BackupMessage.Failed
            }
            _uiState.update { it.copy(isBusy = false, message = message ?: it.message) }
        }
    }
}
