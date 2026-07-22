package com.chemecador.passwordmemory.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordEntryDao {

    @Query("SELECT * FROM entries ORDER BY is_favorite DESC, service_name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<PasswordEntryEntity>>

    @Query("SELECT DISTINCT category FROM entries WHERE category IS NOT NULL AND category != '' ORDER BY category COLLATE NOCASE ASC")
    fun observeCategories(): Flow<List<String>>

    @Query("SELECT * FROM entries WHERE id = :id")
    fun observeById(id: Long): Flow<PasswordEntryEntity?>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: Long): PasswordEntryEntity?

    @Query("SELECT * FROM entries")
    suspend fun getAll(): List<PasswordEntryEntity>

    @Insert
    suspend fun insert(entry: PasswordEntryEntity): Long

    @Insert
    suspend fun insertAll(entries: List<PasswordEntryEntity>)

    @Update
    suspend fun update(entry: PasswordEntryEntity)

    @Delete
    suspend fun delete(entry: PasswordEntryEntity)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE entries SET is_favorite = :isFavorite, updated_at = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean, updatedAt: Long)
}
