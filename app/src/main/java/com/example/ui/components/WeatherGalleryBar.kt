package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherConditionCategory

data class WeatherTypeShowcaseItem(
    val title: String,
    val subtitle: String,
    val category: WeatherConditionCategory,
    val isDay: Boolean = true
)

val ALL_WEATHER_SHOWCASE_ITEMS = listOf(
    WeatherTypeShowcaseItem("Miami Storm", "Thunderstorm", WeatherConditionCategory.THUNDERSTORM, isDay = false),
    WeatherTypeShowcaseItem("Siberia Snow", "Winter Snow", WeatherConditionCategory.SNOW, isDay = false),
    WeatherTypeShowcaseItem("Sunny Day", "Clear Sky", WeatherConditionCategory.CLEAR, isDay = true),
    WeatherTypeShowcaseItem("Starry Night", "Moon & Stars", WeatherConditionCategory.CLEAR, isDay = false),
    WeatherTypeShowcaseItem("Partly Cloudy", "Sun & Clouds", WeatherConditionCategory.PARTLY_CLOUDY, isDay = true),
    WeatherTypeShowcaseItem("Overcast", "Deep Clouds", WeatherConditionCategory.CLOUDY, isDay = true),
    WeatherTypeShowcaseItem("Showers", "Rain Drops", WeatherConditionCategory.RAIN, isDay = true),
    WeatherTypeShowcaseItem("Dense Fog", "Mist Layers", WeatherConditionCategory.FOG, isDay = true)
)

/**
 * Gallery selector allowing the user to preview and verify every single generated weather picture & atmospheric theme
 */
@Composable
fun WeatherGalleryBar(
    currentCategory: WeatherConditionCategory,
    isDay: Boolean,
    onSelectWeatherType: (WeatherConditionCategory, Boolean) -> Unit,
    onResetToLive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weather_gallery_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "WEATHER GALLERY",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Text(
                text = "Live GPS",
                color = AccentCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onResetToLive() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .testTag("reset_live_gps_button")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ALL_WEATHER_SHOWCASE_ITEMS.forEach { item ->
                val isSelected = currentCategory == item.category && isDay == item.isDay

                val borderColor = if (isSelected) AccentCyan else Color(0x33FFFFFF)
                val bgColor = if (isSelected) Color(0x400284C7) else Color(0x221E293B)

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(bgColor)
                        .border(BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor), RoundedCornerShape(22.dp))
                        .clickable { onSelectWeatherType(item.category, item.isDay) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("gallery_chip_${item.category.name}_${if (item.isDay) "day" else "night"}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mini 3D sculpted artwork preview
                    Weather3DArtworkHero(
                        condition = WeatherCondition(
                            code = 0,
                            description = item.subtitle,
                            category = item.category,
                            isDay = item.isDay
                        ),
                        size = 22.dp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = item.title,
                            color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }

                    if (isSelected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(AccentCyan)
                        )
                    }
                }
            }
        }
    }
}
