package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.data.location.LocationTracker
import com.example.data.notification.WeatherNotificationManager
import com.example.ui.WeatherScreen
import com.example.ui.WeatherViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: WeatherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val locationTracker = remember { LocationTracker(context) }
                val notificationManager = remember { WeatherNotificationManager(context.applicationContext) }
                val uiState by viewModel.uiState.collectAsState()

                // Register notification manager with ViewModel
                LaunchedEffect(notificationManager) {
                    viewModel.setNotificationManager(notificationManager)
                }

                // Notification Permission Launcher (Android 13+)
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                // Runtime Location Permission Launcher
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    val isGranted = fineGranted || coarseGranted
                    viewModel.onPermissionResult(isGranted, locationTracker)
                }

                // Check permissions on initial launch
                LaunchedEffect(Unit) {
                    if (locationTracker.hasLocationPermission()) {
                        viewModel.onPermissionResult(true, locationTracker)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                WeatherScreen(
                    uiState = uiState,
                    onRefresh = { viewModel.refreshWeather() },
                    onToggleUnit = { viewModel.toggleTemperatureUnit() },
                    onSearchQueryChange = { query -> viewModel.onSearchQueryChanged(query) },
                    onSelectLocation = { location -> viewModel.selectLocation(location) },
                    onUseCurrentLocation = {
                        if (locationTracker.hasLocationPermission()) {
                            viewModel.requestGpsLocation(locationTracker)
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onRemoveSavedLocation = { location -> viewModel.removeSavedLocation(location) },
                    onRequestLocationPermission = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onDismissPermissionPrompt = {
                        viewModel.dismissPermissionPrompt()
                    },
                    onDismissAlert = { alertId ->
                        viewModel.dismissAlert(alertId)
                    },
                    onTriggerTestAlert = {
                        viewModel.triggerSimulatedAlert()
                    },
                    onSelectWeatherType = { category, isDay ->
                        viewModel.showcaseWeather(category, isDay)
                    },
                    onResetToLive = {
                        viewModel.resetToLiveLocation(locationTracker)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
