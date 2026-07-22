package com.chemecador.passwordmemory.ui.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chemecador.passwordmemory.R
import com.chemecador.passwordmemory.domain.model.ProtectionMode
import com.chemecador.passwordmemory.ui.common.SectionCard
import com.chemecador.passwordmemory.ui.common.icon
import com.chemecador.passwordmemory.ui.common.labelRes

private val FieldShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntryScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditEntryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (state.isEditing) R.string.edit_title else R.string.create_title
                        ),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onFavoriteToggle) {
                        Icon(
                            imageVector = if (state.isFavorite) Icons.Rounded.Star
                            else Icons.Rounded.StarBorder,
                            contentDescription = stringResource(R.string.list_toggle_favorite),
                            tint = if (state.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SectionCard(title = stringResource(R.string.edit_section_service)) {
                OutlinedTextField(
                    value = state.serviceName,
                    onValueChange = viewModel::onServiceNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = FieldShape,
                    label = { Text(stringResource(R.string.field_service)) }
                )
                OutlinedTextField(
                    value = state.username,
                    onValueChange = viewModel::onUsernameChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = FieldShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    label = { Text(stringResource(R.string.field_username)) }
                )
            }

            SectionCard(title = stringResource(R.string.edit_section_password)) {
                if (state.isModeEditable) {
                    ModeSelector(mode = state.mode, onModeChange = viewModel::onModeChange)
                }

                if (state.isPasswordEditable) {
                    val helperRes = when {
                        state.mode == ProtectionMode.HASHED -> R.string.mode_hashed_warning
                        state.isEditing -> R.string.edit_password_optional
                        else -> R.string.mode_encrypted_hint
                    }
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = viewModel::onPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = FieldShape,
                        label = { Text(stringResource(R.string.field_password)) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) {
                                            Icons.Rounded.VisibilityOff
                                        } else {
                                            Icons.Rounded.Visibility
                                        },
                                        contentDescription = stringResource(
                                            R.string.action_toggle_visibility
                                        )
                                    )
                                }
                                IconButton(onClick = viewModel::onGeneratorVisibilityToggle) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = stringResource(
                                            R.string.generator_title
                                        ),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    )
                    Notice(
                        text = stringResource(helperRes),
                        warning = state.mode == ProtectionMode.HASHED
                    )
                    AnimatedVisibility(visible = state.isGeneratorVisible) {
                        PasswordGeneratorPanel(
                            options = state.generatorOptions,
                            onOptionsChange = viewModel::onGeneratorOptionsChange,
                            onGenerateClick = viewModel::onGenerateClick
                        )
                    }
                } else {
                    Notice(
                        text = stringResource(R.string.edit_hashed_password_locked),
                        warning = true
                    )
                }
            }

            SectionCard(title = stringResource(R.string.edit_section_extras)) {
                OutlinedTextField(
                    value = state.hint,
                    onValueChange = viewModel::onHintChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = FieldShape,
                    label = { Text(stringResource(R.string.field_hint)) },
                    supportingText = { Text(stringResource(R.string.field_hint_help)) }
                )
                OutlinedTextField(
                    value = state.category,
                    onValueChange = viewModel::onCategoryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = FieldShape,
                    label = { Text(stringResource(R.string.field_category)) }
                )
            }

            Button(
                onClick = viewModel::onSaveClick,
                enabled = state.canSave,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_save),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/** Inline explanation of what the current choice implies, tinted when it is irreversible. */
@Composable
private fun Notice(text: String, warning: Boolean) {
    val container = if (warning) MaterialTheme.colorScheme.tertiaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHighest
    val content = if (warning) MaterialTheme.colorScheme.onTertiaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(color = container, shape = FieldShape, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (warning) Icons.Rounded.WarningAmber else Icons.Rounded.Info,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = content
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = content
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelector(mode: ProtectionMode, onModeChange: (ProtectionMode) -> Unit) {
    val modes = ProtectionMode.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, option ->
            SegmentedButton(
                selected = mode == option,
                onClick = { onModeChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                icon = {
                    SegmentedButtonDefaults.Icon(active = mode == option) {
                        Icon(
                            imageVector = option.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    }
                }
            ) {
                Text(
                    text = stringResource(option.labelRes()),
                    maxLines = 1
                )
            }
        }
    }
}
