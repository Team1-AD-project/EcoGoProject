package com.ecogo.mapengine.utils

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

/**
 * Map Utility Class
 */
object MapUtils {

    /**
     * Calculate distance between two points (meters)
     * Using Haversine formula
     */
    fun calculateDistance(start: LatLng, end: LatLng): Double {
        val earthRadius = 6371000.0 // Earth radius (meters)

        val lat1 = Math.toRadians(start.latitude)
        val lat2 = Math.toRadians(end.latitude)
        val deltaLat = Math.toRadians(end.latitude - start.latitude)
        val deltaLng = Math.toRadians(end.longitude - start.longitude)

        val a = sin(deltaLat / 2).pow(2) +
                cos(lat1) * cos(lat2) * sin(deltaLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * Calculate total route distance (meters)
     */
    fun calculateTotalDistance(points: List<LatLng>): Double {
        if (points.size < 2) return 0.0

        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            totalDistance += calculateDistance(points[i], points[i + 1])
        }
        return totalDistance
    }

    /**
     * Convert meters to kilometers
     */
    fun metersToKilometers(meters: Double): Double {
        return meters / 1000.0
    }

    /**
     * Format distance for display
     */
    fun formatDistance(meters: Double): String {
        return if (meters < 1000) {
            "${meters.toInt()} m"
        } else {
            String.format("%.2f km", meters / 1000)
        }
    }

    /**
     * Estimate carbon emission (kg)
     * Based on transport mode and distance
     */
    fun estimateCarbonEmission(distanceKm: Double, transportMode: String): Double {
        // Carbon emission factor (kg CO2/km)
        val carbonFactor = when (transportMode) {
            "walk" -> 0.0
            "bike" -> 0.0
            "bus" -> 0.089
            "subway" -> 0.041
            "car" -> 0.21
            else -> 0.0
        }
        return distanceKm * carbonFactor
    }

    /**
     * Calculate carbon reduction (compared to driving)
     */
    fun calculateCarbonSaved(distanceKm: Double, transportMode: String): Double {
        val drivingEmission = estimateCarbonEmission(distanceKm, "car")
        val actualEmission = estimateCarbonEmission(distanceKm, transportMode)
        return drivingEmission - actualEmission
    }

    /**
     * Convert Location to LatLng
     */
    fun locationToLatLng(location: Location): LatLng {
        return LatLng(location.latitude, location.longitude)
    }
}
