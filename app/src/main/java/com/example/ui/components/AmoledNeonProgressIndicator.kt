package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.NeonAccent
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * Custom circular progress indicator engineered specifically for AMOLED (#000000) displays.
 * Uses high-contrast neon accents (default: Cyber Neon Cyan [NeonAccent]) with:
 * - A subtle dark track that prevents visual jarring on pure black.
 * - An outer translucent neon glow halo.
 * - A dynamic sweep angle with smooth indeterminate rotation.
 * - An optional pulsing neon core dot indicating active data telemetry.
 */
@Composable
fun AmoledNeonProgressIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    strokeWidth: Dp = 3.5.dp,
    neonColor: Color = NeonAccent,
    secondaryNeonColor: Color = NeonEmerald,
    glowColor: Color = neonColor.copy(alpha = 0.32f),
    trackColor: Color = neonColor.copy(alpha = 0.12f),
    showCenterPulse: Boolean = true,
    testTag: String = "amoled_neon_progress_indicator"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AmoledNeonProgress")

    // Smooth continuous 360-degree rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Dynamic sweep angle expansion and contraction
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = 280f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sweepAngle"
    )

    // Center core neon pulse alpha
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "Fetching anime metadata" }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val glowStrokePx = strokePx * 1.8f
            val arcSize = Size(
                width = this.size.width - glowStrokePx,
                height = this.size.height - glowStrokePx
            )
            val topLeft = Offset(glowStrokePx / 2f, glowStrokePx / 2f)

            // 1. Subtle AMOLED background track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            rotate(rotation) {
                // 2. Soft outer neon glow halo
                drawArc(
                    color = glowColor,
                    startAngle = 0f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = glowStrokePx, cap = StrokeCap.Round)
                )

                // 3. Foreground primary neon sweep arc
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to neonColor,
                        0.5f to secondaryNeonColor,
                        1.0f to neonColor
                    ),
                    startAngle = 0f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }

            // 4. Center pulsing neon data core
            if (showCenterPulse) {
                val centerRadius = (this.size.minDimension * 0.12f).coerceAtLeast(2.5f)
                drawCircle(
                    color = neonColor.copy(alpha = pulseAlpha),
                    radius = centerRadius,
                    center = this.center
                )
            }
        }
    }
}

/**
 * Dedicated full/container-level metadata fetching status view.
 * Displays the AMOLED neon progress indicator alongside live API status labels.
 */
@Composable
fun AmoledMetadataLoadingView(
    modifier: Modifier = Modifier,
    title: String = "Fetching Anime Metadata...",
    subtitle: String = "Querying AniList GraphQL & Jikan REST APIs",
    neonColor: Color = NeonCyan,
    indicatorSize: Dp = 48.dp,
    testTag: String = "amoled_metadata_loading_view"
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AmoledBlack)
            .padding(24.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Neon AMOLED Indicator
            AmoledNeonProgressIndicator(
                size = indicatorSize,
                strokeWidth = 3.5.dp,
                neonColor = neonColor,
                secondaryNeonColor = NeonEmerald,
                testTag = "${testTag}_spinner"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Status
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Telemetry Subtitle Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(AmoledCard)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Glowing live telemetry dot
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(NeonEmerald)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
