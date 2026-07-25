package com.chemecador.passwordmemory.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemecador.passwordmemory.data.settings.SettingsRepository
import com.chemecador.passwordmemory.domain.model.AppSettings
import com.chemecador.passwordmemory.domain.model.AutoLockTimeout
import com.chemecador.passwordmemory.domain.model.ClipboardClearDelay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<AppSettings> = settings.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = settings.current()
    )

    fun onAutoLockSelected(timeout: AutoLockTimeout) = settings.setAutoLock(timeout)

    fun onClipboardClearSelected(delay: ClipboardClearDelay) = settings.setClipboardClear(delay)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
