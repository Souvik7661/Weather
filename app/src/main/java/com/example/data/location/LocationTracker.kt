package com.example.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.example.data.model.WeatherLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class LocationTracker(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocation || coarseLocation
    }

    suspend fun getCurrentLocation(): WeatherLocation? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) {
            return@withContext null
        }

        val location: Location? = try {
            // Priority 1: High Accuracy GPS (uses true GPS satellites + WiFi triangulation)
            withTimeoutOrNull(7000) {
                getCurrentLocationFromClient(Priority.PRIORITY_HIGH_ACCURACY)
            } ?: withTimeoutOrNull(4000) {
                // Priority 2: Balanced Power Accuracy fallback
                getCurrentLocationFromClient(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            } ?: getLastKnownLocationFromClient()
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }

        if (location == null) {
            return@withContext null
        }

        val geocoded = resolveGeocodedLocation(location.latitude, location.longitude)
        geocoded ?: WeatherLocation(
            name = "Current Location",
            region = String.format(Locale.US, "%.3f, %.3f", location.latitude, location.longitude),
            country = "",
            latitude = location.latitude,
            longitude = location.longitude,
            isGpsLocation = true
        )
    }

    private suspend fun getCurrentLocationFromClient(priority: Int): Location? =
        suspendCancellableCoroutine { continuation ->
            try {
                val cts = CancellationTokenSource()
                continuation.invokeOnCancellation { cts.cancel() }

                fusedLocationClient.getCurrentLocation(
                    priority,
                    cts.token
                ).addOnSuccessListener { loc ->
                    continuation.resume(loc)
                }.addOnFailureListener {
                    continuation.resume(null)
                }
            } catch (e: SecurityException) {
                continuation.resume(null)
            }
        }

    private suspend fun getLastKnownLocationFromClient(): Location? =
        suspendCancellableCoroutine { continuation ->
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { loc ->
                        continuation.resume(loc)
                    }
                    .addOnFailureListener {
                        continuation.resume(null)
                    }
            } catch (e: SecurityException) {
                continuation.resume(null)
            }
        }

    private fun resolveGeocodedLocation(latitude: Double, longitude: Double): WeatherLocation? {
        // 1. Try Android Native Geocoder
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val cityName = address.locality
                        ?: address.subAdminArea
                        ?: address.adminArea
                        ?: address.featureName
                    val region = address.adminArea ?: ""
                    val country = address.countryName ?: ""
                    if (!cityName.isNullOrBlank()) {
                        return WeatherLocation(
                            name = cityName,
                            region = region,
                            country = country,
                            latitude = latitude,
                            longitude = longitude,
                            isGpsLocation = true
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Geocoder service failed or unavailable
        }

        // 2. High-reliability HTTP Reverse Geocoding fallback (BigDataCloud free client API)
        try {
            val url = String.format(
                Locale.US,
                "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=%.6f&longitude=%.6f&localityLanguage=en",
                latitude,
                longitude
            )
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string()
                if (!bodyStr.isNullOrBlank()) {
                    val json = JSONObject(bodyStr)
                    val city = json.optString("city").takeIf { it.isNotBlank() }
                        ?: json.optString("locality").takeIf { it.isNotBlank() }
                        ?: json.optString("principalSubdivision").takeIf { it.isNotBlank() }
                    val region = json.optString("principalSubdivision", "")
                    val country = json.optString("countryName", "")

                    if (!city.isNullOrBlank()) {
                        return WeatherLocation(
                            name = city,
                            region = region,
                            country = country,
                            latitude = latitude,
                            longitude = longitude,
                            isGpsLocation = true
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // HTTP fallback failed
        }

        return null
    }
}
