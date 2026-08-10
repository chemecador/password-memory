package com.chemecador.passwordmemory

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.chemecador.passwordmemory.autofill.AutofillRequestOrigin
import com.chemecador.passwordmemory.data.db.PasswordDatabase
import com.chemecador.passwordmemory.data.db.PasswordEntryEntity
import com.chemecador.passwordmemory.data.repository.PasswordRepository
import com.chemecador.passwordmemory.data.settings.SettingsRepository
import com.chemecador.passwordmemory.domain.model.AutoLockTimeout
import com.chemecador.passwordmemory.domain.model.ClipboardClearDelay
import com.chemecador.passwordmemory.domain.model.ProtectionMode
import com.chemecador.passwordmemory.security.CryptoManager
import com.chemecador.passwordmemory.security.EncryptedPayload
import com.chemecador.passwordmemory.security.PasswordGenerator
import com.chemecador.passwordmemory.security.PasswordGeneratorOptions
import com.chemecador.passwordmemory.security.PasswordHasher
import com.chemecador.passwordmemory.ui.autofill.AutofillSecurityConfirmationScreen
import com.chemecador.passwordmemory.ui.common.SecureClipboard
import com.chemecador.passwordmemory.ui.detail.EntryDetailScreen
import com.chemecador.passwordmemory.ui.detail.EntryDetailViewModel
import com.chemecador.passwordmemory.ui.edit.EditEntryScreen
import com.chemecador.passwordmemory.ui.edit.EditEntryViewModel
import com.chemecador.passwordmemory.ui.list.EntryListScreen
import com.chemecador.passwordmemory.ui.list.EntryListViewModel
import com.chemecador.passwordmemory.ui.lock.LockScreen
import com.chemecador.passwordmemory.ui.navigation.Destinations
import com.chemecador.passwordmemory.ui.privacy.PrivacyPolicyScreen
import com.chemecador.passwordmemory.ui.settings.SettingsScreen
import com.chemecador.passwordmemory.ui.settings.SettingsViewModel
import com.chemecador.passwordmemory.ui.theme.PasswordMemoryTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.SecureRandom

/**
 * Renders production screens with a temporary in-memory vault and public sample data.
 *
 * The host activity belongs to the instrumented test APK. Neither production activity loses
 * FLAG_SECURE, and the sample passwords never reach the installed app database or Android
 * Keystore. `play-store/capture-screenshots.ps1` runs this class at the exact Play dimensions.
 */
@RunWith(AndroidJUnit4::class)
class PlayStoreScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var database: PasswordDatabase
    private lateinit var repository: PasswordRepository
    private lateinit var settings: SettingsRepository

    @Before
    fun createTemporaryVault() {
        database = Room.inMemoryDatabaseBuilder(context, PasswordDatabase::class.java).build()
        val hasher = PasswordHasher(SecureRandom())
        repository = PasswordRepository(database.entryDao(), ScreenshotCryptoManager, hasher)
        settings = SettingsRepository(context).also {
            it.setAutoLock(AutoLockTimeout.IMMEDIATE)
            it.setClipboardClear(ClipboardClearDelay.SECONDS_30)
        }

        val hashed = hasher.hash(DEMO_HASHED_PASSWORD)
        runBlocking {
            database.entryDao().insertAll(
                listOf(
                    PasswordEntryEntity(
                        id = ENCRYPTED_ENTRY_ID,
                        serviceName = "Example Mail",
                        username = "demo.user@example.test",
                        hint = "https://mail.example.test",
                        category = "Personal",
                        isFavorite = true,
                        mode = ProtectionMode.ENCRYPTED,
                        cipherText = byteArrayOf(1, 2, 3),
                        iv = ByteArray(12) { it.toByte() },
                        createdAt = SAMPLE_TIME,
                        updatedAt = SAMPLE_TIME
                    ),
                    PasswordEntryEntity(
                        id = 2,
                        serviceName = "Sample Stream",
                        username = "viewer@example.test",
                        category = "Personal",
                        mode = ProtectionMode.ENCRYPTED,
                        cipherText = byteArrayOf(4, 5, 6),
                        iv = ByteArray(12) { (it + 1).toByte() },
                        createdAt = SAMPLE_TIME,
                        updatedAt = SAMPLE_TIME
                    ),
                    PasswordEntryEntity(
                        id = HASHED_ENTRY_ID,
                        serviceName = "Demo Forum",
                        mode = ProtectionMode.HASHED,
                        hash = hashed.hash,
                        salt = hashed.salt,
                        createdAt = SAMPLE_TIME,
                        updatedAt = SAMPLE_TIME
                    ),
                    PasswordEntryEntity(
                        id = 4,
                        serviceName = "Test Wi-Fi",
                        category = "Home",
                        mode = ProtectionMode.ENCRYPTED,
                        cipherText = byteArrayOf(7, 8, 9),
                        iv = ByteArray(12) { (it + 2).toByte() },
                        createdAt = SAMPLE_TIME,
                        updatedAt = SAMPLE_TIME
                    )
                )
            )
        }
    }

    @After
    fun closeTemporaryVault() {
        database.close()
    }

    @Test
    fun captureVaultOverview() {
        val viewModel = EntryListViewModel(repository)
        setScreen {
            EntryListScreen(
                onEntryClick = {},
                onCreateClick = {},
                onBackupClick = {},
                onAutofillClick = {},
                onSettingsClick = {},
                onPrivacyClick = {},
                modifier = storePreviewModifier(),
                viewModel = viewModel
            )
        }

        waitForText("Example Mail")
        saveRoot("01-vault-overview.jpg")
    }

    @Test
    fun captureUnlockAnimation() {
        val viewModel = EntryListViewModel(repository)
        val unlocking = mutableStateOf(false)
        setScreen {
            Box(modifier = Modifier.fillMaxSize()) {
                EntryListScreen(
                    onEntryClick = {},
                    onCreateClick = {},
                    onBackupClick = {},
                    onAutofillClick = {},
                    onSettingsClick = {},
                    onPrivacyClick = {},
                    modifier = storePreviewModifier(),
                    viewModel = viewModel
                )
                LockScreen(
                    unavailable = false,
                    onUnlockClick = {},
                    modifier = storePreviewModifier(),
                    unlocking = unlocking.value
                )
            }
        }

        waitForText("Example Mail")
        composeRule.mainClock.autoAdvance = false
        composeRule.runOnIdle { unlocking.value = true }
        composeRule.mainClock.advanceTimeBy(UNLOCK_FRAME_MILLIS)
        composeRule.waitForIdle()
        saveRoot("02-unlock-animation.jpg")
        composeRule.mainClock.autoAdvance = true
    }

    @Test
    fun capturePasswordGenerator() {
        val viewModel = EditEntryViewModel(
            repository = repository,
            generator = PasswordGenerator(SecureRandom()),
            savedStateHandle = SavedStateHandle(
                mapOf(Destinations.ARG_ENTRY_ID to Destinations.NEW_ENTRY_ID)
            )
        ).apply {
            onServiceNameChange("Example Mail")
            onUsernameChange("demo.user@example.test")
            onPasswordChange("Generated-Demo-Only!42")
            onCategoryChange("Work")
            onGeneratorOptionsChange(PasswordGeneratorOptions(length = 24))
            onGeneratorVisibilityToggle()
        }
        setScreen {
            EditEntryScreen(
                onDone = {},
                modifier = storePreviewModifier(),
                viewModel = viewModel
            )
        }

        val generateLabel = context.getString(R.string.generator_generate)
        waitForText(generateLabel)
        composeRule.onNodeWithText(generateLabel).performScrollTo()
        saveRoot("03-password-generator.jpg")
    }

    @Test
    fun captureEncryptedEntryDetail() {
        val viewModel = detailViewModel(ENCRYPTED_DETAIL_ENTRY_ID)
        setScreen {
            EntryDetailScreen(
                onBack = {},
                onEditClick = {},
                modifier = storePreviewModifier(),
                viewModel = viewModel
            )
        }

        waitForText("Test Wi-Fi")
        composeRule.runOnIdle(viewModel::onRevealClick)
        waitForText(DEMO_ENCRYPTED_PASSWORD)
        waitForText(context.getString(R.string.detail_copy))
        saveRoot("04-encrypted-detail.jpg")
    }

    @Test
    fun captureHashedPasswordGuess() {
        val viewModel = detailViewModel(HASHED_ENTRY_ID).apply {
            onGuessChange(DEMO_HASHED_PASSWORD)
        }
        setScreen {
            EntryDetailScreen(
                onBack = {},
                onEditClick = {},
                modifier = storePreviewModifier(),
                viewModel = viewModel
            )
        }

        waitForText("Demo Forum")
        val checkLabel = context.getString(R.string.detail_guess_check)
        composeRule.onNodeWithText(checkLabel).performScrollTo()
        saveRoot("05-hashed-password-guess.jpg")
    }

    @Test
    fun captureAutofillSecurityConfirmation() {
        setScreen {
            AutofillSecurityConfirmationScreen(
                requestOrigin = AutofillRequestOrigin.from(
                    claimedWebDomain = "login.example.test",
                    requestingPackage = "com.example.browser"
                ),
                onConfirm = {},
                onCancel = {},
                modifier = storePreviewModifier()
            )
        }

        saveRoot("06-autofill-security.jpg")
    }

    @Test
    fun captureSecuritySettings() {
        setScreen {
            SettingsScreen(
                onBack = {},
                modifier = storePreviewModifier(),
                viewModel = SettingsViewModel(settings)
            )
        }

        waitForText(context.getString(R.string.settings_autolock))
        saveRoot("07-security-settings.jpg")
    }

    @Test
    fun captureInAppPrivacyPolicy() {
        setScreen {
            PrivacyPolicyScreen(
                onBack = {},
                modifier = storePreviewModifier()
            )
        }
        waitForText(context.getString(R.string.privacy_title))
        saveRoot("08-privacy-policy.jpg")
    }

    private fun detailViewModel(entryId: Long): EntryDetailViewModel =
        EntryDetailViewModel(
            repository = repository,
            clipboard = SecureClipboard(context, settings),
            settings = settings,
            savedStateHandle = SavedStateHandle(mapOf(Destinations.ARG_ENTRY_ID to entryId))
        )

    private fun setScreen(content: @Composable () -> Unit) {
        composeRule.setContent {
            PasswordMemoryTheme(darkTheme = false) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(SCREENSHOT_ROOT_TAG)
                ) {
                    content()
                }
            }
        }
    }

    private fun storePreviewModifier(): Modifier =
        Modifier.padding(top = STORE_PREVIEW_TOP_INSET)

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun saveRoot(fileName: String) {
        composeRule.waitForIdle()
        val bitmap = composeRule
            .onNodeWithTag(SCREENSHOT_ROOT_TAG, useUnmergedTree = true)
            .captureToImage()
            .asAndroidBitmap()
        if (isStrictPlayCapture()) {
            assertEquals(PLAY_WIDTH, bitmap.width)
            assertEquals(PLAY_HEIGHT, bitmap.height)
        }

        val outputDirectory = requireNotNull(context.getExternalFilesDir("play-store"))
        val output = File(outputDirectory, fileName)
        output.outputStream().use { stream ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream))
        }
        assertTrue(output.isFile)
        assertTrue(output.length() in 1..MAX_PLAY_ASSET_BYTES)
    }

    private fun isStrictPlayCapture(): Boolean =
        InstrumentationRegistry.getArguments()
            .getString("playStoreCapture")
            .toBoolean()

    private object ScreenshotCryptoManager : CryptoManager {
        override fun encrypt(plainText: String): EncryptedPayload =
            error("Screenshot fixtures never encrypt")

        override fun decrypt(payload: EncryptedPayload): String = DEMO_ENCRYPTED_PASSWORD

        override fun hasLegacyKey(): Boolean = false

        override fun discardLegacyKey() = Unit
    }

    private companion object {
        const val ENCRYPTED_ENTRY_ID = 1L
        const val ENCRYPTED_DETAIL_ENTRY_ID = 4L
        const val HASHED_ENTRY_ID = 3L
        const val SAMPLE_TIME = 1_735_689_600_000L
        const val DEMO_ENCRYPTED_PASSWORD = "Example-Only!42"
        const val DEMO_HASHED_PASSWORD = "Demo-Guess-Only!42"
        const val UNLOCK_FRAME_MILLIS = 420L
        const val SCREENSHOT_ROOT_TAG = "play-store-screenshot-root"
        const val PLAY_WIDTH = 1080
        const val PLAY_HEIGHT = 1920
        const val JPEG_QUALITY = 96
        const val MAX_PLAY_ASSET_BYTES = 8L * 1024L * 1024L
        val STORE_PREVIEW_TOP_INSET = 48.dp
    }
}
