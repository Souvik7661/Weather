package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class WeatherConditionCategory {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN,
    HEAVY_RAIN,
    THUNDERSTORM,
    SNOW
}

enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT
}

data class WeatherCondition(
    val code: Int,
    val description: String,
    val category: WeatherConditionCategory,
    val isDay: Boolean = true
)

object WeatherCodeMapper {
    fun mapCode(code: Int, isDay: Boolean = true): WeatherCondition {
        return when (code) {
            0 -> WeatherCondition(
                code,
                if (isDay) "Clear Sky" else "Clear Night",
                WeatherConditionCategory.CLEAR,
                isDay
            )
            1 -> WeatherCondition(
                code,
                if (isDay) "Mainly Clear" else "Clear Night",
                WeatherConditionCategory.CLEAR,
                isDay
            )
            2 -> WeatherCondition(
                code,
                "Partly cloudy",
                WeatherConditionCategory.PARTLY_CLOUDY,
                isDay
            )
            3 -> WeatherCondition(
                code,
                "Overcast",
                WeatherConditionCategory.CLOUDY,
                isDay
            )
            45, 48 -> WeatherCondition(
                code,
                "Foggy",
                WeatherConditionCategory.FOG,
                isDay
            )
            51, 53, 55 -> WeatherCondition(
                code,
                "Low rain",
                WeatherConditionCategory.DRIZZLE,
                isDay
            )
            56, 57 -> WeatherCondition(
                code,
                "Freezing drizzle",
                WeatherConditionCategory.DRIZZLE,
                isDay
            )
            61 -> WeatherCondition(
                code,
                "Low rain",
                WeatherConditionCategory.RAIN,
                isDay
            )
            63 -> WeatherCondition(
                code,
                "Rain",
                WeatherConditionCategory.RAIN,
                isDay
            )
            65 -> WeatherCondition(
                code,
                "Heavy rain",
                WeatherConditionCategory.HEAVY_RAIN,
                isDay
            )
            66, 67 -> WeatherCondition(
                code,
                "Freezing rain",
                WeatherConditionCategory.RAIN,
                isDay
            )
            71, 73, 75, 77 -> WeatherCondition(
                code,
                "Snow rain",
                WeatherConditionCategory.SNOW,
                isDay
            )
            80, 81 -> WeatherCondition(
                code,
                "Rain showers",
                WeatherConditionCategory.RAIN,
                isDay
            )
            82 -> WeatherCondition(
                code,
                "Violent rain",
                WeatherConditionCategory.HEAVY_RAIN,
                isDay
            )
            85, 86 -> WeatherCondition(
                code,
                "Snow rain",
                WeatherConditionCategory.SNOW,
                isDay
            )
            95 -> WeatherCondition(
                code,
                "Thunderstorm",
                WeatherConditionCategory.THUNDERSTORM,
                isDay
            )
            96, 99 -> WeatherCondition(
                code,
                "Thunderstorm",
                WeatherConditionCategory.THUNDERSTORM,
                isDay
            )
            else -> WeatherCondition(
                code,
                "Partly cloudy",
                WeatherConditionCategory.PARTLY_CLOUDY,
                isDay
            )
        }
    }
}

object WeatherSafetyAdvice {
    fun getReminder(category: WeatherConditionCategory, isDay: Boolean = true): String {
        return when (category) {
            WeatherConditionCategory.THUNDERSTORM ->
                "Try to stay at home. Store some food, weather will be bad for a week."
            WeatherConditionCategory.RAIN,
            WeatherConditionCategory.DRIZZLE ->
                "Go out with an umbrella. It's raining out there. Chances of getting wet."
            WeatherConditionCategory.HEAVY_RAIN ->
                "Heavy downpour expected. Avoid flooded areas and carry a sturdy umbrella."
            WeatherConditionCategory.SNOW ->
                "Try to stay at home and close your window and door. Weather is not good out there."
            WeatherConditionCategory.FOG ->
                "Dense fog reduces visibility. Drive with low beams and keep safe distance."
            WeatherConditionCategory.CLEAR ->
                if (isDay) "Clear and bright today. Stay hydrated and enjoy the beautiful sunshine."
                else "Clear and peaceful night sky. Stargazing conditions are optimal."
            WeatherConditionCategory.PARTLY_CLOUDY ->
                "Pleasant outdoor conditions. Great weather for travel and daily commutes."
            WeatherConditionCategory.CLOUDY ->
                "Overcast skies throughout the day. Keep a light jacket handy just in case."
        }
    }
}

// ----------------- Retrofit Open-Meteo DTOs -----------------

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String?,
    val current: OpenMeteoCurrent?,
    val hourly: OpenMeteoHourly?,
    val daily: OpenMeteoDaily?
)

@JsonClass(generateAdapter = true)
data class OpenMeteoCurrent(
    val time: String,
    @Json(name = "temperature_2m") val temperature2m: Double,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Double,
    @Json(name = "apparent_temperature") val apparentTemperature: Double,
    @Json(name = "is_day") val isDay: Int,
    val precipitation: Double?,
    @Json(name = "weather_code") val weatherCode: Int,
    @Json(name = "surface_pressure") val surfacePressure: Double,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double,
    @Json(name = "wind_direction_10m") val windDirection10m: Double
)

@JsonClass(generateAdapter = true)
data class OpenMeteoHourly(
    val time: List<String>?,
    @Json(name = "temperature_2m") val temperature2m: List<Double>?,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: List<Double>?,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int>?,
    @Json(name = "weather_code") val weatherCode: List<Int>?,
    val visibility: List<Double>?,
    @Json(name = "uv_index") val uvIndex: List<Double>?
)

@JsonClass(generateAdapter = true)
data class OpenMeteoDaily(
    val time: List<String>?,
    @Json(name = "weather_code") val weatherCode: List<Int>?,
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double>?,
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double>?,
    val sunrise: List<String>?,
    val sunset: List<String>?,
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int>?,
    @Json(name = "precipitation_sum") val precipitationSum: List<Double>?,
    @Json(name = "uv_index_max") val uvIndexMax: List<Double>?
)

// ----------------- Geocoding DTOs -----------------

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingResult>?
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val admin1: String?
)

// ----------------- Air Quality DTOs -----------------

@JsonClass(generateAdapter = true)
data class AirQualityResponse(
    val latitude: Double?,
    val longitude: Double?,
    val current: AirQualityCurrent?
)

@JsonClass(generateAdapter = true)
data class AirQualityCurrent(
    val time: String?,
    @Json(name = "us_aqi") val usAqi: Int?,
    @Json(name = "pm2_5") val pm25: Double?,
    val pm10: Double?
)

data class AirQualityUi(
    val aqi: Int,
    val category: String,
    val advice: String,
    val pm25: Double,
    val pm10: Double,
    val progressFraction: Float
)

// ----------------- Clean UI Presentation Models -----------------

data class WeatherLocation(
    val name: String,
    val region: String = "",
    val country: String = "",
    val latitude: Double,
    val longitude: Double,
    val isGpsLocation: Boolean = false
) {
    val displayName: String
        get() = if (region.isNotBlank() && region != name) "$name, $region" else if (country.isNotBlank()) "$name, $country" else name
}

data class CurrentWeatherUi(
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double,
    val condition: WeatherCondition,
    val minTempCelsius: Double,
    val maxTempCelsius: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val windDirectionDegrees: Double,
    val surfacePressureHpa: Double,
    val uvIndex: Double,
    val visibilityMeters: Double,
    val precipitationMm: Double,
    val sunriseIso: String,
    val sunsetIso: String,
    val daylightFraction: Float,
    val airQuality: AirQualityUi? = null
) {
    val isDay: Boolean get() = condition.isDay
}

data class HourlyForecastUi(
    val timeDisplay: String, // e.g., "Now", "2 PM"
    val temperatureCelsius: Double,
    val condition: WeatherCondition,
    val precipitationProbabilityPercent: Int,
    val isCurrentHour: Boolean = false,
    val humidityPercent: Int = 50
)

data class DailyForecastUi(
    val dayName: String, // e.g., "Today", "Thu", "Fri"
    val dateDisplay: String, // e.g., "Sep 23"
    val condition: WeatherCondition,
    val minTempCelsius: Double,
    val maxTempCelsius: Double,
    val precipitationProbabilityPercent: Int,
    val uvIndex: Double
)

// ----------------- Severe Weather Alert Models -----------------

enum class AlertSeverity {
    WARNING,
    WATCH,
    ADVISORY
}

data class WeatherAlert(
    val id: String,
    val event: String,
    val severity: AlertSeverity,
    val headline: String,
    val description: String,
    val instruction: String? = null,
    val area: String,
    val effectiveTime: String,
    val expiresTime: String,
    val source: String = "Meteorological Service",
    val isDismissed: Boolean = false
)

// ----------------- NWS Alerts DTOs -----------------

@JsonClass(generateAdapter = true)
data class NwsAlertsResponse(
    val features: List<NwsAlertFeature>?
)

@JsonClass(generateAdapter = true)
data class NwsAlertFeature(
    val id: String?,
    val properties: NwsAlertProperties?
)

@JsonClass(generateAdapter = true)
data class NwsAlertProperties(
    val id: String?,
    val areaDesc: String?,
    val event: String?,
    val headline: String?,
    val description: String?,
    val instruction: String?,
    val severity: String?,
    val urgency: String?,
    val certainty: String?,
    val effective: String?,
    val expires: String?,
    val senderName: String?
)
