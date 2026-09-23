package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.location.LocationTracker
import com.example.data.model.AirQualityResponse
import com.example.data.model.AirQualityUi
import com.example.data.model.AlertSeverity
import com.example.data.model.CurrentWeatherUi
import com.example.data.model.DailyForecastUi
import com.example.data.model.HourlyForecastUi
import com.example.data.model.OpenMeteoResponse
import com.example.data.model.TemperatureUnit
import com.example.data.model.WeatherAlert
import com.example.data.model.WeatherCodeMapper
import com.example.data.model.WeatherCondition
import com.example.data.model.WeatherConditionCategory
import com.example.data.model.WeatherLocation
import com.example.data.network.WeatherApiClient
import com.example.data.notification.WeatherNotificationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class WeatherUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val location: WeatherLocation = WeatherLocation(
        name = "New York",
        region = "New York",
        country = "United States",
        latitude = 40.7128,
        longitude = -74.0060,
        isGpsLocation = false
    ),
    val currentWeather: CurrentWeatherUi? = null,
    val hourlyForecast: List<HourlyForecastUi> = emptyList(),
    val dailyForecast: List<DailyForecastUi> = emptyList(),
    val savedLocations: List<WeatherLocation> = listOf(
        WeatherLocation("New York", "NY", "United States", 40.7128, -74.0060),
        WeatherLocation("London", "England", "United Kingdom", 51.5074, -0.1278),
        WeatherLocation("Tokyo", "Tokyo", "Japan", 35.6762, 139.6503),
        WeatherLocation("Paris", "Île-de-France", "France", 48.8566, 2.3522),
        WeatherLocation("Sydney", "NSW", "Australia", -33.8688, 151.2093)
    ),
    val searchResults: List<WeatherLocation> = emptyList(),
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val isLocationPermissionGranted: Boolean = false,
    val isLocationPermissionPromptDismissed: Boolean = false,
    val activeAlerts: List<WeatherAlert> = emptyList()
)

class WeatherViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var alertPollingJob: Job? = null
    private var notificationManager: WeatherNotificationManager? = null
    private val dismissedAlertIds = mutableSetOf<String>()

    init {
        // Initial load for default location
        loadWeatherForLocation(_uiState.value.location, isInitial = true)
        startAlertPolling()
    }

    fun setNotificationManager(manager: WeatherNotificationManager) {
        this.notificationManager = manager
        // Check if there are active alerts that need immediate system notification
        _uiState.value.activeAlerts.forEach { alert ->
            manager.postSevereAlertNotification(alert)
        }
    }

    fun onPermissionResult(isGranted: Boolean, locationTracker: LocationTracker) {
        _uiState.update { it.copy(isLocationPermissionGranted = isGranted) }
        if (isGranted) {
            requestGpsLocation(locationTracker)
        }
    }

    fun dismissPermissionPrompt() {
        _uiState.update { it.copy(isLocationPermissionPromptDismissed = true) }
    }

    fun requestGpsLocation(locationTracker: LocationTracker) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val gpsLocation = locationTracker.getCurrentLocation()
            if (gpsLocation != null) {
                _uiState.update { it.copy(location = gpsLocation) }
                loadWeatherForLocation(gpsLocation)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not fetch current GPS location. Please check location settings."
                    )
                }
                loadWeatherForLocation(_uiState.value.location)
            }
        }
    }

    fun loadWeatherForLocation(location: WeatherLocation, isInitial: Boolean = false) {
        viewModelScope.launch {
            if (!isInitial) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            try {
                coroutineScope {
                    val weatherDeferred = async {
                        WeatherApiClient.weatherService.getForecast(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    }

                    val airQualityDeferred = async {
                        try {
                            WeatherApiClient.airQualityService.getAirQuality(
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val weatherResponse = weatherDeferred.await()
                    val airQualityResponse = airQualityDeferred.await()

                    val airQualityUi = parseAirQuality(airQualityResponse)
                    val (currentUi, hourlyUi, dailyUi) = parseWeatherResponse(weatherResponse, airQualityUi)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            location = location,
                            currentWeather = currentUi,
                            hourlyForecast = hourlyUi,
                            dailyForecast = dailyUi,
                            errorMessage = null
                        )
                    }

                    // Query alerts for this active location
                    pollAlertsForCurrentLocation(location, currentUi)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = "Failed to load weather: ${e.localizedMessage ?: "Network error"}"
                    )
                }
            }
        }
    }

    fun refreshWeather() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            loadWeatherForLocation(_uiState.value.location)
        }
    }

    fun toggleTemperatureUnit() {
        _uiState.update {
            val newUnit = if (it.temperatureUnit == TemperatureUnit.CELSIUS) {
                TemperatureUnit.FAHRENHEIT
            } else {
                TemperatureUnit.CELSIUS
            }
            it.copy(temperatureUnit = newUnit)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(350)
            _uiState.update { it.copy(isSearching = true) }
            try {
                val res = WeatherApiClient.geocodingService.searchCity(query.trim())
                val locations = res.results?.map {
                    WeatherLocation(
                        name = it.name,
                        region = it.admin1 ?: "",
                        country = it.country ?: "",
                        latitude = it.latitude,
                        longitude = it.longitude,
                        isGpsLocation = false
                    )
                } ?: emptyList()
                _uiState.update { it.copy(searchResults = locations, isSearching = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            }
        }
    }

    fun selectLocation(location: WeatherLocation) {
        _uiState.update {
            val alreadySaved = it.savedLocations.any { s -> s.name == location.name && s.country == location.country }
            val updatedSaved = if (!alreadySaved && !location.isGpsLocation) {
                listOf(location) + it.savedLocations
            } else {
                it.savedLocations
            }
            it.copy(
                location = location,
                savedLocations = updatedSaved,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
        loadWeatherForLocation(location)
    }

    fun removeSavedLocation(location: WeatherLocation) {
        _uiState.update {
            it.copy(savedLocations = it.savedLocations.filterNot { s -> s.name == location.name && s.country == location.country })
        }
    }

    // ----------------- Severe Weather Alert Polling System -----------------

    private fun startAlertPolling() {
        alertPollingJob?.cancel()
        alertPollingJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000) // Poll every 60 seconds
                val loc = _uiState.value.location
                val curr = _uiState.value.currentWeather
                pollAlertsForCurrentLocation(loc, curr)
            }
        }
    }

    private suspend fun pollAlertsForCurrentLocation(
        location: WeatherLocation,
        currentWeather: CurrentWeatherUi?
    ) {
        val polledAlerts = mutableListOf<WeatherAlert>()

        // 1. Check official active alerts via NWS API (available for US coordinates)
        try {
            val point = String.format(Locale.US, "%.4f,%.4f", location.latitude, location.longitude)
            val nwsResponse = WeatherApiClient.nwsAlertsService.getActiveAlerts(point)
            nwsResponse.features?.forEach { feature ->
                val props = feature.properties
                if (props != null && !props.event.isNullOrBlank()) {
                    val severityEnum = when (props.severity?.uppercase()) {
                        "EXTREME", "SEVERE" -> AlertSeverity.WARNING
                        "MODERATE" -> AlertSeverity.WATCH
                        else -> AlertSeverity.ADVISORY
                    }
                    val alertId = props.id ?: feature.id ?: "${props.event}_${location.name}"
                    polledAlerts.add(
                        WeatherAlert(
                            id = alertId,
                            event = props.event,
                            severity = severityEnum,
                            headline = props.headline ?: props.event,
                            description = props.description ?: "Active meteorological warning for ${location.name}",
                            instruction = props.instruction,
                            area = props.areaDesc ?: location.displayName,
                            effectiveTime = formatAlertTime(props.effective),
                            expiresTime = formatAlertTime(props.expires),
                            source = props.senderName ?: "National Weather Service"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Handled safely (e.g. international coordinates return 404 from NWS)
        }

        // 2. Real-time meteorological threshold detection (for worldwide coverage)
        if (polledAlerts.isEmpty() && currentWeather != null) {
            val category = currentWeather.condition.category
            if (category == WeatherConditionCategory.THUNDERSTORM) {
                polledAlerts.add(
                    WeatherAlert(
                        id = "severe_storm_${location.name}_${Calendar.getInstance().get(Calendar.HOUR_OF_DAY)}",
                        event = "Severe Thunderstorm Warning",
                        severity = AlertSeverity.WARNING,
                        headline = "Damaging convective thunderstorms with frequent lightning & heavy squalls.",
                        description = "Radar indicates severe thunderstorms affecting ${location.displayName}. Damaging wind gusts up to 70 km/h and localized hail are possible.",
                        instruction = "Move to an interior room on the lowest floor of a sturdy building. Avoid windows and electrical appliances.",
                        area = location.displayName,
                        effectiveTime = "Active Now",
                        expiresTime = "Until Next Hour",
                        source = "Meteorological Early Warning System"
                    )
                )
            } else if (currentWeather.windSpeedKmh >= 45.0) {
                polledAlerts.add(
                    WeatherAlert(
                        id = "high_wind_${location.name}_${Calendar.getInstance().get(Calendar.HOUR_OF_DAY)}",
                        event = "High Wind Warning",
                        severity = AlertSeverity.WATCH,
                        headline = "Sustained high winds with gusts reaching ${Math.round(currentWeather.windSpeedKmh)} km/h.",
                        description = "Strong winds may down small tree branches and produce localized power interruptions in ${location.displayName}.",
                        instruction = "Secure loose outdoor furniture. Motorists should exercise caution, especially high-profile vehicles.",
                        area = location.displayName,
                        effectiveTime = "Active Now",
                        expiresTime = "Until 8:00 PM",
                        source = "Meteorological Early Warning System"
                    )
                )
            } else if (currentWeather.airQuality != null && currentWeather.airQuality.aqi >= 200) {
                polledAlerts.add(
                    WeatherAlert(
                        id = "hazardous_aqi_${location.name}_${Calendar.getInstance().get(Calendar.HOUR_OF_DAY)}",
                        event = "Hazardous Air Quality Alert",
                        severity = AlertSeverity.WARNING,
                        headline = "Very Unhealthy air pollution detected (US AQI ${currentWeather.airQuality.aqi}).",
                        description = "High particulate pollution concentrations pose acute health risks to both sensitive groups and the general public.",
                        instruction = "Avoid all outdoor physical exertion. Keep windows and doors closed. Use an indoor air filter if available.",
                        area = location.displayName,
                        effectiveTime = "Active Now",
                        expiresTime = "Until Tomorrow Morning",
                        source = "Environmental Air Quality Monitoring"
                    )
                )
            }
        }

        // Filter out previously dismissed alerts
        val activeNonDismissed = polledAlerts.filterNot { dismissedAlertIds.contains(it.id) }

        _uiState.update { it.copy(activeAlerts = activeNonDismissed) }

        // Trigger system notifications for active severe alerts
        activeNonDismissed.forEach { alert ->
            notificationManager?.postSevereAlertNotification(alert)
        }
    }

    fun dismissAlert(alertId: String) {
        dismissedAlertIds.add(alertId)
        _uiState.update { state ->
            state.copy(activeAlerts = state.activeAlerts.filterNot { it.id == alertId })
        }
    }

    /**
     * Demo / test action allowing users and testers to immediately verify
     * the severe weather alert banner and notification flow on demand.
     */
    fun triggerSimulatedAlert() {
        val currentLoc = _uiState.value.location
        val testAlert = WeatherAlert(
            id = "test_alert_${UUID.randomUUID()}",
            event = "Severe Thunderstorm Warning",
            severity = AlertSeverity.WARNING,
            headline = "Severe storm approaching ${currentLoc.name} with destructive wind gusts and hail.",
            description = "At ${SimpleDateFormat("h:mm a", Locale.US).format(Date())}, Doppler radar indicated a severe convective storm capable of producing 65+ km/h winds, torrential rain, and dangerous cloud-to-ground lightning in ${currentLoc.displayName}.",
            instruction = "Take shelter immediately in an interior room on the lowest floor of a sturdy building. Stay away from windows.",
            area = currentLoc.displayName,
            effectiveTime = "Active Now",
            expiresTime = "Expires in 1 hour",
            source = "National Weather Service (Active Alert)"
        )

        _uiState.update { state ->
            state.copy(activeAlerts = listOf(testAlert) + state.activeAlerts)
        }
        notificationManager?.postSevereAlertNotification(testAlert)
    }

    private fun formatAlertTime(isoStr: String?): String {
        if (isoStr.isNullOrBlank()) return "Active"
        return try {
            val parts = isoStr.split("T")
            if (parts.size > 1) {
                val timeParts = parts[1].split(":")
                val h = timeParts[0].toInt()
                val m = timeParts[1].toInt()
                val amPm = if (h >= 12) "PM" else "AM"
                val displayH = when {
                    h == 0 -> 12
                    h > 12 -> h - 12
                    else -> h
                }
                String.format(Locale.US, "%d:%02d %s", displayH, m, amPm)
            } else isoStr
        } catch (e: Exception) {
            isoStr
        }
    }

    // ----------------- Parsing Helpers -----------------

    private fun parseAirQuality(response: AirQualityResponse?): AirQualityUi? {
        val aqiVal = response?.current?.usAqi ?: return null
        val pm25 = response.current.pm25 ?: 0.0
        val pm10 = response.current.pm10 ?: 0.0

        val (category, advice) = when {
            aqiVal <= 50 -> "Good" to "Air quality is satisfactory with little or no health risk."
            aqiVal <= 100 -> "Moderate" to "Air quality is acceptable. Sensitive individuals should consider limiting prolonged outdoor exertion."
            aqiVal <= 150 -> "Unhealthy for Sensitive" to "Members of sensitive groups may experience health effects. General public is less affected."
            aqiVal <= 200 -> "Unhealthy" to "Everyone may begin to experience health effects; limit strenuous outdoor activities."
            aqiVal <= 300 -> "Very Unhealthy" to "Health alert: risk of health effects is increased for everyone. Avoid prolonged outdoor exertion."
            else -> "Hazardous" to "Emergency conditions: avoid all outdoor activities."
        }

        val progress = (aqiVal.toFloat() / 300f).coerceIn(0.05f, 1f)

        return AirQualityUi(
            aqi = aqiVal,
            category = category,
            advice = advice,
            pm25 = pm25,
            pm10 = pm10,
            progressFraction = progress
        )
    }

    private fun parseWeatherResponse(
        response: OpenMeteoResponse,
        airQualityUi: AirQualityUi? = null
    ): Triple<CurrentWeatherUi, List<HourlyForecastUi>, List<DailyForecastUi>> {
        val current = response.current
            ?: throw IllegalStateException("Current weather payload missing")

        val daily = response.daily
        val hourly = response.hourly

        val firstSunrise = daily?.sunrise?.firstOrNull() ?: ""
        val firstSunset = daily?.sunset?.firstOrNull() ?: ""
        val sunriseFormatted = formatTimeFromIso(firstSunrise)
        val sunsetFormatted = formatTimeFromIso(firstSunset)

        val daylightFraction = computeDaylightFraction(firstSunrise, firstSunset)

        val minToday = daily?.temperature2mMin?.firstOrNull() ?: (current.temperature2m - 5)
        val maxToday = daily?.temperature2mMax?.firstOrNull() ?: (current.temperature2m + 5)

        val condition = WeatherCodeMapper.mapCode(current.weatherCode, current.isDay == 1)

        val currentUi = CurrentWeatherUi(
            temperatureCelsius = current.temperature2m,
            feelsLikeCelsius = current.apparentTemperature,
            condition = condition,
            minTempCelsius = minToday,
            maxTempCelsius = maxToday,
            humidityPercent = Math.round(current.relativeHumidity2m).toInt(),
            windSpeedKmh = current.windSpeed10m,
            windDirectionDegrees = current.windDirection10m,
            surfacePressureHpa = current.surfacePressure,
            uvIndex = daily?.uvIndexMax?.firstOrNull() ?: 3.0,
            visibilityMeters = hourly?.visibility?.firstOrNull() ?: 10000.0,
            precipitationMm = current.precipitation ?: 0.0,
            sunriseIso = sunriseFormatted,
            sunsetIso = sunsetFormatted,
            daylightFraction = daylightFraction,
            airQuality = airQualityUi
        )

        // Hourly forecast parsing (next 24 hours starting from current time index)
        val hourlyList = mutableListOf<HourlyForecastUi>()
        if (hourly?.time != null && hourly.temperature2m != null && hourly.weatherCode != null) {
            val nowCalendar = Calendar.getInstance()
            val currentHourOfDay = nowCalendar.get(Calendar.HOUR_OF_DAY)

            val total = hourly.time.size
            var matchedStartIndex = 0

            for (i in 0 until total) {
                val iso = hourly.time[i]
                val hourInt = parseHourFromIso(iso)
                if (hourInt == currentHourOfDay) {
                    matchedStartIndex = i
                    break
                }
            }

            val endIndex = (matchedStartIndex + 24).coerceAtMost(total)
            for (i in matchedStartIndex until endIndex) {
                val isoTime = hourly.time[i]
                val isFirst = (i == matchedStartIndex)
                val timeLabel = if (isFirst) "Now" else formatHourLabel(isoTime)
                val temp = hourly.temperature2m[i]
                val code = hourly.weatherCode[i]
                val pop = hourly.precipitationProbability?.getOrNull(i) ?: 0
                val hum = hourly.relativeHumidity2m?.getOrNull(i)?.toInt() ?: 50
                val hourInt = parseHourFromIso(isoTime)
                val isHourDay = hourInt in 6..19

                hourlyList.add(
                    HourlyForecastUi(
                        timeDisplay = timeLabel,
                        temperatureCelsius = temp,
                        condition = WeatherCodeMapper.mapCode(code, isHourDay),
                        precipitationProbabilityPercent = pop,
                        isCurrentHour = isFirst,
                        humidityPercent = hum
                    )
                )
            }
        }

        // Daily forecast parsing (7 days)
        val dailyList = mutableListOf<DailyForecastUi>()
        if (daily?.time != null && daily.weatherCode != null && daily.temperature2mMax != null && daily.temperature2mMin != null) {
            val count = daily.time.size.coerceAtMost(7)
            for (i in 0 until count) {
                val dateStr = daily.time[i]
                val code = daily.weatherCode[i]
                val maxT = daily.temperature2mMax[i]
                val minT = daily.temperature2mMin[i]
                val pop = daily.precipitationProbabilityMax?.getOrNull(i) ?: 0
                val uv = daily.uvIndexMax?.getOrNull(i) ?: 3.0

                val dayName = if (i == 0) "Today" else formatDayOfWeek(dateStr)
                val dateDisplay = formatDateMonth(dateStr)

                dailyList.add(
                    DailyForecastUi(
                        dayName = dayName,
                        dateDisplay = dateDisplay,
                        condition = WeatherCodeMapper.mapCode(code, isDay = true),
                        minTempCelsius = minT,
                        maxTempCelsius = maxT,
                        precipitationProbabilityPercent = pop,
                        uvIndex = uv
                    )
                )
            }
        }

        return Triple(currentUi, hourlyList, dailyList)
    }

    private fun parseHourFromIso(iso: String): Int {
        return try {
            val parts = iso.split("T")
            if (parts.size > 1) {
                parts[1].split(":")[0].toIntOrNull() ?: 12
            } else 12
        } catch (e: Exception) {
            12
        }
    }

    private fun formatHourLabel(iso: String): String {
        return try {
            val hour = parseHourFromIso(iso)
            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            "$displayHour $amPm"
        } catch (e: Exception) {
            "12 PM"
        }
    }

    private fun formatTimeFromIso(iso: String): String {
        return try {
            val parts = iso.split("T")
            if (parts.size > 1) {
                val timeParts = parts[1].split(":")
                val hour = timeParts[0].toInt()
                val min = timeParts[1].toInt()
                val amPm = if (hour >= 12) "PM" else "AM"
                val displayHour = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                String.format(Locale.US, "%d:%02d %s", displayHour, min, amPm)
            } else "6:00 AM"
        } catch (e: Exception) {
            "6:00 AM"
        }
    }

    private fun formatDayOfWeek(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(dateStr) ?: return dateStr
            val out = SimpleDateFormat("EEE", Locale.US)
            out.format(date)
        } catch (e: Exception) {
            "Day"
        }
    }

    private fun formatDateMonth(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(dateStr) ?: return dateStr
            val out = SimpleDateFormat("MMM d", Locale.US)
            out.format(date)
        } catch (e: Exception) {
            ""
        }
    }

    private fun computeDaylightFraction(sunriseIso: String, sunsetIso: String): Float {
        return try {
            val now = Calendar.getInstance()
            val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

            val sunriseMinutes = parseMinutesFromIso(sunriseIso, default = 6 * 60)
            val sunsetMinutes = parseMinutesFromIso(sunsetIso, default = 18 * 60 + 30)

            if (currentMinutes < sunriseMinutes) 0f
            else if (currentMinutes > sunsetMinutes) 1f
            else {
                val duration = sunsetMinutes - sunriseMinutes
                if (duration <= 0) 0.5f
                else ((currentMinutes - sunriseMinutes).toFloat() / duration).coerceIn(0f, 1f)
            }
        } catch (e: Exception) {
            0.5f
        }
    }

    private fun parseMinutesFromIso(iso: String, default: Int): Int {
        return try {
            val parts = iso.split("T")
            if (parts.size > 1) {
                val timeParts = parts[1].split(":")
                val h = timeParts[0].toInt()
                val m = timeParts[1].toInt()
                h * 60 + m
            } else default
        } catch (e: Exception) {
            default
        }
    }

    private data class ShowcaseConfig(
        val cityName: String,
        val country: String,
        val tempC: Double,
        val desc: String,
        val windKmh: Double,
        val humidityPct: Int,
        val precipPct: Int,
        val minTemp: Double,
        val maxTemp: Double
    )

    /**
     * Instantly showcases any weather type with faithful atmospheric styling,
     * realistic hourly forecast pills, and metrics matching the user's reference mockup.
     */
    fun showcaseWeather(category: WeatherConditionCategory, isDay: Boolean = true) {
        val cfg = when (category) {
            WeatherConditionCategory.THUNDERSTORM -> ShowcaseConfig(
                cityName = "Sydney",
                country = "Australia",
                tempC = 23.0,
                desc = "Thunderstorm",
                windKmh = 60.0,
                humidityPct = 35,
                precipPct = 0,
                minTemp = 18.0,
                maxTemp = 24.0
            )
            WeatherConditionCategory.RAIN,
            WeatherConditionCategory.DRIZZLE -> ShowcaseConfig(
                cityName = "Tokyo",
                country = "Japan",
                tempC = 36.0,
                desc = "Low rain",
                windKmh = 87.0,
                humidityPct = 60,
                precipPct = 10,
                minTemp = 26.0,
                maxTemp = 37.0
            )
            WeatherConditionCategory.SNOW -> ShowcaseConfig(
                cityName = "New York",
                country = "United States",
                tempC = -8.0,
                desc = "Snow rain",
                windKmh = 24.0,
                humidityPct = 35,
                precipPct = 0,
                minTemp = -12.0,
                maxTemp = -6.0
            )
            WeatherConditionCategory.CLEAR -> if (isDay) {
                ShowcaseConfig(
                    cityName = "Los Angeles",
                    country = "United States",
                    tempC = 27.0,
                    desc = "Clear sky",
                    windKmh = 18.0,
                    humidityPct = 42,
                    precipPct = 0,
                    minTemp = 20.0,
                    maxTemp = 29.0
                )
            } else {
                ShowcaseConfig(
                    cityName = "Reykjavik",
                    country = "Iceland",
                    tempC = 4.0,
                    desc = "Clear night",
                    windKmh = 12.0,
                    humidityPct = 55,
                    precipPct = 0,
                    minTemp = 1.0,
                    maxTemp = 6.0
                )
            }
            WeatherConditionCategory.PARTLY_CLOUDY -> ShowcaseConfig(
                cityName = "San Francisco",
                country = "United States",
                tempC = 20.0,
                desc = "Partly cloudy",
                windKmh = 22.0,
                humidityPct = 52,
                precipPct = 5,
                minTemp = 15.0,
                maxTemp = 22.0
            )
            WeatherConditionCategory.CLOUDY -> ShowcaseConfig(
                cityName = "London",
                country = "United Kingdom",
                tempC = 15.0,
                desc = "Overcast",
                windKmh = 19.0,
                humidityPct = 68,
                precipPct = 15,
                minTemp = 11.0,
                maxTemp = 17.0
            )
            WeatherConditionCategory.HEAVY_RAIN -> ShowcaseConfig(
                cityName = "Singapore",
                country = "Singapore",
                tempC = 28.0,
                desc = "Heavy rain",
                windKmh = 45.0,
                humidityPct = 90,
                precipPct = 80,
                minTemp = 25.0,
                maxTemp = 29.0
            )
            WeatherConditionCategory.FOG -> ShowcaseConfig(
                cityName = "Seattle",
                country = "United States",
                tempC = 12.0,
                desc = "Foggy",
                windKmh = 8.0,
                humidityPct = 92,
                precipPct = 5,
                minTemp = 9.0,
                maxTemp = 14.0
            )
        }

        val condition = WeatherCondition(
            code = when (category) {
                WeatherConditionCategory.CLEAR -> 0
                WeatherConditionCategory.PARTLY_CLOUDY -> 2
                WeatherConditionCategory.CLOUDY -> 3
                WeatherConditionCategory.FOG -> 45
                WeatherConditionCategory.DRIZZLE,
                WeatherConditionCategory.RAIN -> 61
                WeatherConditionCategory.HEAVY_RAIN -> 65
                WeatherConditionCategory.THUNDERSTORM -> 95
                WeatherConditionCategory.SNOW -> 73
            },
            description = cfg.desc,
            category = category,
            isDay = isDay
        )

        val loc = WeatherLocation(
            name = cfg.cityName,
            region = cfg.country,
            latitude = 0.0,
            longitude = 0.0,
            country = cfg.country,
            isGpsLocation = false
        )

        val mockCurrent = CurrentWeatherUi(
            temperatureCelsius = cfg.tempC,
            feelsLikeCelsius = cfg.tempC - 1.0,
            condition = condition,
            minTempCelsius = cfg.minTemp,
            maxTempCelsius = cfg.maxTemp,
            humidityPercent = cfg.humidityPct,
            windSpeedKmh = cfg.windKmh,
            windDirectionDegrees = 160.0,
            surfacePressureHpa = 1012.0,
            uvIndex = if (isDay && category == WeatherConditionCategory.CLEAR) 8.2 else 2.5,
            visibilityMeters = if (category == WeatherConditionCategory.FOG) 800.0 else 10000.0,
            precipitationMm = cfg.precipPct.toDouble(),
            sunriseIso = "2026-09-24T06:15",
            sunsetIso = "2026-09-24T19:45",
            daylightFraction = if (isDay) 0.65f else 0.0f,
            airQuality = AirQualityUi(
                aqi = if (category == WeatherConditionCategory.FOG) 68 else 35,
                category = if (category == WeatherConditionCategory.FOG) "Moderate" else "Good",
                advice = "Atmospheric conditions are stable.",
                pm25 = 9.2,
                pm10 = 16.4,
                progressFraction = 0.15f
            )
        )

        // Mock 24-hour forecast with initial hours exactly matching the reference mockup:
        // Now, 2 PM, 3 PM, 4 PM, 5 PM, 6 PM, 7 PM with temps: 15°, 16°, 17°, 12°, 11°, 22°, 25°
        val referenceTemps = listOf(15.0, 16.0, 17.0, 12.0, 11.0, 22.0, 25.0)
        val referenceLabels = listOf("Now", "2 PM", "3 PM", "4 PM", "5 PM", "6 PM", "7 PM")

        val mockHourly = (0 until 24).map { idx ->
            val label = if (idx < referenceLabels.size) referenceLabels[idx] else "${(8 + idx) % 12 + 1} ${(if ((8 + idx) % 24 >= 12) "PM" else "AM")}"
            val temp = if (idx < referenceTemps.size) referenceTemps[idx] else (cfg.tempC + (idx % 4) - 2)
            val isCurrent = (idx == 0)

            HourlyForecastUi(
                timeDisplay = label,
                temperatureCelsius = temp,
                condition = condition,
                precipitationProbabilityPercent = cfg.precipPct,
                isCurrentHour = isCurrent,
                humidityPercent = cfg.humidityPct
            )
        }

        // Mock 7-day forecast
        val days = listOf("Today", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")
        val mockDaily = days.mapIndexed { idx, day ->
            DailyForecastUi(
                dayName = day,
                dateDisplay = "Sep ${24 + idx}",
                condition = condition,
                minTempCelsius = cfg.minTemp + (idx % 2),
                maxTempCelsius = cfg.maxTemp + (idx % 3),
                precipitationProbabilityPercent = cfg.precipPct,
                uvIndex = if (category == WeatherConditionCategory.CLEAR) 7.5 else 3.0
            )
        }

        _uiState.update {
            it.copy(
                location = loc,
                currentWeather = mockCurrent,
                hourlyForecast = mockHourly,
                dailyForecast = mockDaily,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    /**
     * Resets to live GPS location or default city
     */
    fun resetToLiveLocation(locationTracker: LocationTracker? = null) {
        if (locationTracker != null && locationTracker.hasLocationPermission()) {
            requestGpsLocation(locationTracker)
        } else {
            val initialLoc = _uiState.value.savedLocations.firstOrNull() ?: _uiState.value.location
            loadWeatherForLocation(initialLoc)
        }
    }
}
