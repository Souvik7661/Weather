package com.example

import com.example.data.model.AirQualityCurrent
import com.example.data.model.AirQualityResponse
import com.example.data.model.AirQualityUi
import com.example.data.model.AlertSeverity
import com.example.data.model.NwsAlertFeature
import com.example.data.model.NwsAlertProperties
import com.example.data.model.NwsAlertsResponse
import com.example.data.model.TemperatureUnit
import com.example.data.model.WeatherAlert
import com.example.data.model.WeatherCodeMapper
import com.example.data.model.WeatherConditionCategory
import com.example.data.model.WeatherSafetyAdvice
import com.example.ui.components.MoonPhaseCalculator
import com.example.ui.components.WeatherFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testWeatherCodeMapper_clearSky() {
        val conditionDay = WeatherCodeMapper.mapCode(0, isDay = true)
        assertEquals("Clear Sky", conditionDay.description)
        assertEquals(WeatherConditionCategory.CLEAR, conditionDay.category)

        val conditionNight = WeatherCodeMapper.mapCode(0, isDay = false)
        assertEquals("Clear Night", conditionNight.description)
    }

    @Test
    fun testWeatherCodeMapper_rainAndThunder() {
        val rain = WeatherCodeMapper.mapCode(61, isDay = true)
        assertEquals(WeatherConditionCategory.RAIN, rain.category)

        val thunderstorm = WeatherCodeMapper.mapCode(95, isDay = true)
        assertEquals(WeatherConditionCategory.THUNDERSTORM, thunderstorm.category)
    }

    @Test
    fun testWeatherFormatter_temperatureConversion() {
        val celsiusStr = WeatherFormatter.formatTemperature(20.0, TemperatureUnit.CELSIUS)
        assertEquals("20°", celsiusStr)

        val fahrenheitStr = WeatherFormatter.formatTemperature(20.0, TemperatureUnit.FAHRENHEIT)
        assertEquals("68°", fahrenheitStr)
    }

    @Test
    fun testWeatherFormatter_speedConversion() {
        val kmh = WeatherFormatter.formatSpeed(10.0, TemperatureUnit.CELSIUS)
        assertEquals("10 km/h", kmh)

        val mph = WeatherFormatter.formatSpeed(10.0, TemperatureUnit.FAHRENHEIT)
        assertEquals("6 mph", mph)
    }

    @Test
    fun testAirQualityResponse_mapping() {
        val current = AirQualityCurrent(
            time = "2026-09-22T15:00",
            usAqi = 42,
            pm25 = 8.4,
            pm10 = 15.2
        )
        val response = AirQualityResponse(
            latitude = 40.71,
            longitude = -74.01,
            current = current
        )
        assertNotNull(response.current)
        assertEquals(42, response.current?.usAqi)
        assertEquals(8.4, response.current?.pm25 ?: 0.0, 0.01)
        assertEquals(15.2, response.current?.pm10 ?: 0.0, 0.01)

        val aqiUi = AirQualityUi(
            aqi = 42,
            category = "Good",
            advice = "Air quality is satisfactory with little or no health risk.",
            pm25 = 8.4,
            pm10 = 15.2,
            progressFraction = 42f / 300f
        )
        assertEquals("Good", aqiUi.category)
        assertEquals(42, aqiUi.aqi)
    }

    @Test
    fun testSevereWeatherAlert_creationAndSeverity() {
        val alert = WeatherAlert(
            id = "alert_123",
            event = "Severe Thunderstorm Warning",
            severity = AlertSeverity.WARNING,
            headline = "Severe storm approaching with damaging winds.",
            description = "Radar indicated severe thunderstorms producing gusts up to 70 km/h.",
            instruction = "Take shelter in an interior room on the lowest floor.",
            area = "New York, NY",
            effectiveTime = "4:00 PM",
            expiresTime = "5:30 PM",
            source = "National Weather Service"
        )

        assertEquals("alert_123", alert.id)
        assertEquals(AlertSeverity.WARNING, alert.severity)
        assertEquals("Severe Thunderstorm Warning", alert.event)
        assertEquals(false, alert.isDismissed)
    }

    @Test
    fun testNwsAlertsResponse_parsing() {
        val props = NwsAlertProperties(
            id = "urn:oid:2.49.0.1",
            areaDesc = "Kings; Queens; New York",
            event = "Flood Watch",
            headline = "Flood Watch in effect until midnight",
            description = "Excessive rainfall may lead to flooding.",
            instruction = "Monitor local forecasts and be prepared to take action.",
            severity = "Moderate",
            urgency = "Expected",
            certainty = "Likely",
            effective = "2026-09-22T16:00:00-04:00",
            expires = "2026-09-23T00:00:00-04:00",
            senderName = "NWS New York NY"
        )
        val feature = NwsAlertFeature(
            id = "urn:oid:2.49.0.1",
            properties = props
        )
        val response = NwsAlertsResponse(
            features = listOf(feature)
        )

        assertEquals(1, response.features?.size)
        assertEquals("Flood Watch", response.features?.first()?.properties?.event)
        assertEquals("Moderate", response.features?.first()?.properties?.severity)
    }

    @Test
    fun testMoonPhaseCalculator_newMoon() {
        // Reference new moon epoch
        val newMoonInfo = MoonPhaseCalculator.calculate(1704974220000L)
        assertEquals("New Moon", newMoonInfo.name)
        assertEquals("🌑", newMoonInfo.emoji)
        assertEquals(0, newMoonInfo.illuminationPercent)
        assertEquals(0.0, newMoonInfo.ageDays, 0.2)
    }

    @Test
    fun testMoonPhaseCalculator_fullMoon() {
        // ~14.765 days after reference new moon
        val fullMoonTime = 1704974220000L + (14.765294 * 24 * 3600 * 1000L).toLong()
        val fullMoonInfo = MoonPhaseCalculator.calculate(fullMoonTime)
        assertEquals("Full Moon", fullMoonInfo.name)
        assertEquals("🌕", fullMoonInfo.emoji)
        assertTrue("Illumination should be >= 98%", fullMoonInfo.illuminationPercent >= 98)
    }

    @Test
    fun testMoonPhaseCalculator_firstAndThirdQuarter() {
        // First Quarter: ~7.382 days
        val firstQuarterTime = 1704974220000L + (7.3826 * 24 * 3600 * 1000L).toLong()
        val firstQuarterInfo = MoonPhaseCalculator.calculate(firstQuarterTime)
        assertEquals("First Quarter", firstQuarterInfo.name)
        assertEquals("🌓", firstQuarterInfo.emoji)
        assertTrue("Illumination should be ~50%", firstQuarterInfo.illuminationPercent in 48..52)

        // Third Quarter: ~22.147 days
        val thirdQuarterTime = 1704974220000L + (22.1479 * 24 * 3600 * 1000L).toLong()
        val thirdQuarterInfo = MoonPhaseCalculator.calculate(thirdQuarterTime)
        assertEquals("Third Quarter", thirdQuarterInfo.name)
        assertEquals("🌗", thirdQuarterInfo.emoji)
        assertTrue("Illumination should be ~50%", thirdQuarterInfo.illuminationPercent in 48..52)
    }

    @Test
    fun testMoonPhaseCalculator_currentTimeSanity() {
        val current = MoonPhaseCalculator.calculate()
        assertNotNull(current.name)
        assertNotNull(current.emoji)
        assertTrue(current.phaseFraction in 0.0..1.0)
        assertTrue(current.illuminationPercent in 0..100)
        assertTrue(current.ageDays in 0.0..29.6)
        assertTrue(current.description.isNotEmpty())
    }

    @Test
    fun testWeatherSafetyAdvice_referenceConditions() {
        val stormAdvice = WeatherSafetyAdvice.getReminder(WeatherConditionCategory.THUNDERSTORM)
        assertEquals("Try to stay at home. Store some food, weather will be bad for a week.", stormAdvice)

        val rainAdvice = WeatherSafetyAdvice.getReminder(WeatherConditionCategory.RAIN)
        assertEquals("Go out with an umbrella. It's raining out there. Chances of getting wet.", rainAdvice)

        val snowAdvice = WeatherSafetyAdvice.getReminder(WeatherConditionCategory.SNOW)
        assertEquals("Try to stay at home and close your window and door. Weather is not good out there.", snowAdvice)

        val clearDayAdvice = WeatherSafetyAdvice.getReminder(WeatherConditionCategory.CLEAR, isDay = true)
        assertTrue(clearDayAdvice.contains("Clear and bright today"))

        val clearNightAdvice = WeatherSafetyAdvice.getReminder(WeatherConditionCategory.CLEAR, isDay = false)
        assertTrue(clearNightAdvice.contains("Clear and peaceful night sky"))
    }

    @Test
    fun testWeatherCodeMapper_mockupConditions() {
        val rain = WeatherCodeMapper.mapCode(61, isDay = true)
        assertEquals("Low rain", rain.description)
        assertEquals(WeatherConditionCategory.RAIN, rain.category)

        val snow = WeatherCodeMapper.mapCode(73, isDay = true)
        assertEquals("Snow rain", snow.description)
        assertEquals(WeatherConditionCategory.SNOW, snow.category)

        val thunderstorm = WeatherCodeMapper.mapCode(95, isDay = true)
        assertEquals("Thunderstorm", thunderstorm.description)
        assertEquals(WeatherConditionCategory.THUNDERSTORM, thunderstorm.category)
    }
}
