package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AirQualityUi
import com.example.data.model.CurrentWeatherUi
import com.example.data.model.DailyForecastUi
import com.example.data.model.HourlyForecastUi
import com.example.data.model.TemperatureUnit
import com.example.data.model.WeatherLocation
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val GlassCardBackground = Color(0x331E293B)
val GlassCardBorder = Color(0x33FFFFFF)
val TextSecondaryColor = Color(0xB3FFFFFF)
val AccentCyan = Color(0xFF38BDF8)
val AccentAmber = Color(0xFFFBBF24)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    backgroundColor: Color = GlassCardBackground,
    borderColor: Color = GlassCardBorder,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(shape)
            .border(BorderStroke(1.dp, borderColor), shape),
        color = backgroundColor,
        shape = shape
    ) {
        content()
    }
}

// ----------------- HERO CARD -----------------

@Composable
fun WeatherHeroCard(
    location: WeatherLocation,
    currentWeather: CurrentWeatherUi,
    unit: TemperatureUnit,
    onLocationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Location Selector Badge
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0x33FFFFFF))
                .border(BorderStroke(1.dp, Color(0x4DFFFFFF)), RoundedCornerShape(30.dp))
                .clickable { onLocationClick() }
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("location_badge_button"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = if (location.isGpsLocation) "Current GPS location" else "Selected city",
                tint = if (location.isGpsLocation) AccentCyan else Color.White,
                modifier = Modifier
                    .size(14.dp)
                    .rotate(if (location.isGpsLocation) 0f else 45f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = location.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (location.isGpsLocation) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(AccentCyan, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Center Dynamic Weather Art Hero using Coil
        DynamicWeatherIllustration(
            condition = currentWeather.condition,
            size = 150.dp,
            modifier = Modifier.testTag("weather_hero_art")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Large Temperature Display
        Text(
            text = WeatherFormatter.formatTemperature(currentWeather.temperatureCelsius, unit),
            color = Color.White,
            fontSize = 72.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-2).sp,
            modifier = Modifier.testTag("hero_temperature")
        )

        // Condition Description
        Text(
            text = currentWeather.condition.description,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.testTag("hero_condition_text")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // High / Low, Feels Like, and AQI Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "H: ${WeatherFormatter.formatTemperature(currentWeather.maxTempCelsius, unit)}  •  L: ${WeatherFormatter.formatTemperature(currentWeather.minTempCelsius, unit)}",
                color = TextSecondaryColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x26FFFFFF))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Feels ${WeatherFormatter.formatTemperature(currentWeather.feelsLikeCelsius, unit)}",
                    color = AccentCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (currentWeather.airQuality != null) {
                Spacer(modifier = Modifier.width(8.dp))
                val aqiColor = when (currentWeather.airQuality.category) {
                    "Good" -> Color(0xFF4ADE80)
                    "Moderate" -> AccentAmber
                    "Unhealthy for Sensitive" -> Color(0xFFFB923C)
                    "Unhealthy" -> Color(0xFFF87171)
                    else -> Color(0xFFA855F7)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(aqiColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .testTag("hero_aqi_badge")
                ) {
                    Text(
                        text = "AQI ${currentWeather.airQuality.aqi}",
                        color = aqiColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Night-Time Celestial Moon Phase Badge
            if (!currentWeather.isDay) {
                Spacer(modifier = Modifier.width(8.dp))
                val moonInfo = remember { MoonPhaseCalculator.calculate() }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x3360A5FA))
                        .border(0.5.dp, Color(0x6693C5FD), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .testTag("hero_moon_phase_badge")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MoonPhaseIcon(phaseFraction = moonInfo.phaseFraction, size = 13.dp)
                        Text(
                            text = "${moonInfo.name} • ${moonInfo.illuminationPercent}%",
                            color = Color(0xFF93C5FD),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ----------------- AIR QUALITY CARD -----------------

@Composable
fun AirQualityCard(
    airQuality: AirQualityUi,
    modifier: Modifier = Modifier
) {
    val (statusColor, badgeBg) = when (airQuality.category) {
        "Good" -> Color(0xFF4ADE80) to Color(0x334ADE80)
        "Moderate" -> AccentAmber to Color(0x33FBBF24)
        "Unhealthy for Sensitive" -> Color(0xFFFB923C) to Color(0x33FB923C)
        "Unhealthy" -> Color(0xFFF87171) to Color(0x33F87171)
        "Very Unhealthy" -> Color(0xFFA855F7) to Color(0x33A855F7)
        else -> Color(0xFFEF4444) to Color(0x33EF4444)
    }

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("air_quality_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AIR QUALITY INDEX",
                        color = TextSecondaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = airQuality.category,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${airQuality.aqi}",
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "US AQI",
                        color = TextSecondaryColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "PM2.5",
                            color = TextSecondaryColor,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${airQuality.pm25} µg/m³",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "PM10",
                            color = TextSecondaryColor,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${airQuality.pm10} µg/m³",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-segment color gradient gauge
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            ) {
                val w = size.width
                val h = size.height

                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF4ADE80), // Green (0-50)
                            Color(0xFFFBBF24), // Yellow (51-100)
                            Color(0xFFFB923C), // Orange (101-150)
                            Color(0xFFF87171), // Red (151-200)
                            Color(0xFFA855F7), // Purple (201-300)
                            Color(0xFF7F1D1D)  // Maroon (300+)
                        )
                    ),
                    size = size,
                    cornerRadius = CornerRadius(h / 2, h / 2)
                )

                // Marker dot for current AQI position
                val pinX = (w * airQuality.progressFraction).coerceIn(4f, w - 4f)
                drawCircle(
                    color = Color.White,
                    radius = 5f,
                    center = Offset(pinX, h / 2)
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.4f),
                    radius = 2.5f,
                    center = Offset(pinX, h / 2)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = airQuality.advice,
                color = TextSecondaryColor,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

// ----------------- HOURLY FORECAST -----------------

@Composable
fun HourlyForecastSection(
    hourlyList: List<HourlyForecastUi>,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("hourly_forecast_card")
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = TextSecondaryColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HOURLY FORECAST",
                    color = TextSecondaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            ) {
                items(hourlyList) { hour ->
                    HourlyItemPill(hour = hour, unit = unit)
                }
            }
        }
    }
}

@Composable
private fun HourlyItemPill(
    hour: HourlyForecastUi,
    unit: TemperatureUnit
) {
    val pillBg = if (hour.isCurrentHour) Color(0x4038BDF8) else Color(0x1AFFFFFF)
    val pillBorder = if (hour.isCurrentHour) AccentCyan else Color(0x26FFFFFF)

    Column(
        modifier = Modifier
            .width(68.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(pillBg)
            .border(BorderStroke(1.dp, pillBorder), RoundedCornerShape(20.dp))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = hour.timeDisplay,
            color = if (hour.isCurrentHour) Color.White else TextSecondaryColor,
            fontSize = 13.sp,
            fontWeight = if (hour.isCurrentHour) FontWeight.Bold else FontWeight.Normal
        )

        DynamicWeatherIcon(
            condition = hour.condition,
            size = 32.dp
        )

        Text(
            text = WeatherFormatter.formatTemperature(hour.temperatureCelsius, unit),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        if (hour.precipitationProbabilityPercent > 10) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(10.dp)
                )
                Text(
                    text = "${hour.precipitationProbabilityPercent}%",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

// ----------------- 7-DAY FORECAST -----------------

@Composable
fun DailyForecastSection(
    dailyList: List<DailyForecastUi>,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("daily_forecast_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = TextSecondaryColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "7-DAY FORECAST",
                    color = TextSecondaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Compute the absolute min and max of the week for range bar normalization
            val globalMin = dailyList.minOfOrNull { it.minTempCelsius } ?: 0.0
            val globalMax = dailyList.maxOfOrNull { it.maxTempCelsius } ?: 30.0
            val span = (globalMax - globalMin).coerceAtLeast(1.0)

            dailyList.forEachIndexed { index, day ->
                DailyForecastRow(
                    day = day,
                    unit = unit,
                    weekMin = globalMin,
                    weekSpan = span
                )
                if (index < dailyList.lastIndex) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0x14FFFFFF))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun DailyForecastRow(
    day: DailyForecastUi,
    unit: TemperatureUnit,
    weekMin: Double,
    weekSpan: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day Name & Date
        Column(modifier = Modifier.width(76.dp)) {
            Text(
                text = day.dayName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = day.dateDisplay,
                color = TextSecondaryColor,
                fontSize = 11.sp
            )
        }

        // Weather Icon + Rain %
        Row(
            modifier = Modifier.width(66.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DynamicWeatherIcon(
                condition = day.condition,
                size = 28.dp
            )
            if (day.precipitationProbabilityPercent > 15) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${day.precipitationProbabilityPercent}%",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Min Temp
        Text(
            text = WeatherFormatter.formatTemperature(day.minTempCelsius, unit),
            color = TextSecondaryColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(36.dp)
        )

        // Temperature range gradient indicator bar
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .padding(horizontal = 6.dp)
        ) {
            val barW = size.width
            val barH = size.height

            // Background track
            drawRoundRect(
                color = Color(0x33FFFFFF),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barH / 2, barH / 2)
            )

            // Day's range ratio
            val startRatio = ((day.minTempCelsius - weekMin) / weekSpan).toFloat().coerceIn(0f, 0.9f)
            val endRatio = ((day.maxTempCelsius - weekMin) / weekSpan).toFloat().coerceIn(startRatio + 0.1f, 1f)

            val dayStartX = barW * startRatio
            val dayWidth = (barW * (endRatio - startRatio)).coerceAtLeast(barH)

            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(AccentCyan, AccentAmber),
                    startX = dayStartX,
                    endX = dayStartX + dayWidth
                ),
                topLeft = Offset(dayStartX, 0f),
                size = Size(dayWidth, barH),
                cornerRadius = CornerRadius(barH / 2, barH / 2)
            )
        }

        // Max Temp
        Text(
            text = WeatherFormatter.formatTemperature(day.maxTempCelsius, unit),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(36.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Daily Colored UV Index Exposure Badge
        val uvVal = day.uvIndex
        val (uvColor, uvBg) = when {
            uvVal < 3.0 -> Color(0xFF22C55E) to Color(0x3322C55E)
            uvVal < 6.0 -> Color(0xFFEAB308) to Color(0x33EAB308)
            uvVal < 8.0 -> Color(0xFFF97316) to Color(0x33F97316)
            uvVal < 11.0 -> Color(0xFFEF4444) to Color(0x33EF4444)
            else -> Color(0xFFA855F7) to Color(0x33A855F7)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(uvBg)
                .border(0.5.dp, uvColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text(
                text = "UV ${uvVal.toInt()}",
                color = uvColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ----------------- WEATHER METRICS BENTO GRID -----------------

@Composable
fun WeatherMetricsBentoGrid(
    currentWeather: CurrentWeatherUi,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier,
    onHumidityClick: (() -> Unit)? = null,
    onUvClick: (() -> Unit)? = null,
    onMoonClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Wind & Humidity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WindMetricCard(
                speedKmh = currentWeather.windSpeedKmh,
                directionDegrees = currentWeather.windDirectionDegrees,
                unit = unit,
                modifier = Modifier.weight(1f)
            )
            HumidityMetricCard(
                humidityPercent = currentWeather.humidityPercent,
                onClick = onHumidityClick,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: UV Index & Sun Path
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UvIndexMetricCard(
                uvIndex = currentWeather.uvIndex,
                onClick = onUvClick,
                modifier = Modifier.weight(1f)
            )
            SunPathMetricCard(
                sunrise = currentWeather.sunriseIso,
                sunset = currentWeather.sunsetIso,
                daylightFraction = currentWeather.daylightFraction,
                isDay = currentWeather.isDay,
                onClick = onMoonClick,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Visibility & Pressure
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VisibilityMetricCard(
                visibilityMeters = currentWeather.visibilityMeters,
                unit = unit,
                modifier = Modifier.weight(1f)
            )
            PressureMetricCard(
                pressureHpa = currentWeather.surfacePressureHpa,
                unit = unit,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ----------------- INDIVIDUAL METRIC CARDS -----------------

@Composable
private fun MetricCardHeader(
    icon: ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondaryColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = TextSecondaryColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun WindMetricCard(
    speedKmh: Double,
    directionDegrees: Double,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.height(130.dp)) {
        Column(
            modifier = Modifier
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            MetricCardHeader(icon = Icons.Default.Air, title = "WIND")

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = WeatherFormatter.formatSpeed(speedKmh, unit),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val dirText = degreesToCompassDirection(directionDegrees)
                    Text(
                        text = "$dirText • ${Math.round(directionDegrees)}°",
                        color = TextSecondaryColor,
                        fontSize = 12.sp
                    )
                }

                // Mini Compass dial
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Wind direction pointer",
                        tint = AccentCyan,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(directionDegrees.toFloat())
                    )
                }
            }

            Text(
                text = if (speedKmh > 30) "Strong breeze" else if (speedKmh > 15) "Moderate breeze" else "Light breeze",
                color = TextSecondaryColor,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun HumidityMetricCard(
    humidityPercent: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    GlassCard(
        modifier = modifier
            .height(130.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .testTag("humidity_metric_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricCardHeader(icon = Icons.Default.WaterDrop, title = "HUMIDITY")
                if (onClick != null) {
                    Text(
                        text = "Graph ↗",
                        color = AccentCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column {
                Text(
                    text = "$humidityPercent%",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (humidityPercent > 70) "Humid & damp" else if (humidityPercent < 35) "Dry air" else "Comfortable",
                    color = TextSecondaryColor,
                    fontSize = 12.sp
                )
            }

            // Indicator bar
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            ) {
                drawRoundRect(
                    color = Color(0x33FFFFFF),
                    size = size,
                    cornerRadius = CornerRadius(2f, 2f)
                )
                val fillW = size.width * (humidityPercent / 100f).coerceIn(0f, 1f)
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(AccentCyan, Color(0xFF60A5FA))),
                    size = Size(fillW, size.height),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }
        }
    }
}

@Composable
fun UvIndexMetricCard(
    uvIndex: Double,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (label, color, _) = when {
        uvIndex < 3.0 -> Triple("Low", Color(0xFF22C55E), Color(0x3322C55E))
        uvIndex < 6.0 -> Triple("Moderate", Color(0xFFEAB308), Color(0x33EAB308))
        uvIndex < 8.0 -> Triple("High", Color(0xFFF97316), Color(0x33F97316))
        uvIndex < 11.0 -> Triple("Very High", Color(0xFFEF4444), Color(0x33EF4444))
        else -> Triple("Extreme", Color(0xFFA855F7), Color(0x33A855F7))
    }

    GlassCard(
        modifier = modifier
            .height(130.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .testTag("uv_index_metric_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricCardHeader(icon = Icons.Default.WbSunny, title = "UV INDEX")
                if (onClick != null) {
                    Text(
                        text = "Gauge ↗",
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f", uvIndex),
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = label,
                        color = color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Mini arc visual gauge
                Canvas(modifier = Modifier.size(38.dp)) {
                    val r = size.minDimension / 2f - 3.dp.toPx()
                    val c = Offset(size.width / 2f, size.height * 0.78f)
                    val arcTL = Offset(c.x - r, c.y - r)
                    val arcSz = Size(r * 2f, r * 2f)

                    // Track
                    drawArc(
                        color = Color(0x22FFFFFF),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = arcTL,
                        size = arcSz,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Active sweep
                    val sweep = (180f * (uvIndex.toFloat() / 11f)).coerceIn(8f, 180f)
                    drawArc(
                        color = color,
                        startAngle = 180f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = arcTL,
                        size = arcSz,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    val angleRad = (180f + sweep) * (PI / 180f)
                    val dotX = c.x + r * cos(angleRad).toFloat()
                    val dotY = c.y + r * sin(angleRad).toFloat()
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(dotX, dotY))
                }
            }

            // Colored 5-segment indicator track
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val levels = listOf(Color(0xFF22C55E), Color(0xFFEAB308), Color(0xFFF97316), Color(0xFFEF4444), Color(0xFFA855F7))
                val activeIdx = when {
                    uvIndex < 3.0 -> 0
                    uvIndex < 6.0 -> 1
                    uvIndex < 8.0 -> 2
                    uvIndex < 11.0 -> 3
                    else -> 4
                }
                levels.forEachIndexed { idx, segColor ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .background(segColor.copy(alpha = if (idx == activeIdx) 1.0f else 0.25f))
                    )
                }
            }
        }
    }
}

@Composable
fun SunPathMetricCard(
    sunrise: String,
    sunset: String,
    daylightFraction: Float,
    isDay: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val moonInfo = remember { MoonPhaseCalculator.calculate() }

    GlassCard(
        modifier = modifier
            .height(130.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .testTag("sun_moon_metric_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricCardHeader(
                    icon = if (isDay) Icons.Default.WbSunny else Icons.Default.Star,
                    title = if (isDay) "SUN CYCLE" else "MOON PHASE"
                )
                if (onClick != null) {
                    Text(
                        text = "Astro ↗",
                        color = if (isDay) AccentAmber else Color(0xFF93C5FD),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!isDay) {
                // Night-time celestial view: Moon phase icon, name and illumination
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = moonInfo.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${moonInfo.illuminationPercent}% Illuminated",
                            color = Color(0xFF93C5FD),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    MoonPhaseIcon(
                        phaseFraction = moonInfo.phaseFraction,
                        size = 36.dp
                    )
                }
            } else {
                // Parabolic daylight arc
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, h - 4f)
                        quadraticTo(w / 2f, -4f, w, h - 4f)
                    }

                    // Dotted/solid arc line
                    drawPath(
                        path = path,
                        color = Color(0x40FFFFFF),
                        style = Stroke(width = 2f, cap = StrokeCap.Round)
                    )

                    // Sun marker on arc
                    val t = daylightFraction.coerceIn(0f, 1f)
                    // Quadratic bezier formula: B(t) = (1-t)^2 P0 + 2(1-t)t P1 + t^2 P2
                    val sunX = (1 - t) * (1 - t) * 0f + 2 * (1 - t) * t * (w / 2f) + t * t * w
                    val sunY = (1 - t) * (1 - t) * (h - 4f) + 2 * (1 - t) * t * (-4f) + t * t * (h - 4f)

                    drawCircle(
                        color = AccentAmber,
                        radius = 4f,
                        center = Offset(sunX, sunY)
                    )
                    drawCircle(
                        color = AccentAmber.copy(alpha = 0.35f),
                        radius = 8f,
                        center = Offset(sunX, sunY)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Rise: $sunrise",
                    color = TextSecondaryColor,
                    fontSize = 10.sp
                )
                Text(
                    text = "Set: $sunset",
                    color = TextSecondaryColor,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun VisibilityMetricCard(
    visibilityMeters: Double,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    val description = if (visibilityMeters >= 10000) "Clear view" else if (visibilityMeters >= 5000) "Moderate haze" else "Low visibility"

    GlassCard(modifier = modifier.height(115.dp)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            MetricCardHeader(icon = Icons.Default.Visibility, title = "VISIBILITY")

            Text(
                text = WeatherFormatter.formatVisibility(visibilityMeters, unit),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = description,
                color = TextSecondaryColor,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun PressureMetricCard(
    pressureHpa: Double,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    val desc = if (pressureHpa >= 1013.25) "High pressure" else "Low pressure"

    GlassCard(modifier = modifier.height(115.dp)) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            MetricCardHeader(icon = Icons.Default.Compress, title = "PRESSURE")

            Text(
                text = WeatherFormatter.formatPressure(pressureHpa, unit),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = desc,
                color = TextSecondaryColor,
                fontSize = 11.sp
            )
        }
    }
}

private fun degreesToCompassDirection(degrees: Double): String {
    val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val index = ((degrees + 11.25) / 22.5).toInt() % 16
    return directions[index]
}
