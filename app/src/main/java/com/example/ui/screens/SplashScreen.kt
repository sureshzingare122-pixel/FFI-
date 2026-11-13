package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ElevatedDarkGrey
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing)
        )
        delay(350L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A1410),
                        MatteBlack
                    )
                )
            )
            .systemBarsPadding()
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Canvas(modifier = Modifier.size(136.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension * 0.42f
                val p = progress.value

                rotate(degrees = (1f - p) * 90f, pivot = c) {
                    drawArc(
                        color = NeonOrange,
                        startAngle = 0f,
                        sweepAngle = 360f * p,
                        useCenter = false,
                        topLeft = Offset(c.x - radius, c.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Crosshair ticks
                val tick = 14.dp.toPx() * p
                drawLine(NeonOrange, Offset(c.x - radius - tick, c.y), Offset(c.x - radius * 0.55f, c.y), 3.dp.toPx(), StrokeCap.Round)
                drawLine(NeonOrange, Offset(c.x + radius * 0.55f, c.y), Offset(c.x + radius + tick, c.y), 3.dp.toPx(), StrokeCap.Round)
                drawLine(NeonOrange, Offset(c.x, c.y - radius - tick), Offset(c.x, c.y - radius * 0.55f), 3.dp.toPx(), StrokeCap.Round)
                drawLine(NeonOrange, Offset(c.x, c.y + radius * 0.55f), Offset(c.x, c.y + radius + tick), 3.dp.toPx(), StrokeCap.Round)

                // Crimson inner target core
                val triSize = radius * 0.44f * p
                val tri = Path().apply {
                    moveTo(c.x, c.y - triSize)
                    lineTo(c.x + triSize * 0.88f, c.y + triSize * 0.65f)
                    lineTo(c.x - triSize * 0.88f, c.y + triSize * 0.65f)
                    close()
                }
                drawPath(tri, color = CrimsonRed, style = Stroke(width = 3.dp.toPx()))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "FFI TRAINER PRO",
                style = MaterialTheme.typography.displayLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "TACTICAL AIM & MOVEMENT SIMULATOR",
                style = MaterialTheme.typography.labelLarge,
                color = NeonOrange
            )

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ElevatedDarkGrey,
                border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder)
            ) {
                Text(
                    text = "${stringResource(R.string.offline_badge)}  •  v1.0.0",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}
