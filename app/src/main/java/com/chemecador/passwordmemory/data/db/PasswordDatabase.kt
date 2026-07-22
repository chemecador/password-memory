package com.chemecador.passwordmemory.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.chemecador.passwordmemory.domain.model.ProtectionMode

class ProtectionModeConverter {

    @TypeConverter
    fun toStored(mode: ProtectionMode): String = mode.name

    @TypeConverter
    fun fromStored(value: String): ProtectionMode = ProtectionMode.valueOf(value)
}

@Database(entities = [PasswordEntryEntity::class], version = 1, exportSchema = false)
@TypeConverters(ProtectionModeConverter::class)
abstract class PasswordDatabase : RoomDatabase() {

    abstract fun entryDao(): PasswordEntryDao

    companion object {
        const val NAME = "password_memory.db"
    }
}
