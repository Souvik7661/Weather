package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherConditionCategory

object WeatherIllustrationProvider {
    /**
     * Resolves high-resolution dynamic weather illustration URL for the given condition
     */
    fun getIllustrationUrl(condition: WeatherCondition): String {
        val timeOfDay = if (condition.isDay) "day" else "night"
        val iconCode = when (condition.category) {
            WeatherConditionCategory.CLEAR -> "113"
            WeatherConditionCategory.PARTLY_CLOUDY -> "116"
            WeatherConditionCategory.CLOUDY -> "122"
            WeatherConditionCategory.FOG -> "248"
            WeatherConditionCategory.DRIZZLE -> "266"
            WeatherConditionCategory.RAIN -> "296"
            WeatherConditionCategory.HEAVY_RAIN -> "308"
            WeatherConditionCategory.THUNDERSTORM -> "389"
            WeatherConditionCategory.SNOW -> "338"
        }
        return "https://cdn.weatherapi.com/weather/128x128/$timeOfDay/$iconCode.png"
    }

    /**
     * Returns ambient glow colors tailored to the current weather condition
     */
    fun getGlowColors(category: WeatherConditionCategory, isDay: Boolean): List<Color> {
        return if (!isDay) {
            listOf(Color(0x66818CF8), Color(0x2238BDF8), Color.Transparent)
        } else {
            when (category) {
                WeatherConditionCategory.CLEAR -> listOf(
                    Color(0x80FDE047),
                    Color(0x40F59E0B),
                    Color.Transparent
                )
                WeatherConditionCategory.PARTLY_CLOUDY -> listOf(
                    Color(0x66FBBF24),
                    Color(0x3338BDF8),
                    Color.Transparent
                )
                WeatherConditionCategory.CLOUDY -> listOf(
                    Color(0x4094A3B8),
                    Color(0x20CBD5E1),
                    Color.Transparent
                )
                WeatherConditionCategory.DRIZZLE,
                WeatherConditionCategory.RAIN,
                WeatherConditionCategory.HEAVY_RAIN -> listOf(
                    Color(0x6638BDF8),
                    Color(0x330284C7),
                    Color.Transparent
                )
                WeatherConditionCategory.THUNDERSTORM -> listOf(
                    Color(0x80A855F7),
                    Color(0x40FBBF24),
                    Color.Transparent
                )
                WeatherConditionCategory.SNOW -> listOf(
                    Color(0x80E0F2FE),
                    Color(0x33BAE6FD),
                    Color.Transparent
                )
                WeatherConditionCategory.FOG -> listOf(
                    Color(0x40CBD5E1),
                    Color(0x2094A3B8),
                    Color.Transparent
                )
            }
        }
    }
}

/**
 * Dynamic Weather Illustration displaying high-fidelity 3D claymorphic / paper-cut
 * weather artwork matching the user's reference design with smooth idle animations.
 */
@Composable
fun DynamicWeatherIllustration(
    condition: WeatherCondition,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp
) {
    val glowColors = WeatherIllustrationProvider.getGlowColors(condition.category, condition.isDay)

    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow_transition")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_anim"
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag("dynamic_weather_illustration"),
        contentAlignment = Alignment.Center
    ) {
        // Ambient soft radial aura behind the illustration
        Box(
            modifier = Modifier
                .size(size * 0.9f * pulse)
                .blur(22.dp)
                .background(
                    brush = Brush.radialGradient(colors = glowColors),
                    shape = CircleShape
                )
        )

        // Sculpted 3D claymorphic / paper-cut weather artwork
        Weather3DArtworkHero(
            condition = condition,
            size = size,
            modifier = Modifier.testTag("hero_3d_artwork_${condition.category.name}")
        )
    }
}

/**
 * Dynamic Weather Icon for hourly forecast pills and daily rows using 3D sculpted artwork
 */
@Composable
fun DynamicWeatherIcon(
    condition: WeatherCondition,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    Weather3DArtworkHero(
        condition = condition,
        size = size,
        modifier = modifier
    )
}
