package com.chemecador.passwordmemory.ui.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.chemecador.passwordmemory.R

@Composable
internal fun EntrySearchField(
    query: String,
    scope: SearchScope,
    onQueryChange: (String) -> Unit,
    onScopeChange: (SearchScope) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        singleLine = true,
        shape = CircleShape,
        placeholder = {
            Text(
                stringResource(
                    if (scope == SearchScope.SERVICE_NAME) R.string.list_search_service
                    else R.string.list_search_anywhere
                )
            )
        },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            Row {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.list_clear_search)
                        )
                    }
                }
                SearchScopeMenu(scope = scope, onScopeChange = onScopeChange)
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun SearchScopeMenu(scope: SearchScope, onScopeChange: (SearchScope) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val scopeLabel = stringResource(
        if (scope == SearchScope.SERVICE_NAME) R.string.list_search_scope_service
        else R.string.list_search_scope_anywhere
    )
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics { stateDescription = scopeLabel }
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = stringResource(R.string.list_search_scope),
                modifier = Modifier.size(20.dp),
                tint = if (scope == SearchScope.ANYWHERE) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SearchScope.entries.forEach { option ->
                DropdownMenuItem(
                    modifier = Modifier.semantics { selected = scope == option },
                    onClick = {
                        onScopeChange(option)
                        expanded = false
                    },
                    text = {
                        Text(
                            stringResource(
                                if (option == SearchScope.SERVICE_NAME) R.string.list_search_scope_service
                                else R.string.list_search_scope_anywhere
                            )
                        )
                    },
                    leadingIcon = {
                        Box(modifier = Modifier.size(24.dp)) {
                            if (scope == option) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                            }
                        }
                    }
                )
            }
        }
    }
}
