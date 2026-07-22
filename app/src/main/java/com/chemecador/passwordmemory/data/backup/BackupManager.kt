package com.chemecador.passwordmemory.data.backup

import android.content.Context
import android.net.Uri
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
        val entries = repository.exportEntries()
        val bytes = codec.encode(entries, passphrase)
        context.contentResolver.openOutputStream(target, "wt")?.use { it.write(bytes) }
            ?: throw IOException("Cannot write to the selected file")
        entries.size
    }

    /** @return how many entries were restored. */
    suspend fun import(source: Uri, passphrase: CharArray): Int = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(source)?.use { it.readBytes() }
            ?: throw IOException("Cannot read the selected file")
        repository.importEntries(codec.decode(bytes, passphrase))
    }

    companion object {
        const val MIME_TYPE = "application/octet-stream"

        fun suggestedFileName(timestampMillis: Long = System.currentTimeMillis()): String {
            val stamp = android.text.format.DateFormat.format("yyyyMMdd-HHmmss", timestampMillis)
            return "password-memory-$stamp.pmbk"
        }
    }
}
