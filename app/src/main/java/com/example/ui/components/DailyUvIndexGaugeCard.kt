package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * WHO / Global Solar UV Index Standard Risk Categories
 */
enum class UvRiskLevel(
    val title: String,
    val rangeDisplay: String,
    val minUv: Double,
    val maxUv: Double,
    val primaryColor: Color,
    val containerColor: Color,
    val burnTimeEstimate: String,
    val warningMessage: String
) {
    LOW(
        title = "Low Exposure",
        rangeDisplay = "0 - 2",
        minUv = 0.0,
        maxUv = 2.9,
        primaryColor = Color(0xFF22C55E), // Emerald Green
        containerColor = Color(0x3322C55E),
        burnTimeEstimate = "60+ minutes",
        warningMessage = "Minimal sun exposure hazard. You can safely stay outside without significant risk of sunburn."
    ),
    MODERATE(
        title = "Moderate Risk",
        rangeDisplay = "3 - 5",
        minUv = 3.0,
        maxUv = 5.9,
        primaryColor = Color(0xFFEAB308), // Vibrant Amber Yellow
        containerColor = Color(0x33EAB308),
        burnTimeEstimate = "30 - 45 min",
        warningMessage = "Moderate risk of harm from unprotected sun exposure. Seek shade during midday hours."
    ),
    HIGH(
        title = "High Risk",
        rangeDisplay = "6 - 7",
        minUv = 6.0,
        maxUv = 7.9,
        primaryColor = Color(0xFFF97316), // Vivid Orange
        containerColor = Color(0x33F97316),
        burnTimeEstimate = "15 - 25 min",
        warningMessage = "High risk of skin damage! Protection essential. Reduce outdoor time between 10 AM and 4 PM."
    ),
    VERY_HIGH(
        title = "Very High Risk",
        rangeDisplay = "8 - 10",
        minUv = 8.0,
        maxUv = 10.9,
        primaryColor = Color(0xFFEF4444), // Crimson Red
        containerColor = Color(0x33EF4444),
        burnTimeEstimate = "10 - 15 min",
        warningMessage = "Very high risk of sunburn. Extra precautions required. Avoid direct sun during solar noon."
    ),
    EXTREME(
        title = "Extreme Risk",
        rangeDisplay = "11+",
        minUv = 11.0,
        maxUv = 16.0,
        primaryColor = Color(0xFFA855F7), // Neon Purple/Violet
        containerColor = Color(0x33A855F7),
        burnTimeEstimate = "< 10 minutes",
        warningMessage = "Extreme danger! Unprotected skin and eyes can burn in minutes. Avoid being outside around midday."
    );

    companion object {
        fun fromUv(uv: Double): UvRiskLevel {
            return when {
                uv < 3.0 -> LOW
                uv < 6.0 -> MODERATE
                uv < 8.0 -> HIGH
                uv < 11.0 -> VERY_HIGH
                else -> EXTREME
            }
        }
    }
}

/**
 * Dedicated visual gauge card for the daily UV index to warn users about sun exposure levels.
 * Features a circular swept radial arc gauge, risk tier indicators, burn time countdown warning,
 * and WHO sun protection checklist.
 */
@Composable
fun DailyUvIndexGaugeCard(
    uvIndex: Double,
    maxDailyUv: Double = uvIndex,
    modifier: Modifier = Modifier
) {
    val riskLevel = remember(uvIndex) { UvRiskLevel.fromUv(uvIndex) }
    var selectedRiskTier by remember { mutableStateOf<UvRiskLevel?>(null) }
    val displayRisk = selectedRiskTier ?: riskLevel

    // Animated progress for arc needle
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(uvIndex) {
        val target = (uvIndex.toFloat() / 12f).coerceIn(0f, 1f)
        animatedProgress.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    // Gentle pulsing halo on the needle indicator dot
    val infiniteTransition = rememberInfiniteTransition(label = "uv_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "uv_halo"
    )

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_uv_gauge_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // 1. Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x33F59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = "Daily UV Index",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "DAILY UV INDEX & SUN RISK",
                                color = TextSecondaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(riskLevel.containerColor)
                                    .border(0.5.dp, riskLevel.primaryColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "WHO Standard",
                                    color = riskLevel.primaryColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "Daily Solar Radiation & Skin Protection",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Peak Max Daily Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Peak: ${String.format(java.util.Locale.US, "%.1f", maxDailyUv)}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Radial Arc Visual Gauge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp),
                contentAlignment = Alignment.Center
            ) {
                UvRadialArcGauge(
                    uvProgress = animatedProgress.value,
                    currentUv = uvIndex,
                    riskLevel = riskLevel,
                    pulseScale = pulseScale,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("uv_arc_gauge_canvas")
                )

                // Digital Center Readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 28.dp)
                ) {
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f", uvIndex),
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp,
                        modifier = Modifier.testTag("uv_numerical_value")
                    )

                    // Colored Exposure Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(riskLevel.containerColor)
                            .border(1.dp, riskLevel.primaryColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                            .testTag("uv_risk_badge")
                    ) {
                        Text(
                            text = riskLevel.title.uppercase(),
                            color = riskLevel.primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = riskLevel.primaryColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Skin burn time ~${riskLevel.burnTimeEstimate}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.testTag("uv_burn_time_text")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Multi-Stop Colored Risk Spectrum Indicator Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXPOSURE SCALE",
                        color = TextSecondaryColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Peak: 10 AM - 4 PM",
                        color = Color(0xFFFBBF24),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 5-segment colored spectrum bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    UvRiskLevel.values().forEach { level ->
                        val isActive = (riskLevel == level)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(level.primaryColor.copy(alpha = if (isActive) 1.0f else 0.45f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Segment Labels (Low, Mod, High, Very High, Ext)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    UvRiskLevel.values().forEach { level ->
                        val isSelected = (displayRisk == level)
                        Text(
                            text = level.name.take(3),
                            color = if (isSelected) level.primaryColor else Color.White.copy(alpha = 0.5f),
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.clickable {
                                selectedRiskTier = if (selectedRiskTier == level) null else level
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Sun Exposure Warning & Advice Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(displayRisk.containerColor)
                    .border(1.dp, displayRisk.primaryColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
                    .testTag("uv_warning_advisory_box")
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(displayRisk.primaryColor.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Protection recommendation",
                            tint = displayRisk.primaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "${displayRisk.title} (${displayRisk.rangeDisplay} UV)",
                            color = displayRisk.primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = displayRisk.warningMessage,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Actionable Sun Protection Checklist (Sunscreen, Sunglasses, Hat, Shade)
            Text(
                text = "RECOMMENDED PROTECTION",
                color = TextSecondaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("uv_protection_checklist"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProtectionGearPill(
                    icon = "🧴",
                    title = if (uvIndex >= 8.0) "SPF 50+" else "SPF 30+",
                    active = uvIndex >= 3.0,
                    accentColor = riskLevel.primaryColor,
                    modifier = Modifier.weight(1f)
                )
                ProtectionGearPill(
                    icon = "🕶️",
                    title = "UV Eyewear",
                    active = uvIndex >= 3.0,
                    accentColor = riskLevel.primaryColor,
                    modifier = Modifier.weight(1f)
                )
                ProtectionGearPill(
                    icon = "🧢",
                    title = "Sun Hat",
                    active = uvIndex >= 6.0,
                    accentColor = riskLevel.primaryColor,
                    modifier = Modifier.weight(1f)
                )
                ProtectionGearPill(
                    icon = "⛱️",
                    title = "Seek Shade",
                    active = uvIndex >= 6.0,
                    accentColor = riskLevel.primaryColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ----------------- RADIAL ARC GAUGE CANVAS -----------------

@Composable
private fun UvRadialArcGauge(
    uvProgress: Float,
    currentUv: Double,
    riskLevel: UvRiskLevel,
    pulseScale: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f
        val centerY = h * 0.82f
        val radius = (minOf(w, h * 1.8f) / 2f) - 24f

        val startAngle = 180f
        val sweepAngle = 180f
        val strokeWidth = 14.dp.toPx()

        val arcTopLeft = Offset(centerX - radius, centerY - radius)
        val arcSize = Size(radius * 2f, radius * 2f)

        // 1. Background Arc Track
        drawArc(
            color = Color(0x1FFFFFFF),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 2. Multi-Color UV Gradient Arc Progress
        val progressSweep = (sweepAngle * uvProgress).coerceIn(0.01f, sweepAngle)
        val gradientBrush = Brush.sweepGradient(
            colors = listOf(
                Color(0xFF22C55E), // 0-2 Green
                Color(0xFFEAB308), // 3-5 Yellow
                Color(0xFFF97316), // 6-7 Orange
                Color(0xFFEF4444), // 8-10 Red
                Color(0xFFA855F7), // 11+ Violet
                Color(0xFFA855F7)
            ),
            center = Offset(centerX, centerY)
        )

        drawArc(
            brush = gradientBrush,
            startAngle = startAngle,
            sweepAngle = progressSweep,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 3. Tick Marks and Labels along Arc Edge (0, 3, 6, 8, 11+)
        val tickLevels = listOf(
            0.0f to "0",
            (3f / 12f) to "3",
            (6f / 12f) to "6",
            (8f / 12f) to "8",
            (11f / 12f) to "11+"
        )

        val tickPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(130, 255, 255, 255)
            textSize = 9.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        tickLevels.forEach { (fraction, label) ->
            val angleRad = (startAngle + sweepAngle * fraction) * (PI / 180f)
            val innerTickR = radius - strokeWidth / 2f - 4f
            val outerTickR = radius + strokeWidth / 2f + 4f
            val labelR = radius + strokeWidth / 2f + 16f

            // Tick line
            val tStartX = centerX + innerTickR * cos(angleRad).toFloat()
            val tStartY = centerY + innerTickR * sin(angleRad).toFloat()
            val tEndX = centerX + outerTickR * cos(angleRad).toFloat()
            val tEndY = centerY + outerTickR * sin(angleRad).toFloat()

            drawLine(
                color = Color(0x44FFFFFF),
                start = Offset(tStartX, tStartY),
                end = Offset(tEndX, tEndY),
                strokeWidth = 1.5f
            )

            // Text Label
            val lblX = centerX + labelR * cos(angleRad).toFloat()
            val lblY = centerY + labelR * sin(angleRad).toFloat() + 3f
            drawContext.canvas.nativeCanvas.drawText(label, lblX, lblY, tickPaint)
        }

        // 4. Indicator Needle / Pointer Dot at current progress
        val pointerAngleRad = (startAngle + progressSweep) * (PI / 180f)
        val pointerX = centerX + radius * cos(pointerAngleRad).toFloat()
        val pointerY = centerY + radius * sin(pointerAngleRad).toFloat()

        // Outer pulsing glow
        drawCircle(
            color = riskLevel.primaryColor.copy(alpha = 0.3f),
            radius = (strokeWidth * 0.9f) * pulseScale,
            center = Offset(pointerX, pointerY)
        )

        // Needle core dot
        drawCircle(
            color = Color.White,
            radius = strokeWidth * 0.48f,
            center = Offset(pointerX, pointerY)
        )
        drawCircle(
            color = riskLevel.primaryColor,
            radius = strokeWidth * 0.32f,
            center = Offset(pointerX, pointerY)
        )
    }
}

// ----------------- PROTECTION GEAR PILL -----------------

@Composable
private fun ProtectionGearPill(
    icon: String,
    title: String,
    active: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) Color(0x331E293B) else Color(0x14FFFFFF))
            .border(
                1.dp,
                if (active) accentColor.copy(alpha = 0.5f) else Color(0x1AFFFFFF),
                RoundedCornerShape(10.dp)
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title,
                color = if (active) Color.White else Color.White.copy(alpha = 0.45f),
                fontSize = 10.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
