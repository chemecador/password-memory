package com.chemecador.passwordmemory.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chemecador.passwordmemory.R
import com.chemecador.passwordmemory.security.PasswordGeneratorOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorPanel(
    options: PasswordGeneratorOptions,
    onOptionsChange: (PasswordGeneratorOptions) -> Unit,
    onGenerateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.generator_length, options.length))
            }
            Slider(
                value = options.length.toFloat(),
                onValueChange = { onOptionsChange(options.copy(length = it.toInt())) },
                valueRange = PasswordGeneratorOptions.MIN_LENGTH.toFloat()..
                    PasswordGeneratorOptions.MAX_LENGTH.toFloat()
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = options.useUppercase,
                    onClick = { onOptionsChange(options.copy(useUppercase = !options.useUppercase)) },
                    label = { Text(stringResource(R.string.generator_uppercase)) }
                )
                FilterChip(
                    selected = options.useLowercase,
                    onClick = { onOptionsChange(options.copy(useLowercase = !options.useLowercase)) },
                    label = { Text(stringResource(R.string.generator_lowercase)) }
                )
                FilterChip(
                    selected = options.useDigits,
                    onClick = { onOptionsChange(options.copy(useDigits = !options.useDigits)) },
                    label = { Text(stringResource(R.string.generator_digits)) }
                )
                FilterChip(
                    selected = options.useSymbols,
                    onClick = { onOptionsChange(options.copy(useSymbols = !options.useSymbols)) },
                    label = { Text(stringResource(R.string.generator_symbols)) }
                )
            }
            Button(
                onClick = onGenerateClick,
                enabled = options.hasAnyCharacterSet,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.generator_generate))
            }
        }
    }
}
