package com.chemecador.passwordmemory.ui.autofill

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.chemecador.passwordmemory.R
import com.chemecador.passwordmemory.autofill.AutofillRequestOrigin

/**
 * Explicit consent gate shown before authentication and decryption. The copy intentionally calls
 * the domain "claimed": Android exposes both identifiers, but an offline service cannot prove that
 * the requesting package owns that web domain.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AutofillSecurityConfirmationScreen(
    requestOrigin: AutofillRequestOrigin,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.autofill_security_confirm_title),
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(
                                R.string.autofill_security_cancel
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(R.string.autofill_security_confirm_explanation),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AutofillOriginDisclosureCard(
                requestOrigin = requestOrigin,
                title = R.string.autofill_security_identifiers_title
            )
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.autofill_security_continue))
            }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.autofill_security_cancel))
            }
        }
    }
}

/** The same unverified request metadata used in the confirmation and entry picker. */
@Composable
internal fun AutofillOriginDisclosureCard(
    requestOrigin: AutofillRequestOrigin,
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    @StringRes explanation: Int? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.semantics { heading() }
                )
            }
            explanation?.let {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            OriginValue(
                label = stringResource(R.string.autofill_security_claimed_domain_label),
                value = requestOrigin.claimedWebDomain
            )
            OriginValue(
                label = stringResource(R.string.autofill_security_requesting_package_label),
                value = requestOrigin.requestingPackage
            )
        }
    }
}

@Composable
private fun OriginValue(label: String, value: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
        SelectionContainer {
            Text(
                text = value ?: stringResource(R.string.autofill_security_missing_value),
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}
