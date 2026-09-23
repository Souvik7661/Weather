package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HourlyForecastUi
import com.example.data.model.TemperatureUnit

/**
 * 24-Hour Forecast Section matching the reference screenshot:
 * - "24 hour forecast" centered title
 * - Frosted dark glass rounded pill container (3 rows: Temperatures, Weather Icons, Hours)
 */
@Composable
fun Forecast24HourPill(
    hourlyList: List<HourlyForecastUi>,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    if (hourlyList.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Centered Subtitle: "24 hour forecast"
        Text(
            text = "24 hour forecast",
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.2.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Frosted Dark Pill Container
        val shape = RoundedCornerShape(26.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(
                    BorderStroke(1.dp, Color(0x2BFFFFFF)),
                    shape
                ),
            color = Color(0x3B151F2E),
            shape = shape
        ) {
            val scrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                hourlyList.forEach { item ->
                    HourlyPillColumn(
                        item = item,
                        unit = unit
                    )
                }
            }
        }
    }
}

@Composable
private fun HourlyPillColumn(
    item: HourlyForecastUi,
    unit: TemperatureUnit,
    modifier: Modifier = Modifier
) {
    val tempVal = when (unit) {
        TemperatureUnit.CELSIUS -> item.temperatureCelsius
        TemperatureUnit.FAHRENHEIT -> (item.temperatureCelsius * 9.0 / 5.0) + 32.0
    }
    val formattedTemp = "${Math.round(tempVal)}°"
    val icon = getConditionIcon(item.condition.category, item.condition.isDay)

    Column(
        modifier = modifier.width(44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Temperature on top (e.g. "15°")
        Text(
            text = formattedTemp,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Weather condition icon in middle
        Icon(
            imageVector = icon,
            contentDescription = item.condition.description,
            tint = Color.White.copy(alpha = 0.90f),
            modifier = Modifier.size(19.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Hour label at bottom (e.g. "Now", "2 PM")
        Text(
            text = item.timeDisplay,
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1
        )
    }
}
