package com.chemecador.passwordmemory.ui.list

import com.chemecador.passwordmemory.data.repository.EntryDraft
import com.chemecador.passwordmemory.data.repository.FakeCryptoManager
import com.chemecador.passwordmemory.data.repository.FakePasswordEntryDao
import com.chemecador.passwordmemory.data.repository.PasswordRepository
import com.chemecador.passwordmemory.domain.model.ServiceGroup
import com.chemecador.passwordmemory.security.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.security.SecureRandom

/**
 * The list filters entries and then groups them, in that order: a row therefore stands for the
 * accounts that got through the filters, not for everything saved under that service. Getting it
 * the other way round would show "2 accounts" while filtering favourites and open a screen with
 * one of them on it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EntryListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val dao = FakePasswordEntryDao()
    private val repository =
        PasswordRepository(dao, FakeCryptoManager(), PasswordHasher(SecureRandom()))

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `two accounts on the same service are a single row`() = runTest(dispatcher) {
        save(service = "x.com", username = "work")
        save(service = "X.com", username = "personal")
        save(service = "Gmail", username = "me")

        val state = stateOf(EntryListViewModel(repository))

        assertEquals(listOf("gmail", "x"), state.groups.map(ServiceGroup::key))
        assertEquals(2, state.groups.last().size)
        assertNull(state.groups.last().single)
        // The header counts passwords, which is still three of them behind two rows.
        assertEquals(3, state.entries.size)
    }

    @Test
    fun `a search matching the service keeps its accounts together`() = runTest(dispatcher) {
        save(service = "x.com", username = "work")
        save(service = "x.com", username = "personal")
        save(service = "Gmail", username = "me")
        val viewModel = EntryListViewModel(repository)

        viewModel.onQueryChange("x.co")
        val state = stateOf(viewModel)

        assertEquals(listOf("x"), state.groups.map(ServiceGroup::key))
        assertEquals(2, state.groups.single().size)
    }

    @Test
    fun `a search matching a username keeps only that account`() = runTest(dispatcher) {
        save(service = "x.com", username = "work@mail.com")
        save(service = "x.com", username = "personal@mail.com")
        save(service = "Gmail", username = "me")
        val viewModel = EntryListViewModel(repository)

        viewModel.onSearchScopeChange(SearchScope.ANYWHERE)
        viewModel.onQueryChange("work@")
        val state = stateOf(viewModel)

        val group = state.groups.single()
        assertEquals("x", group.key)
        assertEquals("work@mail.com", group.single?.username)
    }

    @Test
    fun `entry search ignores gmail in other accounts until anywhere is selected`() =
        runTest(dispatcher) {
            save(service = "Gmail", username = "personal")
            save(service = "gmail.com", username = "work")
            save(service = "Netflix", username = "demo@gmail.com")
            save(service = "GitHub", username = "developer")
            val viewModel = EntryListViewModel(repository)

            viewModel.onQueryChange("  GMAIL  ")
            val initial = stateOf(viewModel)
            assertEquals(SearchScope.SERVICE_NAME, initial.searchScope)
            assertEquals(listOf("gmail"), initial.groups.map(ServiceGroup::key))
            assertEquals(2, initial.groups.single().size)

            viewModel.onSearchScopeChange(SearchScope.ANYWHERE)
            advanceUntilIdle()
            val anywhere = viewModel.uiState.value
            assertEquals("  GMAIL  ", anywhere.query)
            assertEquals(listOf("gmail", "netflix"), anywhere.groups.map(ServiceGroup::key))

            viewModel.onSearchScopeChange(SearchScope.SERVICE_NAME)
            advanceUntilIdle()
            assertEquals(initial, viewModel.uiState.value)
        }

    @Test
    fun `anywhere includes hints and categories but never passwords`() = runTest(dispatcher) {
        save(service = "Mail", hint = "Recovery via GMAIL")
        save(service = "Forum", category = "Gmail accounts")
        save(service = "Bank")
        val viewModel = EntryListViewModel(repository)

        viewModel.onQueryChange("gmail")
        assertEquals(emptyList<String>(), stateOf(viewModel).entries.map { it.serviceName })

        viewModel.onSearchScopeChange(SearchScope.ANYWHERE)
        advanceUntilIdle()
        assertEquals(listOf("Forum", "Mail"), viewModel.uiState.value.entries.map { it.serviceName })

        viewModel.onQueryChange("s3cr3t")
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.entries.size)
    }

    @Test
    fun `changing scope and clearing search preserve category and favorite filters`() =
        runTest(dispatcher) {
            save(service = "Gmail", category = "Work", favorite = true)
            save(service = "Netflix", username = "demo@gmail.com", category = "Work", favorite = true)
            save(service = "Forum", username = "demo@gmail.com", category = "Work")
            save(service = "Shop", username = "demo@gmail.com", category = "Personal", favorite = true)
            val viewModel = EntryListViewModel(repository)
            viewModel.onQueryChange("gmail")
            viewModel.onCategorySelected("Work")
            viewModel.onFavoritesOnlyToggle()
            assertEquals(listOf("Gmail"), stateOf(viewModel).entries.map { it.serviceName })

            viewModel.onSearchScopeChange(SearchScope.ANYWHERE)
            advanceUntilIdle()
            assertEquals(listOf("Gmail", "Netflix"), viewModel.uiState.value.entries.map { it.serviceName })

            viewModel.onQueryChange("")
            advanceUntilIdle()
            val cleared = viewModel.uiState.value
            assertEquals(SearchScope.ANYWHERE, cleared.searchScope)
            assertEquals("Work", cleared.selectedCategory)
            assertEquals(true, cleared.favoritesOnly)
            assertEquals(listOf("Gmail", "Netflix"), cleared.entries.map { it.serviceName })
        }

    @Test
    fun `filtering by favourites leaves the row standing for the favourite account`() =
        runTest(dispatcher) {
            save(service = "x.com", username = "work")
            save(service = "x.com", username = "personal", favorite = true)
            val viewModel = EntryListViewModel(repository)

            viewModel.onFavoritesOnlyToggle()
            val state = stateOf(viewModel)

            val group = state.groups.single()
            assertEquals(1, group.size)
            assertEquals("personal", group.single?.username)
        }

    @Test
    fun `filtering by category narrows the accounts behind the service`() = runTest(dispatcher) {
        save(service = "x.com", username = "work", category = "Work")
        save(service = "x.com", username = "personal", category = "Personal")
        val viewModel = EntryListViewModel(repository)

        viewModel.onCategorySelected("Work")
        val state = stateOf(viewModel)

        val group = state.groups.single()
        assertEquals(1, group.size)
        assertEquals("work", group.single?.username)
    }

    private suspend fun save(
        service: String,
        username: String? = null,
        hint: String? = null,
        category: String? = null,
        favorite: Boolean = false
    ) {
        repository.createEncrypted(
            EntryDraft(
                serviceName = service,
                username = username,
                hint = hint,
                category = category,
                isFavorite = favorite
            ),
            password = "s3cr3t"
        )
    }

    /** [EntryListUiState] only builds itself while something is collecting it. */
    private fun TestScope.stateOf(viewModel: EntryListViewModel): EntryListUiState {
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        return viewModel.uiState.value
    }
}
