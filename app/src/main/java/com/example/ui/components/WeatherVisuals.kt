package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.TemperatureUnit
import com.example.data.model.WeatherConditionCategory
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object WeatherFormatter {
    fun formatTemperature(tempCelsius: Double, unit: TemperatureUnit): String {
        val value = when (unit) {
            TemperatureUnit.CELSIUS -> tempCelsius
            TemperatureUnit.FAHRENHEIT -> (tempCelsius * 9.0 / 5.0) + 32.0
        }
        return "${Math.round(value)}°"
    }

    fun formatSpeed(speedKmh: Double, unit: TemperatureUnit): String {
        return when (unit) {
            TemperatureUnit.CELSIUS -> "${Math.round(speedKmh)} km/h"
            TemperatureUnit.FAHRENHEIT -> "${Math.round(speedKmh * 0.621371)} mph"
        }
    }

    fun formatPressure(pressureHpa: Double, unit: TemperatureUnit): String {
        return when (unit) {
            TemperatureUnit.CELSIUS -> "${Math.round(pressureHpa)} hPa"
            TemperatureUnit.FAHRENHEIT -> String.format(java.util.Locale.US, "%.2f inHg", pressureHpa * 0.02953)
        }
    }

    fun formatVisibility(meters: Double, unit: TemperatureUnit): String {
        return when (unit) {
            TemperatureUnit.CELSIUS -> {
                val km = meters / 1000.0
                if (km >= 10.0) "${Math.round(km)} km" else String.format(java.util.Locale.US, "%.1f km", km)
            }
            TemperatureUnit.FAHRENHEIT -> {
                val miles = (meters / 1000.0) * 0.621371
                String.format(java.util.Locale.US, "%.1f mi", miles)
            }
        }
    }
}

@Composable
fun WeatherArtworkHero(
    category: WeatherConditionCategory,
    isDay: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "weather_anim")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rainOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain"
    )

    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sun_rotation"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            when (category) {
                WeatherConditionCategory.CLEAR -> {
                    if (isDay) {
                        drawSunArtwork(w, h, pulse, rotateAngle)
                    } else {
                        drawMoonArtwork(w, h, pulse)
                    }
                }
                WeatherConditionCategory.PARTLY_CLOUDY -> {
                    if (isDay) {
                        drawSunArtwork(w * 0.75f, h * 0.75f, pulse, rotateAngle, Offset(w * 0.7f, h * 0.35f))
                    } else {
                        drawMoonArtwork(w * 0.75f, h * 0.75f, pulse, Offset(w * 0.7f, h * 0.35f))
                    }
                    drawCloudArtwork(w, h, Offset(w * 0.42f, h * 0.58f), scale = 0.95f)
                }
                WeatherConditionCategory.CLOUDY -> {
                    drawCloudArtwork(w, h, Offset(w * 0.58f, h * 0.42f), scale = 0.85f, alpha = 0.6f)
                    drawCloudArtwork(w, h, Offset(w * 0.44f, h * 0.56f), scale = 1.0f)
                }
                WeatherConditionCategory.DRIZZLE,
                WeatherConditionCategory.RAIN -> {
                    drawCloudArtwork(w, h, Offset(w * 0.48f, h * 0.42f), scale = 0.95f)
                    drawRainDroplets(w, h, rainOffset, isHeavy = false)
                }
                WeatherConditionCategory.HEAVY_RAIN -> {
                    drawCloudArtwork(w, h, Offset(w * 0.48f, h * 0.40f), scale = 1.0f, isDark = true)
                    drawRainDroplets(w, h, rainOffset, isHeavy = true)
                }
                WeatherConditionCategory.THUNDERSTORM -> {
                    drawCloudArtwork(w, h, Offset(w * 0.48f, h * 0.38f), scale = 1.05f, isDark = true)
                    drawLightningBolt(w, h, pulse)
                    drawRainDroplets(w, h, rainOffset, isHeavy = true)
                }
                WeatherConditionCategory.SNOW -> {
                    drawCloudArtwork(w, h, Offset(w * 0.48f, h * 0.40f), scale = 0.95f)
                    drawSnowflakes(w, h, rainOffset)
                }
                WeatherConditionCategory.FOG -> {
                    drawFogLayers(w, h, pulse)
                }
            }
        }
    }
}

// ----------------- DRAW HELPERS -----------------

private fun DrawScope.drawSunArtwork(
    w: Float,
    h: Float,
    pulse: Float,
    rotateAngle: Float,
    centerOffset: Offset = Offset(w / 2f, h / 2f)
) {
    val sunRadius = (w * 0.28f) * pulse

    // Outer warm atmospheric glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x66FBBF24),
                Color(0x22F59E0B),
                Color(0x00000000)
            ),
            center = centerOffset,
            radius = sunRadius * 2.2f
        ),
        radius = sunRadius * 2.2f,
        center = centerOffset
    )

    // Ray accents
    val rayCount = 8
    val rayInner = sunRadius * 1.15f
    val rayOuter = sunRadius * 1.45f
    for (i in 0 until rayCount) {
        val angleDeg = (i * (360f / rayCount)) + rotateAngle
        val angleRad = (angleDeg * PI / 180.0).toFloat()
        val start = Offset(
            centerOffset.x + rayInner * cos(angleRad),
            centerOffset.y + rayInner * sin(angleRad)
        )
        val end = Offset(
            centerOffset.x + rayOuter * cos(angleRad),
            centerOffset.y + rayOuter * sin(angleRad)
        )
        drawLine(
            color = Color(0xFFFDE68A).copy(alpha = 0.75f),
            start = start,
            end = end,
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
    }

    // Core Glowing Sun Sphere
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFEF08A),
                Color(0xFFF59E0B),
                Color(0xFFD97706)
            ),
            start = Offset(centerOffset.x - sunRadius, centerOffset.y - sunRadius),
            end = Offset(centerOffset.x + sunRadius, centerOffset.y + sunRadius)
        ),
        radius = sunRadius,
        center = centerOffset
    )

    // Specular shine highlight
    drawCircle(
        color = Color.White.copy(alpha = 0.45f),
        radius = sunRadius * 0.35f,
        center = Offset(centerOffset.x - sunRadius * 0.35f, centerOffset.y - sunRadius * 0.35f)
    )
}

private fun DrawScope.drawMoonArtwork(
    w: Float,
    h: Float,
    pulse: Float,
    centerOffset: Offset = Offset(w / 2f, h / 2f)
) {
    val moonRadius = (w * 0.28f) * pulse

    // Soft celestial glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x5593C5FD),
                Color(0x1160A5FA),
                Color(0x00000000)
            ),
            center = centerOffset,
            radius = moonRadius * 2.2f
        ),
        radius = moonRadius * 2.2f,
        center = centerOffset
    )

    // Moon body
    val path = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(
            centerOffset.x - moonRadius,
            centerOffset.y - moonRadius,
            centerOffset.x + moonRadius,
            centerOffset.y + moonRadius
        ))
    }
    drawPath(
        path = path,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFE2E8F0),
                Color(0xFF94A3B8),
                Color(0xFF64748B)
            ),
            start = Offset(centerOffset.x - moonRadius, centerOffset.y - moonRadius),
            end = Offset(centerOffset.x + moonRadius, centerOffset.y + moonRadius)
        )
    )

    // Moon craters
    drawCircle(
        color = Color(0xFF475569).copy(alpha = 0.3f),
        radius = moonRadius * 0.22f,
        center = Offset(centerOffset.x - moonRadius * 0.25f, centerOffset.y - moonRadius * 0.2f)
    )
    drawCircle(
        color = Color(0xFF475569).copy(alpha = 0.25f),
        radius = moonRadius * 0.16f,
        center = Offset(centerOffset.x + moonRadius * 0.3f, centerOffset.y + moonRadius * 0.1f)
    )
    drawCircle(
        color = Color(0xFF475569).copy(alpha = 0.25f),
        radius = moonRadius * 0.12f,
        center = Offset(centerOffset.x - moonRadius * 0.1f, centerOffset.y + moonRadius * 0.35f)
    )
}

private fun DrawScope.drawCloudArtwork(
    w: Float,
    h: Float,
    centerOffset: Offset,
    scale: Float = 1.0f,
    alpha: Float = 1.0f,
    isDark: Boolean = false
) {
    val cloudWidth = w * 0.65f * scale
    val cloudHeight = h * 0.38f * scale
    val startX = centerOffset.x - cloudWidth / 2f
    val baseY = centerOffset.y + cloudHeight * 0.2f

    val topColor = if (isDark) Color(0xFF94A3B8).copy(alpha = alpha) else Color(0xFFFFFFFF).copy(alpha = alpha)
    val bottomColor = if (isDark) Color(0xFF475569).copy(alpha = alpha) else Color(0xFFCBD5E1).copy(alpha = alpha)

    // Base pill shape of cloud
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(topColor, bottomColor),
            startY = baseY - cloudHeight * 0.5f,
            endY = baseY + cloudHeight * 0.3f
        ),
        topLeft = Offset(startX, baseY - cloudHeight * 0.3f),
        size = Size(cloudWidth, cloudHeight * 0.6f),
        cornerRadius = CornerRadius(cloudHeight * 0.3f, cloudHeight * 0.3f)
    )

    // Left puff
    val leftRadius = cloudHeight * 0.34f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = alpha), bottomColor),
            center = Offset(startX + cloudWidth * 0.3f, baseY - cloudHeight * 0.1f),
            radius = leftRadius
        ),
        radius = leftRadius,
        center = Offset(startX + cloudWidth * 0.3f, baseY - cloudHeight * 0.1f)
    )

    // Center/Top large puff
    val mainRadius = cloudHeight * 0.44f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = alpha), topColor, bottomColor),
            center = Offset(startX + cloudWidth * 0.55f, baseY - cloudHeight * 0.25f),
            radius = mainRadius
        ),
        radius = mainRadius,
        center = Offset(startX + cloudWidth * 0.55f, baseY - cloudHeight * 0.25f)
    )

    // Soft glass ambient border
    drawRoundRect(
        color = Color.White.copy(alpha = 0.25f * alpha),
        topLeft = Offset(startX, baseY - cloudHeight * 0.3f),
        size = Size(cloudWidth, cloudHeight * 0.6f),
        cornerRadius = CornerRadius(cloudHeight * 0.3f, cloudHeight * 0.3f),
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawRainDroplets(
    w: Float,
    h: Float,
    animationFraction: Float,
    isHeavy: Boolean
) {
    val count = if (isHeavy) 6 else 4
    val startX = w * 0.26f
    val stepX = (w * 0.48f) / (count - 1)
    val dropLength = if (isHeavy) 16f else 12f

    for (i in 0 until count) {
        val x = startX + i * stepX
        val stagger = (i * 0.25f) % 1f
        val currentProgress = (animationFraction + stagger) % 1f
        val y = (h * 0.65f) + currentProgress * (h * 0.28f)
        val alpha = sin(currentProgress * PI).toFloat()

        drawLine(
            color = Color(0xFF38BDF8).copy(alpha = (alpha * 0.85f).coerceIn(0f, 1f)),
            start = Offset(x, y),
            end = Offset(x - 3f, y + dropLength),
            strokeWidth = if (isHeavy) 3f else 2f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawLightningBolt(w: Float, h: Float, pulse: Float) {
    val centerX = w * 0.52f
    val startY = h * 0.56f

    val path = Path().apply {
        moveTo(centerX, startY)
        lineTo(centerX - 10f, startY + 22f)
        lineTo(centerX - 2f, startY + 22f)
        lineTo(centerX - 12f, startY + 44f)
        lineTo(centerX + 6f, startY + 26f)
        lineTo(centerX - 1f, startY + 26f)
        close()
    }

    // Glow
    drawPath(
        path = path,
        color = Color(0xFFFDE047).copy(alpha = (0.5f * pulse).coerceIn(0.2f, 0.8f))
    )
    // Core bolt
    drawPath(
        path = path,
        color = Color(0xFFFEF08A)
    )
}

private fun DrawScope.drawSnowflakes(w: Float, h: Float, animationFraction: Float) {
    val count = 5
    val startX = w * 0.28f
    val stepX = (w * 0.44f) / (count - 1)

    for (i in 0 until count) {
        val x = startX + i * stepX
        val stagger = (i * 0.3f) % 1f
        val currentProgress = (animationFraction + stagger) % 1f
        val y = (h * 0.64f) + currentProgress * (h * 0.28f)
        val alpha = sin(currentProgress * PI).toFloat()

        drawCircle(
            color = Color.White.copy(alpha = (alpha * 0.9f).coerceIn(0f, 1f)),
            radius = 3.5f,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawFogLayers(w: Float, h: Float, pulse: Float) {
    val lines = listOf(
        Pair(0.40f, 0.70f),
        Pair(0.50f, 0.85f),
        Pair(0.60f, 0.65f),
        Pair(0.70f, 0.80f)
    )

    lines.forEachIndexed { index, (yFraction, widthFraction) ->
        val y = h * yFraction
        val lineW = w * widthFraction * (0.95f + 0.05f * pulse)
        val startX = (w - lineW) / 2f
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFE2E8F0).copy(alpha = 0.55f),
                    Color(0xFFCBD5E1).copy(alpha = 0.65f),
                    Color.Transparent
                ),
                startX = startX,
                endX = startX + lineW
            ),
            topLeft = Offset(startX, y - 4f),
            size = Size(lineW, 8f),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }
}

@Composable
fun WeatherConditionMiniIcon(
    category: WeatherConditionCategory,
    isDay: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    WeatherArtworkHero(
        category = category,
        isDay = isDay,
        modifier = modifier,
        size = size
    )
}
