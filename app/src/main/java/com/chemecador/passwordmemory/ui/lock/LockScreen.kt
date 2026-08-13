package com.chemecador.passwordmemory.ui.lock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chemecador.passwordmemory.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Shown whenever the app is locked. It has no content of its own: nothing is readable until the
 * user authenticates.
 *
 * The system prompt comes up on its own, so this is normally only the backdrop behind it. There is
 * a button to bring the prompt back, but only once the user has turned it away and [canRetry] says
 * so: offering it up front would be an extra tap on the one path everybody takes.
 *
 * Once [unlocking] turns true the padlock springs open, throws a burst of light and the whole
 * screen dissolves into the content waiting underneath. [onUnlockAnimationEnd] fires when the
 * screen has finished fading, which is the caller's cue to stop composing it.
 */
@Composable
fun LockScreen(
    unavailable: Boolean,
    onUnlockClick: () -> Unit,
    modifier: Modifier = Modifier,
    canRetry: Boolean = false,
    unlocking: Boolean = false,
    onUnlockAnimationEnd: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val shackle = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }

    LaunchedEffect(unlocking) {
        if (!unlocking) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        launch {
            shackle.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium)
            )
        }
        delay(120)
        launch { burst.animateTo(1f, tween(640, easing = LinearOutSlowInEasing)) }
        delay(380)
        exit.animateTo(1f, tween(420, easing = FastOutLinearInEasing))
        onUnlockAnimationEnd()
    }

    // Raw for the shackle, so the spring's overshoot flings it open past its resting angle.
    val open = shackle.value
    val fade = 1f - exit.value

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (unlocking) Modifier.consumeTouches() else Modifier)
            .graphicsLayer {
                alpha = fade
                val grow = 1f + 0.14f * exit.value
                scaleX = grow
                scaleY = grow
            }
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.primaryContainer.copy(alpha = 0.55f),
                        colors.background,
                        colors.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LockMark(
                breathing = !unavailable && !unlocking,
                open = open,
                burst = burst.value
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium
            )
            // The supporting copy and the button step aside as soon as the lock gives way, so the
            // last thing on screen is the lock itself.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = (1f - open).coerceIn(0f, 1f) },
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.lock_tagline),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(
                        if (unavailable) R.string.lock_unavailable else R.string.lock_message
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 320.dp)
                )
                if (!unavailable && canRetry) {
                    Button(
                        onClick = onUnlockClick,
                        enabled = !unlocking,
                        shape = CircleShape,
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .height(56.dp)
                            .widthIn(min = 200.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(R.string.lock_unlock),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

/** The padlock inside a soft halo that breathes while the app waits to be unlocked. */
@Composable
private fun LockMark(breathing: Boolean, open: Float, burst: Float) {
    val colors = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "lockMark")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    // A single half-sine: the lock swells as the burst leaves it and settles back down.
    val pop = 1f + 0.09f * sin(PI.toFloat() * burst)
    // Colours never take the spring's overshoot: past 1 the interpolation leaves the palette.
    val tint = open.coerceIn(0f, 1f)

    Box(contentAlignment = Alignment.Center) {
        UnlockBurst(
            progress = burst,
            color = colors.secondary,
            modifier = Modifier.size(220.dp)
        )
        Box(
            modifier = Modifier
                .size(148.dp)
                .scale(if (breathing) pulse else pop)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            colors.primary.copy(alpha = 0.22f + 0.25f * burst),
                            colors.primary.copy(alpha = 0f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(96.dp)
                .scale(pop)
                .clip(CircleShape)
                .background(colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Padlock(
                open = open,
                bodyStart = blend(colors.primary, colors.secondary, 0.15f + 0.35f * tint),
                bodyEnd = colors.primary,
                shackleColor = blend(colors.onPrimaryContainer, colors.secondary, tint),
                keyholeColor = colors.onPrimary,
                modifier = Modifier.size(58.dp)
            )
        }
    }
}

/**
 * Swallows every pointer event so taps during the unlock animation cannot reach the content that
 * is already composed underneath this screen.
 */
private fun Modifier.consumeTouches(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
        }
    }
}
