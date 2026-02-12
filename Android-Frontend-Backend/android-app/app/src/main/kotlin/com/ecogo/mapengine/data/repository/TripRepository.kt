package com.ecogo.mapengine.data.repository

import android.util.Log
import com.ecogo.auth.TokenManager
import com.ecogo.mapengine.data.model.*
import com.ecogo.mapengine.data.remote.RetrofitClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Trip repository - integrates local storage and remote API
 *
 * Features:
 * 1. Start trip -> call backend API + local recording
 * 2. Complete trip -> upload track to backend + save locally
 * 3. Get history -> local first, supports cloud sync
 *
 * Usage:
 * ```kotlin
 * val repo = TripRepository.getInstance()
 *
 * // Start trip
 * val result = repo.startTrip(startLat, startLng, placeName, address)
 *
 * // Complete trip
 * val result = repo.completeTrip(tripId, endLat, endLng, ...)
 * ```
 */
class TripRepository private constructor() {

    private val tripApiService = RetrofitClient.tripApiService
    private val historyRepo by lazy { NavigationHistoryRepository.getInstance() }

    // Current trip ID (obtained from backend)
    private var currentTripId: String? = null

    // Token management: prefer getting login token from TokenManager (no longer hardcoded)
    private fun resolveAuthToken(): String =
        TokenManager.getAuthHeader() ?: "Bearer test_token_123"

    companion object {
        private const val TAG = "TripRepository"

        @Volatile
        private var INSTANCE: TripRepository? = null

        fun getInstance(): TripRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TripRepository().also { INSTANCE = it }
            }
        }
    }

    /**
     * Get current auth token (dynamically read from TokenManager)
     */
    fun getAuthToken(): String = resolveAuthToken()

    // ============================================================
    // 1. Start trip
    // ============================================================

    /**
     * Start trip
     *
     * @return Result<String> returns tripId on success, error message on failure
     */
    suspend fun startTrip(
        startLat: Double,
        startLng: Double,
        startPlaceName: String,
        startAddress: String,
        startCampusZone: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = TripStartRequest(
                startLng = startLng,
                startLat = startLat,
                startAddress = startAddress,
                startPlaceName = startPlaceName,
                startCampusZone = startCampusZone
            )

            Log.d(TAG, "Starting trip: $request")

            val response = tripApiService.startTrip(resolveAuthToken(), request)

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess && apiResponse.data != null) {
                    val tripId = apiResponse.data.tripId
                    currentTripId = tripId
                    Log.d(TAG, "Trip started successfully: tripId=$tripId")
                    Result.success(tripId)
                } else {
                    val error = "API returned error: ${apiResponse.message}"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
            } else {
                val error = "Failed to start trip: ${response.code()} ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting trip", e)
            Result.failure(e)
        }
    }

    // ============================================================
    // 2. Complete trip
    // ============================================================

    /**
     * Complete trip and upload data
     *
     * @param tripId Trip ID (obtained from startTrip)
     * @param endLat Destination latitude
     * @param endLng Destination longitude
     * @param endPlaceName Destination name
     * @param endAddress Destination address
     * @param distance Total distance (meters)
     * @param trackPoints Actual track points
     * @param transportMode Primary transport mode (user-selected)
     * @param detectedMode AI-detected primary transport mode
     * @param mlConfidence ML confidence score
     * @param carbonSaved Carbon saved (grams)
     * @param isGreenTrip Whether it is a green trip
     * @param transportModeSegments ML-detected transport mode segment list (pass what the UI displays)
     *
     * @return Result<TripCompleteResponse>
     */
    suspend fun completeTrip(
        tripId: String,
        endLat: Double,
        endLng: Double,
        endPlaceName: String,
        endAddress: String,
        distance: Double,
        trackPoints: List<LatLng>,
        transportMode: String,
        detectedMode: String? = null,
        mlConfidence: Double? = null,
        carbonSaved: Long = 0L,
        isGreenTrip: Boolean = false,
        transportModeSegments: List<TransportModeSegment>? = null
    ): Result<TripCompleteResponse> = withContext(Dispatchers.IO) {
        try {
            val request = buildCompleteRequest(
                endLat, endLng, endPlaceName, endAddress, distance,
                trackPoints, transportMode, detectedMode, mlConfidence,
                carbonSaved, isGreenTrip, transportModeSegments
            )

            Log.d(TAG, "Completing trip: tripId=$tripId, points=${trackPoints.size}")

            val response = tripApiService.completeTrip(tripId, resolveAuthToken(), request)

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess && apiResponse.data != null) {
                    val result = apiResponse.data
                    Log.d(TAG, "Trip completed successfully: $result")
                    clearTripIdIfMatch(tripId)
                    Result.success(result)
                } else {
                    val error = "API returned error: ${apiResponse.message}"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
            } else {
                val error = "Failed to complete trip: ${response.code()} ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error completing trip", e)
            Result.failure(e)
        }
    }

    private fun buildCompleteRequest(
        endLat: Double, endLng: Double,
        endPlaceName: String, endAddress: String,
        distance: Double, trackPoints: List<LatLng>,
        transportMode: String, detectedMode: String?,
        mlConfidence: Double?, carbonSaved: Long,
        isGreenTrip: Boolean,
        transportModeSegments: List<TransportModeSegment>?
    ): TripCompleteRequest {
        val polylinePoints = trackPoints.map {
            PolylinePoint(lng = it.longitude, lat = it.latitude)
        }
        val transportModes = if (!transportModeSegments.isNullOrEmpty()) {
            transportModeSegments
        } else {
            listOf(TransportModeSegment(mode = transportMode, subDistance = distance / 1000.0, subDuration = 0))
        }
        return TripCompleteRequest(
            endLng = endLng, endLat = endLat,
            endAddress = endAddress, endPlaceName = endPlaceName,
            distance = distance / 1000.0,
            detectedMode = detectedMode, mlConfidence = mlConfidence,
            isGreenTrip = isGreenTrip, carbonSaved = carbonSaved,
            transportModes = transportModes, polylinePoints = polylinePoints
        )
    }

    private fun clearTripIdIfMatch(tripId: String) {
        if (currentTripId == tripId) {
            currentTripId = null
        }
    }

    // ============================================================
    // 3. Cancel trip
    // ============================================================

    /**
     * Cancel trip
     */
    suspend fun cancelTrip(tripId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Canceling trip: tripId=$tripId")

            val response = tripApiService.cancelTrip(tripId, resolveAuthToken())

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess && apiResponse.data != null) {
                    Log.d(TAG, "Trip canceled successfully: ${apiResponse.data}")

                    // Clear current trip ID
                    if (currentTripId == tripId) {
                        currentTripId = null
                    }

                    Result.success(apiResponse.data)
                } else {
                    val error = "API returned error: ${apiResponse.message}"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
            } else {
                val error = "Failed to cancel trip: ${response.code()} ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling trip", e)
            Result.failure(e)
        }
    }

    // ============================================================
    // 4. Get trip list
    // ============================================================

    /**
     * Get trip list from cloud
     */
    suspend fun getTripListFromCloud(
        page: Int? = null,
        pageSize: Int? = null,
        status: String? = null
    ): Result<List<TripDetail>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching trip list from cloud")

            val response = tripApiService.getTripList(resolveAuthToken(), page, pageSize, status)

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess && apiResponse.data != null) {
                    val trips = apiResponse.data
                    Log.d(TAG, "Fetched ${trips.size} trips from cloud")
                    Result.success(trips)
                } else {
                    val error = "API returned error: ${apiResponse.message}"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
            } else {
                val error = "Failed to fetch trips: ${response.code()} ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching trips", e)
            Result.failure(e)
        }
    }

    /**
     * Get trip list from local storage (fast)
     */
    suspend fun getTripListFromLocal(): Result<List<com.ecogo.mapengine.data.local.entity.NavigationHistory>> {
        return try {
            val histories = historyRepo.getAllHistories()
            Result.success(histories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // 5. Get trip details
    // ============================================================

    /**
     * Get trip details
     */
    suspend fun getTripDetail(tripId: String): Result<TripDetail> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching trip detail: tripId=$tripId")

            val response = tripApiService.getTripDetail(tripId, resolveAuthToken())

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess && apiResponse.data != null) {
                    val trip = apiResponse.data
                    Log.d(TAG, "Fetched trip detail successfully")
                    Result.success(trip)
                } else {
                    val error = "API returned error: ${apiResponse.message}"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
            } else {
                val error = "Failed to fetch trip detail: ${response.code()} ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching trip detail", e)
            Result.failure(e)
        }
    }

    // ============================================================
    // 6. Get current tracking trip
    // ============================================================

    /**
     * Get current tracking trip
     */
    suspend fun getCurrentTrip(): Result<TripDetail?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching current trip")

            val response = tripApiService.getCurrentTrip(resolveAuthToken())

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess) {
                    val trip = apiResponse.data
                    if (trip != null) {
                        currentTripId = trip.tripId
                        Log.d(TAG, "Current trip found: ${trip.tripId}")
                        Result.success(trip)
                    } else {
                        Log.d(TAG, "No current trip")
                        Result.success(null)
                    }
                } else {
                    val error = "API returned error: ${apiResponse.message}"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
            } else {
                val error = "Failed to fetch current trip: ${response.code()} ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching current trip", e)
            Result.failure(e)
        }
    }

    // ============================================================
    // Helper methods
    // ============================================================

    /**
     * Get current trip ID
     */
    fun getCurrentTripId(): String? = currentTripId

    /**
     * Clear current trip ID
     */
    fun clearCurrentTripId() {
        currentTripId = null
    }
}
