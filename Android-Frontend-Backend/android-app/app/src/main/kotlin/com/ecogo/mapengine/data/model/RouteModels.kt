package com.ecogo.mapengine.data.model

/**
 * ========================================
 * Route recommendation related models
 * Corresponding to RecommendService microservice API
 * ========================================
 */

/**
 * Route recommendation request
 * POST /api/mobile/route/recommend/low-carbon
 * POST /api/mobile/route/recommend/balance
 */
data class RouteRecommendRequest(
    val user_id: String,
    val start_point: GeoPoint,
    val end_point: GeoPoint
)

/**
 * Route recommendation response
 */
data class RouteRecommendData(
    val route_id: String? = null,
    val route_type: String? = null,
    val total_distance: Double = 0.0,
    val estimated_duration: Int = 0,  // minutes
    val total_carbon: Double = 0.0,
    val carbon_saved: Double = 0.0,
    val route_segments: List<RouteSegment>? = null,
    val route_points: List<GeoPoint>? = null,
    val route_steps: List<RouteStep>? = null,  // Detailed steps (for transit and multi-mode routes)
    val route_alternatives: List<RouteAlternative>? = null,  // Multiple route options (transit mode only)
    // Legacy field compatibility
    val green_route: List<GeoPoint>? = null,
    val duration: Int? = null
)

/**
 * Route alternative (for displaying multiple routes for user selection)
 */
data class RouteAlternative(
    val index: Int,                      // Route index
    val total_distance: Double,          // Total distance (km)
    val estimated_duration: Int,         // Estimated duration (minutes)
    val total_carbon: Double,            // Carbon emission
    val route_points: List<GeoPoint>,    // Route points
    val route_steps: List<RouteStep>,    // Detailed steps
    val summary: String                  // Route summary (e.g. "Metro Line 1 -> Bus 46")
)

/**
 * Route segment (a single segment in a multi-segment route)
 */
data class RouteSegment(
    val transport_mode: String,
    val distance: Double,
    val duration: Int,
    val carbon_emission: Double,
    val instructions: String? = null,
    val polyline: List<GeoPoint>? = null
)

/**
 * Route recommendation type
 */
enum class RouteType(val value: String) {
    LOW_CARBON("low-carbon"),      // Lowest carbon emission
    BALANCE("balance")              // Time-carbon balanced
}

/**
 * Route cache data
 * GET /api/mobile/route/cache/{user_id}
 */
data class RouteCacheData(
    val route_info: RouteRecommendData?,
    val expire_time: String?
)

/**
 * Transport mode
 */
enum class TransportMode(val value: String, val displayName: String) {
    WALKING("walk", "Walking"),
    CYCLING("bike", "Cycling"),
    BUS("bus", "Bus"),
    SUBWAY("subway", "Subway"),
    DRIVING("car", "Driving")
}

/**
 * Route detailed step (for displaying transit transfer details, etc.)
 */
data class RouteStep(
    val instruction: String,           // Step instruction (e.g. "Walk to bus stop", "Take Bus X")
    val distance: Double,               // Distance (meters)
    val duration: Int,                  // Duration (seconds)
    val travel_mode: String,            // Travel mode (WALKING, TRANSIT, DRIVING, etc.)
    val transit_details: TransitDetails? = null,  // Transit details (TRANSIT mode only)
    val polyline_points: List<GeoPoint>? = null   // Route points for this step (for drawing segments in different colors)
)

/**
 * Transit details
 */
data class TransitDetails(
    val line_name: String,              // Line name (e.g. "Metro Line 1", "Bus 46")
    val line_short_name: String? = null, // Line short name (e.g. "Line 1", "46")
    val departure_stop: String,         // Departure stop
    val arrival_stop: String,           // Arrival stop
    val num_stops: Int,                 // Number of stops
    val vehicle_type: String,           // Vehicle type (BUS, SUBWAY, RAIL, etc.)
    val headsign: String? = null        // Direction sign (e.g. "Towards XX")
)
