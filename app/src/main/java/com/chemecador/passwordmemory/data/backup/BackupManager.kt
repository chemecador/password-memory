package com.chemecador.passwordmemory.data.backup

import android.content.Context
import android.net.Uri
import com.chemecador.passwordmemory.data.io.readBytesLimited
import com.chemecador.passwordmemory.data.repository.PasswordRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes and reads backup files chosen by the user through the Storage Access Framework. The app
 * never uploads them anywhere: moving the file around is entirely up to the user.
 */
@Singleton
class BackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: PasswordRepository,
    private val codec: BackupCodec
) {

    /** @return how many entries were written. */
    suspend fun export(target: Uri, passphrase: CharArray): Int = withContext(Dispatchers.IO) {
        requireStrongExportPassphrase(passphrase)
        val entries = repository.exportEntries()
        val bytes = codec.encode(entries, passphrase)
        context.contentResolver.openOutputStream(target, "wt")?.use { it.write(bytes) }
            ?: throw IOException("Cannot write to the selected file")
        entries.size
    }

    /** @return how many entries were restored. */
    suspend fun import(source: Uri, passphrase: CharArray): Int = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(source)?.use {
            it.readBytesLimited(MAX_BACKUP_IMPORT_BYTES)
        }
            ?: throw IOException("Cannot read the selected file")
        repository.importEntries(codec.decode(bytes, passphrase))
    }

    companion object {
        const val MIME_TYPE = "application/octet-stream"

        /** New backups require a one-time passphrase long enough to resist casual guessing. */
        const val MIN_EXPORT_PASSPHRASE_LENGTH = 12

        /** Hard ceiling for an encrypted backup loaded in memory from a SAF provider. */
        const val MAX_BACKUP_IMPORT_BYTES = 16 * 1024 * 1024

        fun suggestedFileName(timestampMillis: Long = System.currentTimeMillis()): String {
            val stamp = android.text.format.DateFormat.format("yyyyMMdd-HHmmss", timestampMillis)
            return "password-memory-$stamp.pmbk"
        }
    }
}

/**
 * Applies only to new exports. Imports intentionally accept older backups protected with a shorter
 * passphrase so tightening the policy never locks users out of their existing safety copy.
 */
internal fun requireStrongExportPassphrase(passphrase: CharArray) {
    require(passphrase.size >= BackupManager.MIN_EXPORT_PASSPHRASE_LENGTH) {
        "Export passphrase must contain at least " +
            "${BackupManager.MIN_EXPORT_PASSPHRASE_LENGTH} characters"
    }
}
