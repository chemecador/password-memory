package com.chemecador.passwordmemory.ui.autofill

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.chemecador.passwordmemory.R
import com.chemecador.passwordmemory.ui.lock.LocalAppLockSuppressor
import kotlinx.coroutines.launch

/**
 * Turns Password Memory into the system autofill service, which is what makes Android offer to save
 * passwords here — and fill them back in — instead of whichever service was doing it before.
 *
 * The choice itself belongs to the system: an app cannot make itself the autofill service, it can
 * only send the user to the picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutofillSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val supported = remember { context.autofillManager()?.isAutofillSupported == true }
    var enabled by remember { mutableStateOf(context.isAutofillProvider()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val unavailable = stringResource(R.string.autofill_unsupported)
    // Choosing the service happens in system settings: without this the app would re-lock on the
    // way out and this screen would be gone by the time the user comes back.
    val lockSuppressor = LocalAppLockSuppressor.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        enabled = context.isAutofillProvider()
    }

    fun openSystemPicker() {
        lockSuppressor.onLeavingForPicker()
        try {
            launcher.launch(context.autofillSettingsIntent())
        } catch (_: ActivityNotFoundException) {
            scope.launch { snackbarHostState.showSnackbar(unavailable) }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.autofill_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(supported = supported, enabled = enabled)
            if (supported) {
                Button(
                    onClick = ::openSystemPicker,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(
                            if (enabled) R.string.autofill_settings else R.string.autofill_enable
                        )
                    )
                }
            }
            Note(text = stringResource(R.string.autofill_how))
            Note(text = stringResource(R.string.autofill_privacy))
        }
    }
}

@Composable
private fun StatusCard(supported: Boolean, enabled: Boolean) {
    val title = when {
        !supported -> R.string.autofill_unsupported
        enabled -> R.string.autofill_enabled
        else -> R.string.autofill_disabled
    }
    val description = when {
        !supported -> null
        enabled -> R.string.autofill_enabled_detail
        else -> R.string.autofill_disabled_detail
    }
    Surface(
        color = if (enabled) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (enabled) Icons.Rounded.CheckCircle else Icons.Rounded.Keyboard,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(title), style = MaterialTheme.typography.titleMedium)
                description?.let {
                    Text(
                        text = stringResource(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Context.autofillManager(): AutofillManager? =
    getSystemService(AutofillManager::class.java)

private fun Context.isAutofillProvider(): Boolean =
    autofillManager()?.let { it.isAutofillSupported && it.hasEnabledAutofillServices() } == true

/**
 * The system picker, opened straight on this app. Some manufacturers ship without the dedicated
 * screen, hence the caller catching [ActivityNotFoundException].
 */
private fun Context.autofillSettingsIntent(): Intent =
    Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).setData("package:$packageName".toUri())
