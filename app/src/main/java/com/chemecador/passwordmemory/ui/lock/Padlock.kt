package com.chemecador.passwordmemory.ui.lock

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * How far the shackle swings open, in degrees, at full progress. Much less reads as a crooked lock
 * rather than an open one; much more and the arch runs out of the canvas once the spring
 * overshoots.
 */
private const val OPEN_ANGLE = 48f

/**
 * A padlock whose shackle lifts and swings aside as [open] goes from 0 (locked) to 1 (unlocked).
 * Drawn rather than an icon so the shackle can be transformed independently of the body.
 */
@Composable
fun Padlock(
    open: Float,
    bodyStart: Color,
    bodyEnd: Color,
    shackleColor: Color,
    keyholeColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        drawShackle(s, open, shackleColor)
        drawBody(s, bodyStart, bodyEnd, keyholeColor)
    }
}

/**
 * The burst of light that leaves the lock the moment it opens: two expanding rings and a ring of
 * sparks, all of which fade as [progress] runs to 1.
 */
@Composable
fun UnlockBurst(progress: Float, color: Color, modifier: Modifier = Modifier) {
    if (progress <= 0f || progress >= 1f) return
    Canvas(modifier = modifier) {
        val half = size.minDimension / 2f
        drawBurstRing(progress, half, color)
        drawBurstRing(((progress - 0.2f) / 0.8f).coerceAtLeast(0f), half, color)
        drawSparks(progress, half, color)
    }
}

private fun DrawScope.drawShackle(s: Float, open: Float, color: Color) {
    val radius = 0.19f * s
    val centerX = s / 2f
    val archY = 0.31f * s
    val legBottom = 0.50f * s

    val path = Path().apply {
        moveTo(centerX - radius, legBottom)
        lineTo(centerX - radius, archY)
        arcTo(
            rect = Rect(
                offset = Offset(centerX - radius, archY - radius),
                size = Size(radius * 2f, radius * 2f)
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        lineTo(centerX + radius, legBottom)
    }

    // Pivot on the right leg so the lock hinges the way a real shackle does, swinging clockwise so
    // the free leg rises clear of the body: the other way round the arch sweeps down behind it and
    // the lock just looks crooked. The lift is small enough that the pivot stays inside the body,
    // or the shackle would look detached from it.
    withTransform({
        translate(top = -0.05f * s * open)
        rotate(degrees = OPEN_ANGLE * open, pivot = Offset(centerX + radius, legBottom))
    }) {
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 0.115f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

private fun DrawScope.drawBody(s: Float, start: Color, end: Color, keyhole: Color) {
    val width = 0.68f * s
    val height = 0.50f * s
    val left = (size.width - width) / 2f
    val top = 0.44f * s
    val corner = 0.17f * s

    drawRoundRect(
        brush = Brush.verticalGradient(listOf(start, end), startY = top, endY = top + height),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(corner)
    )

    val keyholeCenter = Offset(size.width / 2f, top + height * 0.38f)
    drawCircle(color = keyhole, radius = 0.072f * s, center = keyholeCenter)
    drawLine(
        color = keyhole,
        start = keyholeCenter,
        end = Offset(keyholeCenter.x, top + height * 0.76f),
        strokeWidth = 0.058f * s,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawBurstRing(progress: Float, half: Float, color: Color) {
    if (progress <= 0f) return
    val radius = half * lerpFloat(0.34f, 0.98f, progress)
    drawCircle(
        color = color.copy(alpha = (1f - progress) * 0.7f),
        radius = radius,
        style = Stroke(width = 3.dp.toPx() * (1f - progress) + 0.5f)
    )
}

private fun DrawScope.drawSparks(progress: Float, half: Float, color: Color) {
    val inner = half * lerpFloat(0.36f, 0.86f, progress)
    val length = half * 0.12f * (1f - progress)
    val alpha = (1f - progress) * 0.9f
    repeat(SPARKS) { index ->
        val angle = Math.toRadians((index * (360.0 / SPARKS)) + 14.0 * progress)
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        drawLine(
            color = color.copy(alpha = alpha),
            start = center + Offset(dx * inner, dy * inner),
            end = center + Offset(dx * (inner + length), dy * (inner + length)),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private const val SPARKS = 8

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

/** Colour interpolation, kept here so callers do not need the graphics `lerp` import. */
fun blend(start: Color, stop: Color, fraction: Float): Color = lerp(start, stop, fraction)
