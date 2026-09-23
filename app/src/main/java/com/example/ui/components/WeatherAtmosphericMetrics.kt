package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrentWeatherUi
import com.example.data.model.TemperatureUnit

/**
 * 3-Column Atmospheric Metrics Row matching the reference screenshot:
 * - Wind (e.g. "60 km")
 * - Humidity (e.g. "35 %")
 * - Precip (e.g. "0 %")
 */
@Composable
fun WeatherAtmosphericMetrics(
    currentWeather: CurrentWeatherUi,
    unit: TemperatureUnit,
    precipitationProbability: Int = 0,
    modifier: Modifier = Modifier
) {
    val windVal = Math.round(
        if (unit == TemperatureUnit.CELSIUS) currentWeather.windSpeedKmh
        else currentWeather.windSpeedKmh * 0.621371
    )
    val windUnit = if (unit == TemperatureUnit.CELSIUS) "km" else "mph"
    val humidityVal = currentWeather.humidityPercent
    val precipVal = precipitationProbability.coerceIn(0, 100)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Wind
        MetricColumn(
            label = "Wind",
            value = "$windVal",
            unit = windUnit
        )

        // 2. Humidity
        MetricColumn(
            label = "Humidity",
            value = "$humidityVal",
            unit = "%"
        )

        // 3. Precip
        MetricColumn(
            label = "Precip",
            value = "$precipVal",
            unit = "%"
        )
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Label on top (e.g., "Wind", "Humidity", "Precip")
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Value + Unit (e.g. "60 km", "35 %")
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value,
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = " $unit",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(bottom = 2.5.dp)
            )
        }
    }
}
