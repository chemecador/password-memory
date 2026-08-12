package com.chemecador.passwordmemory.data.importer

import com.chemecador.passwordmemory.data.backup.BackupEntry
import com.chemecador.passwordmemory.domain.model.ProtectionMode
import com.chemecador.passwordmemory.domain.model.ServiceName
import javax.inject.Inject
import javax.inject.Singleton

/** Result of reading an exported CSV: what can be imported, and how many rows were unusable. */
data class ParsedCsv(
    val entries: List<BackupEntry>,
    val skipped: Int
)

/**
 * Reads the CSV that Google Password Manager and Chrome produce (`name,url,username,password,note`)
 * and turns it into [BackupEntry] rows.
 *
 * Columns are located by header name, never by position: the order and the presence of `note` change
 * between Chrome versions, and other managers use their own spellings. Everything is imported as
 * [ProtectionMode.ENCRYPTED]; hashing a password the user has not chosen to memorise makes no sense.
 */
@Singleton
class CredentialCsvParser @Inject constructor() {

    fun parse(bytes: ByteArray, now: Long = System.currentTimeMillis()): ParsedCsv {
        val text = bytes.toString(Charsets.UTF_8).removePrefix(BYTE_ORDER_MARK)
        val rows = Csv.parse(text)
        val header = rows.firstOrNull() ?: return ParsedCsv(emptyList(), skipped = 0)

        val columns = header.map { it.trim().lowercase() }
        val passwordAt = columns.indexOfFirst { it in PASSWORD_COLUMNS }
        if (passwordAt < 0) {
            throw CsvFormatException("No password column found in the header")
        }
        val nameAt = columns.indexOfFirst { it in NAME_COLUMNS }
        val urlAt = columns.indexOfFirst { it in URL_COLUMNS }
        val usernameAt = columns.indexOfFirst { it in USERNAME_COLUMNS }
        val noteAt = columns.indexOfFirst { it in NOTE_COLUMNS }

        var skipped = 0
        val entries = rows.drop(1).mapNotNull { row ->
            val password = row.field(passwordAt)
            val url = row.field(urlAt)
            // Chrome writes the URL into the name column as often as not, so the name is cleaned
            // here rather than at the end: the preview has to show what will actually be stored.
            val service = (row.field(nameAt) ?: url?.let(::hostOf))?.let(ServiceName::clean)
            if (password == null || service == null) {
                skipped++
                return@mapNotNull null
            }
            BackupEntry(
                serviceName = service,
                username = row.field(usernameAt),
                hint = listOfNotNull(url, row.field(noteAt)).joinToString("\n").ifEmpty { null },
                category = null,
                isFavorite = false,
                mode = ProtectionMode.ENCRYPTED,
                plainPassword = password,
                createdAt = now,
                updatedAt = now
            )
        }
        return ParsedCsv(entries, skipped)
    }

    private fun List<String>.field(index: Int): String? =
        if (index < 0) null else getOrNull(index)?.trim()?.takeIf(String::isNotEmpty)

    /**
     * Best-effort service name out of a URL. Chrome writes Android credentials as
     * `android://<hash>@com.example.app/`, so the package name is what survives there.
     */
    private fun hostOf(url: String): String? {
        val withoutScheme = url.substringAfter("://", url)
        return withoutScheme
            .substringBefore('/')
            .substringBefore('?')
            .substringAfterLast('@')
            .substringBefore(':')
            .removePrefix("www.")
            .takeIf(String::isNotEmpty)
    }

    private companion object {
        /** UTF-8 BOM: Chrome on Windows writes one, and it would corrupt the first header cell. */
        const val BYTE_ORDER_MARK = "\uFEFF"
        val NAME_COLUMNS = setOf("name", "title", "display name")
        val URL_COLUMNS = setOf("url", "urls", "website", "login_uri", "web site")
        val USERNAME_COLUMNS = setOf("username", "user name", "login_username", "login", "email")
        val PASSWORD_COLUMNS = setOf("password", "login_password", "pwd")
        val NOTE_COLUMNS = setOf("note", "notes", "comments")
    }
}
