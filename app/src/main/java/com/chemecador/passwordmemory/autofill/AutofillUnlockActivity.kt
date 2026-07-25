package com.chemecador.passwordmemory.autofill

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.service.autofill.Dataset
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.chemecador.passwordmemory.R
import com.chemecador.passwordmemory.data.repository.PasswordRepository
import com.chemecador.passwordmemory.domain.model.PasswordEntry
import com.chemecador.passwordmemory.ui.autofill.EntryPickerScreen
import com.chemecador.passwordmemory.ui.autofill.UnlockingScreen
import com.chemecador.passwordmemory.ui.lock.LockResult
import com.chemecador.passwordmemory.ui.lock.promptUnlock
import com.chemecador.passwordmemory.ui.theme.PasswordMemoryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The authentication step behind every dataset offered by [PasswordMemoryAutofillService]: the
 * password is decrypted here, after a biometric prompt, and handed back to the system as a finished
 * dataset. Without an entry id it lists the entries first, for when the suggestions missed.
 */
@AndroidEntryPoint
class AutofillUnlockActivity : FragmentActivity() {

    @Inject
    lateinit var repository: PasswordRepository

    private var target: FillTarget? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val parsed = FillTarget.from(intent)
        if (parsed == null) {
            finish()
            return
        }
        target = parsed
        enableEdgeToEdge()
        setContent { PasswordMemoryTheme { UnlockingScreen() } }
        unlock(parsed.entryId)
    }

    private fun unlock(entryId: Long?) {
        promptUnlock(
            title = getString(R.string.lock_prompt_title),
            subtitle = getString(R.string.autofill_unlock_subtitle)
        ) { result ->
            when (result) {
                LockResult.Unlocked -> if (entryId != null) fill(entryId) else showPicker()
                LockResult.Cancelled -> finish()
                LockResult.Unavailable -> {
                    Toast.makeText(this, R.string.lock_unavailable, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun showPicker() {
        setContent {
            PasswordMemoryTheme {
                EntryPickerScreen(onPick = { fill(it.id) }, onCancel = ::finish)
            }
        }
    }

    private fun fill(entryId: Long) {
        val fields = target ?: run {
            finish()
            return
        }
        lifecycleScope.launch {
            val entry = repository.getEntry(entryId)
            val password = entry?.let { repository.revealPassword(it.id) }
            if (entry == null || password == null) {
                Toast.makeText(
                    this@AutofillUnlockActivity,
                    R.string.detail_reveal_failed,
                    Toast.LENGTH_LONG
                ).show()
                finish()
                return@launch
            }
            val result = Intent().putExtra(
                AutofillManager.EXTRA_AUTHENTICATION_RESULT,
                dataset(fields, entry, password)
            )
            setResult(RESULT_OK, result)
            finish()
        }
    }

    @Suppress("DEPRECATION") // The `Presentations` overload needs API 33, this app starts at 26.
    private fun dataset(fields: FillTarget, entry: PasswordEntry, password: String): Dataset =
        Dataset.Builder(datasetPresentation(entry.serviceName, entry.username)).apply {
            val usernameId = fields.usernameId
            val username = entry.username
            // An entry without a username must not wipe whatever the user already typed there.
            if (usernameId != null && username != null) {
                setValue(usernameId, AutofillValue.forText(username))
            }
            fields.passwordIds.forEach { setValue(it, AutofillValue.forText(password)) }
        }.build()

    companion object {

        private const val EXTRA_ENTRY_ID = "entry_id"
        private const val EXTRA_USERNAME_ID = "username_id"
        private const val EXTRA_PASSWORD_IDS = "password_ids"
        private const val NO_ENTRY = 0L

        /**
         * The [PendingIntent] a dataset authenticates with. It has to be mutable, because the system
         * adds its own extras before launching it, and each row needs its own `data` URI: extras are
         * ignored when the framework decides whether two pending intents are the same one.
         */
        fun authentication(context: Context, form: LoginForm, entryId: Long?): PendingIntent {
            val intent = Intent(context, AutofillUnlockActivity::class.java)
                .setData("password-memory://autofill/${entryId ?: "pick"}".toUri())
                .putExtra(EXTRA_ENTRY_ID, entryId ?: NO_ENTRY)
                .putParcelableArrayListExtra(EXTRA_PASSWORD_IDS, ArrayList(form.passwordIds))
                .apply { form.usernameId?.let { putExtra(EXTRA_USERNAME_ID, it) } }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }
    }

    /** Where the chosen credential has to end up, and which entry was chosen, if any. */
    private data class FillTarget(
        val entryId: Long?,
        val usernameId: AutofillId?,
        val passwordIds: List<AutofillId>
    ) {
        companion object {
            fun from(intent: Intent): FillTarget? {
                val passwordIds = intent
                    .parcelableList<AutofillId>(EXTRA_PASSWORD_IDS)
                    ?.takeIf { it.isNotEmpty() }
                    ?: return null
                return FillTarget(
                    entryId = intent.getLongExtra(EXTRA_ENTRY_ID, NO_ENTRY).takeIf { it != NO_ENTRY },
                    usernameId = intent.parcelable(EXTRA_USERNAME_ID),
                    passwordIds = passwordIds
                )
            }
        }
    }
}

@Suppress("DEPRECATION")
private inline fun <reified T : Parcelable> Intent.parcelable(name: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(name, T::class.java)
    } else {
        getParcelableExtra(name)
    }

@Suppress("DEPRECATION")
private inline fun <reified T : Parcelable> Intent.parcelableList(name: String): List<T>? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(name, T::class.java)
    } else {
        getParcelableArrayListExtra(name)
    }
