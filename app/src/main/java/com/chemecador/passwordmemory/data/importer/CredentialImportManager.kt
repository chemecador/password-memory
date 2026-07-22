package com.chemecador.passwordmemory.data.importer

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.chemecador.passwordmemory.data.backup.BackupEntry
import com.chemecador.passwordmemory.data.repository.PasswordRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What a CSV would add to the database, before anything is written.
 *
 * @property fresh entries whose service and username are not in the database yet.
 * @property duplicates entries that already exist, or that repeat inside the file itself.
 * @property skipped rows with no password or no recognisable service name.
 */
data class CredentialImportPreview(
    val fresh: List<BackupEntry>,
    val duplicates: List<BackupEntry>,
    val skipped: Int
) {
    val isEmpty: Boolean get() = fresh.isEmpty() && duplicates.isEmpty()
}

/**
 * Imports credentials exported from Google Password Manager or Chrome.
 *
 * The file is read straight from the user's chosen location and parsed in memory: it is never
 * copied into the app's storage, because it holds every password in clear text. Once the entries
 * are in the database, [deleteSource] offers to get rid of it.
 */
@Singleton
class CredentialImportManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: PasswordRepository,
    private val parser: CredentialCsvParser
) {

    suspend fun preview(source: Uri): CredentialImportPreview = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(source)?.use { it.readBytes() }
            ?: throw IOException("Cannot read the selected file")
        val parsed = parser.parse(bytes)

        val seen = repository.existingKeys().toMutableSet()
        val fresh = mutableListOf<BackupEntry>()
        val duplicates = mutableListOf<BackupEntry>()
        parsed.entries.forEach { entry ->
            val key = EntryKey.of(entry.serviceName, entry.username)
            if (seen.add(key)) fresh += entry else duplicates += entry
        }
        CredentialImportPreview(fresh = fresh, duplicates = duplicates, skipped = parsed.skipped)
    }

    /** @return how many entries were stored. */
    suspend fun import(entries: List<BackupEntry>): Int = repository.importEntries(entries)

    /**
     * Deletes the plaintext CSV, best effort: the provider may not support deletion, or may not
     * have granted write access.
     *
     * @return true if the file is gone.
     */
    suspend fun deleteSource(source: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            DocumentsContract.deleteDocument(context.contentResolver, source)
        }.getOrDefault(false)
    }

    companion object {
        /**
         * Deliberately wide: providers disagree on the type of a `.csv` (`text/csv`,
         * `text/comma-separated-values`, `application/vnd.ms-excel`…) and a filtered picker would
         * grey out the very file the user is looking for.
         */
        val MIME_TYPES = arrayOf("*/*")
    }
}
