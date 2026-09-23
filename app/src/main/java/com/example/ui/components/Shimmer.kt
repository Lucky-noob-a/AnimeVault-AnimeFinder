package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.AmoledSurfaceVariant
import com.example.ui.theme.TextTertiary

/**
 * Custom AMOLED shimmer modifier.
 *
 * Designed specifically for true OLED/AMOLED pure black (#000000) displays:
 * Uses deep dark slate tones [baseColor] sweeping gracefully into a dark metallic
 * luminance [highlightColor] without producing washed-out white/light gray pixel glare.
 */
fun Modifier.amoledShimmer(
    baseColor: Color = Color(0xFF0F0F13),
    highlightColor: Color = Color(0xFF262634),
    durationMillis: Int = 1250
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "AmoledShimmerTransition")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AmoledShimmerProgress"
    )

    this.drawBehind {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@drawBehind

        val totalDistance = width * 1.6f + height
        val currentOffset = totalDistance * progress - width

        val brush = Brush.linearGradient(
            colors = listOf(
                baseColor,
                baseColor,
                highlightColor,
                baseColor,
                baseColor
            ),
            start = Offset(x = currentOffset, y = 0f),
            end = Offset(x = currentOffset + width, y = height)
        )
        drawRect(brush = brush)
    }
}

/**
 * Standalone shimmer placeholder with customizable shape, base and highlight colors.
 */
@Composable
fun AmoledShimmerPlaceholder(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    baseColor: Color = Color(0xFF0F0F13),
    highlightColor: Color = Color(0xFF262634),
    showIcon: Boolean = false,
    icon: ImageVector = Icons.Default.Movie
) {
    Box(
        modifier = modifier
            .clip(shape)
            .amoledShimmer(baseColor = baseColor, highlightColor = highlightColor),
        contentAlignment = Alignment.Center
    ) {
        if (showIcon) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextTertiary.copy(alpha = 0.25f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Drop-in image loader tailored for the AMOLED design system.
 * While the image is downloading, displays a fluid [AmoledShimmerPlaceholder].
 * Gracefully handles loading errors or empty URLs with an AMOLED dark card placeholder.
 */
@Composable
fun AmoledAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: Shape = RectangleShape,
    alignment: Alignment = Alignment.Center,
    placeholderIcon: ImageVector = Icons.Default.Movie,
    showShimmer: Boolean = true
) {
    val isModelEmpty = model == null || (model is String && model.isBlank())

    if (isModelEmpty) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(AmoledSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = placeholderIcon,
                contentDescription = null,
                tint = TextTertiary.copy(alpha = 0.35f),
                modifier = Modifier.size(24.dp)
            )
        }
    } else {
        SubcomposeAsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = modifier.clip(shape),
            contentScale = contentScale,
            alignment = alignment,
            loading = {
                if (showShimmer) {
                    AmoledShimmerPlaceholder(
                        modifier = Modifier.fillMaxSize(),
                        showIcon = false
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AmoledSurfaceVariant)
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AmoledSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = placeholderIcon,
                        contentDescription = null,
                        tint = TextTertiary.copy(alpha = 0.35f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            success = {
                SubcomposeAsyncImageContent()
            }
        )
    }
}
