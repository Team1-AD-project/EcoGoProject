package com.ecogo.mapengine.data.model

/**
 * ========================================
 * Trip tracking related models
 * Corresponding to TripService microservice API
 * ========================================
 */

/**
 * Start trip tracking request
 * POST /api/mobile/trips/track
 */
data class TripTrackRequest(
    val user_id: String,
    val start_point: GeoPoint,
    val start_location: LocationInfo? = null
)

/**
 * Start trip tracking response
 */
data class TripTrackData(
    val trip_id: String,
    val status: String = "tracking",
    val start_time: String,
    val message: String? = null
)

/**
 * Cancel trip request
 * PUT /api/mobile/trips/{trip_id}/cancel
 */
data class TripCancelRequest(
    val user_id: String,
    val cancel_reason: String? = null
)

/**
 * Cancel trip response
 */
data class TripCancelData(
    val trip_id: String,
    val status: String,
    val cancel_time: String? = null,
    val message: String? = null
)

/**
 * Track point
 */
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val speed: Double? = null
)

/**
 * Real-time map response
 * GET /api/mobile/trips/{trip_id}/map
 */
data class TripMapData(
    val trip_id: String? = null,
    val track_points: List<TrackPoint> = emptyList(),
    val current_distance: Double = 0.0,
    val duration_seconds: Int = 0,
    val status: String = "tracking",
    // Legacy field compatibility
    val polyline_points: List<GeoPoint>? = null,
    val current_point: GeoPoint? = null
)

/**
 * Save trip request
 * POST /api/mobile/trips/save
 */
data class TripSaveRequest(
    val trip_id: String,
    val user_id: String,
    val end_point: GeoPoint,
    val end_location: LocationInfo? = null,
    val distance: Double,
    val end_time: String
)

/**
 * Save trip response
 */
data class TripSaveData(
    val trip_id: String,
    val status: String,
    val total_distance: Double? = null,
    val duration_minutes: Int? = null,
    val message: String? = null
)

/**
 * Carbon footprint calculation request
 * POST /api/mobile/trips/carbon/calculate
 */
data class CarbonCalculateRequest(
    val trip_id: String,
    val transport_modes: List<String>
)

/**
 * Carbon footprint calculation response
 */
data class CarbonCalculateData(
    val trip_id: String? = null,
    val total_carbon_emission: Double = 0.0,
    val carbon_saved: Double,
    val green_points: Int = 0,
    val is_green_trip: Boolean = true,
    val transport_breakdown: Map<String, Double>? = null
)

/**
 * Trip status enum
 */
enum class TripStatus(val value: String) {
    TRACKING("tracking"),
    COMPLETED("completed"),
    CANCELED("canceled")
}
