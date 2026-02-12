package com.ecogo.mapengine.data.repository

import android.util.Log
import com.ecogo.mapengine.data.model.*
import com.ecogo.mapengine.service.DirectionsService
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Mock map data repository
 *
 * Purpose: Returns mock data for testing and demo when no backend server is available
 *
 * Usage:
 * 1. Replace MapRepository with MockMapRepository in MapViewModel
 * 2. Or use dependency injection to switch automatically in debug mode
 */
class MockMapRepository : IMapRepository {

    companion object {
        private const val TAG = "MockMapRepository"
    }

    // Mock trip ID counter
    private var tripIdCounter = 1000

    // Current mock trip data
    private var currentTripStartTime: String? = null
    private var currentTripStartPoint: GeoPoint? = null

    // ========================================
    // Trip tracking
    // ========================================

    /**
     * Start trip tracking - Mock implementation
     *
     * Mock behavior:
     * 1. Delay 500ms to simulate network request
     * 2. Generate a unique trip_id
     * 3. Return success status
     */
    override suspend fun startTripTracking(
        userId: String,
        startPoint: GeoPoint,
        startLocation: LocationInfo?
    ): Result<TripTrackData> = withContext(Dispatchers.IO) {
        // Simulate network delay
        delay(500)

        // Save start point info for later calculations
        currentTripStartPoint = startPoint
        currentTripStartTime = getCurrentTimeString()

        // Generate mock data
        val mockData = TripTrackData(
            trip_id = "MOCK_TRIP_${tripIdCounter++}",
            status = "tracking",
            start_time = currentTripStartTime!!,
            message = "Trip recording started"
        )

        Result.success(mockData)
    }

    /**
     * Cancel trip tracking - Mock implementation
     */
    override suspend fun cancelTripTracking(
        tripId: String,
        userId: String,
        reason: String?
    ): Result<TripCancelData> = withContext(Dispatchers.IO) {
        delay(300)

        // Clear current trip data
        currentTripStartTime = null
        currentTripStartPoint = null

        val mockData = TripCancelData(
            trip_id = tripId,
            status = "cancelled",
            cancel_time = getCurrentTimeString(),
            message = "Trip cancelled"
        )

        Result.success(mockData)
    }

    /**
     * Get real-time map data - Mock implementation
     *
     * Returns mock track points
     */
    override suspend fun getTripMap(
        tripId: String,
        userId: String
    ): Result<TripMapData> = withContext(Dispatchers.IO) {
        delay(200)

        // Generate mock track points (based on start point) - GeoPoint(lng, lat)
        val startPoint = currentTripStartPoint ?: GeoPoint(lng = 121.4737, lat = 31.2304)
        val trackPoints = generateMockTrackPoints(startPoint)

        val mockData = TripMapData(
            trip_id = tripId,
            track_points = trackPoints,
            current_distance = calculateMockDistance(trackPoints),
            duration_seconds = 600, // 10 minutes
            status = "tracking"
        )

        Result.success(mockData)
    }

    /**
     * Save trip - Mock implementation
     */
    override suspend fun saveTrip(
        tripId: String,
        userId: String,
        endPoint: GeoPoint,
        endLocation: LocationInfo?,
        distance: Double,
        endTime: String
    ): Result<TripSaveData> = withContext(Dispatchers.IO) {
        delay(500)

        val mockData = TripSaveData(
            trip_id = tripId,
            status = "completed",
            total_distance = distance,
            duration_minutes = 15,
            message = "Trip saved"
        )

        // Clear current trip data
        currentTripStartTime = null
        currentTripStartPoint = null

        Result.success(mockData)
    }

    /**
     * Calculate carbon footprint - Mock implementation
     *
     * Mock carbon footprint calculation logic:
     * - Walking: 0 g/km
     * - Cycling: 0 g/km
     * - Bus: 50 g/km
     * - Subway: 30 g/km
     * - Driving: 150 g/km
     */
    override suspend fun calculateCarbon(
        tripId: String,
        transportModes: List<String>
    ): Result<CarbonCalculateData> = withContext(Dispatchers.IO) {
        delay(400)

        // Mock carbon emission factors for different transport modes (unit: g/km)
        val carbonFactors = mapOf(
            "walk" to 0.0,
            "bike" to 0.0,
            "bus" to 50.0,
            "subway" to 30.0,
            "car" to 150.0
        )

        // Mock 5 km trip
        val mockDistance = 5.0

        // Calculate total carbon emission
        val totalCarbon = transportModes.sumOf { mode ->
            carbonFactors[mode] ?: 100.0
        } * mockDistance / transportModes.size

        // Calculate carbon savings (compared to driving)
        val drivingCarbon = 150.0 * mockDistance
        val carbonSaved = (drivingCarbon - totalCarbon).coerceAtLeast(0.0)

        val mockData = CarbonCalculateData(
            trip_id = tripId,
            total_carbon_emission = totalCarbon / 1000, // Convert to kg
            carbon_saved = carbonSaved / 1000, // Convert to kg
            green_points = (carbonSaved / 10).toInt(), // 1 point per 10g of carbon saved
            transport_breakdown = transportModes.associateWith { mode ->
                (carbonFactors[mode] ?: 100.0) * mockDistance / transportModes.size / 1000
            }
        )

        Result.success(mockData)
    }

    // ========================================
    // Route recommendation
    // ========================================

    /**
     * Get lowest carbon emission route - Mock implementation
     *
     * Returns low-carbon route primarily using walking/cycling/bus
     * Uses Google Directions API to get real road routes
     */
    override suspend fun getLowestCarbonRoute(
        userId: String,
        startPoint: GeoPoint,
        endPoint: GeoPoint
    ): Result<RouteRecommendData> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Getting low carbon route from (${startPoint.lat}, ${startPoint.lng}) to (${endPoint.lat}, ${endPoint.lng})")

        // Use Google Directions API to get real route (walking mode)
        val origin = LatLng(startPoint.lat, startPoint.lng)
        val destination = LatLng(endPoint.lat, endPoint.lng)

        val directionsResult = DirectionsService.getRoute(origin, destination, "walking")

        val routePoints: List<GeoPoint>
        val distance: Double
        val duration: Int

        if (directionsResult != null) {
            // Use real route
            Log.d(TAG, "Got real route with ${directionsResult.points.size} points")
            routePoints = directionsResult.points.map { GeoPoint(lng = it.longitude, lat = it.latitude) }
            distance = directionsResult.distanceMeters / 1000.0  // Convert to km
            duration = directionsResult.durationSeconds / 60     // Convert to minutes
        } else {
            // Fallback to straight line (when API fails)
            Log.w(TAG, "Directions API failed, using fallback straight line")
            routePoints = generateRoutePoints(startPoint, endPoint)
            distance = calculateDistance(startPoint, endPoint)
            duration = (distance / 4 * 60).toInt()
        }

        // Low-carbon route: prioritize walking + bus
        val mockData = RouteRecommendData(
            route_id = "LOW_CARBON_${System.currentTimeMillis()}",
            route_type = "low_carbon",
            total_distance = distance,
            estimated_duration = duration,
            total_carbon = distance * 0.02, // 低碳约 20g/km
            carbon_saved = distance * 0.13, // 比驾车节省约 130g/km
            route_segments = listOf(
                RouteSegment(
                    transport_mode = "walk",
                    distance = distance * 0.3,
                    duration = (duration * 0.3).toInt(),
                    carbon_emission = 0.0,
                    instructions = "Walk to bus stop"
                ),
                RouteSegment(
                    transport_mode = "bus",
                    distance = distance * 0.6,
                    duration = (duration * 0.5).toInt(),
                    carbon_emission = distance * 0.6 * 0.05,
                    instructions = "Take the bus"
                ),
                RouteSegment(
                    transport_mode = "walk",
                    distance = distance * 0.1,
                    duration = (duration * 0.2).toInt(),
                    carbon_emission = 0.0,
                    instructions = "Walk to destination"
                )
            ),
            route_points = routePoints
        )

        Result.success(mockData)
    }

    /**
     * Get balanced route - Mock implementation
     *
     * Returns a time-carbon balanced route (may include ride-hailing segments)
     * Uses Google Directions API to get real road routes
     */
    override suspend fun getBalancedRoute(
        userId: String,
        startPoint: GeoPoint,
        endPoint: GeoPoint
    ): Result<RouteRecommendData> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Getting balanced route from (${startPoint.lat}, ${startPoint.lng}) to (${endPoint.lat}, ${endPoint.lng})")

        // Use Google Directions API to get real route (transit mode)
        val origin = LatLng(startPoint.lat, startPoint.lng)
        val destination = LatLng(endPoint.lat, endPoint.lng)

        val directionsResult = DirectionsService.getRoute(origin, destination, "transit")
            ?: DirectionsService.getRoute(origin, destination, "driving")  // Fallback to driving if transit fails

        val routePoints: List<GeoPoint>
        val distance: Double
        val duration: Int

        if (directionsResult != null) {
            // Use real route
            Log.d(TAG, "Got real route with ${directionsResult.points.size} points")
            routePoints = directionsResult.points.map { GeoPoint(lng = it.longitude, lat = it.latitude) }
            distance = directionsResult.distanceMeters / 1000.0  // Convert to km
            duration = directionsResult.durationSeconds / 60     // Convert to minutes
        } else {
            // Fallback to straight line (when API fails)
            Log.w(TAG, "Directions API failed, using fallback straight line")
            routePoints = generateRoutePoints(startPoint, endPoint)
            distance = calculateDistance(startPoint, endPoint)
            duration = (distance / 15 * 60).toInt()
        }

        // Balanced route: primarily subway, may include ride-hailing
        val mockData = RouteRecommendData(
            route_id = "BALANCED_${System.currentTimeMillis()}",
            route_type = "balanced",
            total_distance = distance,
            estimated_duration = duration,
            total_carbon = distance * 0.06, // 约 60g/km
            carbon_saved = distance * 0.09, // 比驾车节省约 90g/km
            route_segments = listOf(
                RouteSegment(
                    transport_mode = "walk",
                    distance = distance * 0.1,
                    duration = (duration * 0.1).toInt(),
                    carbon_emission = 0.0,
                    instructions = "Walk to subway station"
                ),
                RouteSegment(
                    transport_mode = "subway",
                    distance = distance * 0.7,
                    duration = (duration * 0.6).toInt(),
                    carbon_emission = distance * 0.7 * 0.03,
                    instructions = "Take the subway"
                ),
                RouteSegment(
                    transport_mode = "car",
                    distance = distance * 0.2,
                    duration = (duration * 0.3).toInt(),
                    carbon_emission = distance * 0.2 * 0.15,
                    instructions = "Take a ride to destination"
                )
            ),
            route_points = routePoints
        )

        Result.success(mockData)
    }

    /**
     * Get route by transport mode - Mock implementation
     */
    override suspend fun getRouteByTransportMode(
        userId: String,
        startPoint: GeoPoint,
        endPoint: GeoPoint,
        transportMode: TransportMode
    ): Result<RouteRecommendData> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Getting route for mode: ${transportMode.displayName}")

        // Map to Google Directions API mode
        val apiMode = when (transportMode) {
            TransportMode.DRIVING -> "driving"
            TransportMode.WALKING -> "walking"
            TransportMode.CYCLING -> "bicycling"
            TransportMode.BUS, TransportMode.SUBWAY -> "transit"
        }

        val origin = LatLng(startPoint.lat, startPoint.lng)
        val destination = LatLng(endPoint.lat, endPoint.lng)

        val routeData = if (apiMode == "transit") {
            fetchTransitRoute(origin, destination, startPoint, endPoint, transportMode)
        } else {
            fetchSingleRoute(origin, destination, apiMode, startPoint, endPoint, transportMode)
        }

        // Calculate carbon emission
        val carbonData = calculateCarbonForMode(routeData.distance, transportMode)

        Result.success(RouteRecommendData(
            route_id = "${transportMode.value.uppercase()}_${System.currentTimeMillis()}",
            route_type = transportMode.value,
            total_distance = routeData.distance,
            estimated_duration = routeData.duration,
            total_carbon = carbonData.totalCarbon,
            carbon_saved = carbonData.carbonSaved,
            route_segments = emptyList(),
            route_points = routeData.routePoints,
            route_steps = routeData.steps,
            route_alternatives = routeData.alternatives
        ))
    }

    /**
     * Get transit route (with multiple alternative options)
     */
    private suspend fun fetchTransitRoute(
        origin: LatLng,
        destination: LatLng,
        startPoint: GeoPoint,
        endPoint: GeoPoint,
        transportMode: TransportMode
    ): RouteResult {
        val allRoutes = DirectionsService.getRoutes(origin, destination, "transit")
        val transitRoutes = allRoutes.filter { route ->
            route.steps.any { it.travel_mode == "TRANSIT" }
        }

        if (transitRoutes.isNotEmpty()) {
            val firstRoute = transitRoutes[0]
            val alternatives = transitRoutes.mapIndexed { index, route ->
                val routeDist = route.distanceMeters / 1000.0
                RouteAlternative(
                    index = index,
                    total_distance = routeDist,
                    estimated_duration = route.durationSeconds / 60,
                    total_carbon = calculateCarbonForMode(routeDist, transportMode).totalCarbon,
                    route_points = route.points.map { GeoPoint(lng = it.longitude, lat = it.latitude) },
                    route_steps = route.steps,
                    summary = generateRouteSummary(route.steps)
                )
            }
            Log.d(TAG, "Found ${alternatives.size} transit routes (filtered from ${allRoutes.size} total routes)")
            return RouteResult(
                routePoints = firstRoute.points.map { GeoPoint(lng = it.longitude, lat = it.latitude) },
                distance = firstRoute.distanceMeters / 1000.0,
                duration = firstRoute.durationSeconds / 60,
                steps = firstRoute.steps,
                alternatives = alternatives
            )
        }

        // Fallback to walking
        Log.w(TAG, "Transit mode failed, falling back to walking")
        return fetchSingleRoute(origin, destination, "walking", startPoint, endPoint, transportMode)
    }

    /**
     * Get single route (non-transit mode or fallback)
     */
    private suspend fun fetchSingleRoute(
        origin: LatLng,
        destination: LatLng,
        apiMode: String,
        startPoint: GeoPoint,
        endPoint: GeoPoint,
        transportMode: TransportMode
    ): RouteResult {
        val directionsResult = DirectionsService.getRoute(origin, destination, apiMode)
        if (directionsResult != null) {
            return RouteResult(
                routePoints = directionsResult.points.map { GeoPoint(lng = it.longitude, lat = it.latitude) },
                distance = directionsResult.distanceMeters / 1000.0,
                duration = directionsResult.durationSeconds / 60,
                steps = directionsResult.steps,
                alternatives = null
            )
        }
        // Fallback: straight line
        Log.w(TAG, "Directions API failed, using straight line")
        return RouteResult(
            routePoints = generateRoutePoints(startPoint, endPoint),
            distance = calculateDistance(startPoint, endPoint),
            duration = estimateDuration(calculateDistance(startPoint, endPoint), transportMode),
            steps = emptyList(),
            alternatives = null
        )
    }

    /**
     * Route result data class
     */
    private data class RouteResult(
        val routePoints: List<GeoPoint>,
        val distance: Double,
        val duration: Int,
        val steps: List<RouteStep>,
        val alternatives: List<RouteAlternative>?
    )

    // ========================================
    // Helper methods
    // ========================================

    /**
     * Get current time string
     */
    private fun getCurrentTimeString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    /**
     * Calculate straight-line distance between two points (unit: km)
     * Uses Haversine formula
     */
    private fun calculateDistance(p1: GeoPoint, p2: GeoPoint): Double {
        val R = 6371.0 // Earth radius (km)

        val lat1 = Math.toRadians(p1.lat)
        val lat2 = Math.toRadians(p2.lat)
        val deltaLat = Math.toRadians(p2.lat - p1.lat)
        val deltaLng = Math.toRadians(p2.lng - p1.lng)

        val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return R * c
    }

    /**
     * Generate route points (linear interpolation between start and end points)
     */
    private fun generateRoutePoints(start: GeoPoint, end: GeoPoint): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        val steps = 10

        for (i in 0..steps) {
            val ratio = i.toDouble() / steps
            points.add(GeoPoint(
                lng = start.lng + (end.lng - start.lng) * ratio,
                lat = start.lat + (end.lat - start.lat) * ratio
            ))
        }

        return points
    }

    /**
     * Generate mock track points
     */
    private fun generateMockTrackPoints(start: GeoPoint): List<TrackPoint> {
        val points = mutableListOf<TrackPoint>()
        var currentLat = start.lat
        var currentLng = start.lng

        repeat(10) { i ->
            // Slightly offset each point to simulate movement trajectory
            currentLat += 0.0001 * (1 + Math.random())
            currentLng += 0.0001 * (1 + Math.random())

            points.add(TrackPoint(
                latitude = currentLat,
                longitude = currentLng,
                timestamp = System.currentTimeMillis() - (10 - i) * 60000,
                speed = 4.0 + Math.random() * 2 // 4-6 km/h
            ))
        }

        return points
    }

    /**
     * Calculate total track distance
     */
    private fun calculateMockDistance(points: List<TrackPoint>): Double {
        if (points.size < 2) return 0.0

        var total = 0.0
        for (i in 1 until points.size) {
            val p1 = GeoPoint(lng = points[i-1].longitude, lat = points[i-1].latitude)
            val p2 = GeoPoint(lng = points[i].longitude, lat = points[i].latitude)
            total += calculateDistance(p1, p2)
        }

        return total
    }

    /**
     * Estimate trip duration
     */
    private fun estimateDuration(distanceKm: Double, mode: TransportMode): Int {
        val speedKmh = when (mode) {
            TransportMode.WALKING -> 4.0
            TransportMode.CYCLING -> 15.0
            TransportMode.BUS -> 20.0
            TransportMode.SUBWAY -> 35.0
            TransportMode.DRIVING -> 40.0
        }
        return (distanceKm / speedKmh * 60).toInt()  // Convert to minutes
    }

    /**
     * Carbon emission data
     */
    private data class CarbonData(val totalCarbon: Double, val carbonSaved: Double)

    /**
     * Generate route summary (e.g. "Metro Line 1 -> Bus 46")
     */
    private fun generateRouteSummary(steps: List<RouteStep>): String {
        val transitSteps = steps.filter { it.travel_mode == "TRANSIT" && it.transit_details != null }

        if (transitSteps.isEmpty()) {
            return "Walking route"
        }

        val summary = transitSteps.mapNotNull { step ->
            step.transit_details?.let {
                it.line_short_name ?: it.line_name
            }
        }.joinToString(" → ")

        return summary.ifEmpty { "Transit route" }
    }

    /**
     * Calculate carbon emission
     */
    private fun calculateCarbonForMode(distanceKm: Double, mode: TransportMode): CarbonData {
        // Carbon emission factor (kg CO2 / km)
        val emissionFactor = when (mode) {
            TransportMode.WALKING -> 0.0
            TransportMode.CYCLING -> 0.0
            TransportMode.BUS -> 0.05      // Bus 50g/km
            TransportMode.SUBWAY -> 0.03   // Subway 30g/km
            TransportMode.DRIVING -> 0.15  // Driving 150g/km
        }

        val totalCarbon = distanceKm * emissionFactor
        val drivingCarbon = distanceKm * 0.15  // Compared to driving
        val carbonSaved = (drivingCarbon - totalCarbon).coerceAtLeast(0.0)

        return CarbonData(totalCarbon, carbonSaved)
    }
}
