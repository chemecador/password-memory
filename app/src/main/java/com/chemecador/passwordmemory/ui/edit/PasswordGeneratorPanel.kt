package com.chemecador.passwordmemory.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.generator_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(R.string.generator_length, options.length),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = options.length.toFloat(),
                onValueChange = { onOptionsChange(options.copy(length = it.toInt())) },
                valueRange = PasswordGeneratorOptions.MIN_LENGTH.toFloat()..
                    PasswordGeneratorOptions.MAX_LENGTH.toFloat()
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CharacterSetChip(
                    selected = options.useUppercase,
                    labelRes = R.string.generator_uppercase,
                    onClick = { onOptionsChange(options.copy(useUppercase = !options.useUppercase)) }
                )
                CharacterSetChip(
                    selected = options.useLowercase,
                    labelRes = R.string.generator_lowercase,
                    onClick = { onOptionsChange(options.copy(useLowercase = !options.useLowercase)) }
                )
                CharacterSetChip(
                    selected = options.useDigits,
                    labelRes = R.string.generator_digits,
                    onClick = { onOptionsChange(options.copy(useDigits = !options.useDigits)) }
                )
                CharacterSetChip(
                    selected = options.useSymbols,
                    labelRes = R.string.generator_symbols,
                    onClick = { onOptionsChange(options.copy(useSymbols = !options.useSymbols)) }
                )
            }
            Button(
                onClick = onGenerateClick,
                enabled = options.hasAnyCharacterSet,
                shape = CircleShape,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.generator_generate),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun CharacterSetChip(selected: Boolean, labelRes: Int, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(labelRes)) },
        shape = CircleShape
    )
}
