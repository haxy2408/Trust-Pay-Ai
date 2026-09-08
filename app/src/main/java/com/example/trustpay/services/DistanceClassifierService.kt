package com.example.trustpay.services

import com.example.trustpay.model.ClassifiedLocation
import com.example.trustpay.model.DistanceCategory
import com.example.trustpay.model.TargetLocation
import kotlin.math.*

object DistanceClassifierService {

    const val EARTH_RADIUS_KM = 6371.0

    /**
     * Calculates great-circle distance between two geographic coordinates using the Haversine formula.
     */
    fun calculateHaversineDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        if (lat1 == lat2 && lon1 == lon2) return 0.0

        val toRad = { deg: Double -> deg * Math.PI / 180.0 }
        val dLat = toRad(lat2 - lat1)
        val dLon = toRad(lon2 - lon1)

        val rLat1 = toRad(lat1)
        val rLat2 = toRad(lat2)

        val a = sin(dLat / 2).pow(2) + cos(rLat1) * cos(rLat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = EARTH_RADIUS_KM * c

        return round(distance * 100.0) / 100.0
    }

    /**
     * Classifies distance strictly according to rules:
     * - 0–10 km: "Usual distance" (Green)
     * - >10 to 20 km: "Medium distance" (Yellow)
     * - >20 to 30 km: "Far distance" (Orange)
     * - >30 km: "High distance" (Red)
     */
    fun classifyDistance(distanceKm: Double): Pair<DistanceCategory, String> {
        return when {
            distanceKm <= 10.0 -> Pair(DistanceCategory.USUAL, "GREEN")
            distanceKm <= 20.0 -> Pair(DistanceCategory.MEDIUM, "YELLOW")
            distanceKm <= 30.0 -> Pair(DistanceCategory.FAR, "ORANGE")
            else -> Pair(DistanceCategory.HIGH, "RED")
        }
    }

    fun formatDistanceDisplay(distanceKm: Double, category: DistanceCategory): Pair<String, String> {
        val isWhole = abs(distanceKm - round(distanceKm)) < 0.05
        val formattedDistance = if (isWhole) {
            "${round(distanceKm).toInt()} km"
        } else {
            "${String.format(java.util.Locale.US, "%.1f", distanceKm)} km"
        }
        val displayLabel = "$formattedDistance — ${category.label}"
        return Pair(formattedDistance, displayLabel)
    }

    fun classifyLocation(
        userLat: Double,
        userLon: Double,
        location: TargetLocation
    ): ClassifiedLocation {
        val distKm = calculateHaversineDistanceKm(userLat, userLon, location.latitude, location.longitude)
        val (category, colorIndicator) = classifyDistance(distKm)
        val (formatted, label) = formatDistanceDisplay(distKm, category)

        return ClassifiedLocation(
            location = location,
            distanceKm = distKm,
            formattedDistance = formatted,
            displayLabel = label,
            category = category,
            colorIndicator = colorIndicator
        )
    }
}
