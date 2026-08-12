package com.chemecador.passwordmemory.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chemecador.passwordmemory.domain.model.ProtectionMode
import com.chemecador.passwordmemory.domain.model.ServiceName

/** Bumped with every schema change, alongside a new entry in [PasswordDatabase.MIGRATIONS]. */
const val DATABASE_VERSION = 2

/**
 * Strips from the stored names the URL prefixes that [ServiceName.clean] now removes on the way in.
 *
 * The schema is untouched: this migration exists for the rows written before the rule did, so an
 * entry pasted as `https://www.netflix.com` sorts under N like the one typed as `netflix.com`, and
 * shares its row instead of sitting under H on its own.
 *
 * `updated_at` is deliberately left alone — this is not an edit the user made.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE entries SET service_name = trim(service_name)")
        // The order matches ServiceName.PREFIXES, so `https://www.x.com` loses both. `_%` demands
        // at least one character after the prefix, which keeps a name that is nothing but a prefix
        // from being emptied — the column is NOT NULL and the field is required. LIKE is case
        // insensitive over ASCII, which is all these prefixes are made of.
        listOf("https://" to 9, "http://" to 8, "www." to 5).forEach { (prefix, from) ->
            db.execSQL(
                """
                UPDATE entries
                SET service_name = trim(substr(service_name, $from))
                WHERE service_name LIKE '${prefix}_%'
                """.trimIndent()
            )
        }
    }
}

class ProtectionModeConverter {

    @TypeConverter
    fun toStored(mode: ProtectionMode): String = mode.name

    @TypeConverter
    fun fromStored(value: String): ProtectionMode = ProtectionMode.valueOf(value)
}

/**
 * Schemas are exported to `app/schemas` and committed: they are what
 * `PasswordEntryMigrationTest` diffs a migrated database against, and the only record of what
 * version 1 looked like once the entity has moved on. There is deliberately no destructive
 * fallback — losing entries this app cannot re-download is worse than failing to open.
 */
@Database(entities = [PasswordEntryEntity::class], version = DATABASE_VERSION, exportSchema = true)
@TypeConverters(ProtectionModeConverter::class)
abstract class PasswordDatabase : RoomDatabase() {

    abstract fun entryDao(): PasswordEntryDao

    companion object {
        const val NAME = "password_memory.db"

        /** Every schema change adds its migration here, in order. */
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
    }
}
