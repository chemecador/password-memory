package com.chemecador.passwordmemory.autofill

import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.widget.RemoteViews
import com.chemecador.passwordmemory.R
import com.chemecador.passwordmemory.data.repository.PasswordRepository
import com.chemecador.passwordmemory.domain.model.PasswordEntry
import com.chemecador.passwordmemory.domain.model.ProtectionMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fills logins in other apps and browsers, and takes over the "save password" prompt from whoever
 * was answering it before.
 *
 * Nothing is decrypted here: every dataset is authenticated, so the system only ever gets a row
 * with a service name on it, and the password is put together by [AutofillUnlockActivity] once the
 * user has picked that row and unlocked the app.
 */
@AndroidEntryPoint
class PasswordMemoryAutofillService : AutofillService() {

    @Inject
    lateinit var repository: PasswordRepository

    @Inject
    lateinit var pendingCapture: PendingCapture

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val form = request.fillContexts.lastOrNull()?.structure?.let(LoginFormParser::parse)
        if (form == null || !form.isLogin) {
            callback.onSuccess(null)
            return
        }
        val work = scope.launch {
            callback.onSuccess(runCatching { fillResponse(form) }.getOrNull())
        }
        cancellationSignal.setOnCancelListener { work.cancel() }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val form = LoginFormParser.merge(
            request.fillContexts.map { LoginFormParser.parse(it.structure) }
        )
        val credential = form?.credential
        if (form == null || credential == null) {
            callback.onFailure(getString(R.string.autofill_save_failed))
            return
        }
        val (username, password) = credential
        val capture = Capture(
            serviceName = serviceNameFor(form),
            username = username,
            password = password,
            origin = Sites.originOf(form)
        )

        // Storing it encrypts it, and the Keystore key wants an authentication a service cannot ask
        // for. Park the credential and let AutofillUnlockActivity prompt and commit it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pendingCapture.put(capture)
            callback.onSuccess(AutofillUnlockActivity.saveIntentSender(this))
            return
        }

        // Before API 28 the save callback cannot hand back an activity to launch, so this only
        // works while the key is still inside its window — right after the user used the app.
        scope.launch {
            val saved = runCatching {
                repository.saveCaptured(
                    serviceName = capture.serviceName,
                    username = capture.username,
                    password = capture.password,
                    origin = capture.origin
                )
            }
            if (saved.isSuccess) {
                callback.onSuccess()
            } else {
                callback.onFailure(getString(R.string.autofill_save_failed))
            }
        }
    }

    private suspend fun fillResponse(form: LoginForm): FillResponse {
        val entries = repository.getEntries().filter { it.mode == ProtectionMode.ENCRYPTED }
        val builder = FillResponse.Builder()
        CredentialMatcher.matching(entries, form)
            .take(MAX_SUGGESTIONS)
            .forEach { entry -> builder.addDataset(dataset(form, entry)) }
        if (entries.isNotEmpty()) {
            builder.addDataset(pickerDataset(form))
        }
        // The whole point of the feature: this is what makes the system offer to save the password
        // here instead of anywhere else.
        builder.setSaveInfo(saveInfo(form))
        return builder.build()
    }

    private fun dataset(form: LoginForm, entry: PasswordEntry): Dataset = dataset(
        form = form,
        presentation = datasetPresentation(entry.serviceName, entry.username),
        entryId = entry.id
    )

    /** Last row of the dropdown, for when the guessed matches are not the entry the user wants. */
    private fun pickerDataset(form: LoginForm): Dataset = dataset(
        form = form,
        presentation = datasetPresentation(
            title = getString(R.string.app_name),
            subtitle = getString(R.string.autofill_pick)
        ),
        entryId = null
    )

    @Suppress("DEPRECATION") // The `Presentations` overload needs API 33, this app starts at 26.
    private fun dataset(form: LoginForm, presentation: RemoteViews, entryId: Long?): Dataset =
        Dataset.Builder(presentation).apply {
            setAuthentication(
                AutofillUnlockActivity.authentication(
                    context = this@PasswordMemoryAutofillService,
                    form = form,
                    entryId = entryId
                ).intentSender
            )
            // An authenticated dataset still has to declare which fields it is going to fill.
            form.usernameId?.let { setValue(it, null) }
            form.passwordIds.forEach { setValue(it, null) }
        }.build()

    /**
     * Web forms usually navigate away instead of closing an activity, so the prompt is also asked
     * for once every field of the form is gone; without it, logging in on a browser would often
     * save nothing.
     */
    private fun saveInfo(form: LoginForm): SaveInfo =
        SaveInfo.Builder(SaveInfo.SAVE_DATA_TYPE_PASSWORD, form.passwordIds.toTypedArray())
            .setFlags(SaveInfo.FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE)
            .apply { form.usernameId?.let { setOptionalIds(arrayOf(it)) } }
            .build()

    /** What an entry captured from this form is called: the site, or the app it was typed into. */
    private fun serviceNameFor(form: LoginForm): String {
        Sites.hostOf(form.webDomain)?.let { return it }
        val packageName = form.packageName ?: return getString(R.string.autofill_unknown_service)
        return runCatching { appLabel(packageName) }.getOrDefault(packageName)
    }

    @Suppress("DEPRECATION") // `ApplicationInfoFlags` needs API 33.
    private fun appLabel(packageName: String): String =
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))
            .toString()

    private companion object {
        /** Enough rows to cover the usual "same site, several accounts" case without a wall of them. */
        const val MAX_SUGGESTIONS = 5
    }
}
