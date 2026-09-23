package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WeatherConditionCategory
import com.example.data.model.WeatherLocation
import com.example.ui.components.AccentCyan
import com.example.ui.components.AirQualityCard
import com.example.ui.components.AtmosphericWeatherCanvas
import com.example.ui.components.DailyForecastSection
import com.example.ui.components.DailyUvIndexGaugeCard
import com.example.ui.components.Forecast24HourPill
import com.example.ui.components.HourlyHumidityRechartsCard
import com.example.ui.components.MoonPhaseCard
import com.example.ui.components.SevereWeatherAlertBanner
import com.example.ui.components.TextSecondaryColor
import com.example.ui.components.WeatherAtmosphericMetrics
import com.example.ui.components.WeatherHeroDisplay
import com.example.ui.components.WeatherMenuDrawer
import com.example.ui.components.WeatherSafetyCard
import com.example.ui.components.WeatherSettingsSheet
import com.example.ui.components.WeatherTopHeader
import com.example.ui.components.getAtmosphericGradient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    uiState: WeatherUiState,
    onRefresh: () -> Unit,
    onToggleUnit: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectLocation: (WeatherLocation) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onRemoveSavedLocation: (WeatherLocation) -> Unit,
    onRequestLocationPermission: () -> Unit,
    onDismissPermissionPrompt: () -> Unit,
    onDismissAlert: (String) -> Unit = {},
    onTriggerTestAlert: () -> Unit = {},
    onSelectWeatherType: (WeatherConditionCategory, Boolean) -> Unit = { _, _ -> },
    onResetToLive: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenuDrawer by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val menuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val currentCategory = uiState.currentWeather?.condition?.category ?: WeatherConditionCategory.CLEAR
    val isDay = uiState.currentWeather?.condition?.isDay ?: true

    // Dynamic atmospheric gradient matching each weather condition
    val backgroundBrush = remember(currentCategory, isDay) {
        getAtmosphericGradient(currentCategory, isDay)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        // Fullscreen atmospheric weather animations (Tokyo glass raindrops, Sydney lightning, New York snow, sun, stars, fog)
        AtmosphericWeatherCanvas(
            category = currentCategory,
            isDay = isDay,
            modifier = Modifier.fillMaxSize()
        )

        Scaffold(
            containerColor = Color.Transparent,
            contentColor = Color.White
        ) { innerPadding ->
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(top = statusBarPadding + 4.dp, bottom = navBarPadding + 32.dp)
            ) {
                // 1. Top Header: Quad Grid Plus (Menu) + Centered City with GPS + Hexagon Dot (Settings)
                WeatherTopHeader(
                    location = uiState.location,
                    onOpenMenu = { showMenuDrawer = true },
                    onOpenSettings = { showSettingsSheet = true }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Active Severe Weather Alert Banner if any
                AnimatedVisibility(
                    visible = uiState.activeAlerts.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        uiState.activeAlerts.forEach { alert ->
                            SevereWeatherAlertBanner(
                                alert = alert,
                                onDismiss = { onDismissAlert(alert.id) }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                if (uiState.isLoading && uiState.currentWeather == null) {
                    // Initial Loading Screen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                color = AccentCyan,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(46.dp)
                            )
                            Text(
                                text = "Acquiring live GPS & satellite weather...",
                                color = TextSecondaryColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else if (uiState.currentWeather != null) {
                    // 3. Safety Reminder Card matching the reference mockup
                    WeatherSafetyCard(
                        category = currentCategory,
                        isDay = isDay
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Hero Temperature and Condition display (e.g. 23° Thunderstorm, 36° Low rain, -8° Snow rain)
                    WeatherHeroDisplay(
                        currentWeather = uiState.currentWeather,
                        unit = uiState.temperatureUnit,
                        onToggleUnit = onToggleUnit
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 5. 3-Column Atmospheric Metrics Row (Wind: 60 km, Humidity: 35 %, Precip: 0 %)
                    val currentPrecipProb = uiState.hourlyForecast.firstOrNull()?.precipitationProbabilityPercent
                        ?: Math.round(uiState.currentWeather.precipitationMm).toInt()

                    WeatherAtmosphericMetrics(
                        currentWeather = uiState.currentWeather,
                        unit = uiState.temperatureUnit,
                        precipitationProbability = currentPrecipProb
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 6. 24-Hour Forecast Pill Container (3 rows: Temperatures, Weather Icons, Hours)
                    Forecast24HourPill(
                        hourlyList = uiState.hourlyForecast,
                        unit = uiState.temperatureUnit
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // 7. Extended Details (7-Day Forecast, Air Quality, Humidity Curves, UV Index)
                    if (uiState.dailyForecast.isNotEmpty()) {
                        DailyForecastSection(
                            dailyList = uiState.dailyForecast,
                            unit = uiState.temperatureUnit
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (uiState.currentWeather.airQuality != null) {
                        AirQualityCard(airQuality = uiState.currentWeather.airQuality)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (uiState.hourlyForecast.isNotEmpty()) {
                        HourlyHumidityRechartsCard(
                            hourlyList = uiState.hourlyForecast,
                            currentWeather = uiState.currentWeather,
                            unit = uiState.temperatureUnit,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    DailyUvIndexGaugeCard(
                        uvIndex = uiState.currentWeather.uvIndex,
                        maxDailyUv = uiState.dailyForecast.firstOrNull()?.uvIndex ?: uiState.currentWeather.uvIndex,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    MoonPhaseCard(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Footer branding
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Accurate GPS & Live Atmospheric Data • Open-Meteo",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Location & Condition Showcase Drawer (opens from top-left icon)
        if (showMenuDrawer) {
            WeatherMenuDrawer(
                sheetState = menuSheetState,
                searchQuery = uiState.searchQuery,
                searchResults = uiState.searchResults,
                savedLocations = uiState.savedLocations,
                isSearching = uiState.isSearching,
                onDismiss = { showMenuDrawer = false },
                onSearchQueryChange = onSearchQueryChange,
                onSelectLocation = { loc ->
                    showMenuDrawer = false
                    onSelectLocation(loc)
                },
                onUseCurrentLocation = {
                    showMenuDrawer = false
                    onUseCurrentLocation()
                },
                onSelectWeatherShowcase = { category, isDayTime ->
                    showMenuDrawer = false
                    onSelectWeatherType(category, isDayTime)
                },
                onResetToLive = {
                    showMenuDrawer = false
                    onResetToLive()
                }
            )
        }

        // Settings Sheet (opens from top-right icon)
        if (showSettingsSheet) {
            WeatherSettingsSheet(
                sheetState = settingsSheetState,
                unit = uiState.temperatureUnit,
                location = uiState.location,
                onDismiss = { showSettingsSheet = false },
                onToggleUnit = onToggleUnit,
                onRefresh = onRefresh,
                onUseCurrentLocation = {
                    showSettingsSheet = false
                    onUseCurrentLocation()
                }
            )
        }
    }
}
