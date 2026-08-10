package com.chemecador.passwordmemory.data.backup

import org.junit.Assert.assertThrows
import org.junit.Test

class BackupPassphrasePolicyTest {

    @Test
    fun `an export passphrase at the minimum length is accepted`() {
        requireStrongExportPassphrase(CharArray(BackupManager.MIN_EXPORT_PASSPHRASE_LENGTH))
    }

    @Test
    fun `an export passphrase below the minimum length is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            requireStrongExportPassphrase(
                CharArray(BackupManager.MIN_EXPORT_PASSPHRASE_LENGTH - 1)
            )
        }
    }
}
