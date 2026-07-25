package com.chemecador.passwordmemory.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.chemecador.passwordmemory.domain.model.ProtectionMode

/** Bumped with every schema change, alongside a new entry in [PasswordDatabase.MIGRATIONS]. */
const val DATABASE_VERSION = 1

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
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
