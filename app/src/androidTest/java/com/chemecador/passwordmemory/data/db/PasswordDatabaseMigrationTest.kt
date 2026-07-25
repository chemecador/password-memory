package com.chemecador.passwordmemory.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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

    private companion object {
        const val TEST_DB = "migration-test.db"
        const val FIRST_VERSION = 1
    }
}
