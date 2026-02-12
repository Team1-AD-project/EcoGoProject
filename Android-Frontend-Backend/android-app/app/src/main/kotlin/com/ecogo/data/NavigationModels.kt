package com.ecogo.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Location type enum
 */
enum class LocationType {
    FACULTY,      // College
    CANTEEN,      // Cafeteria
    LIBRARY,      // Library
    RESIDENCE,    // Dormitory
    FACILITY,     // Facility
    BUS_STOP,     // Bus stop
    OTHER         // Other
}

/**
 * Transport mode enum
 */
enum class TransportMode {
    WALK,         // Walking
    CYCLE,        // Cycling
    BUS,          // Bus/Transit
    MIXED         // Mixed
}

/**
 * Trip status enum
 */
enum class TripStatus {
    PLANNING,     // Planning
    ACTIVE,       // In progress
    PAUSED,       // Paused
    COMPLETED,    // Completed
    CANCELLED     // Cancelled
}

/**
 * Location model
 */
@Parcelize
data class NavLocation(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val type: LocationType,
    val icon: String,
    val isFavorite: Boolean = false,
    val visitCount: Int = 0,
    val lastVisitTime: Long = 0
) : Parcelable

/**
 * Route step
 */
@Parcelize
data class RouteStep(
    val instruction: String,
    val distance: Double,        // meters
    val duration: Int,            // seconds
    val mode: TransportMode,
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double,
    val polyline: String = ""
) : Parcelable

/**
 * Route model
 */
@Parcelize
data class NavRoute(
    val id: String,
    val origin: NavLocation,
    val destination: NavLocation,
    val mode: TransportMode,
    val distance: Double,         // kilometers
    val duration: Int,            // minutes
    val carbonEmission: Double,   // g CO2
    val carbonSaved: Double,      // g CO2
    val points: Int,              // green points
    val steps: List<RouteStep>,
    val polyline: String,
    val isRecommended: Boolean = false,
    val badge: String = ""        // Label: most eco-friendly, fastest, etc.
) : Parcelable

/**
 * Trip record
 */
@Parcelize
data class Trip(
    val id: String,
    val route: NavRoute,
    val startTime: Long,
    val endTime: Long? = null,
    val status: TripStatus,
    val actualDistance: Double = 0.0,
    val actualCarbonSaved: Double = 0.0,
    val pointsEarned: Int = 0,
    val achievementUnlocked: String? = null
) : Parcelable

/**
 * Bus information
 */
@Parcelize
data class BusInfo(
    val busId: String,
    val routeName: String,
    val destination: String,
    val currentLat: Double,
    val currentLng: Double,
    val etaMinutes: Int,
    val stopsAway: Int,
    val crowdLevel: String,      // Low, Medium, High
    val plateNumber: String,
    val status: String,           // arriving, coming, delayed
    val color: String = "#DB2777"
) : Parcelable

/**
 * Map settings
 */
@Parcelize
data class MapSettings(
    val preferEcoRoute: Boolean = true,
    val avoidStairs: Boolean = false,
    val preferIndoor: Boolean = true,
    val showBusStops: Boolean = true,
    val showBikePaths: Boolean = true,
    val showGreenRoutes: Boolean = true,
    val showCrowdData: Boolean = false,
    val show3DBuildings: Boolean = false,
    val showTraffic: Boolean = true
) : Parcelable

/**
 * Navigation state
 */
enum class NavigationState {
    IDLE,         // Idle
    SEARCHING,    // Searching
    PLANNING,     // Planning
    NAVIGATING,   // Navigating
    COMPLETED     // Completed
}

/**
 * Search history
 */
@Parcelize
data class SearchHistory(
    val query: String,
    val location: NavLocation?,
    val timestamp: Long
) : Parcelable

/**
 * Route option comparison
 */
@Parcelize
data class RouteOption(
    val route: NavRoute,
    val carbonComparison: Double,  // Carbon emissions saved compared to driving
    val moneySaved: Double,         // Money saved
    val healthBenefit: String       // Health benefit description
) : Parcelable
