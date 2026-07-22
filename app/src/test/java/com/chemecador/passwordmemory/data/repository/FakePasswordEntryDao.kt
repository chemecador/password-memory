package com.chemecador.passwordmemory.data.repository

import com.chemecador.passwordmemory.data.db.PasswordEntryDao
import com.chemecador.passwordmemory.data.db.PasswordEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory stand-in for Room, mirroring the ordering and filtering of the real queries. */
class FakePasswordEntryDao : PasswordEntryDao {

    private val entries = MutableStateFlow<List<PasswordEntryEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<PasswordEntryEntity>> = entries.map { list ->
        list.sortedWith(
            compareByDescending<PasswordEntryEntity> { it.isFavorite }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.serviceName }
        )
    }

    override fun observeCategories(): Flow<List<String>> = entries.map { list ->
        list.mapNotNull { it.category }
            .filter(String::isNotEmpty)
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    override fun observeById(id: Long): Flow<PasswordEntryEntity?> =
        entries.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getById(id: Long): PasswordEntryEntity? =
        entries.value.firstOrNull { it.id == id }

    override suspend fun getAll(): List<PasswordEntryEntity> = entries.value

    override suspend fun insert(entry: PasswordEntryEntity): Long {
        val id = nextId++
        entries.value += entry.copy(id = id)
        return id
    }

    override suspend fun insertAll(entries: List<PasswordEntryEntity>) {
        entries.forEach { insert(it) }
    }

    override suspend fun update(entry: PasswordEntryEntity) {
        entries.value = entries.value.map { if (it.id == entry.id) entry else it }
    }

    override suspend fun delete(entry: PasswordEntryEntity) = deleteById(entry.id)

    override suspend fun deleteById(id: Long) {
        entries.value = entries.value.filterNot { it.id == id }
    }

    override suspend fun setFavorite(id: Long, isFavorite: Boolean, updatedAt: Long) {
        entries.value = entries.value.map {
            if (it.id == id) it.copy(isFavorite = isFavorite, updatedAt = updatedAt) else it
        }
    }
}
