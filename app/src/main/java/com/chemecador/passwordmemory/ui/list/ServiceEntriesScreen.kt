package com.chemecador.passwordmemory.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chemecador.passwordmemory.R
import com.chemecador.passwordmemory.domain.model.PasswordEntry
import com.chemecador.passwordmemory.ui.common.ModeBadge
import com.chemecador.passwordmemory.ui.common.ServiceAvatar

/**
 * The accounts saved under one service. The service name is already the title of the screen, so the
 * rows lead with what tells the accounts apart: the username.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceEntriesScreen(
    onBack: () -> Unit,
    onEntryClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ServiceEntriesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Deleting the last account leaves a screen with nothing on it and nothing to do: the service
    // it was about no longer exists, so step back to the list.
    LaunchedEffect(state.isEmpty) {
        if (state.isEmpty) onBack()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.serviceName,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = pluralStringResource(
                                R.plurals.list_accounts,
                                state.entries.size,
                                state.entries.size
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.entries, key = { it.id }) { entry ->
                AccountCard(
                    entry = entry,
                    onClick = { onEntryClick(entry.id) },
                    onFavoriteClick = { viewModel.onFavoriteToggle(entry) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
private fun AccountCard(
    entry: PasswordEntry,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // The avatar is keyed on the username here: every account of the service would
            // otherwise get the same circle, which tells the user nothing.
            ServiceAvatar(serviceName = entry.username ?: entry.serviceName)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = entry.username ?: stringResource(R.string.service_no_username),
                    style = MaterialTheme.typography.titleMedium,
                    fontStyle = if (entry.username == null) FontStyle.Italic else FontStyle.Normal,
                    color = if (entry.username == null) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                ModeBadge(entry.mode)
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (entry.isFavorite) Icons.Rounded.Star
                    else Icons.Rounded.StarBorder,
                    contentDescription = stringResource(R.string.list_toggle_favorite),
                    tint = if (entry.isFavorite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
