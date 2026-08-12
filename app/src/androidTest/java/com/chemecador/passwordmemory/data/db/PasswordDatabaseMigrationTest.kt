package com.chemecador.passwordmemory.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Walks a database created at the oldest shipped version through every migration and checks the
 * result against the schema Room exported for the current one. Without a destructive fallback, a
 * schema change that forgets its migration crashes on the user's device — this catches it here.
 */
@RunWith(AndroidJUnit4::class)
class PasswordDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PasswordDatabase::class.java
    )

    @Test
    fun migratesFromFirstVersionToCurrent() {
        helper.createDatabase(TEST_DB, FIRST_VERSION).close()
        helper.runMigrationsAndValidate(
            TEST_DB,
            DATABASE_VERSION,
            true,
            *PasswordDatabase.MIGRATIONS
        ).close()
    }

    /**
     * [MIGRATION_1_2] changes no schema, so the validation above would pass just as happily if it
     * did nothing at all. What it promises is about the rows, and only reading them back says so.
     */
    @Test
    fun migrationTwoStripsUrlPrefixesFromServiceNames() {
        helper.createDatabase(TEST_DB, FIRST_VERSION).use { db ->
            NAMES.keys.forEachIndexed { index, stored -> db.insertEntry(id = index + 1L, stored) }
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            DATABASE_VERSION,
            true,
            *PasswordDatabase.MIGRATIONS
        )

        migrated.use { db ->
            NAMES.entries.forEachIndexed { index, (stored, expected) ->
                assertEquals(
                    "'$stored' should have migrated to '$expected'",
                    expected,
                    db.serviceNameOf(id = index + 1L)
                )
            }
        }
    }

    private fun SupportSQLiteDatabase.insertEntry(id: Long, serviceName: String) = execSQL(
        """
        INSERT INTO entries
            (id, service_name, is_favorite, mode, cipher_text, iv, created_at, updated_at)
        VALUES (?, ?, 0, 'ENCRYPTED', x'00', x'00', 1, 1)
        """.trimIndent(),
        arrayOf<Any>(id, serviceName)
    )

    private fun SupportSQLiteDatabase.serviceNameOf(id: Long): String =
        query("SELECT service_name FROM entries WHERE id = ?", arrayOf(id)).use { cursor ->
            cursor.moveToFirst()
            cursor.getString(0)
        }

    private companion object {
        const val TEST_DB = "migration-test.db"
        const val FIRST_VERSION = 1

        /** What version 1 could hold, against what the user should see after the update. */
        val NAMES = linkedMapOf(
            "https://netflix.com" to "netflix.com",
            "http://netflix.com" to "netflix.com",
            "www.netflix.com" to "netflix.com",
            "https://www.netflix.com" to "netflix.com",
            "HTTPS://WWW.Netflix.com" to "Netflix.com",
            "  https://netflix.com  " to "netflix.com",
            // Untouched: no prefix, an incomplete one, and one with nothing behind it to keep.
            "Netflix" to "Netflix",
            "wwwgrande" to "wwwgrande",
            "https://" to "https://"
        )
    }
}
