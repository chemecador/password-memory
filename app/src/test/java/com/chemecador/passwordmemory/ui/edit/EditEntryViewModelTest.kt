package com.chemecador.passwordmemory.ui.edit

import androidx.lifecycle.SavedStateHandle
import com.chemecador.passwordmemory.data.repository.FakeCryptoManager
import com.chemecador.passwordmemory.data.repository.FakePasswordEntryDao
import com.chemecador.passwordmemory.data.repository.PasswordRepository
import com.chemecador.passwordmemory.security.PasswordGenerator
import com.chemecador.passwordmemory.security.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.SecureRandom

/**
 * Saving encrypts, so it needs the Keystore key to be inside its authentication window — and
 * filling in the form comfortably outlasts that window. The first version of this screen let the
 * resulting exception escape the coroutine and took the whole app down with it, losing everything
 * the user had typed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditEntryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val dao = FakePasswordEntryDao()
    private val crypto = FakeCryptoManager()
    private val repository = PasswordRepository(dao, crypto, PasswordHasher(SecureRandom()))

    private fun viewModel() = EditEntryViewModel(
        repository = repository,
        generator = PasswordGenerator(SecureRandom()),
        savedStateHandle = SavedStateHandle()
    )

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `saving inside the key window stores the entry`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onServiceNameChange("Gmail")
        viewModel.onPasswordChange("s3cr3t")

        viewModel.onSaveClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals(1, dao.getAll().size)
    }

    @Test
    fun `a closed key window asks for authentication instead of crashing`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onServiceNameChange("Gmail")
        viewModel.onPasswordChange("s3cr3t")
        crypto.simulateClosedAuthWindow()

        viewModel.onSaveClick()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.needsAuthentication)
        assertFalse(viewModel.uiState.value.isSaved)
        // Nothing half-written, and the form still holds what the user typed.
        assertTrue(dao.getAll().isEmpty())
        assertEquals("s3cr3t", viewModel.uiState.value.password)
    }

    @Test
    fun `authenticating finishes the save that was interrupted`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onServiceNameChange("Gmail")
        viewModel.onPasswordChange("s3cr3t")
        crypto.simulateClosedAuthWindow()
        viewModel.onSaveClick()
        advanceUntilIdle()

        crypto.simulateOpenAuthWindow()
        viewModel.onAuthenticationResult(granted = true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        assertFalse(viewModel.uiState.value.needsAuthentication)
        assertEquals(1, dao.getAll().size)
    }

    @Test
    fun `declining leaves the form untouched so nothing typed is lost`() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onServiceNameChange("Gmail")
        viewModel.onPasswordChange("s3cr3t")
        crypto.simulateClosedAuthWindow()
        viewModel.onSaveClick()
        advanceUntilIdle()

        viewModel.onAuthenticationResult(granted = false)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.needsAuthentication)
        assertFalse(viewModel.uiState.value.isSaved)
        assertEquals("Gmail", viewModel.uiState.value.serviceName)
        assertEquals("s3cr3t", viewModel.uiState.value.password)
    }
}
