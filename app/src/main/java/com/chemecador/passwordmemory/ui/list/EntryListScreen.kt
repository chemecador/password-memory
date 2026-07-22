package com.chemecador.passwordmemory.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chemecador.passwordmemory.R
import com.chemecador.passwordmemory.domain.model.PasswordEntry
import com.chemecador.passwordmemory.domain.model.ProtectionMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryListScreen(
    onEntryClick: (Long) -> Unit,
    onCreateClick: () -> Unit,
    onBackupClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EntryListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = { OverflowMenu(onBackupClick = onBackupClick) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.list_add))
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text(stringResource(R.string.list_search)) }
            )
            FilterRow(
                state = state,
                onCategorySelected = viewModel::onCategorySelected,
                onFavoritesOnlyToggle = viewModel::onFavoritesOnlyToggle
            )
            if (state.isEmpty) {
                EmptyState(hasFilters = state.hasFilters)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(state.entries, key = { it.id }) { entry ->
                        EntryRow(
                            entry = entry,
                            onClick = { onEntryClick(entry.id) },
                            onFavoriteClick = { viewModel.onFavoriteToggle(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverflowMenu(onBackupClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.list_menu))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.backup_title)) },
            onClick = {
                expanded = false
                onBackupClick()
            }
        )
    }
}

@Composable
private fun FilterRow(
    state: EntryListUiState,
    onCategorySelected: (String?) -> Unit,
    onFavoritesOnlyToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = state.favoritesOnly,
            onClick = onFavoritesOnlyToggle,
            label = { Text(stringResource(R.string.list_favorites)) }
        )
        state.categories.forEach { category ->
            FilterChip(
                selected = state.selectedCategory == category,
                onClick = {
                    onCategorySelected(category.takeIf { it != state.selectedCategory })
                },
                label = { Text(category) }
            )
        }
    }
}

@Composable
private fun EntryRow(
    entry: PasswordEntry,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(entry.serviceName) },
        supportingContent = {
            val subtitle = entry.username ?: stringResource(
                when (entry.mode) {
                    ProtectionMode.ENCRYPTED -> R.string.mode_encrypted
                    ProtectionMode.HASHED -> R.string.mode_hashed
                }
            )
            Text(subtitle)
        },
        trailingContent = {
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (entry.isFavorite) Icons.Default.Star
                    else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(R.string.list_toggle_favorite)
                )
            }
        }
    )
}

@Composable
private fun EmptyState(hasFilters: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(
                if (hasFilters) R.string.list_empty_filtered else R.string.list_empty
            ),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}
