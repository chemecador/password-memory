package com.chemecador.passwordmemory.data.importer

/** The selected file is not usable CSV: unknown columns, or malformed quoting. */
class CsvFormatException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Minimal RFC 4180 reader.
 *
 * Password managers happily export values containing commas, quotes and line breaks, so splitting
 * on `,` is not an option. Blank lines are dropped; a row keeps whatever number of fields it has,
 * mapping them to columns is up to the caller.
 */
internal object Csv {

    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false

        fun endRow() {
            row.add(field.toString())
            field.setLength(0)
            if (row.size > 1 || row.first().isNotEmpty()) rows.add(row)
            row = mutableListOf()
        }

        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                when {
                    c != '"' -> field.append(c)
                    text.getOrNull(i + 1) == '"' -> {
                        field.append('"')
                        i++
                    }

                    else -> inQuotes = false
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        row.add(field.toString())
                        field.setLength(0)
                    }

                    '\n' -> endRow()
                    '\r' -> if (text.getOrNull(i + 1) != '\n') endRow()
                    else -> field.append(c)
                }
            }
            i++
        }
        if (inQuotes) throw CsvFormatException("Unterminated quoted field")
        if (field.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }
}
