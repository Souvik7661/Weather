package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
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
import com.example.data.model.WeatherConditionCategory
import com.example.data.model.WeatherLocation

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WeatherMenuDrawer(
    sheetState: SheetState,
    searchQuery: String,
    searchResults: List<WeatherLocation>,
    savedLocations: List<WeatherLocation>,
    isSearching: Boolean,
    onDismiss: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectLocation: (WeatherLocation) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onSelectWeatherShowcase: (WeatherConditionCategory, Boolean) -> Unit,
    onResetToLive: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x55FFFFFF))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Locations & Weather Conditions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 1. Current GPS Location button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        onUseCurrentLocation()
                        onDismiss()
                    },
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF0284C7).copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "GPS",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Use Current Location",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Accurate GPS & high-precision local weather",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. City Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        text = "Search any city in the world...",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            color = Color(0xFF38BDF8),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Search results list
            if (searchResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    searchResults.forEach { loc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectLocation(loc)
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = loc.displayName,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Featured Cities from mockup
            Text(
                text = "Featured Cities (Reference Mockup)",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeaturedCityChip(name = "Sydney", sub = "Thunderstorm") {
                    onSelectWeatherShowcase(WeatherConditionCategory.THUNDERSTORM, true)
                    onDismiss()
                }
                FeaturedCityChip(name = "Tokyo", sub = "Low rain") {
                    onSelectWeatherShowcase(WeatherConditionCategory.RAIN, true)
                    onDismiss()
                }
                FeaturedCityChip(name = "New York", sub = "Snow rain") {
                    onSelectWeatherShowcase(WeatherConditionCategory.SNOW, true)
                    onDismiss()
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 4. Live Atmospheric Animations Showcase
            Text(
                text = "Animated Weather Conditions",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Tap any weather condition to preview its full cinematic animation",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeatherConditionChip("🌧 Low rain", WeatherConditionCategory.RAIN, isDay = true) {
                    onSelectWeatherShowcase(WeatherConditionCategory.RAIN, true)
                    onDismiss()
                }
                WeatherConditionChip("⛈ Thunderstorm", WeatherConditionCategory.THUNDERSTORM, isDay = true) {
                    onSelectWeatherShowcase(WeatherConditionCategory.THUNDERSTORM, true)
                    onDismiss()
                }
                WeatherConditionChip("❄ Snow rain", WeatherConditionCategory.SNOW, isDay = true) {
                    onSelectWeatherShowcase(WeatherConditionCategory.SNOW, true)
                    onDismiss()
                }
                WeatherConditionChip("☀️ Clear sky", WeatherConditionCategory.CLEAR, isDay = true) {
                    onSelectWeatherShowcase(WeatherConditionCategory.CLEAR, true)
                    onDismiss()
                }
                WeatherConditionChip("🌙 Clear night", WeatherConditionCategory.CLEAR, isDay = false) {
                    onSelectWeatherShowcase(WeatherConditionCategory.CLEAR, false)
                    onDismiss()
                }
                WeatherConditionChip("⛅ Partly cloudy", WeatherConditionCategory.PARTLY_CLOUDY, isDay = true) {
                    onSelectWeatherShowcase(WeatherConditionCategory.PARTLY_CLOUDY, true)
                    onDismiss()
                }
                WeatherConditionChip("☁️ Overcast", WeatherConditionCategory.CLOUDY, isDay = true) {
                    onSelectWeatherShowcase(WeatherConditionCategory.CLOUDY, true)
                    onDismiss()
                }
                WeatherConditionChip("🌫️ Foggy", WeatherConditionCategory.FOG, isDay = true) {
                    onSelectWeatherShowcase(WeatherConditionCategory.FOG, true)
                    onDismiss()
                }
                WeatherConditionChip("🌊 Heavy rain", WeatherConditionCategory.HEAVY_RAIN, isDay = true) {
                    onSelectWeatherShowcase(WeatherConditionCategory.HEAVY_RAIN, true)
                    onDismiss()
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun FeaturedCityChip(
    name: String,
    sub: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = Color(0xFF1E293B)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = sub,
                color = Color(0xFF38BDF8),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun WeatherConditionChip(
    label: String,
    category: WeatherConditionCategory,
    isDay: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(BorderStroke(1.dp, Color(0x2BFFFFFF)), RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = Color(0xFF162032)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
