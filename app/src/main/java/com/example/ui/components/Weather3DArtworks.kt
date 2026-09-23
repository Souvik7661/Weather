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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherConditionCategory
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D Claymorphic Layered Weather Illustration
 * Faithfully matches the user's reference design with sculpted 3D paper-cut clouds,
 * electric lightning bolts, crescent moons, crystalline snowflakes, and raindrops.
 */
@Composable
fun Weather3DArtworkHero(
    condition: WeatherCondition,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "weather_3d_anim")

    // Smooth breathing / floating idle physics
    val floatY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_anim"
    )

    // Pulse for glow and lightning
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_anim"
    )

    // Continuous rainfall / snowfall progression
    val rainProgression by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_progression"
    )

    // Lightning flash flicker
    val lightningFlash by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lightning_flash"
    )

    // Sun corona slow rotation
    val sunRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sun_rotation"
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag("weather_3d_artwork_hero"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            when (condition.category) {
                // 1. CLEAR (Sunny Day or Starry Night)
                WeatherConditionCategory.CLEAR -> {
                    if (condition.isDay) {
                        draw3DSun(w, h, pulse, sunRotation, floatY)
                    } else {
                        draw3DCrescentMoon(w, h, pulse, floatY)
                    }
                }

                // 2. PARTLY CLOUDY (Sun/Moon peeking behind cloud)
                WeatherConditionCategory.PARTLY_CLOUDY -> {
                    if (condition.isDay) {
                        draw3DSun(w, h, pulse, sunRotation, floatY, scale = 0.70f, offset = Offset(w * 0.70f, h * 0.35f))
                    } else {
                        draw3DCrescentMoon(w, h, pulse, floatY, scale = 0.70f, offset = Offset(w * 0.70f, h * 0.35f))
                    }
                    draw3DCloud(w, h, offset = Offset(w * 0.46f, h * 0.58f + floatY), scale = 0.95f)
                }

                // 3. CLOUDY / OVERCAST (Multi-layered 3D clouds with depth)
                WeatherConditionCategory.CLOUDY -> {
                    // Back deep shadow cloud
                    draw3DCloud(w, h, offset = Offset(w * 0.62f, h * 0.40f - floatY * 0.5f), scale = 0.85f, style = CloudStyle.DARK_SLATE)
                    // Mid cloud
                    draw3DCloud(w, h, offset = Offset(w * 0.35f, h * 0.46f + floatY * 0.5f), scale = 0.88f, style = CloudStyle.SLATE)
                    // Front hero cloud
                    draw3DCloud(w, h, offset = Offset(w * 0.48f, h * 0.60f + floatY), scale = 1.05f, style = CloudStyle.PEARL_WHITE)
                }

                // 4. DRIZZLE (Soft cloud with light rain droplets)
                WeatherConditionCategory.DRIZZLE -> {
                    draw3DCloud(w, h, offset = Offset(w * 0.50f, h * 0.42f + floatY), scale = 1.0f, style = CloudStyle.PEARL_WHITE)
                    draw3DRaindrops(w, h, rainProgression, count = 4, isHeavy = false)
                }

                // 5. RAIN / SHOWERS (Sculpted cloud with teardrop raindrops)
                WeatherConditionCategory.RAIN -> {
                    // Soft background cloud shadow
                    draw3DCloud(w, h, offset = Offset(w * 0.56f, h * 0.38f), scale = 0.90f, style = CloudStyle.SLATE)
                    // Front puffy cloud
                    draw3DCloud(w, h, offset = Offset(w * 0.48f, h * 0.44f + floatY), scale = 1.05f, style = CloudStyle.PEARL_WHITE)
                    // 3D Teardrop raindrops
                    draw3DRaindrops(w, h, rainProgression, count = 6, isHeavy = false)
                }

                // 6. HEAVY RAIN (Dark stormy cloud with intense raindrops)
                WeatherConditionCategory.HEAVY_RAIN -> {
                    draw3DCloud(w, h, offset = Offset(w * 0.58f, h * 0.36f), scale = 0.95f, style = CloudStyle.DARK_STORMY)
                    draw3DCloud(w, h, offset = Offset(w * 0.48f, h * 0.42f + floatY), scale = 1.08f, style = CloudStyle.STORMY_BLUE)
                    draw3DRaindrops(w, h, rainProgression, count = 8, isHeavy = true)
                }

                // 7. THUNDERSTORM (Signature Miami style: Dark cloud + 3D electric lightning + rain)
                WeatherConditionCategory.THUNDERSTORM -> {
                    // Ambient lightning aura behind cloud
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x6638BDF8).copy(alpha = (0.45f * lightningFlash).coerceIn(0.2f, 0.7f)),
                                Color(0x2260A5FA),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.52f, h * 0.52f),
                            radius = w * 0.48f
                        ),
                        radius = w * 0.48f,
                        center = Offset(w * 0.52f, h * 0.52f)
                    )

                    // 3D Electric Lightning Bolt protruding below cloud
                    draw3DLightningBolt(w, h, floatY, lightningFlash)

                    // Foreground 3D Puffy Thundercloud casting shadow
                    draw3DCloud(w, h, offset = Offset(w * 0.48f, h * 0.38f + floatY), scale = 1.10f, style = CloudStyle.STORMY_BLUE)

                    // Sculpted 3D teardrop raindrops
                    draw3DRaindrops(w, h, rainProgression, count = 7, isHeavy = true)
                }

                // 8. SNOW / WINTER (Signature Siberia style: Frost cloud + 3D snowflakes)
                WeatherConditionCategory.SNOW -> {
                    // Frost ambient halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x55E0F2FE),
                                Color(0x1A7DD3FC),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.50f, h * 0.45f),
                            radius = w * 0.45f
                        ),
                        radius = w * 0.45f,
                        center = Offset(w * 0.50f, h * 0.45f)
                    )

                    // 3D Frost Cloud
                    draw3DCloud(w, h, offset = Offset(w * 0.48f, h * 0.40f + floatY), scale = 1.05f, style = CloudStyle.ICY_WHITE)

                    // Sculpted 3D Snowflakes and snow particles
                    draw3DSnowflakes(w, h, rainProgression)
                }

                // 9. FOG / MIST (Ethereal 3D floating mist bands)
                WeatherConditionCategory.FOG -> {
                    draw3DFog(w, h, pulse, floatY)
                }
            }
        }
    }
}

// ----------------- CLOUD STYLES -----------------

private enum class CloudStyle {
    PEARL_WHITE,
    SLATE,
    DARK_SLATE,
    STORMY_BLUE,
    DARK_STORMY,
    ICY_WHITE
}

// ----------------- 3D CLOUD DRAWING -----------------

private fun DrawScope.draw3DCloud(
    w: Float,
    h: Float,
    offset: Offset,
    scale: Float = 1.0f,
    style: CloudStyle = CloudStyle.PEARL_WHITE
) {
    val cloudW = w * 0.68f * scale
    val cloudH = h * 0.40f * scale
    val left = offset.x - cloudW / 2f
    val top = offset.y - cloudH / 2f

    // Color palettes for claymorphic depth
    val (shadowColor, bevelColor, bodyTop, bodyBottom, highlightColor) = when (style) {
        CloudStyle.PEARL_WHITE -> listOf(
            Color(0x55090D1A),
            Color(0xFF94A3B8),
            Color(0xFFFFFFFF),
            Color(0xFFE2E8F0),
            Color(0xFFFFFFFF)
        )
        CloudStyle.SLATE -> listOf(
            Color(0x6605070E),
            Color(0xFF64748B),
            Color(0xFFCBD5E1),
            Color(0xFF94A3B8),
            Color(0xFFE2E8F0)
        )
        CloudStyle.DARK_SLATE -> listOf(
            Color(0x77030509),
            Color(0xFF334155),
            Color(0xFF64748B),
            Color(0xFF475569),
            Color(0xFF94A3B8)
        )
        CloudStyle.STORMY_BLUE -> listOf(
            Color(0x77030611),
            Color(0xFF1E293B),
            Color(0xFFE2E8F0),
            Color(0xFF94A3B8),
            Color(0xFFF1F5F9)
        )
        CloudStyle.DARK_STORMY -> listOf(
            Color(0x8802040A),
            Color(0xFF0F172A),
            Color(0xFF475569),
            Color(0xFF1E293B),
            Color(0xFF64748B)
        )
        CloudStyle.ICY_WHITE -> listOf(
            Color(0x55081E38),
            Color(0xFF93C5FD),
            Color(0xFFFFFFFF),
            Color(0xFFE0F2FE),
            Color(0xFFFFFFFF)
        )
    }

    // 1. Soft ambient drop shadow underneath cloud
    drawCloudSilhouette(
        left = left + 2f,
        top = top + 8f,
        width = cloudW,
        height = cloudH,
        brush = Brush.radialGradient(
            colors = listOf(shadowColor, Color.Transparent),
            center = Offset(offset.x, offset.y + 10f),
            radius = cloudW * 0.65f
        )
    )

    // 2. Beveled perimeter / shadow rim for 3D depth
    drawCloudSilhouette(
        left = left,
        top = top + 3f,
        width = cloudW,
        height = cloudH,
        brush = Brush.verticalGradient(
            colors = listOf(bevelColor.copy(alpha = 0.4f), bevelColor),
            startY = top,
            endY = top + cloudH
        )
    )

    // 3. Main sculpted 3D clay body
    drawCloudSilhouette(
        left = left,
        top = top,
        width = cloudW,
        height = cloudH,
        brush = Brush.verticalGradient(
            colors = listOf(bodyTop, bodyBottom),
            startY = top,
            endY = top + cloudH
        )
    )

    // 4. Specular rim highlight on top lobes
    drawCloudTopRim(
        left = left,
        top = top,
        width = cloudW,
        height = cloudH,
        color = highlightColor.copy(alpha = 0.8f)
    )
}

/**
 * Renders the multi-lobed cloud geometry combining base pill and rounded puffy lobes
 */
private fun DrawScope.drawCloudSilhouette(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    brush: Brush
) {
    val pillHeight = height * 0.52f
    val pillTop = top + height - pillHeight

    // Base horizontal rounded pill
    drawRoundRect(
        brush = brush,
        topLeft = Offset(left, pillTop),
        size = Size(width, pillHeight),
        cornerRadius = CornerRadius(pillHeight / 2f, pillHeight / 2f)
    )

    // Left puff lobe
    val leftR = height * 0.36f
    drawCircle(
        brush = brush,
        radius = leftR,
        center = Offset(left + width * 0.28f, top + height * 0.52f)
    )

    // Center tall hero lobe
    val centerR = height * 0.48f
    drawCircle(
        brush = brush,
        radius = centerR,
        center = Offset(left + width * 0.54f, top + height * 0.46f)
    )

    // Right small puff lobe
    val rightR = height * 0.32f
    drawCircle(
        brush = brush,
        radius = rightR,
        center = Offset(left + width * 0.76f, top + height * 0.56f)
    )
}

private fun DrawScope.drawCloudTopRim(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    color: Color
) {
    // Subtle white arc on the center top dome
    val center = Offset(left + width * 0.54f, top + height * 0.46f)
    val r = height * 0.46f
    drawArc(
        color = color,
        startAngle = 200f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(center.x - r, center.y - r),
        size = Size(r * 2f, r * 2f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )

    // Subtle white arc on left lobe
    val leftCenter = Offset(left + width * 0.28f, top + height * 0.52f)
    val leftR = height * 0.34f
    drawArc(
        color = color.copy(alpha = color.alpha * 0.7f),
        startAngle = 210f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = Offset(leftCenter.x - leftR, leftCenter.y - leftR),
        size = Size(leftR * 2f, leftR * 2f),
        style = Stroke(width = 2f, cap = StrokeCap.Round)
    )
}

// ----------------- 3D SUN DRAWING -----------------

private fun DrawScope.draw3DSun(
    w: Float,
    h: Float,
    pulse: Float,
    rotation: Float,
    floatY: Float,
    scale: Float = 1.0f,
    offset: Offset? = null
) {
    val center = offset ?: Offset(w / 2f, h / 2f + floatY)
    val coreR = (w * 0.26f * scale) * pulse

    // 1. Multi-layered warm corona glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x66FBBF24),
                Color(0x26F59E0B),
                Color.Transparent
            ),
            center = center,
            radius = coreR * 2.5f
        ),
        radius = coreR * 2.5f,
        center = center
    )

    // 2. Sculpted 3D Sunbeams (8 rounded trapezoids with drop shadow)
    val rayCount = 8
    val innerR = coreR * 1.16f
    val outerR = coreR * 1.50f
    for (i in 0 until rayCount) {
        val angleDeg = (i * (360f / rayCount)) + rotation
        val angleRad = (angleDeg * PI / 180.0).toFloat()
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)

        val start = Offset(center.x + innerR * cosA, center.y + innerR * sinA)
        val end = Offset(center.x + outerR * cosA, center.y + outerR * sinA)

        // Drop shadow ray
        drawLine(
            color = Color(0x33000000),
            start = Offset(start.x + 1f, start.y + 2f),
            end = Offset(end.x + 1f, end.y + 2f),
            strokeWidth = 6f * scale,
            cap = StrokeCap.Round
        )

        // Golden beam ray
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B)),
                start = start,
                end = end
            ),
            start = start,
            end = end,
            strokeWidth = 5.5f * scale,
            cap = StrokeCap.Round
        )
    }

    // 3. Drop shadow for the sun sphere
    drawCircle(
        color = Color(0x40000000),
        radius = coreR,
        center = Offset(center.x + 2f, center.y + 5f)
    )

    // 4. 3D Sun Sphere with rich warm bevel gradient
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFBEB), // Highlight top-left
                Color(0xFFFDE047),
                Color(0xFFF59E0B),
                Color(0xFFD97706)  // Shadow bottom-right
            ),
            start = Offset(center.x - coreR, center.y - coreR),
            end = Offset(center.x + coreR, center.y + coreR)
        ),
        radius = coreR,
        center = center
    )

    // 5. Specular highlight for 3D curvature
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.7f), Color.Transparent),
            center = Offset(center.x - coreR * 0.35f, center.y - coreR * 0.35f),
            radius = coreR * 0.40f
        ),
        radius = coreR * 0.40f,
        center = Offset(center.x - coreR * 0.35f, center.y - coreR * 0.35f)
    )
}

// ----------------- 3D CRESCENT MOON (Siberia style) -----------------

private fun DrawScope.draw3DCrescentMoon(
    w: Float,
    h: Float,
    pulse: Float,
    floatY: Float,
    scale: Float = 1.0f,
    offset: Offset? = null
) {
    val center = offset ?: Offset(w * 0.48f, h * 0.46f + floatY)
    val moonR = (w * 0.28f * scale) * pulse
    val innerCutOffset = Offset(center.x + moonR * 0.45f, center.y - moonR * 0.22f)
    val innerR = moonR * 0.88f

    // 1. Soft lunar glow behind moon
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x4493C5FD),
                Color(0x1A60A5FA),
                Color.Transparent
            ),
            center = center,
            radius = moonR * 2.2f
        ),
        radius = moonR * 2.2f,
        center = center
    )

    // 2. Sculpted Crescent Path via PathOperation.Difference
    val outerPath = Path().apply {
        addOval(Rect(center.x - moonR, center.y - moonR, center.x + moonR, center.y + moonR))
    }
    val innerCutPath = Path().apply {
        addOval(Rect(innerCutOffset.x - innerR, innerCutOffset.y - innerR, innerCutOffset.x + innerR, innerCutOffset.y + innerR))
    }
    val crescentPath = Path().apply {
        op(outerPath, innerCutPath, PathOperation.Difference)
    }

    // 3. Drop shadow
    val shadowPath = Path().apply {
        val sCenter = Offset(center.x + 3f, center.y + 6f)
        val sOuter = Path().apply {
            addOval(Rect(sCenter.x - moonR, sCenter.y - moonR, sCenter.x + moonR, sCenter.y + moonR))
        }
        val sInner = Path().apply {
            val sCut = Offset(sCenter.x + moonR * 0.45f, sCenter.y - moonR * 0.22f)
            addOval(Rect(sCut.x - innerR, sCut.y - innerR, sCut.x + innerR, sCut.y + innerR))
        }
        op(sOuter, sInner, PathOperation.Difference)
    }
    drawPath(path = shadowPath, color = Color(0x6602050E))

    // 4. 3D Crescent Moon Body with pearlescent clay gradient
    drawPath(
        path = crescentPath,
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFE2E8F0),
                Color(0xFFCBD5E1),
                Color(0xFF94A3B8)
            ),
            start = Offset(center.x - moonR, center.y - moonR),
            end = Offset(center.x + moonR, center.y + moonR)
        )
    )

    // 5. Craters on the moon surface
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF64748B).copy(alpha = 0.4f), Color(0xFF94A3B8).copy(alpha = 0.1f)),
            center = Offset(center.x - moonR * 0.30f, center.y - moonR * 0.10f),
            radius = moonR * 0.16f
        ),
        radius = moonR * 0.16f,
        center = Offset(center.x - moonR * 0.30f, center.y - moonR * 0.10f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF64748B).copy(alpha = 0.35f), Color(0xFF94A3B8).copy(alpha = 0.1f)),
            center = Offset(center.x - moonR * 0.12f, center.y + moonR * 0.28f),
            radius = moonR * 0.12f
        ),
        radius = moonR * 0.12f,
        center = Offset(center.x - moonR * 0.12f, center.y + moonR * 0.28f)
    )

    // 6. Rim highlight along the outer curve
    drawArc(
        color = Color.White.copy(alpha = 0.7f),
        startAngle = 110f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(center.x - moonR, center.y - moonR),
        size = Size(moonR * 2f, moonR * 2f),
        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
    )

    // 7. Small floating dark companion cloud silhouette (like in Siberia pic)
    val cloudOffset = Offset(center.x + moonR * 0.75f, center.y - moonR * 0.10f)
    draw3DCloud(w, h, offset = cloudOffset, scale = 0.48f, style = CloudStyle.DARK_SLATE)
}

// ----------------- 3D LIGHTNING BOLT (Miami style) -----------------

private fun DrawScope.draw3DLightningBolt(
    w: Float,
    h: Float,
    floatY: Float,
    flash: Float
) {
    val cX = w * 0.50f
    val cY = h * 0.54f + floatY

    // Path for lightning zigzag
    val boltPath = Path().apply {
        moveTo(cX + 6f, cY)
        lineTo(cX - 12f, cY + 28f)
        lineTo(cX - 2f, cY + 28f)
        lineTo(cX - 16f, cY + 58f)
        lineTo(cX + 8f, cY + 34f)
        lineTo(cX - 1f, cY + 34f)
        lineTo(cX + 12f, cY)
        close()
    }

    // 1. Ambient outer cyan glow
    val glowPath = Path().apply {
        moveTo(cX + 9f, cY - 2f)
        lineTo(cX - 15f, cY + 28f)
        lineTo(cX - 4f, cY + 28f)
        lineTo(cX - 19f, cY + 62f)
        lineTo(cX + 11f, cY + 34f)
        lineTo(cX + 1f, cY + 34f)
        lineTo(cX + 15f, cY - 2f)
        close()
    }
    drawPath(
        path = glowPath,
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF38BDF8).copy(alpha = (0.7f * flash).coerceIn(0.3f, 0.95f)),
                Color(0x220284C7),
                Color.Transparent
            ),
            center = Offset(cX, cY + 30f),
            radius = 35f
        )
    )

    // 2. Drop shadow
    val shadowPath = Path().apply {
        val sX = cX + 3f
        val sY = cY + 4f
        moveTo(sX + 6f, sY)
        lineTo(sX - 12f, sY + 28f)
        lineTo(sX - 2f, sY + 28f)
        lineTo(sX - 16f, sY + 58f)
        lineTo(sX + 8f, sY + 34f)
        lineTo(sX - 1f, sY + 34f)
        lineTo(sX + 12f, sY)
        close()
    }
    drawPath(path = shadowPath, color = Color(0x66050B18))

    // 3. 3D Beveled Lightning core
    drawPath(
        path = boltPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFE0F2FE),
                Color(0xFF38BDF8),
                Color(0xFF0284C7)
            ),
            startY = cY,
            endY = cY + 58f
        )
    )

    // 4. White hot core highlight
    val coreLine = Path().apply {
        moveTo(cX + 3f, cY + 4f)
        lineTo(cX - 8f, cY + 26f)
        lineTo(cX - 10f, cY + 50f)
    }
    drawPath(
        path = coreLine,
        color = Color.White.copy(alpha = 0.85f),
        style = Stroke(width = 2f, cap = StrokeCap.Round)
    )
}

// ----------------- 3D RAINDROPS (Teardrops with depth) -----------------

private fun DrawScope.draw3DRaindrops(
    w: Float,
    h: Float,
    progression: Float,
    count: Int = 6,
    isHeavy: Boolean = false
) {
    val startX = w * 0.28f
    val spreadW = w * 0.46f
    val stepX = spreadW / (count - 1)
    val dropH = if (isHeavy) 16f else 13f
    val dropW = if (isHeavy) 5f else 4f

    for (i in 0 until count) {
        val x = startX + i * stepX
        val stagger = (i * 0.28f) % 1f
        val currentP = (progression + stagger) % 1f
        val y = (h * 0.64f) + currentP * (h * 0.30f)
        val alpha = sin(currentP * PI).toFloat().coerceIn(0f, 1f)

        // Drop shadow
        drawTeardrop(
            center = Offset(x + 1.5f, y + 2f),
            width = dropW,
            height = dropH,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0x33050B18), Color.Transparent),
                startY = y,
                endY = y + dropH
            )
        )

        // 3D Teardrop Raindrop
        drawTeardrop(
            center = Offset(x, y),
            width = dropW,
            height = dropH,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFBAE6FD).copy(alpha = alpha),
                    Color(0xFF38BDF8).copy(alpha = alpha),
                    Color(0xFF0284C7).copy(alpha = alpha * 0.8f)
                ),
                startY = y - dropH / 2f,
                endY = y + dropH / 2f
            )
        )
    }
}

private fun DrawScope.drawTeardrop(
    center: Offset,
    width: Float,
    height: Float,
    brush: Brush
) {
    val path = Path().apply {
        // Pointed top
        moveTo(center.x, center.y - height / 2f)
        // Right curve
        cubicTo(
            center.x + width / 2f, center.y,
            center.x + width / 2f, center.y + height / 2f,
            center.x, center.y + height / 2f
        )
        // Left curve
        cubicTo(
            center.x - width / 2f, center.y + height / 2f,
            center.x - width / 2f, center.y,
            center.x, center.y - height / 2f
        )
        close()
    }
    drawPath(path = path, brush = brush)
}

// ----------------- 3D SNOWFLAKES (Siberia style) -----------------

private fun DrawScope.draw3DSnowflakes(
    w: Float,
    h: Float,
    progression: Float
) {
    val count = 6
    val startX = w * 0.26f
    val spreadW = w * 0.48f
    val stepX = spreadW / (count - 1)

    for (i in 0 until count) {
        val x = startX + i * stepX
        val stagger = (i * 0.23f) % 1f
        val currentP = (progression + stagger) % 1f
        val y = (h * 0.62f) + currentP * (h * 0.32f)
        val alpha = sin(currentP * PI).toFloat().coerceIn(0f, 1f)

        if (i % 2 == 0) {
            // Hexagonal 6-branch snowflake
            drawHexSnowflake(Offset(x, y), radius = 6.5f, alpha = alpha)
        } else {
            // Soft glowing 3D snow orb
            drawCircle(
                color = Color(0x33000000),
                radius = 3.5f,
                center = Offset(x + 1f, y + 1.5f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha),
                        Color(0xFFBAE6FD).copy(alpha = alpha * 0.8f)
                    ),
                    center = Offset(x - 1f, y - 1f),
                    radius = 3.5f
                ),
                radius = 3.5f,
                center = Offset(x, y)
            )
        }
    }
}

private fun DrawScope.drawHexSnowflake(center: Offset, radius: Float, alpha: Float) {
    val color = Color.White.copy(alpha = alpha * 0.95f)
    for (i in 0 until 3) {
        val angleDeg = i * 60f
        val angleRad = (angleDeg * PI / 180.0).toFloat()
        val dX = radius * cos(angleRad)
        val dY = radius * sin(angleRad)

        drawLine(
            color = color,
            start = Offset(center.x - dX, center.y - dY),
            end = Offset(center.x + dX, center.y + dY),
            strokeWidth = 1.8f,
            cap = StrokeCap.Round
        )
    }
}

// ----------------- 3D FOG / MIST -----------------

private fun DrawScope.draw3DFog(
    w: Float,
    h: Float,
    pulse: Float,
    floatY: Float
) {
    val bands = listOf(
        Triple(0.38f, 0.70f, floatY * 0.6f),
        Triple(0.48f, 0.85f, -floatY * 0.8f),
        Triple(0.58f, 0.65f, floatY * 0.4f),
        Triple(0.68f, 0.80f, -floatY * 0.5f)
    )

    bands.forEachIndexed { i, (yFrac, wFrac, shiftY) ->
        val y = h * yFrac + shiftY
        val bandW = w * wFrac * (0.96f + 0.04f * pulse)
        val startX = (w - bandW) / 2f
        val bandH = 10f

        // Soft drop shadow
        drawRoundRect(
            color = Color(0x33000000),
            topLeft = Offset(startX, y - bandH / 2f + 2f),
            size = Size(bandW, bandH),
            cornerRadius = CornerRadius(bandH / 2f, bandH / 2f)
        )

        // 3D Soft Mist Pill
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFE2E8F0).copy(alpha = 0.55f),
                    Color(0xFFCBD5E1).copy(alpha = 0.75f),
                    Color(0xFFE2E8F0).copy(alpha = 0.55f),
                    Color.Transparent
                ),
                startX = startX,
                endX = startX + bandW
            ),
            topLeft = Offset(startX, y - bandH / 2f),
            size = Size(bandW, bandH),
            cornerRadius = CornerRadius(bandH / 2f, bandH / 2f)
        )
    }
}

// ----------------- WEATHER WAVE DIVIDER (Signature organic curves) -----------------

/**
 * Organic wave divider inspired by the reference image:
 * Fluid undulating water wave for rain/thunderstorm;
 * Sculpted snowdrift with icicle drips for winter snow.
 */
@Composable
fun WeatherWaveDivider(
    category: WeatherConditionCategory,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (category) {
            WeatherConditionCategory.SNOW -> {
                // Siberia winter snowdrift with frosty rim and drip
                val path = Path().apply {
                    moveTo(0f, h * 0.5f)
                    cubicTo(w * 0.25f, h * 0.35f, w * 0.55f, h * 0.65f, w * 0.75f, h * 0.30f)
                    cubicTo(w * 0.85f, h * 0.15f, w * 0.95f, h * 0.40f, w, h * 0.35f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }

                // Snow crest highlight cap
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFE0F2FE), Color(0x330F172A)),
                        startY = 0f,
                        endY = h
                    )
                )

                // Frosty top stroke
                val strokePath = Path().apply {
                    moveTo(0f, h * 0.5f)
                    cubicTo(w * 0.25f, h * 0.35f, w * 0.55f, h * 0.65f, w * 0.75f, h * 0.30f)
                    cubicTo(w * 0.85f, h * 0.15f, w * 0.95f, h * 0.40f, w, h * 0.35f)
                }
                drawPath(
                    path = strokePath,
                    color = Color.White.copy(alpha = 0.8f),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )

                // Small icicle drips along crest
                val icicleX = w * 0.85f
                val icicleY = h * 0.20f
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(icicleX - 2.5f, icicleY),
                    size = Size(5f, 16f),
                    cornerRadius = CornerRadius(2.5f, 2.5f)
                )
            }
            WeatherConditionCategory.RAIN,
            WeatherConditionCategory.HEAVY_RAIN,
            WeatherConditionCategory.THUNDERSTORM -> {
                // Fluid organic liquid wave like in Miami pic
                val path = Path().apply {
                    moveTo(0f, h * 0.40f)
                    cubicTo(w * 0.20f, h * 0.60f, w * 0.45f, h * 0.25f, w * 0.70f, h * 0.50f)
                    cubicTo(w * 0.85f, h * 0.65f, w * 0.95f, h * 0.35f, w, h * 0.45f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0x3338BDF8), Color(0x1A0F172A)),
                        startY = 0f,
                        endY = h
                    )
                )
                // Fluid rim
                val strokePath = Path().apply {
                    moveTo(0f, h * 0.40f)
                    cubicTo(w * 0.20f, h * 0.60f, w * 0.45f, h * 0.25f, w * 0.70f, h * 0.50f)
                    cubicTo(w * 0.85f, h * 0.65f, w * 0.95f, h * 0.35f, w, h * 0.45f)
                }
                drawPath(
                    path = strokePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0x6638BDF8), Color(0x9993C5FD), Color(0x6638BDF8))
                    ),
                    style = Stroke(width = 2f, cap = StrokeCap.Round)
                )
            }
            else -> {
                // Smooth subtle curve for clear/cloudy days
                val path = Path().apply {
                    moveTo(0f, h * 0.45f)
                    cubicTo(w * 0.35f, h * 0.35f, w * 0.65f, h * 0.55f, w, h * 0.40f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = path,
                    color = Color(0x1AFFFFFF)
                )
            }
        }
    }
}

// ----------------- ATMOSPHERIC PARTICLES BACKGROUND -----------------

/**
 * Animated background particles corresponding to each weather type:
 * - Falling angled rain streaks for Rain & Thunderstorm (Miami style)
 * - Drifting crystalline snowflakes for Snow (Siberia style)
 * - Twinkling stars for Clear Night
 * - Golden sun shimmer for Clear Day
 */
@Composable
fun AtmosphericWeatherParticles(
    category: WeatherConditionCategory,
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles_anim")

    val progression by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_streaks"
    )

    val twinkle by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (category) {
            WeatherConditionCategory.RAIN,
            WeatherConditionCategory.HEAVY_RAIN,
            WeatherConditionCategory.THUNDERSTORM -> {
                // Angled rain streaks (like Miami screen in reference photo)
                val streakCount = if (category == WeatherConditionCategory.HEAVY_RAIN || category == WeatherConditionCategory.THUNDERSTORM) 18 else 12
                for (i in 0 until streakCount) {
                    val posX = (w * (0.05f + (i * 0.055f))) % w
                    val stagger = (i * 0.17f) % 1f
                    val currentProgress = (progression + stagger) % 1f
                    val posY = (currentProgress * h * 1.1f) - 50f
                    val alpha = sin(currentProgress * PI).toFloat().coerceIn(0f, 1f)

                    drawLine(
                        color = Color(0xFF60A5FA).copy(alpha = alpha * 0.28f),
                        start = Offset(posX, posY),
                        end = Offset(posX - 8f, posY + 35f),
                        strokeWidth = 1.6f,
                        cap = StrokeCap.Round
                    )
                }
            }

            WeatherConditionCategory.SNOW -> {
                // Drifting snow particles across the whole background (Siberia style)
                val snowCount = 20
                for (i in 0 until snowCount) {
                    val posX = (w * (0.04f + (i * 0.048f))) % w
                    val stagger = (i * 0.21f) % 1f
                    val currentProgress = (progression + stagger) % 1f
                    val posY = (currentProgress * h * 1.1f) - 40f
                    val sway = sin(currentProgress * PI * 2f).toFloat() * 10f
                    val alpha = sin(currentProgress * PI).toFloat().coerceIn(0f, 1f)
                    val r = if (i % 3 == 0) 2.2f else 1.5f

                    drawCircle(
                        color = Color.White.copy(alpha = alpha * 0.50f),
                        radius = r,
                        center = Offset(posX + sway, posY)
                    )
                }
            }

            WeatherConditionCategory.CLEAR -> {
                if (!isDay) {
                    // Star field twinkling
                    val starPositions = listOf(
                        Offset(w * 0.10f, h * 0.06f),
                        Offset(w * 0.25f, h * 0.12f),
                        Offset(w * 0.42f, h * 0.05f),
                        Offset(w * 0.62f, h * 0.10f),
                        Offset(w * 0.82f, h * 0.04f),
                        Offset(w * 0.92f, h * 0.15f),
                        Offset(w * 0.06f, h * 0.22f),
                        Offset(w * 0.74f, h * 0.20f),
                        Offset(w * 0.50f, h * 0.18f),
                        Offset(w * 0.35f, h * 0.26f),
                        Offset(w * 0.88f, h * 0.28f)
                    )
                    starPositions.forEachIndexed { i, pos ->
                        val r = if (i % 3 == 0) 1.8f else 1.2f
                        val factor = if (i % 2 == 0) twinkle else (1.4f - twinkle).coerceIn(0.3f, 1f)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.7f * factor),
                            radius = r,
                            center = pos
                        )
                    }
                }
            }

            else -> {
                // Subtle ambient specks for overcast / fog
            }
        }
    }
}
