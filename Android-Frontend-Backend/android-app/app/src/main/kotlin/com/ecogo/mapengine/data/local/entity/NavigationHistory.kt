package com.ecogo.mapengine.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Navigation history entity
 * Stores completed navigation route data for the user
 */
@Entity(tableName = "navigation_history")
@TypeConverters(NavigationHistoryConverters::class)
data class NavigationHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Trip basic info
    val tripId: String? = null,                    // Trip ID (if backend record exists)
    val userId: String? = null,                    // User ID
    val startTime: Long,                           // Start time (timestamp)
    val endTime: Long,                             // End time (timestamp)
    val durationSeconds: Int,                      // Trip duration (seconds)

    // Location info
    val originLat: Double,                         // Origin latitude
    val originLng: Double,                         // Origin longitude
    val originName: String,                        // Origin name
    val destinationLat: Double,                    // Destination latitude
    val destinationLng: Double,                    // Destination longitude
    val destinationName: String,                   // Destination name

    // Route info
    val routePoints: String,                       // Route point list (JSON format)
    val trackPoints: String,                       // Actual track point list (JSON format)
    val totalDistance: Double,                     // Total distance (meters)
    val traveledDistance: Double,                  // Actual traveled distance (meters)

    // Transport mode
    val transportMode: String,                     // Primary transport mode
    val detectedMode: String? = null,              // AI-detected transport mode

    // Environmental data
    val totalCarbon: Double,                       // Total carbon emission (kg)
    val carbonSaved: Double,                       // Carbon emission saved (kg)
    val isGreenTrip: Boolean,                      // Whether it is a green trip
    val greenPoints: Int = 0,                      // Green points earned

    // Route type
    val routeType: String? = null,                 // Route type (low_carbon, balanced)

    // Notes
    val notes: String? = null                      // User notes
)

/**
 * Simplified version of navigation history (for list display)
 */
data class NavigationHistorySummary(
    val id: Long,
    val startTime: Long,
    val originName: String,
    val destinationName: String,
    val totalDistance: Double,
    val transportMode: String,
    val carbonSaved: Double,
    val durationSeconds: Int
)

/**
 * 类型转换器（用于Room存储复杂类型）
 */
class NavigationHistoryConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromLatLngList(value: String): List<LatLng> {
        val type = object : TypeToken<List<Map<String, Double>>>() {}.type
        val list: List<Map<String, Double>> = gson.fromJson(value, type)
        return list.map { LatLng(it["lat"] ?: 0.0, it["lng"] ?: 0.0) }
    }

    @TypeConverter
    fun toLatLngList(list: List<LatLng>): String {
        val simplified = list.map { mapOf("lat" to it.latitude, "lng" to it.longitude) }
        return gson.toJson(simplified)
    }
}
