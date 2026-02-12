package com.ecogo.mapengine.data.repository

import com.ecogo.mapengine.data.model.*
import com.ecogo.mapengine.data.remote.ApiService
import com.ecogo.mapengine.data.remote.RetrofitClient
import com.ecogo.mapengine.service.DirectionsService
import com.ecogo.mapengine.utils.MapUtils
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Map data repository
 * Handles trip tracking and route recommendation data logic
 * Implements IMapRepository interface, calls real backend API
 */
class MapRepository(
    private val apiService: ApiService = RetrofitClient.apiService
) : IMapRepository {

    companion object {
        private const val ERR_EMPTY_RESPONSE = "Response data is empty"
    }

    // ========================================
    // Trip tracking
    // ========================================

    /**
     * Start trip tracking
     */
    override suspend fun startTripTracking(
        userId: String,
        startPoint: GeoPoint,
        startLocation: LocationInfo?
    ): Result<TripTrackData> = withContext(Dispatchers.IO) {
        try {
            val request = TripTrackRequest(
                user_id = userId,
                start_point = startPoint,
                start_location = startLocation
            )
            val response = apiService.startTripTracking(request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception(ERR_EMPTY_RESPONSE))
            } else {
                Result.failure(Exception(response.body()?.msg ?: "Failed to start trip"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cancel trip tracking
     */
    override suspend fun cancelTripTracking(
        tripId: String,
        userId: String,
        reason: String?
    ): Result<TripCancelData> = withContext(Dispatchers.IO) {
        try {
            val request = TripCancelRequest(
                user_id = userId,
                cancel_reason = reason
            )
            val response = apiService.cancelTripTracking(tripId, request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception(ERR_EMPTY_RESPONSE))
            } else {
                Result.failure(Exception(response.body()?.msg ?: "Failed to cancel trip"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get real-time map data
     */
    override suspend fun getTripMap(
        tripId: String,
        userId: String
    ): Result<TripMapData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTripMap(tripId, userId)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception(ERR_EMPTY_RESPONSE))
            } else {
                Result.failure(Exception(response.body()?.msg ?: "Failed to get map data"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save trip
     */
    override suspend fun saveTrip(
        tripId: String,
        userId: String,
        endPoint: GeoPoint,
        endLocation: LocationInfo?,
        distance: Double,
        endTime: String
    ): Result<TripSaveData> = withContext(Dispatchers.IO) {
        try {
            val request = TripSaveRequest(
                trip_id = tripId,
                user_id = userId,
                end_point = endPoint,
                end_location = endLocation,
                distance = distance,
                end_time = endTime
            )
            val response = apiService.saveTrip(request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception(ERR_EMPTY_RESPONSE))
            } else {
                Result.failure(Exception(response.body()?.msg ?: "Failed to save trip"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calculate carbon footprint
     */
    override suspend fun calculateCarbon(
        tripId: String,
        transportModes: List<String>
    ): Result<CarbonCalculateData> = withContext(Dispatchers.IO) {
        try {
            val request = CarbonCalculateRequest(
                trip_id = tripId,
                transport_modes = transportModes
            )
            val response = apiService.calculateCarbon(request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception(ERR_EMPTY_RESPONSE))
            } else {
                Result.failure(Exception(response.body()?.msg ?: "Failed to calculate carbon footprint"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // Route recommendation
    // ========================================

    /**
     * Get lowest carbon emission route
     */
    override suspend fun getLowestCarbonRoute(
        userId: String,
        startPoint: GeoPoint,
        endPoint: GeoPoint
    ): Result<RouteRecommendData> = withContext(Dispatchers.IO) {
        try {
            val request = RouteRecommendRequest(
                user_id = userId,
                start_point = startPoint,
                end_point = endPoint
            )
            val response = apiService.getLowestCarbonRoute(request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception(ERR_EMPTY_RESPONSE))
            } else {
                Result.failure(Exception(response.body()?.msg ?: "Failed to get route"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get balanced route (time-carbon balanced)
     */
    override suspend fun getBalancedRoute(
        userId: String,
        startPoint: GeoPoint,
        endPoint: GeoPoint
    ): Result<RouteRecommendData> = withContext(Dispatchers.IO) {
        try {
            val request = RouteRecommendRequest(
                user_id = userId,
                start_point = startPoint,
                end_point = endPoint
            )
            val response = apiService.getBalancedRoute(request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception(ERR_EMPTY_RESPONSE))
            } else {
                Result.failure(Exception(response.body()?.msg ?: "Failed to get route"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get route by transport mode
     * Calls Google Directions API directly, bypassing the backend
     */
    override suspend fun getRouteByTransportMode(
        userId: String,
        startPoint: GeoPoint,
        endPoint: GeoPoint,
        transportMode: TransportMode
    ): Result<RouteRecommendData> = withContext(Dispatchers.IO) {
        try {
            val origin = LatLng(startPoint.lat, startPoint.lng)
            val destination = LatLng(endPoint.lat, endPoint.lng)

            // TransportMode -> Google Directions API mode parameter
            val googleMode = when (transportMode) {
                TransportMode.WALKING -> "walking"
                TransportMode.CYCLING -> "bicycling"
                TransportMode.BUS, TransportMode.SUBWAY -> "transit"
                TransportMode.DRIVING -> "driving"
            }

            val result = DirectionsService.getRoute(origin, destination, googleMode)
                ?: return@withContext Result.failure(Exception("Unable to get route, please check network connection"))

            val distanceKm = result.distanceMeters / 1000.0
            val durationMinutes = result.durationSeconds / 60

            // Carbon emission calculation
            val totalCarbon = MapUtils.estimateCarbonEmission(distanceKm, transportMode.value)
            val carbonSaved = MapUtils.calculateCarbonSaved(distanceKm, transportMode.value)

            val routePoints = result.points.map { GeoPoint(lng = it.longitude, lat = it.latitude) }

            Result.success(RouteRecommendData(
                route_id = "directions_${System.currentTimeMillis()}",
                route_type = transportMode.value,
                total_distance = distanceKm,
                estimated_duration = durationMinutes,
                total_carbon = totalCarbon,
                carbon_saved = carbonSaved,
                route_points = routePoints,
                route_steps = result.steps
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
