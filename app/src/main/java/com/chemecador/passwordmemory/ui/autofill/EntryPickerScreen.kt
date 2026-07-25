package com.chemecador.passwordmemory.ui.autofill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chemecador.passwordmemory.R
import com.chemecador.passwordmemory.domain.model.PasswordEntry
import com.chemecador.passwordmemory.domain.model.ProtectionMode
import com.chemecador.passwordmemory.ui.common.AppMark
import com.chemecador.passwordmemory.ui.common.ServiceAvatar
import com.chemecador.passwordmemory.ui.list.EntryListViewModel

/** Shown while the biometric prompt is up, so the autofill window is never a blank rectangle. */
@Composable
fun UnlockingScreen(modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppMark(modifier = Modifier.size(56.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleMedium
            )
            CircularProgressIndicator()
        }
    }
}

/**
 * The list of entries, for filling a form the suggestions did not recognise. Only encrypted entries
 * are offered: a hashed password cannot be recovered, so there is nothing to fill with.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryPickerScreen(
    onPick: (PasswordEntry) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EntryListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val entries = state.entries.filter { it.mode == ProtectionMode.ENCRYPTED }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.autofill_pick_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.action_cancel)
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
        ) {
            TextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(CircleShape),
                singleLine = true,
                shape = CircleShape,
                placeholder = { Text(stringResource(R.string.list_search)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            if (entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.autofill_pick_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        PickerRow(entry = entry, onClick = { onPick(entry) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(entry: PasswordEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ServiceAvatar(serviceName = entry.serviceName)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.serviceName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                entry.username?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
