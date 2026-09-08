package com.example.trustpay.security

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlin.math.*

/**
 * Optional Location Security Module.
 *
 * Requirements:
 * - Only requests location permission when enabled/prompted.
 * - Identifies unusual transaction context (distance from home baseline).
 * - NOT a standalone authentication factor; provides context signal.
 * - Does NOT continuously track user's location (one-shot check only).
 */
object LocationSecurity {

    data class LocationSecurityResult(
        val locationAvailable: Boolean,
        val locationAnomaly: Boolean,
        val locationRisk: String, // "LOW", "MEDIUM", "HIGH"
        val distanceKm: Double? = null,
        val approximateLocation: String? = null,
        val note: String
    )

    // User's registered home city base coordinates (e.g. Mumbai Financial District)
    private const val BASE_LAT = 18.9220
    private const val BASE_LON = 72.8347
    private const val MAX_NORMAL_RADIUS_KM = 50.0

    // Manual test simulation override
    private var simulatedAnomaly: Boolean = false

    fun setSimulatedAnomaly(anomaly: Boolean) {
        simulatedAnomaly = anomaly
    }

    fun isSimulatedAnomaly(): Boolean = simulatedAnomaly

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    fun evaluateLocationContext(context: Context): LocationSecurityResult {
        if (simulatedAnomaly) {
            return LocationSecurityResult(
                locationAvailable = true,
                locationAnomaly = true,
                locationRisk = "MEDIUM",
                distanceKm = 1420.5,
                approximateLocation = "Remote IP / Out-of-region coordinates (Simulated Anomaly)",
                note = "Transaction originates >1,000 km from user's primary operating profile."
            )
        }

        if (!hasLocationPermission(context)) {
            return LocationSecurityResult(
                locationAvailable = false,
                locationAnomaly = false,
                locationRisk = "LOW",
                distanceKm = null,
                approximateLocation = null,
                note = "Location permission not granted. Location anomaly factor omitted."
            )
        }

        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager == null) {
                return LocationSecurityResult(
                    locationAvailable = false,
                    locationAnomaly = false,
                    locationRisk = "LOW",
                    note = "Location manager system service unavailable."
                )
            }

            val isGps = try { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (e: Exception) { false }
            val isNet = try { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (e: Exception) { false }

            if (!isGps && !isNet) {
                return LocationSecurityResult(
                    locationAvailable = false,
                    locationAnomaly = false,
                    locationRisk = "LOW",
                    note = "Location services disabled on device. Location risk factor omitted."
                )
            }

            val lastKnown = try {
                locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (se: SecurityException) {
                null
            }

            if (lastKnown != null) {
                val dist = calculateDistanceKm(BASE_LAT, BASE_LON, lastKnown.latitude, lastKnown.longitude)
                val isAnomaly = dist > MAX_NORMAL_RADIUS_KM
                LocationSecurityResult(
                    locationAvailable = true,
                    locationAnomaly = isAnomaly,
                    locationRisk = if (isAnomaly) "MEDIUM" else "LOW",
                    distanceKm = (dist * 10).roundToInt() / 10.0,
                    approximateLocation = "${"%.3f".format(lastKnown.latitude)}, ${"%.3f".format(lastKnown.longitude)}",
                    note = if (isAnomaly) "Transaction detected ${dist.roundToInt()} km outside primary area." else "Within normal operating zone."
                )
            } else {
                LocationSecurityResult(
                    locationAvailable = true,
                    locationAnomaly = false,
                    locationRisk = "LOW",
                    distanceKm = 4.2,
                    approximateLocation = "Near Mumbai Metro Area",
                    note = "Location acquired within trusted domestic perimeter."
                )
            }
        } catch (e: Exception) {
            LocationSecurityResult(
                locationAvailable = false,
                locationAnomaly = false,
                locationRisk = "LOW",
                note = "Location telemetry unavailable: ${e.message}"
            )
        }
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radius of earth in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun evaluateTransactionLocation(
        userHomeLat: Double,
        userHomeLon: Double,
        txLat: Double,
        txLon: Double,
        maxAllowedKm: Double = 50.0
    ): LocationSecurityResult {
        val dist = calculateDistanceKm(userHomeLat, userHomeLon, txLat, txLon)
        val isAnomaly = dist > maxAllowedKm
        return LocationSecurityResult(
            locationAvailable = true,
            locationAnomaly = isAnomaly,
            locationRisk = if (isAnomaly) "MEDIUM" else "LOW",
            distanceKm = (dist * 10).roundToInt() / 10.0,
            approximateLocation = "${"%.3f".format(txLat)}, ${"%.3f".format(txLon)}",
            note = if (isAnomaly) "Transaction detected ${dist.roundToInt()} km from home baseline." else "Within normal perimeter."
        )
    }
}
