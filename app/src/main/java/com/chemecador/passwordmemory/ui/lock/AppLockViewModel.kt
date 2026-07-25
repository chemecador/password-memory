package com.chemecador.passwordmemory.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chemecador.passwordmemory.data.repository.PasswordRepository
import com.chemecador.passwordmemory.security.AuthenticationRequiredException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val repository: PasswordRepository
) : ViewModel() {

    /**
     * Entries written before the key was bound to authentication are moved across here, right after
     * an unlock, which is the one moment the new key is guaranteed to be usable. It is a no-op once
     * the old key is gone, and safe to interrupt: whatever is left is picked up on the next unlock.
     */
    fun onUnlocked() {
        viewModelScope.launch {
            try {
                repository.rotateEncryptionKey()
            } catch (_: AuthenticationRequiredException) {
                // The window closed mid-rotation. Nothing is lost; the next unlock resumes it.
            }
        }
    }
}
