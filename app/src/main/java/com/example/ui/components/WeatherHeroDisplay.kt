package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.FilterDrama
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrentWeatherUi
import com.example.data.model.TemperatureUnit
import com.example.data.model.WeatherConditionCategory

/**
 * Hero Temperature and Condition display faithfully matching the reference design:
 * - Huge bold temperature text (e.g. "23°", "36°", "-8°") with subtle breathing animation
 * - Weather condition icon + text (e.g. "⛈ Thunderstorm", "🌧 Low rain", "❄ Snow rain")
 * - Tapping temperature toggles Celsius / Fahrenheit
 */
@Composable
fun WeatherHeroDisplay(
    currentWeather: CurrentWeatherUi,
    unit: TemperatureUnit,
    onToggleUnit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_anim")
    // Subtle organic floating float
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_float"
    )

    val tempValue = when (unit) {
        TemperatureUnit.CELSIUS -> currentWeather.temperatureCelsius
        TemperatureUnit.FAHRENHEIT -> (currentWeather.temperatureCelsius * 9.0 / 5.0) + 32.0
    }
    val formattedTemp = "${Math.round(tempValue)}°"

    val conditionCategory = currentWeather.condition.category
    val conditionDescription = currentWeather.condition.description
    val conditionIcon = getConditionIcon(conditionCategory, currentWeather.isDay)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Giant Temperature Number (clickable to toggle °C / °F)
        Text(
            text = formattedTemp,
            color = Color.White,
            fontSize = 76.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = (-2).sp,
            modifier = Modifier
                .offset(y = floatOffset.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 50.dp)
                ) { onToggleUnit() }
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Weather Condition (Icon + Label)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = conditionIcon,
                contentDescription = conditionDescription,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = conditionDescription,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.2.sp
            )
        }
    }
}

fun getConditionIcon(category: WeatherConditionCategory, isDay: Boolean = true): ImageVector {
    return when (category) {
        WeatherConditionCategory.THUNDERSTORM -> Icons.Outlined.Thunderstorm
        WeatherConditionCategory.RAIN,
        WeatherConditionCategory.DRIZZLE -> Icons.Outlined.WaterDrop
        WeatherConditionCategory.HEAVY_RAIN -> Icons.Outlined.Grain
        WeatherConditionCategory.SNOW -> Icons.Outlined.AcUnit
        WeatherConditionCategory.CLEAR -> if (isDay) Icons.Outlined.WbSunny else Icons.Outlined.FilterDrama
        WeatherConditionCategory.PARTLY_CLOUDY -> Icons.Outlined.WbCloudy
        WeatherConditionCategory.CLOUDY -> Icons.Outlined.Cloud
        WeatherConditionCategory.FOG -> Icons.Outlined.FilterDrama
    }
}
