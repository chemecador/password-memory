package com.chemecador.passwordmemory.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chemecador.passwordmemory.R
import com.chemecador.passwordmemory.domain.model.ProtectionMode
import com.chemecador.passwordmemory.ui.theme.AvatarGradients

/** The shield-and-keyhole mark from the launcher icon, for use inside the app. */
@Composable
fun AppMark(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Icon(
        painter = painterResource(R.drawable.ic_shield_key),
        contentDescription = null,
        modifier = modifier,
        tint = tint
    )
}

/** Circular badge with the service initial over a colour derived from the service name. */
@Composable
fun ServiceAvatar(
    serviceName: String,
    modifier: Modifier = Modifier,
    size: Int = 44
) {
    val (start, end) = AvatarGradients[serviceName.gradientIndex()]
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(start, end))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = serviceName.initial(),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}

/** Small pill telling encrypted entries apart from hashed ones at a glance. */
@Composable
fun ModeBadge(mode: ProtectionMode, modifier: Modifier = Modifier) {
    val container = when (mode) {
        ProtectionMode.ENCRYPTED -> MaterialTheme.colorScheme.secondaryContainer
        ProtectionMode.HASHED -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val content = when (mode) {
        ProtectionMode.ENCRYPTED -> MaterialTheme.colorScheme.onSecondaryContainer
        ProtectionMode.HASHED -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(color = container, shape = CircleShape, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = mode.icon(),
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = content
            )
            Text(
                text = stringResource(mode.labelRes()),
                style = MaterialTheme.typography.labelSmall,
                color = content
            )
        }
    }
}

/** A titled block of related fields. Replaces the flat stack of controls the screens started as. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(PaddingValues(16.dp)),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

fun ProtectionMode.labelRes(): Int = when (this) {
    ProtectionMode.ENCRYPTED -> R.string.mode_encrypted
    ProtectionMode.HASHED -> R.string.mode_hashed
}

fun ProtectionMode.icon(): ImageVector = when (this) {
    ProtectionMode.ENCRYPTED -> Icons.Rounded.Lock
    ProtectionMode.HASHED -> Icons.Rounded.Tag
}

private fun String.initial(): String =
    firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"

// The double modulo keeps the index positive: `Int.MIN_VALUE.absoluteValue` is still negative.
private fun String.gradientIndex(): Int {
    val size = AvatarGradients.size
    return ((trim().lowercase().hashCode() % size) + size) % size
}
