package com.ecogo.utils

/**
 * Map utility class
 * Note: Map functionality is temporarily disabled; Google Maps SDK configuration required
 */
object MapUtils {

    /**
     * Format duration (seconds to minutes or hours)
     */
    fun formatDuration(seconds: Int): String {
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}min"
            else -> {
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                if (minutes > 0) {
                    "${hours}h ${minutes}min"
                } else {
                    "${hours}h"
                }
            }
        }
    }

    /**
     * Format distance (meters to meters or kilometers)
     */
    fun formatDistance(meters: Double): String {
        return if (meters < 1000) {
            "${meters.toInt()}m"
        } else {
            String.format("%.1fkm", meters / 1000)
        }
    }
}
