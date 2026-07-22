package com.chemecador.passwordmemory.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chemecador.passwordmemory.R
import com.chemecador.passwordmemory.domain.model.PasswordEntry
import com.chemecador.passwordmemory.domain.model.ProtectionMode
import com.chemecador.passwordmemory.ui.common.copySensitiveToClipboard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    onBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EntryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val entry = state.entry
    val copiedMessage = stringResource(R.string.detail_copied)

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) onBack()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(entry?.serviceName.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onFavoriteToggle) {
                        Icon(
                            imageVector = if (entry?.isFavorite == true) Icons.Default.Star
                            else Icons.Outlined.StarBorder,
                            contentDescription = stringResource(R.string.list_toggle_favorite)
                        )
                    }
                    IconButton(onClick = { entry?.let { onEditClick(it.id) } }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.action_edit)
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_delete)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (entry == null) return@Scaffold

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            entry.username?.let { Field(R.string.field_username, it) }
            entry.category?.let { Field(R.string.field_category, it) }
            entry.hint?.let { Field(R.string.field_hint, it) }
            Field(
                labelRes = R.string.detail_mode,
                value = stringResource(
                    when (entry.mode) {
                        ProtectionMode.ENCRYPTED -> R.string.mode_encrypted
                        ProtectionMode.HASHED -> R.string.mode_hashed
                    }
                )
            )

            when (entry.mode) {
                ProtectionMode.ENCRYPTED -> EncryptedSection(
                    state = state,
                    onRevealClick = viewModel::onRevealClick,
                    onHideClick = viewModel::onHideClick
                )

                ProtectionMode.HASHED -> HashedSection(
                    state = state,
                    onGuessChange = viewModel::onGuessChange,
                    onGuessSubmit = viewModel::onGuessSubmit
                )
            }

            state.revealedPassword?.let { password ->
                Button(
                    onClick = {
                        context.copySensitiveToClipboard(entry.serviceName, password)
                        scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.detail_copy))
                }
            }
        }
    }

    if (showDeleteDialog) {
        DeleteDialog(
            entry = entry,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.onDeleteConfirm()
            }
        )
    }
}

@Composable
private fun EncryptedSection(
    state: EntryDetailUiState,
    onRevealClick: () -> Unit,
    onHideClick: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val password = state.revealedPassword
            if (password != null) {
                Text(
                    text = password,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace
                )
                OutlinedButton(onClick = onHideClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.detail_hide))
                }
            } else {
                if (state.revealFailed) {
                    Text(
                        text = stringResource(R.string.detail_reveal_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Button(onClick = onRevealClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.detail_reveal))
                }
            }
        }
    }
}

@Composable
private fun HashedSection(
    state: EntryDetailUiState,
    onGuessChange: (String) -> Unit,
    onGuessSubmit: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.detail_guess_explanation),
                style = MaterialTheme.typography.bodyMedium
            )
            if (state.guessResult == GuessResult.CORRECT) {
                Text(
                    text = state.revealedPassword.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = stringResource(R.string.detail_guess_correct),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                OutlinedTextField(
                    value = state.guess,
                    onValueChange = onGuessChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = state.guessResult == GuessResult.WRONG,
                    label = { Text(stringResource(R.string.detail_guess_label)) },
                    supportingText = {
                        if (state.guessResult == GuessResult.WRONG) {
                            Text(stringResource(R.string.detail_guess_wrong))
                        }
                    }
                )
                Button(
                    onClick = onGuessSubmit,
                    enabled = state.guess.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.detail_guess_check))
                }
            }
        }
    }
}

@Composable
private fun Field(labelRes: Int, value: String) {
    Column {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DeleteDialog(
    entry: PasswordEntry?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_title)) },
        text = { Text(stringResource(R.string.delete_message, entry?.serviceName.orEmpty())) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
