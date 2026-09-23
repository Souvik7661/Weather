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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrentWeatherUi
import com.example.data.model.HourlyForecastUi
import com.example.data.model.TemperatureUnit
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class HumidityTimeSpan(val hours: Int, val label: String) {
    NEXT_12_HOURS(12, "12 Hours"),
    FULL_DAY(24, "24 Hours")
}

/**
 * High-fidelity D3 / Recharts-style interactive line graph visualizing hourly humidity trends
 * throughout the day with monotone cubic spline curves, gradient area fills, dashed Cartesian grid lines,
 * an EPA comfort zone band (30%-60%), and interactive touch scrubbing with floating tooltips.
 */
@Composable
fun HourlyHumidityRechartsCard(
    hourlyList: List<HourlyForecastUi>,
    currentWeather: CurrentWeatherUi,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    if (hourlyList.isEmpty()) return

    var selectedTimeSpan by remember { mutableStateOf(HumidityTimeSpan.FULL_DAY) }
    val displayList = remember(hourlyList, selectedTimeSpan) {
        hourlyList.take(selectedTimeSpan.hours)
    }

    // Touch scrubbing state: selected index along the graph
    var activeIndex by remember { mutableStateOf<Int?>(null) }

    // Pulsing halo animation for active Recharts dot
    val infiniteTransition = rememberInfiniteTransition(label = "recharts_pulse")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )

    val currentPoint = activeIndex?.let { displayList.getOrNull(it) } ?: displayList.firstOrNull()
    val focusedHumidity = currentPoint?.humidityPercent ?: currentWeather.humidityPercent
    val focusedTemp = currentPoint?.temperatureCelsius ?: currentWeather.temperatureCelsius
    val focusedTime = currentPoint?.timeDisplay ?: "Now"

    // Statistics across the displayed hourly window
    val allHumidities = displayList.map { it.humidityPercent }
    val minHumidity = allHumidities.minOrNull() ?: focusedHumidity
    val maxHumidity = allHumidities.maxOrNull() ?: focusedHumidity
    val avgHumidity = if (allHumidities.isNotEmpty()) allHumidities.average().roundToInt() else focusedHumidity

    // Dew point calculation (Magnus-Tetens formula)
    val dewPointCelsius = calculateDewPoint(focusedTemp, focusedHumidity.toDouble())

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("hourly_humidity_chart_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // 1. Recharts Header & Time Span Toggle
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
                            .background(Color(0x330284C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "Humidity trend icon",
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "HOURLY HUMIDITY TRENDS",
                                color = TextSecondaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x3338BDF8))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "D3 / Recharts",
                                    color = AccentCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Text(
                            text = "24-Hour Relative Moisture Progression",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Time Span Selector Pills
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    HumidityTimeSpan.values().forEach { span ->
                        val isSelected = selectedTimeSpan == span
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) Color(0x660284C7) else Color.Transparent)
                                .clickable {
                                    selectedTimeSpan = span
                                    activeIndex = null
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("humidity_timespan_${span.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = span.label,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Big Focus Number & Comfort Assessment Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$focusedHumidity",
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("focused_humidity_value")
                    )
                    Text(
                        text = "%",
                        color = AccentCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 2.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    val (badgeText, badgeColor, badgeBg) = getComfortBadge(focusedHumidity)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg)
                            .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Focus Time Indicator (e.g. "Now" or "Selected: 16:00")
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (activeIndex != null) "Scrubbed Time" else "Current",
                        color = TextSecondaryColor,
                        fontSize = 10.sp
                    )
                    Text(
                        text = focusedTime,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Stats Strip (Min, Avg, Max, Dew Point)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x18FFFFFF))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricStatPill(label = "MIN", value = "$minHumidity%")
                MetricStatPill(label = "AVG", value = "$avgHumidity%")
                MetricStatPill(label = "MAX", value = "$maxHumidity%")
                MetricStatPill(
                    label = "DEW POINT",
                    value = WeatherFormatter.formatTemperature(dewPointCelsius, unit)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Interactive D3 / Recharts Line Graph Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .testTag("recharts_humidity_canvas_container")
            ) {
                D3RechartsHumidityCanvas(
                    data = displayList,
                    activeIndex = activeIndex,
                    haloScale = haloScale,
                    onActiveIndexChange = { activeIndex = it },
                    modifier = Modifier.fillMaxSize()
                )

                // Floating Recharts Tooltip Card
                if (activeIndex != null && currentPoint != null) {
                    RechartsInteractiveTooltip(
                        point = currentPoint,
                        totalPoints = displayList.size,
                        activeIndex = activeIndex!!,
                        unit = unit,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Legend & Gesture Hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Recharts legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentCyan)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Humidity (%)",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(2.dp)
                                .background(Color(0xFF34D399).copy(alpha = 0.8f))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Comfort (30-60%)",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }

                Text(
                    text = "Touch or scrub along graph",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 9.sp
                )
            }
        }
    }
}

// ----------------- D3 / RECHARTS CANVAS ENGINE -----------------

@Composable
private fun D3RechartsHumidityCanvas(
    data: List<HourlyForecastUi>,
    activeIndex: Int?,
    haloScale: Float,
    onActiveIndexChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(data) {
                detectTapGestures(
                    onPress = { offset ->
                        val index = computeNearestIndex(offset.x, size.width.toFloat(), data.size)
                        onActiveIndexChange(index)
                    }
                )
            }
            .pointerInput(data) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val index = computeNearestIndex(offset.x, size.width.toFloat(), data.size)
                        onActiveIndexChange(index)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val index = computeNearestIndex(change.position.x, size.width.toFloat(), data.size)
                        onActiveIndexChange(index)
                    },
                    onDragEnd = {
                        // Keep selected or release
                    },
                    onDragCancel = {
                        onActiveIndexChange(null)
                    }
                )
            }
    ) {
        if (data.size < 2) return@Canvas

        val w = size.width
        val h = size.height
        val bottomMargin = 26f
        val topMargin = 16f
        val rightMargin = 28f
        val leftMargin = 12f

        val chartW = w - leftMargin - rightMargin
        val chartH = h - topMargin - bottomMargin

        // 1. Cartesian Grid & Y-Axis Guideline Labels (0%, 25%, 50%, 75%, 100%)
        val gridLevels = listOf(1.0f, 0.75f, 0.50f, 0.25f, 0.0f)
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

        gridLevels.forEach { frac ->
            val y = topMargin + chartH * (1.0f - frac)

            // Dashed horizontal grid line
            drawLine(
                color = Color(0x1FFFFFFF),
                start = Offset(leftMargin, y),
                end = Offset(w - rightMargin, y),
                strokeWidth = 1f,
                pathEffect = dashEffect
            )

            // Y-axis percentage text label
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(90, 255, 255, 255)
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.LEFT
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                "${(frac * 100).toInt()}%",
                w - rightMargin + 4f,
                y + 3f,
                textPaint
            )
        }

        // 2. EPA Comfort Zone Band (30% to 60%)
        val comfortTopY = topMargin + chartH * (1.0f - 0.60f)
        val comfortBottomY = topMargin + chartH * (1.0f - 0.30f)
        val comfortHeight = comfortBottomY - comfortTopY

        drawRect(
            color = Color(0x0C34D399),
            topLeft = Offset(leftMargin, comfortTopY),
            size = Size(chartW, comfortHeight)
        )
        // Subtle dotted bounds for comfort zone
        val comfortDash = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
        drawLine(
            color = Color(0x3334D399),
            start = Offset(leftMargin, comfortTopY),
            end = Offset(w - rightMargin, comfortTopY),
            strokeWidth = 1.2f,
            pathEffect = comfortDash
        )
        drawLine(
            color = Color(0x3334D399),
            start = Offset(leftMargin, comfortBottomY),
            end = Offset(w - rightMargin, comfortBottomY),
            strokeWidth = 1.2f,
            pathEffect = comfortDash
        )

        // 3. Compute Data Coordinates (X, Y) for each hourly entry
        val stepX = chartW / (data.size - 1)
        val points = data.mapIndexed { idx, item ->
            val x = leftMargin + idx * stepX
            val normY = (item.humidityPercent.toFloat() / 100f).coerceIn(0f, 1f)
            val y = topMargin + chartH * (1f - normY)
            Offset(x, y)
        }

        // 4. Construct D3 Monotone Cubic Spline Path
        val splinePath = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 0 until points.size - 1) {
                val p0 = if (i > 0) points[i - 1] else points[i]
                val p1 = points[i]
                val p2 = points[i + 1]
                val p3 = if (i + 2 < points.size) points[i + 2] else p2

                // Smooth natural tangent control points (Monotone cubic spline emulation)
                val tension = 0.22f
                val cp1X = p1.x + (p2.x - p0.x) * tension
                val cp1Y = (p1.y + (p2.y - p0.y) * tension).coerceIn(topMargin, topMargin + chartH)

                val cp2X = p2.x - (p3.x - p1.x) * tension
                val cp2Y = (p2.y - (p3.y - p1.y) * tension).coerceIn(topMargin, topMargin + chartH)

                cubicTo(cp1X, cp1Y, cp2X, cp2Y, p2.x, p2.y)
            }
        }

        // 5. Fill Recharts Area Gradient under the curve
        val fillPath = Path().apply {
            addPath(splinePath)
            lineTo(points.last().x, topMargin + chartH)
            lineTo(points.first().x, topMargin + chartH)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    AccentCyan.copy(alpha = 0.35f),
                    Color(0xFF60A5FA).copy(alpha = 0.15f),
                    Color.Transparent
                ),
                startY = topMargin,
                endY = topMargin + chartH
            )
        )

        // 6. Draw Gradient Line Stroke
        drawPath(
            path = splinePath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    AccentCyan,
                    Color(0xFF60A5FA),
                    Color(0xFF818CF8)
                ),
                startX = leftMargin,
                endX = w - rightMargin
            ),
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 7. Render Discrete Data Point Nodes
        val stride = if (data.size > 14) 2 else 1
        points.forEachIndexed { idx, pt ->
            if (idx % stride == 0 || idx == data.size - 1 || idx == 0) {
                // Outer ring
                drawCircle(
                    color = Color(0x33000000),
                    radius = 3.5f,
                    center = Offset(pt.x, pt.y + 1f)
                )
                drawCircle(
                    color = Color(0xFF0F172A),
                    radius = 3.5f,
                    center = pt
                )
                drawCircle(
                    color = AccentCyan,
                    radius = 2.5f,
                    center = pt
                )
            }
        }

        // 8. Recharts Active Cursor & Highlight Dot
        if (activeIndex != null && activeIndex in points.indices) {
            val activePt = points[activeIndex]

            // Vertical dashed cursor line
            drawLine(
                color = AccentCyan.copy(alpha = 0.7f),
                start = Offset(activePt.x, topMargin),
                end = Offset(activePt.x, topMargin + chartH),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
            )

            // Outer pulsing halo ring
            drawCircle(
                color = AccentCyan.copy(alpha = 0.25f),
                radius = 12f * haloScale,
                center = activePt
            )

            // Middle bright ring
            drawCircle(
                color = AccentCyan,
                radius = 6.5f,
                center = activePt
            )

            // Inner core dot
            drawCircle(
                color = Color.White,
                radius = 3.5f,
                center = activePt
            )
        }

        // 9. X-Axis Time Labels along bottom
        val labelStep = max(1, data.size / 5)
        for (i in 0 until data.size step labelStep) {
            val pt = points[i]
            val timeText = data[i].timeDisplay
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(140, 255, 255, 255)
                textSize = 9.5.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                timeText,
                pt.x.coerceIn(leftMargin + 10f, w - rightMargin - 10f),
                h - 4f,
                textPaint
            )
        }
    }
}

// ----------------- FLOATING RECHARTS TOOLTIP -----------------

@Composable
private fun RechartsInteractiveTooltip(
    point: HourlyForecastUi,
    totalPoints: Int,
    activeIndex: Int,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    val (badgeText, badgeColor, _) = getComfortBadge(point.humidityPercent)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xE60F172A))
            .border(BorderStroke(1.dp, Color(0x6638BDF8)), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("recharts_active_tooltip")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column {
                Text(
                    text = point.timeDisplay,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = WeatherFormatter.formatTemperature(point.temperatureCelsius, unit),
                    color = TextSecondaryColor,
                    fontSize = 10.sp
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(Color(0x33FFFFFF))
            )

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${point.humidityPercent}%",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = badgeText,
                    color = badgeColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ----------------- STAT PILL -----------------

@Composable
private fun MetricStatPill(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = TextSecondaryColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ----------------- HELPERS -----------------

private fun computeNearestIndex(touchX: Float, width: Float, count: Int): Int {
    if (count <= 1) return 0
    val leftMargin = 12f
    val rightMargin = 28f
    val chartW = (width - leftMargin - rightMargin).coerceAtLeast(1f)
    val clampedX = (touchX - leftMargin).coerceIn(0f, chartW)
    val fraction = clampedX / chartW
    return (fraction * (count - 1)).roundToInt().coerceIn(0, count - 1)
}

/**
 * Evaluates EPA and ASHRAE comfort classification based on relative humidity percentage
 */
private fun getComfortBadge(humidity: Int): Triple<String, Color, Color> {
    return when {
        humidity < 30 -> Triple("Dry Air", Color(0xFFFBBF24), Color(0x33FBBF24))
        humidity in 30..60 -> Triple("Comfortable", Color(0xFF34D399), Color(0x3334D399))
        humidity in 61..79 -> Triple("Humid", Color(0xFF38BDF8), Color(0x3338BDF8))
        else -> Triple("Very Damp", Color(0xFF818CF8), Color(0x33818CF8))
    }
}

/**
 * Calculates dew point temperature using the Magnus-Tetens approximation formula
 */
private fun calculateDewPoint(tempCelsius: Double, relativeHumidity: Double): Double {
    val b = 17.27
    val c = 237.7
    val rhFraction = (relativeHumidity / 100.0).coerceIn(0.01, 1.0)
    val gamma = (b * tempCelsius) / (c + tempCelsius) + ln(rhFraction)
    return (c * gamma) / (b - gamma)
}
