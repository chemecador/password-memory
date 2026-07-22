package com.chemecador.passwordmemory.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chemecador.passwordmemory.R
import com.chemecador.passwordmemory.domain.model.PasswordEntry
import com.chemecador.passwordmemory.domain.model.ProtectionMode
import com.chemecador.passwordmemory.ui.common.ModeBadge
import com.chemecador.passwordmemory.ui.common.SectionCard
import com.chemecador.passwordmemory.ui.common.ServiceAvatar
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
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onFavoriteToggle) {
                        Icon(
                            imageVector = if (entry?.isFavorite == true) Icons.Rounded.Star
                            else Icons.Rounded.StarBorder,
                            contentDescription = stringResource(R.string.list_toggle_favorite),
                            tint = if (entry?.isFavorite == true) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { entry?.let { onEditClick(it.id) } }) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.action_edit)
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Hero(entry = entry)

            if (entry.hasDetails) {
                SectionCard(title = stringResource(R.string.detail_section_details)) {
                    entry.username?.let {
                        DetailRow(Icons.Rounded.AlternateEmail, R.string.field_username, it)
                    }
                    entry.category?.let {
                        DetailRow(Icons.Rounded.Folder, R.string.field_category, it)
                    }
                    entry.hint?.let {
                        DetailRow(Icons.Rounded.Lightbulb, R.string.field_hint, it)
                    }
                }
            }

            SectionCard(title = stringResource(R.string.detail_section_password)) {
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

                AnimatedVisibility(visible = state.revealedPassword != null) {
                    Button(
                        onClick = {
                            state.revealedPassword?.let { password ->
                                context.copySensitiveToClipboard(entry.serviceName, password)
                                scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                    ) {
                        Icon(
                            Icons.Rounded.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.detail_copy),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
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
private fun Hero(entry: PasswordEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ServiceAvatar(serviceName = entry.serviceName, size = 64)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = entry.serviceName,
                style = MaterialTheme.typography.headlineSmall
            )
            ModeBadge(entry.mode)
        }
    }
}

@Composable
private fun EncryptedSection(
    state: EntryDetailUiState,
    onRevealClick: () -> Unit,
    onHideClick: () -> Unit
) {
    val password = state.revealedPassword
    if (password != null) {
        PasswordPlate(password)
        FilledTonalButton(onClick = onHideClick, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Rounded.VisibilityOff,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.detail_hide),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    } else {
        PasswordPlate(text = "•".repeat(12), obscured = true)
        if (state.revealFailed) {
            Text(
                text = stringResource(R.string.detail_reveal_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        FilledTonalButton(onClick = onRevealClick, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Rounded.Visibility,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.detail_reveal),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun HashedSection(
    state: EntryDetailUiState,
    onGuessChange: (String) -> Unit,
    onGuessSubmit: () -> Unit
) {
    Text(
        text = stringResource(R.string.detail_guess_explanation),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (state.guessResult == GuessResult.CORRECT) {
        PasswordPlate(state.revealedPassword.orEmpty())
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
            shape = RoundedCornerShape(16.dp),
            isError = state.guessResult == GuessResult.WRONG,
            label = { Text(stringResource(R.string.detail_guess_label)) },
            supportingText = {
                if (state.guessResult == GuessResult.WRONG) {
                    Text(stringResource(R.string.detail_guess_wrong))
                }
            }
        )
        FilledTonalButton(
            onClick = onGuessSubmit,
            enabled = state.guess.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.detail_guess_check))
        }
    }
}

/** Monospaced, generously tracked: a password is read character by character. */
@Composable
private fun PasswordPlate(text: String, obscured: Boolean = false) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            color = if (obscured) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DetailRow(icon: ImageVector, labelRes: Int, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
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
        icon = {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(R.string.delete_title)) },
        text = { Text(stringResource(R.string.delete_message, entry?.serviceName.orEmpty())) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

private val PasswordEntry.hasDetails: Boolean
    get() = username != null || category != null || hint != null
