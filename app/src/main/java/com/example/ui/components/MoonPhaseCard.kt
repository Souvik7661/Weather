package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Astronomical model for lunar phase information.
 */
data class MoonPhaseInfo(
    val name: String,
    val emoji: String,
    val phaseFraction: Double,       // 0.0 .. 1.0 (0=New, 0.25=1st Q, 0.5=Full, 0.75=3rd Q)
    val illuminationPercent: Int,    // 0 .. 100
    val ageDays: Double,             // 0.0 .. 29.53
    val nextPhaseEvent: String,
    val stargazingCondition: String,
    val description: String
)

/**
 * High-precision astronomical calculator for the synodic lunar cycle.
 */
object MoonPhaseCalculator {
    private const val LUNAR_CYCLE_MS = 29.530588853 * 24 * 60 * 60 * 1000.0
    // Reference New Moon: Jan 11, 2024, 11:57:00 UTC
    private const val REFERENCE_NEW_MOON_MS = 1704974220000L

    fun calculate(timeMillis: Long = System.currentTimeMillis()): MoonPhaseInfo {
        var diff = (timeMillis - REFERENCE_NEW_MOON_MS) % LUNAR_CYCLE_MS
        if (diff < 0) diff += LUNAR_CYCLE_MS
        val phaseFraction = diff / LUNAR_CYCLE_MS
        val ageDays = phaseFraction * 29.530588853

        val illumination = ((1.0 - cos(2.0 * Math.PI * phaseFraction)) / 2.0 * 100.0).roundToInt()

        val (name, emoji, event, stargaze, desc) = when {
            phaseFraction < 0.03 || phaseFraction >= 0.97 -> Tuple5(
                "New Moon",
                "🌑",
                "Waxing Crescent in ~2 days",
                "Optimal (Dark Skies)",
                "The Moon sits directly between Earth and the Sun; its illuminated side faces away from Earth. Night skies are pitch dark, creating perfect conditions for deep-sky stargazing and meteor watching."
            )
            phaseFraction < 0.22 -> Tuple5(
                "Waxing Crescent",
                "🌒",
                "First Quarter in ${(7.38 - ageDays).coerceAtLeast(1.0).roundToInt()} days",
                "Excellent Stargazing",
                "A delicate silver crescent curves through the western sky just after twilight, casting minimal light pollution and setting early in the evening."
            )
            phaseFraction < 0.28 -> Tuple5(
                "First Quarter",
                "🌓",
                "Waxing Gibbous in ~2 days",
                "Good (After Midnight)",
                "Exactly half of the lunar disc shines brightly. The Moon rises near noon and sets around midnight, revealing dramatic shadows along the lunar terminator craters."
            )
            phaseFraction < 0.47 -> Tuple5(
                "Waxing Gibbous",
                "🌔",
                "Full Moon in ${(14.76 - ageDays).coerceAtLeast(1.0).roundToInt()} days",
                "Moderate Moonwash",
                "More than half the lunar surface is illuminated, casting a silvery glow across the landscape throughout the evening."
            )
            phaseFraction < 0.53 -> Tuple5(
                "Full Moon",
                "🌕",
                "Waning Gibbous in ~2 days",
                "Bright Lunar Wash",
                "The Moon is 100% illuminated by direct sunlight, shining at peak nocturnal brilliance. It rises at sunset and stays visible all night long, ideal for night hiking."
            )
            phaseFraction < 0.72 -> Tuple5(
                "Waning Gibbous",
                "🌖",
                "Third Quarter in ${(22.14 - ageDays).coerceAtLeast(1.0).roundToInt()} days",
                "Good (Before Midnight)",
                "Illumination gradually wanes. The Moon rises later in the evening, leaving early nighttime skies clear and dark for viewing constellations."
            )
            phaseFraction < 0.78 -> Tuple5(
                "Third Quarter",
                "🌗",
                "Waning Crescent in ~2 days",
                "Very Good Early Night",
                "Half of the disc is illuminated on the opposite side. It rises around midnight and stays visible high into the morning blue sky."
            )
            else -> Tuple5(
                "Waning Crescent",
                "🌘",
                "New Moon in ${(29.53 - ageDays).coerceAtLeast(1.0).roundToInt()} days",
                "Optimal (Dark Skies)",
                "A faint, ethereal sliver of silver light heralds dawn in the eastern sky, disappearing into the morning sunrise."
            )
        }

        return MoonPhaseInfo(
            name = name,
            emoji = emoji,
            phaseFraction = phaseFraction,
            illuminationPercent = illumination,
            ageDays = (ageDays * 10.0).roundToInt() / 10.0,
            nextPhaseEvent = event,
            stargazingCondition = stargaze,
            description = desc
        )
    }

    private data class Tuple5(
        val name: String,
        val emoji: String,
        val event: String,
        val stargaze: String,
        val desc: String
    )
}

/**
 * Standard 8 lunar phases for the interactive progression timeline.
 */
data class StandardPhaseItem(
    val name: String,
    val emoji: String,
    val targetFraction: Double,
    val illumination: Int
)

val STANDARD_LUNAR_PHASES = listOf(
    StandardPhaseItem("New Moon", "🌑", 0.0, 0),
    StandardPhaseItem("Waxing Crescent", "🌒", 0.125, 25),
    StandardPhaseItem("First Quarter", "🌓", 0.25, 50),
    StandardPhaseItem("Waxing Gibbous", "🌔", 0.375, 75),
    StandardPhaseItem("Full Moon", "🌕", 0.5, 100),
    StandardPhaseItem("Waning Gibbous", "🌖", 0.625, 75),
    StandardPhaseItem("Third Quarter", "🌗", 0.75, 50),
    StandardPhaseItem("Waning Crescent", "🌘", 0.875, 25)
)

/**
 * High-fidelity Moon Phase Icon rendered via Jetpack Compose Canvas.
 * Accurately shades the 3D lunar sphere, craters, and terminator boundary
 * based on the phase fraction.
 */
@Composable
fun MoonPhaseIcon(
    phaseFraction: Double,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    Canvas(
        modifier = modifier
            .size(size)
            .testTag("moon_phase_canvas_icon")
    ) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2f, h / 2f)
        val radius = (minOf(w, h) / 2f) - 2f

        // 1. Celestial Outer Ambient Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x33CBD5E1), Color(0x00CBD5E1)),
                center = center,
                radius = radius * 1.35f
            ),
            radius = radius * 1.35f,
            center = center
        )

        // 2. Base Lunar Disc (Dark Side of Moon with deep charcoal starlight tone)
        drawCircle(
            color = Color(0xFF181E29),
            radius = radius,
            center = center
        )

        // Subtle dark craters on the unlit surface
        drawCircle(color = Color(0xFF141923), radius = radius * 0.18f, center = Offset(center.x - radius * 0.28f, center.y - radius * 0.22f))
        drawCircle(color = Color(0xFF141923), radius = radius * 0.24f, center = Offset(center.x + radius * 0.22f, center.y + radius * 0.18f))
        drawCircle(color = Color(0xFF141923), radius = radius * 0.15f, center = Offset(center.x - radius * 0.10f, center.y + radius * 0.38f))

        // 3. Illuminated Region
        val normalizedPhase = (phaseFraction % 1.0 + 1.0) % 1.0
        val isWaxing = normalizedPhase < 0.5
        // Illumination ratio 0.0 .. 1.0
        val illumRatio = ((1.0 - cos(2.0 * Math.PI * normalizedPhase)) / 2.0).toFloat()

        if (illumRatio > 0.01f) {
            val discClipPath = Path().apply {
                addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
            }

            clipPath(discClipPath) {
                val illumBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF1F5F9),
                        Color(0xFFE2E8F0)
                    ),
                    start = Offset(center.x - radius, center.y),
                    end = Offset(center.x + radius, center.y)
                )

                if (illumRatio >= 0.98f) {
                    // Full illumination
                    drawCircle(brush = illumBrush, radius = radius, center = center)
                } else {
                    // Draw illuminated hemisphere and terminator
                    val illumPath = Path()
                    val terminatorScale = (cos(2.0 * Math.PI * normalizedPhase)).toFloat()

                    if (isWaxing) {
                        // Waxing: light on right side
                        illumPath.moveTo(center.x, center.y - radius)
                        // Arc around right edge
                        illumPath.arcTo(
                            rect = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                            startAngleDegrees = -90f,
                            sweepAngleDegrees = 180f,
                            forceMoveTo = false
                        )
                        // Terminator curve back to top
                        val terminatorWidth = radius * terminatorScale
                        illumPath.cubicTo(
                            center.x + terminatorWidth, center.y + radius * 0.55f,
                            center.x + terminatorWidth, center.y - radius * 0.55f,
                            center.x, center.y - radius
                        )
                    } else {
                        // Waning: light on left side
                        illumPath.moveTo(center.x, center.y - radius)
                        // Arc around left edge
                        illumPath.arcTo(
                            rect = Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
                            startAngleDegrees = -90f,
                            sweepAngleDegrees = -180f,
                            forceMoveTo = false
                        )
                        // Terminator curve back to top
                        val terminatorWidth = radius * terminatorScale
                        illumPath.cubicTo(
                            center.x - terminatorWidth, center.y + radius * 0.55f,
                            center.x - terminatorWidth, center.y - radius * 0.55f,
                            center.x, center.y - radius
                        )
                    }
                    illumPath.close()

                    drawPath(path = illumPath, brush = illumBrush)
                }

                // Craters on illuminated face (Mare Tranquillitatis, Tycho, Copernicus)
                val craterColor = Color(0x22475569)
                drawCircle(color = craterColor, radius = radius * 0.18f, center = Offset(center.x - radius * 0.28f, center.y - radius * 0.22f))
                drawCircle(color = craterColor, radius = radius * 0.24f, center = Offset(center.x + radius * 0.22f, center.y + radius * 0.18f))
                drawCircle(color = craterColor, radius = radius * 0.14f, center = Offset(center.x - radius * 0.10f, center.y + radius * 0.38f))
                drawCircle(color = craterColor, radius = radius * 0.12f, center = Offset(center.x + radius * 0.12f, center.y - radius * 0.35f))
                // Tycho ray bright spot
                drawCircle(color = Color(0x66FFFFFF), radius = radius * 0.08f, center = Offset(center.x - radius * 0.05f, center.y + radius * 0.50f))
            }
        }

        // 4. Fine Moon Limb Rim Stroke
        drawCircle(
            color = Color(0x33FFFFFF),
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
    }
}

/**
 * Astronomical Moon Phase Card for Night-Time Views.
 * Displays the current moon phase icon, name, illumination %, lunar cycle age,
 * next phase countdown, and interactive 8-phase progression timeline.
 */
@Composable
fun MoonPhaseCard(
    modifier: Modifier = Modifier,
    currentTimeMillis: Long = System.currentTimeMillis()
) {
    val currentPhaseInfo = remember(currentTimeMillis) {
        MoonPhaseCalculator.calculate(currentTimeMillis)
    }

    var selectedPhaseOverride by remember { mutableStateOf<StandardPhaseItem?>(null) }
    val displayPhaseFraction = selectedPhaseOverride?.targetFraction ?: currentPhaseInfo.phaseFraction
    val displayPhaseName = selectedPhaseOverride?.name ?: currentPhaseInfo.name
    val displayIllumination = selectedPhaseOverride?.illumination ?: currentPhaseInfo.illuminationPercent
    val displayEmoji = selectedPhaseOverride?.emoji ?: currentPhaseInfo.emoji

    // Ambient twinkling stars animation in background
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val starGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_twinkle"
    )

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("moon_phase_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // 1. Header with Celestial Pill
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
                            .background(Color(0x3360A5FA)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Celestial Moon Phase",
                            tint = Color(0xFF93C5FD),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LUNAR ASTRONOMY",
                                color = TextSecondaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x2E60A5FA))
                                    .border(0.5.dp, Color(0x6693C5FD), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "Night Sky Detail",
                                    color = Color(0xFF93C5FD),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "Synodic Lunar Cycle • 29.5 Days",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Reset button if overridden
                if (selectedPhaseOverride != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x26FFFFFF))
                            .clickable { selectedPhaseOverride = null }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Live Now",
                            color = AccentCyan,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22FFFFFF))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Day ${currentPhaseInfo.ageDays}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Hero Moon Illustration + Celestial Readout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x220F172A))
                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large 3D Moon Canvas Icon with Star Field
                Box(
                    modifier = Modifier
                        .size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Ambient star particles around moon
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = Color.White.copy(alpha = starGlow * 0.7f), radius = 1.5f, center = Offset(10f, 15f))
                        drawCircle(color = Color.White.copy(alpha = (1.2f - starGlow) * 0.8f), radius = 1.8f, center = Offset(70f, 20f))
                        drawCircle(color = Color.White.copy(alpha = starGlow * 0.6f), radius = 1.2f, center = Offset(15f, 65f))
                        drawCircle(color = Color.White.copy(alpha = (1.1f - starGlow) * 0.7f), radius = 1.6f, center = Offset(68f, 62f))
                    }

                    MoonPhaseIcon(
                        phaseFraction = displayPhaseFraction,
                        size = 72.dp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Detailed Readout
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = displayPhaseName,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("moon_phase_name")
                        )
                        Text(
                            text = displayEmoji,
                            fontSize = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x3360A5FA))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$displayIllumination% Illumination",
                                color = Color(0xFF93C5FD),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = currentPhaseInfo.stargazingCondition,
                            color = TextSecondaryColor,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = currentPhaseInfo.nextPhaseEvent,
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Astronomical Metrics Bento Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AstronomicalStatPill(
                    label = "CYCLE PROGRESS",
                    value = "${(currentPhaseInfo.phaseFraction * 100.0).roundToInt()}%",
                    modifier = Modifier.weight(1f)
                )
                AstronomicalStatPill(
                    label = "MOON AGE",
                    value = "${currentPhaseInfo.ageDays}d",
                    modifier = Modifier.weight(1f)
                )
                AstronomicalStatPill(
                    label = "STARGAZING",
                    value = if (currentPhaseInfo.illuminationPercent < 40) "Optimal" else if (currentPhaseInfo.illuminationPercent < 75) "Moderate" else "Washout",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Interactive 8-Phase Lunar Journey Timeline
            Text(
                text = "LUNAR CYCLE TIMELINE (TAP TO EXPLORE)",
                color = TextSecondaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            val timelineScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(timelineScrollState)
                    .testTag("moon_phases_timeline"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                STANDARD_LUNAR_PHASES.forEach { phase ->
                    val isCurrent = (currentPhaseInfo.name == phase.name)
                    val isSelected = (selectedPhaseOverride?.name == phase.name)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) Color(0x4D60A5FA)
                                else if (isCurrent) Color(0x2660A5FA)
                                else Color(0x14FFFFFF)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else if (isCurrent) 1.dp else 0.5.dp,
                                color = if (isSelected) Color(0xFF60A5FA)
                                else if (isCurrent) Color(0x6693C5FD)
                                else Color(0x1AFFFFFF),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                selectedPhaseOverride = if (selectedPhaseOverride?.name == phase.name) null else phase
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            MoonPhaseIcon(
                                phaseFraction = phase.targetFraction,
                                size = 28.dp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = phase.name,
                                color = if (isSelected || isCurrent) Color.White else Color.White.copy(alpha = 0.65f),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected || isCurrent) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = "${phase.illumination}%",
                                color = if (isSelected || isCurrent) AccentCyan else TextSecondaryColor,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Educational Astronomy Insight Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x1A1E293B))
                    .border(0.5.dp, Color(0x26FFFFFF), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Astronomical insight",
                        tint = Color(0xFF93C5FD),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = currentPhaseInfo.description,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AstronomicalStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x1FFFFFFF))
            .border(0.5.dp, Color(0x14FFFFFF), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = TextSecondaryColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
