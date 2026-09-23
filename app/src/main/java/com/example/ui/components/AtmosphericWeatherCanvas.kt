package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.data.model.WeatherConditionCategory
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Atmospheric background gradient tailored to each weather condition and time of day,
 * matching the dark, moody, cinematic aesthetic of the reference design.
 */
fun getAtmosphericGradient(category: WeatherConditionCategory, isDay: Boolean = true): Brush {
    return when (category) {
        WeatherConditionCategory.THUNDERSTORM -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A),
                Color(0xFF1E293B),
                Color(0xFF0F1923),
                Color(0xFF050B14)
            )
        )
        WeatherConditionCategory.RAIN,
        WeatherConditionCategory.DRIZZLE,
        WeatherConditionCategory.HEAVY_RAIN -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0B131F),
                Color(0xFF131D2E),
                Color(0xFF101926),
                Color(0xFF080D14)
            )
        )
        WeatherConditionCategory.SNOW -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF151D28),
                Color(0xFF1E2836),
                Color(0xFF161F2A),
                Color(0xFF0C131B)
            )
        )
        WeatherConditionCategory.CLEAR -> if (isDay) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0A2240),
                    Color(0xFF113865),
                    Color(0xFF1A4778),
                    Color(0xFF0D2545)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF030712),
                    Color(0xFF0B1120),
                    Color(0xFF0A101D),
                    Color(0xFF02040A)
                )
            )
        }
        WeatherConditionCategory.PARTLY_CLOUDY -> if (isDay) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0F2137),
                    Color(0xFF1A334F),
                    Color(0xFF1E354F),
                    Color(0xFF0D1C2E)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF070E1A),
                    Color(0xFF0F1928),
                    Color(0xFF0A1320),
                    Color(0xFF040810)
                )
            )
        }
        WeatherConditionCategory.CLOUDY -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF141A23),
                Color(0xFF1E2633),
                Color(0xFF17202B),
                Color(0xFF0B0F15)
            )
        )
        WeatherConditionCategory.FOG -> Brush.verticalGradient(
            colors = listOf(
                Color(0xFF151E28),
                Color(0xFF222F3D),
                Color(0xFF1B2633),
                Color(0xFF0F161E)
            )
        )
    }
}

// ----------------- STATIC PARTICLES CONFIGURATIONS -----------------

private data class RaindropGlass(
    val relX: Float,
    val relY: Float,
    val radius: Float,
    val isDripping: Boolean,
    val dripSpeed: Float,
    val dripPhase: Float,
    val trailLength: Float
)

private data class SnowflakeParticle(
    val relX: Float,
    val relY: Float,
    val radius: Float,
    val alpha: Float,
    val speed: Float,
    val swayAmplitude: Float,
    val swayFrequency: Float,
    val phase: Float
)

private data class StarParticle(
    val relX: Float,
    val relY: Float,
    val size: Float,
    val baseAlpha: Float,
    val twinkleSpeed: Float,
    val twinklePhase: Float
)

private data class FogBand(
    val relY: Float,
    val height: Float,
    val speed: Float,
    val baseAlpha: Float,
    val direction: Float
)

@Composable
fun AtmosphericWeatherCanvas(
    category: WeatherConditionCategory,
    isDay: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "atmospheric_anim")

    // Continuous loop animators
    val rainLoop by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_loop"
    )

    val snowLoop by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snow_loop"
    )

    val thunderLoop by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "thunder_loop"
    )

    val sunLoop by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sun_loop"
    )

    val cloudDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloud_drift"
    )

    // Pre-calculated deterministic particles for high performance (no reallocation in draw)
    val glassDroplets = remember {
        val r = Random(42)
        (0 until 45).map {
            RaindropGlass(
                relX = r.nextFloat(),
                relY = r.nextFloat(),
                radius = 2.5f + r.nextFloat() * 6.5f,
                isDripping = r.nextFloat() < 0.28f, // 28% of droplets slide down the screen
                dripSpeed = 0.5f + r.nextFloat() * 1.0f,
                dripPhase = r.nextFloat(),
                trailLength = 20f + r.nextFloat() * 50f
            )
        }
    }

    val snowflakes = remember {
        val r = Random(101)
        (0 until 70).map {
            SnowflakeParticle(
                relX = r.nextFloat(),
                relY = r.nextFloat(),
                radius = 1.8f + r.nextFloat() * 5.0f,
                alpha = 0.35f + r.nextFloat() * 0.55f,
                speed = 0.6f + r.nextFloat() * 1.2f,
                swayAmplitude = 12f + r.nextFloat() * 20f,
                swayFrequency = 1.5f + r.nextFloat() * 2.5f,
                phase = r.nextFloat() * (2 * PI).toFloat()
            )
        }
    }

    val stars = remember {
        val r = Random(303)
        (0 until 65).map {
            StarParticle(
                relX = r.nextFloat(),
                relY = r.nextFloat() * 0.75f, // Mostly in upper sky
                size = 1.0f + r.nextFloat() * 2.2f,
                baseAlpha = 0.25f + r.nextFloat() * 0.65f,
                twinkleSpeed = 0.8f + r.nextFloat() * 2.0f,
                twinklePhase = r.nextFloat() * (2 * PI).toFloat()
            )
        }
    }

    val fogBands = remember {
        val r = Random(505)
        (0 until 6).map { idx ->
            FogBand(
                relY = 0.25f + (idx * 0.12f),
                height = 70f + r.nextFloat() * 90f,
                speed = 0.3f + r.nextFloat() * 0.5f,
                baseAlpha = 0.12f + r.nextFloat() * 0.18f,
                direction = if (idx % 2 == 0) 1f else -1f
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            when (category) {
                // 1. Rain / Low Rain (Faithful Tokyo rain drops on glass)
                WeatherConditionCategory.RAIN,
                WeatherConditionCategory.DRIZZLE,
                WeatherConditionCategory.HEAVY_RAIN -> {
                    val isHeavy = category == WeatherConditionCategory.HEAVY_RAIN
                    drawRainStreaks(width, height, rainLoop, isHeavy)
                    drawGlassDroplets(width, height, glassDroplets, rainLoop, isHeavy)
                }

                // 2. Thunderstorm (Faithful Sydney dark rolling clouds + realistic lightning flash)
                WeatherConditionCategory.THUNDERSTORM -> {
                    drawThunderstormSky(width, height, cloudDrift, thunderLoop)
                    drawRainStreaks(width, height, rainLoop, isHeavy = true)
                }

                // 3. Snow / Snow Rain (Faithful New York multi-layered snowfall)
                WeatherConditionCategory.SNOW -> {
                    drawSnowfall(width, height, snowflakes, snowLoop)
                }

                // 4. Clear Day / Clear Night
                WeatherConditionCategory.CLEAR -> {
                    if (isDay) {
                        drawClearDaySun(width, height, sunLoop)
                    } else {
                        drawClearNightStars(width, height, stars, snowLoop)
                    }
                }

                // 5. Partly Cloudy
                WeatherConditionCategory.PARTLY_CLOUDY -> {
                    if (isDay) {
                        drawClearDaySun(width, height, sunLoop, isMuted = true)
                    } else {
                        drawClearNightStars(width, height, stars, snowLoop, isMuted = true)
                    }
                    drawDriftingClouds(width, height, cloudDrift, density = 0.45f)
                }

                // 6. Overcast / Cloudy
                WeatherConditionCategory.CLOUDY -> {
                    drawOvercastSky(width, height, cloudDrift)
                }

                // 7. Fog / Mist
                WeatherConditionCategory.FOG -> {
                    drawFoggyAtmosphere(width, height, fogBands, cloudDrift)
                }
            }
        }
    }
}

// ----------------- DRAWING IMPLEMENTATIONS -----------------

/**
 * Tokyo-style rain on glass:
 * Realistic liquid droplets on camera lens/window pane with refraction highlight,
 * water beads, and sliding moisture trails.
 */
private fun DrawScope.drawGlassDroplets(
    w: Float,
    h: Float,
    droplets: List<RaindropGlass>,
    loopFraction: Float,
    isHeavy: Boolean
) {
    droplets.forEach { drop ->
        val x = drop.relX * w

        // Calculate Y position with sliding physics if it's a dripping droplet
        val y = if (drop.isDripping) {
            val dripProgress = (loopFraction * drop.dripSpeed + drop.dripPhase) % 1f
            // Sliding drops accelerate slightly down the glass
            val currentRelY = (drop.relY + dripProgress * 0.65f) % 1f
            currentRelY * h
        } else {
            drop.relY * h
        }

        val r = drop.radius * if (isHeavy) 1.25f else 1.0f

        // Draw running moisture trail behind dripping droplet
        if (drop.isDripping) {
            val trailLen = drop.trailLength
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.0f),
                        Color(0x22FFFFFF),
                        Color(0x44CBD5E1)
                    ),
                    startY = y - trailLen,
                    endY = y
                ),
                start = Offset(x, y - trailLen),
                end = Offset(x, y),
                strokeWidth = r * 0.7f,
                cap = StrokeCap.Round
            )
        }

        // Droplet outer shadow/refraction ring
        drawCircle(
            color = Color(0x33000000),
            radius = r + 1.2f,
            center = Offset(x + 0.8f, y + 1.2f)
        )

        // Droplet body (glass water droplet)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x44E2E8F0),
                    Color(0x2294A3B8),
                    Color(0x110F172A)
                ),
                center = Offset(x - r * 0.2f, y - r * 0.2f),
                radius = r
            ),
            radius = r,
            center = Offset(x, y)
        )

        // Glass specular highlight reflection (tiny white glossy glint)
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = r * 0.35f,
            center = Offset(x - r * 0.35f, y - r * 0.35f)
        )

        // Secondary bottom bounce reflection
        drawCircle(
            color = Color(0x66BAE6FD),
            radius = r * 0.2f,
            center = Offset(x + r * 0.25f, y + r * 0.3f)
        )
    }
}

/**
 * Slanted background rain streaks
 */
private fun DrawScope.drawRainStreaks(
    w: Float,
    h: Float,
    progress: Float,
    isHeavy: Boolean
) {
    val streakCount = if (isHeavy) 45 else 25
    val streakLength = if (isHeavy) 55f else 35f
    val slantX = -12f

    for (i in 0 until streakCount) {
        val relX = ((i * 0.043f) + (i % 7) * 0.11f) % 1f
        val stagger = (i * 0.17f) % 1f
        val streakProg = (progress * 1.5f + stagger) % 1f

        val startX = relX * w + (streakProg * slantX)
        val startY = streakProg * (h + streakLength * 2) - streakLength

        val alpha = if (isHeavy) 0.35f else 0.22f

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0x0038BDF8),
                    Color(0xFF38BDF8).copy(alpha = alpha),
                    Color(0x0038BDF8)
                ),
                start = Offset(startX, startY),
                end = Offset(startX + slantX, startY + streakLength)
            ),
            start = Offset(startX, startY),
            end = Offset(startX + slantX, startY + streakLength),
            strokeWidth = if (isHeavy) 2.2f else 1.5f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Sydney-style Thunderstorm:
 * Moody turbulent clouds with realistic branched lightning strike and volumetric screen flash!
 */
private fun DrawScope.drawThunderstormSky(
    w: Float,
    h: Float,
    cloudDrift: Float,
    thunderLoop: Float
) {
    // Determine lightning strike timing (occurs at 0.15 - 0.22 and 0.65 - 0.72 of thunderLoop)
    val isFlash1 = thunderLoop in 0.15f..0.22f
    val isFlash2 = thunderLoop in 0.65f..0.72f
    val isStriking = isFlash1 || isFlash2

    val flashIntensity = if (isStriking) {
        // Double-flash pulse curve
        val localProg = if (isFlash1) (thunderLoop - 0.15f) / 0.07f else (thunderLoop - 0.65f) / 0.07f
        (sin(localProg * PI * 3).toFloat().coerceAtLeast(0f) * 0.55f)
    } else 0f

    // 1. Storm cloud billows in upper atmosphere
    val cloudOffset1 = (cloudDrift * 0.2f) % 1f
    drawCircle(
        color = Color(0x331E293B),
        radius = w * 0.65f,
        center = Offset(w * 0.3f + cloudOffset1 * w * 0.1f, -h * 0.05f)
    )
    drawCircle(
        color = Color(0x440F172A),
        radius = w * 0.75f,
        center = Offset(w * 0.8f - cloudOffset1 * w * 0.1f, h * 0.05f)
    )

    // 2. Volumetric lightning flash that illuminates the whole sky
    if (flashIntensity > 0.05f) {
        drawRect(
            color = Color(0xFFC7D2FE).copy(alpha = flashIntensity),
            size = Size(w, h)
        )

        // 3. Branched lightning bolt arcing down from the storm cloud
        val boltStartX = if (isFlash1) w * 0.42f else w * 0.68f
        val boltPath = Path().apply {
            moveTo(boltStartX, h * 0.06f)
            lineTo(boltStartX - 18f, h * 0.14f)
            lineTo(boltStartX - 6f, h * 0.15f)
            lineTo(boltStartX - 28f, h * 0.24f)
            lineTo(boltStartX - 10f, h * 0.26f)
            lineTo(boltStartX - 35f, h * 0.38f)
            lineTo(boltStartX - 16f, h * 0.40f)
            lineTo(boltStartX - 42f, h * 0.52f)
        }

        // Branch 1
        val branchPath = Path().apply {
            moveTo(boltStartX - 28f, h * 0.24f)
            lineTo(boltStartX + 12f, h * 0.30f)
            lineTo(boltStartX + 4f, h * 0.32f)
            lineTo(boltStartX + 22f, h * 0.39f)
        }

        // Bolt outer glow
        drawPath(
            path = boltPath,
            color = Color(0x8893C5FD),
            style = Stroke(width = 8f, cap = StrokeCap.Round)
        )
        // Bolt core
        drawPath(
            path = boltPath,
            color = Color.White,
            style = Stroke(width = 3.0f, cap = StrokeCap.Round)
        )
        // Branch glow & core
        drawPath(
            path = branchPath,
            color = Color(0x6693C5FD),
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
        drawPath(
            path = branchPath,
            color = Color.White,
            style = Stroke(width = 1.8f, cap = StrokeCap.Round)
        )
    }
}

/**
 * New York-style snowfall:
 * Multi-depth falling snowflakes with gentle wind sway and soft focus.
 */
private fun DrawScope.drawSnowfall(
    w: Float,
    h: Float,
    particles: List<SnowflakeParticle>,
    loopFraction: Float
) {
    particles.forEach { flake ->
        val progress = (loopFraction * flake.speed + (flake.phase / (2 * PI).toFloat())) % 1f
        val y = progress * (h + 40f) - 20f

        // Horizontal sinusoidal drift (natural fluttering wind sway)
        val sway = sin(flake.phase + progress * flake.swayFrequency * 2 * PI).toFloat() * flake.swayAmplitude
        val x = (flake.relX * w + sway) % w

        val isForeground = flake.radius > 4.5f

        if (isForeground) {
            // Soft large foreground snowflake with soft glow halo
            drawCircle(
                color = Color.White.copy(alpha = flake.alpha * 0.35f),
                radius = flake.radius * 1.6f,
                center = Offset(x, y)
            )
            drawCircle(
                color = Color.White.copy(alpha = flake.alpha),
                radius = flake.radius,
                center = Offset(x, y)
            )
        } else {
            // Crisp midground/background flake
            drawCircle(
                color = Color.White.copy(alpha = flake.alpha),
                radius = flake.radius,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * Clear Day:
 * Warm glowing solar disc, rotating radiant sunbeams, and floating dust motes.
 */
private fun DrawScope.drawClearDaySun(
    w: Float,
    h: Float,
    rotateDeg: Float,
    isMuted: Boolean = false
) {
    val sunCenterX = w * 0.78f
    val sunCenterY = h * 0.18f
    val coreRadius = 46f
    val alphaScale = if (isMuted) 0.55f else 1.0f

    // Outer warm atmospheric glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x66FBBF24).copy(alpha = 0.35f * alphaScale),
                Color(0x33F59E0B).copy(alpha = 0.15f * alphaScale),
                Color.Transparent
            ),
            center = Offset(sunCenterX, sunCenterY),
            radius = coreRadius * 4.8f
        ),
        radius = coreRadius * 4.8f,
        center = Offset(sunCenterX, sunCenterY)
    )

    // Rotating radiant sun rays
    val rayCount = 12
    val rayLen = 95f
    for (i in 0 until rayCount) {
        val angle = ((rotateDeg + (i * 360f / rayCount)) * PI / 180f).toFloat()
        val startDist = coreRadius * 1.15f
        val endDist = startDist + rayLen

        val start = Offset(
            sunCenterX + cos(angle) * startDist,
            sunCenterY + sin(angle) * startDist
        )
        val end = Offset(
            sunCenterX + cos(angle) * endDist,
            sunCenterY + sin(angle) * endDist
        )

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0x88FDE047).copy(alpha = 0.45f * alphaScale),
                    Color.Transparent
                ),
                start = start,
                end = end
            ),
            start = start,
            end = end,
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
    }

    // Solar core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFBEB),
                Color(0xFFFEF08A),
                Color(0xFFFBBF24)
            ),
            center = Offset(sunCenterX - 8f, sunCenterY - 8f),
            radius = coreRadius
        ),
        radius = coreRadius,
        center = Offset(sunCenterX, sunCenterY)
    )
}

/**
 * Clear Night:
 * Twinkling star field, shooting stars (meteors), and gentle moon halo.
 */
private fun DrawScope.drawClearNightStars(
    w: Float,
    h: Float,
    stars: List<StarParticle>,
    loopFraction: Float,
    isMuted: Boolean = false
) {
    val scale = if (isMuted) 0.5f else 1.0f

    // 1. Twinkling stars
    stars.forEach { star ->
        val x = star.relX * w
        val y = star.relY * h
        val twinkle = ((sin(loopFraction * star.twinkleSpeed * 2 * PI + star.twinklePhase) + 1.0) / 2.0).toFloat()
        val currentAlpha = (star.baseAlpha * (0.3f + 0.7f * twinkle) * scale).coerceIn(0f, 1f)

        drawCircle(
            color = Color.White.copy(alpha = currentAlpha),
            radius = star.size,
            center = Offset(x, y)
        )
    }

    // 2. Occasional shooting star / meteor
    val meteorProgress = (loopFraction * 1.8f) % 1f
    if (meteorProgress < 0.25f) {
        val norm = meteorProgress / 0.25f
        val startX = w * 0.85f - (norm * w * 0.45f)
        val startY = h * 0.08f + (norm * h * 0.22f)
        val trailLen = 70f
        val angle = 30f * PI / 180f
        val endX = startX + cos(angle).toFloat() * trailLen
        val endY = startY - sin(angle).toFloat() * trailLen

        val alpha = (sin(norm * PI).toFloat() * 0.75f).coerceIn(0f, 1f)

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = alpha),
                    Color(0x00BAE6FD)
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY)
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2.0f,
            cap = StrokeCap.Round
        )
    }

    // 3. Soft Moon halo in top-right
    val moonX = w * 0.82f
    val moonY = h * 0.16f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x33CBD5E1),
                Color(0x1194A3B8),
                Color.Transparent
            ),
            center = Offset(moonX, moonY),
            radius = 65f
        ),
        radius = 65f,
        center = Offset(moonX, moonY)
    )
}

/**
 * Partly Cloudy:
 * Soft volumetric clouds floating across the screen.
 */
private fun DrawScope.drawDriftingClouds(
    w: Float,
    h: Float,
    driftFraction: Float,
    density: Float
) {
    val cloudCount = 4
    for (i in 0 until cloudCount) {
        val relProg = (driftFraction * (0.4f + i * 0.15f) + (i * 0.25f)) % 1f
        val x = (relProg * (w + 200f)) - 100f
        val y = h * (0.12f + i * 0.09f)
        val cloudW = 140f + (i * 35f)
        val cloudH = 45f + (i * 12f)

        drawRoundRect(
            color = Color(0x3394A3B8).copy(alpha = density * 0.4f),
            topLeft = Offset(x, y),
            size = Size(cloudW, cloudH),
            cornerRadius = CornerRadius(cloudH / 2f, cloudH / 2f)
        )
        drawCircle(
            color = Color(0x33E2E8F0).copy(alpha = density * 0.5f),
            radius = cloudH * 0.85f,
            center = Offset(x + cloudW * 0.4f, y + cloudH * 0.3f)
        )
        drawCircle(
            color = Color(0x22CBD5E1).copy(alpha = density * 0.45f),
            radius = cloudH * 0.7f,
            center = Offset(x + cloudW * 0.7f, y + cloudH * 0.4f)
        )
    }
}

/**
 * Cloudy / Overcast:
 * Dense rolling dark cloud layers with moody atmospheric tone.
 */
private fun DrawScope.drawOvercastSky(
    w: Float,
    h: Float,
    drift: Float
) {
    val layerCount = 3
    for (layer in 0 until layerCount) {
        val speed = 0.2f + layer * 0.15f
        val offset = (drift * speed) % 1f
        val y = h * (0.05f + layer * 0.12f)
        val alpha = 0.25f + layer * 0.12f

        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF334155).copy(alpha = alpha),
                    Color(0xFF475569).copy(alpha = alpha * 1.2f),
                    Color(0xFF334155).copy(alpha = alpha),
                    Color.Transparent
                ),
                startX = -w * 0.3f + offset * w,
                endX = w * 1.3f + offset * w
            ),
            topLeft = Offset(0f, y),
            size = Size(w, 85f),
            cornerRadius = CornerRadius(42f, 42f)
        )
    }
}

/**
 * Fog / Mist:
 * Floating horizontal volumetric fog banks with undulating opacity breath.
 */
private fun DrawScope.drawFoggyAtmosphere(
    w: Float,
    h: Float,
    bands: List<FogBand>,
    driftFraction: Float
) {
    bands.forEachIndexed { idx, band ->
        val prog = (driftFraction * band.speed) % 1f
        val shiftX = sin(prog * 2 * PI + idx).toFloat() * 35f
        val y = band.relY * h

        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFCBD5E1).copy(alpha = band.baseAlpha * 0.7f),
                    Color(0xFFE2E8F0).copy(alpha = band.baseAlpha),
                    Color(0xFF94A3B8).copy(alpha = band.baseAlpha * 0.8f),
                    Color.Transparent
                ),
                startX = shiftX,
                endX = w + shiftX
            ),
            topLeft = Offset(0f, y - band.height / 2f),
            size = Size(w, band.height),
            cornerRadius = CornerRadius(band.height / 2f, band.height / 2f)
        )
    }
}
