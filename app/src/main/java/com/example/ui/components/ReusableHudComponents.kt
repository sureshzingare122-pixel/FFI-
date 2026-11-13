package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.CrosshairStyle
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ElevatedDarkGrey
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonOrangeBright
import com.example.ui.theme.ObsidianPanel
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.hypot
import kotlin.math.min

@Composable
fun TacticalCard(
    modifier: Modifier = Modifier,
    borderColor: Color = TacticalBorder,
    backgroundColor: Color = ObsidianPanel,
    contentPadding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun HudStatBadge(
    label: String,
    value: String,
    accentColor: Color = NeonOrange,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = ElevatedDarkGrey.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, TacticalBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HudPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = NeonOrange,
    contentColor: Color = MatteBlack
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = ElevatedDarkGrey,
            disabledContentColor = TextMuted
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun HudSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    borderColor: Color = TacticalBorder,
    contentColor: Color = TextPrimary
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = ElevatedDarkGrey.copy(alpha = 0.7f),
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

/**
 * Multi-touch safe virtual joystick built with Jetpack Compose Canvas.
 * Outputs normalized (-1f..1f, -1f..1f) vectors and displays the configured dead zone ring.
 */
@Composable
fun VirtualJoystick(
    deadZone: Float,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    accentColor: Color = NeonOrange,
    onVectorChange: (Float, Float) -> Unit
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .testTag("virtual_joystick")
            .semantics { contentDescription = "Virtual movement joystick" }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    val center = Offset(this.size.width / 2f, this.size.height / 2f)
                    val maxRadius = (min(this.size.width, this.size.height) / 2f) * 0.72f

                    fun updateFromPosition(pos: Offset) {
                        val delta = pos - center
                        val dist = hypot(delta.x, delta.y)
                        val clampedDist = min(dist, maxRadius)
                        val angleX = if (dist > 0.001f) delta.x / dist else 0f
                        val angleY = if (dist > 0.001f) delta.y / dist else 0f
                        val clamped = Offset(angleX * clampedDist, angleY * clampedDist)
                        knobOffset = clamped
                        val normX = if (maxRadius > 0f) (clamped.x / maxRadius).coerceIn(-1f, 1f) else 0f
                        val normY = if (maxRadius > 0f) (clamped.y / maxRadius).coerceIn(-1f, 1f) else 0f
                        onVectorChange(normX, normY)
                    }

                    updateFromPosition(down.position)
                    down.consume()

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            break
                        }
                        updateFromPosition(change.position)
                        change.consume()
                    }
                    isPressed = false
                    knobOffset = Offset.Zero
                    onVectorChange(0f, 0f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = min(this.size.width, this.size.height) / 2f - 4.dp.toPx()
            val maxMoveRadius = outerRadius * 0.74f

            // Outer housing ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ElevatedDarkGrey.copy(alpha = 0.65f),
                        MatteBlack.copy(alpha = 0.85f)
                    ),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = if (isPressed) accentColor else TacticalBorder,
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Cardinal cross lines
            val axisAlpha = 0.25f
            drawLine(
                color = TextMuted.copy(alpha = axisAlpha),
                start = Offset(center.x - outerRadius * 0.8f, center.y),
                end = Offset(center.x + outerRadius * 0.8f, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = TextMuted.copy(alpha = axisAlpha),
                start = Offset(center.x, center.y - outerRadius * 0.8f),
                end = Offset(center.x, center.y + outerRadius * 0.8f),
                strokeWidth = 1.dp.toPx()
            )

            // Dead zone ring
            val dzRadius = maxMoveRadius * deadZone.coerceIn(0.02f, 0.50f)
            drawCircle(
                color = CrimsonRed.copy(alpha = 0.50f),
                radius = dzRadius,
                center = center,
                style = Stroke(
                    width = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                )
            )

            // Active thumb puck
            val puckCenter = center + knobOffset
            val puckRadius = outerRadius * 0.34f
            drawCircle(
                color = if (isPressed) accentColor.copy(alpha = 0.32f) else ElevatedDarkGrey.copy(alpha = 0.7f),
                radius = puckRadius + 4.dp.toPx(),
                center = puckCenter
            )
            drawCircle(
                color = if (isPressed) accentColor else NeonOrangeBright.copy(alpha = 0.75f),
                radius = puckRadius,
                center = puckCenter
            )
            drawCircle(
                color = MatteBlack,
                radius = puckRadius * 0.38f,
                center = puckCenter
            )
        }
    }
}

fun DrawScope.drawBattlefieldGrid(
    showGrid: Boolean = true
) {
    // Matte Black base gradient
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0A0B0E),
                Color(0xFF101319),
                Color(0xFF090A0D)
            )
        )
    )

    val center = Offset(size.width / 2f, size.height / 2f)

    if (showGrid) {
        val step = 36.dp.toPx()
        var x = step
        while (x < size.width) {
            drawLine(
                color = TacticalBorder.copy(alpha = 0.28f),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 1f
            )
            x += step
        }
        var y = step
        while (y < size.height) {
            drawLine(
                color = TacticalBorder.copy(alpha = 0.28f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += step
        }

        // Tactical range rings
        val minDim = min(size.width, size.height)
        listOf(0.24f, 0.44f, 0.68f).forEach { factor ->
            drawCircle(
                color = NeonOrange.copy(alpha = 0.08f),
                radius = minDim * factor,
                center = center,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                )
            )
        }
    }

    // Subtle corner tactical HUD brackets
    val pad = 12.dp.toPx()
    val len = 18.dp.toPx()
    val bracketColor = NeonOrange.copy(alpha = 0.45f)
    val stroke = 2.dp.toPx()
    // Top-left
    drawLine(bracketColor, Offset(pad, pad), Offset(pad + len, pad), stroke)
    drawLine(bracketColor, Offset(pad, pad), Offset(pad, pad + len), stroke)
    // Top-right
    drawLine(bracketColor, Offset(size.width - pad, pad), Offset(size.width - pad - len, pad), stroke)
    drawLine(bracketColor, Offset(size.width - pad, pad), Offset(size.width - pad, pad + len), stroke)
    // Bottom-left
    drawLine(bracketColor, Offset(pad, size.height - pad), Offset(pad + len, size.height - pad), stroke)
    drawLine(bracketColor, Offset(pad, size.height - pad), Offset(pad, size.height - pad - len), stroke)
    // Bottom-right
    drawLine(bracketColor, Offset(size.width - pad, size.height - pad), Offset(size.width - pad - len, size.height - pad), stroke)
    drawLine(bracketColor, Offset(size.width - pad, size.height - pad), Offset(size.width - pad, size.height - pad - len), stroke)
}

fun DrawScope.drawGeometricTarget(
    center: Offset,
    radius: Float,
    isHighlighted: Boolean = false,
    accentColor: Color = CrimsonRed
) {
    // Outer halo
    drawCircle(
        color = accentColor.copy(alpha = if (isHighlighted) 0.28f else 0.16f),
        radius = radius * 1.18f,
        center = center
    )
    // Outer ring
    drawCircle(
        color = accentColor,
        radius = radius,
        center = center,
        style = Stroke(width = 2.5.dp.toPx())
    )
    // Mid geometric hexagon / diamond accent
    val diamond = Path().apply {
        moveTo(center.x, center.y - radius * 0.72f)
        lineTo(center.x + radius * 0.72f, center.y)
        lineTo(center.x, center.y + radius * 0.72f)
        lineTo(center.x - radius * 0.72f, center.y)
        close()
    }
    drawPath(
        path = diamond,
        color = NeonOrange.copy(alpha = 0.85f),
        style = Stroke(width = 1.6.dp.toPx())
    )
    // Center-zone bullseye (inner 40% radius)
    drawCircle(
        color = if (isHighlighted) TacticalGreen else CrimsonRed,
        radius = radius * 0.40f,
        center = center
    )
    drawCircle(
        color = TextPrimary,
        radius = radius * 0.14f,
        center = center
    )
}

fun DrawScope.drawTacticalCrosshair(
    center: Offset,
    style: CrosshairStyle,
    hitFlash: Boolean = false,
    color: Color = NeonOrange
) {
    val activeColor = if (hitFlash) TacticalGreen else color
    val strokeW = 2.2.dp.toPx()
    val gap = if (hitFlash) 10.dp.toPx() else 6.dp.toPx()
    val arm = 13.dp.toPx()

    when (style) {
        CrosshairStyle.TACTICAL_DOT -> {
            drawCircle(
                color = activeColor.copy(alpha = 0.25f),
                radius = 16.dp.toPx(),
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawLine(activeColor, Offset(center.x - gap - arm, center.y), Offset(center.x - gap, center.y), strokeW, StrokeCap.Round)
            drawLine(activeColor, Offset(center.x + gap, center.y), Offset(center.x + gap + arm, center.y), strokeW, StrokeCap.Round)
            drawLine(activeColor, Offset(center.x, center.y - gap - arm), Offset(center.x, center.y - gap), strokeW, StrokeCap.Round)
            drawLine(activeColor, Offset(center.x, center.y + gap), Offset(center.x, center.y + gap + arm), strokeW, StrokeCap.Round)
            drawCircle(
                color = CrimsonRed,
                radius = 3.dp.toPx(),
                center = center
            )
        }
        CrosshairStyle.CLASSIC_CROSS -> {
            drawLine(activeColor, Offset(center.x - arm - 4.dp.toPx(), center.y), Offset(center.x + arm + 4.dp.toPx(), center.y), strokeW, StrokeCap.Round)
            drawLine(activeColor, Offset(center.x, center.y - arm - 4.dp.toPx()), Offset(center.x, center.y + arm + 4.dp.toPx()), strokeW, StrokeCap.Round)
            drawCircle(color = TextPrimary, radius = 2.5.dp.toPx(), center = center)
        }
        CrosshairStyle.OPEN_BRACKET -> {
            val r = 14.dp.toPx()
            drawArc(
                color = activeColor,
                startAngle = -35f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
            drawArc(
                color = activeColor,
                startAngle = 145f,
                sweepAngle = 70f,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
            drawCircle(color = CrimsonRed, radius = 2.8.dp.toPx(), center = center)
        }
    }
}
