package com.ecogo.mapengine.data.repository

import android.content.Context
import com.ecogo.mapengine.data.local.AppDatabase
import com.ecogo.mapengine.data.local.entity.NavigationHistory
import com.ecogo.mapengine.data.local.entity.NavigationHistorySummary
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Navigation history repository
 * Singleton pattern, provides a globally unique instance
 *
 * Usage:
 * ```kotlin
 * // Initialize (in Application)
 * NavigationHistoryRepository.initialize(context)
 *
 * // Get instance
 * val repository = NavigationHistoryRepository.getInstance()
 *
 * // Save navigation history
 * repository.saveNavigationHistory(...)
 *
 * // Get all records
 * val histories = repository.getAllHistories()
 * ```
 */
class NavigationHistoryRepository private constructor(context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val dao = database.navigationHistoryDao()

    /**
     * Save navigation history
     *
     * @param tripId Trip ID (optional)
     * @param userId User ID (optional)
     * @param startTime Start time (timestamp)
     * @param endTime End time (timestamp)
     * @param origin Origin coordinates
     * @param originName Origin name
     * @param destination Destination coordinates
     * @param destinationName Destination name
     * @param routePoints Planned route point list
     * @param trackPoints Actual track point list
     * @param totalDistance Total distance (meters)
     * @param traveledDistance Actual traveled distance (meters)
     * @param transportMode Transport mode
     * @param detectedMode AI-detected transport mode (optional)
     * @param totalCarbon Total carbon emission (kg)
     * @param carbonSaved Carbon emission saved (kg)
     * @param isGreenTrip Whether it is a green trip
     * @param greenPoints Green points earned
     * @param routeType Route type (optional)
     * @param notes Notes (optional)
     * @return ID of the inserted record
     */
    suspend fun saveNavigationHistory(
        tripId: String? = null,
        userId: String? = null,
        startTime: Long,
        endTime: Long,
        origin: LatLng,
        originName: String,
        destination: LatLng,
        destinationName: String,
        routePoints: List<LatLng>,
        trackPoints: List<LatLng>,
        totalDistance: Double,
        traveledDistance: Double,
        transportMode: String,
        detectedMode: String? = null,
        totalCarbon: Double,
        carbonSaved: Double,
        isGreenTrip: Boolean,
        greenPoints: Int = 0,
        routeType: String? = null,
        notes: String? = null
    ): Long {
        val history = NavigationHistory(
            tripId = tripId,
            userId = userId,
            startTime = startTime,
            endTime = endTime,
            durationSeconds = ((endTime - startTime) / 1000).toInt(),
            originLat = origin.latitude,
            originLng = origin.longitude,
            originName = originName,
            destinationLat = destination.latitude,
            destinationLng = destination.longitude,
            destinationName = destinationName,
            routePoints = convertLatLngListToJson(routePoints),
            trackPoints = convertLatLngListToJson(trackPoints),
            totalDistance = totalDistance,
            traveledDistance = traveledDistance,
            transportMode = transportMode,
            detectedMode = detectedMode,
            totalCarbon = totalCarbon,
            carbonSaved = carbonSaved,
            isGreenTrip = isGreenTrip,
            greenPoints = greenPoints,
            routeType = routeType,
            notes = notes
        )
        return dao.insert(history)
    }

    /**
     * Get all navigation history records
     */
    suspend fun getAllHistories(): List<NavigationHistory> {
        return dao.getAll()
    }

    /**
     * Get all navigation history records (Flow, real-time updates)
     */
    fun getAllHistoriesFlow(): Flow<List<NavigationHistory>> {
        return dao.getAllFlow()
    }

    /**
     * Get simplified history record list
     */
    suspend fun getAllSummaries(): List<NavigationHistorySummary> {
        return dao.getAllSummaries()
    }

    /**
     * Get a single record by ID
     */
    suspend fun getHistoryById(id: Long): NavigationHistory? {
        return dao.getById(id)
    }

    /**
     * Get the most recent N records
     */
    suspend fun getRecentHistories(limit: Int = 10): List<NavigationHistory> {
        return dao.getRecent(limit)
    }

    /**
     * Query by time range
     * @param startTime Start time (timestamp)
     * @param endTime End time (timestamp)
     */
    suspend fun getHistoriesByTimeRange(startTime: Long, endTime: Long): List<NavigationHistory> {
        return dao.getByTimeRange(startTime, endTime)
    }

    /**
     * Get today's history records
     */
    suspend fun getTodayHistories(): List<NavigationHistory> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        return dao.getByTimeRange(startOfDay, endOfDay)
    }

    /**
     * Get this week's history records
     */
    suspend fun getThisWeekHistories(): List<NavigationHistory> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.timeInMillis

        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        val endOfWeek = calendar.timeInMillis

        return dao.getByTimeRange(startOfWeek, endOfWeek)
    }

    /**
     * Query by transport mode
     */
    suspend fun getHistoriesByTransportMode(mode: String): List<NavigationHistory> {
        return dao.getByTransportMode(mode)
    }

    /**
     * Get all green trip records
     */
    suspend fun getGreenTrips(): List<NavigationHistory> {
        return dao.getGreenTrips()
    }

    /**
     * Query by user ID
     */
    suspend fun getHistoriesByUserId(userId: String): List<NavigationHistory> {
        return dao.getByUserId(userId)
    }

    /**
     * Search history records (by origin or destination name)
     */
    suspend fun searchHistories(keyword: String): List<NavigationHistory> {
        return dao.search(keyword)
    }

    /**
     * Update history record
     */
    suspend fun updateHistory(history: NavigationHistory) {
        dao.update(history)
    }

    /**
     * Delete history record
     */
    suspend fun deleteHistory(history: NavigationHistory) {
        dao.delete(history)
    }

    /**
     * Delete record by ID
     */
    suspend fun deleteHistoryById(id: Long) {
        dao.deleteById(id)
    }

    /**
     * Delete all history records
     */
    suspend fun deleteAllHistories() {
        dao.deleteAll()
    }

    /**
     * Get statistics
     */
    suspend fun getStatistics(): NavigationStatistics {
        val totalCount = dao.getCount()
        val greenTripCount = dao.getGreenTripCount()
        val totalDistance = dao.getTotalDistance() ?: 0.0
        val totalCarbonSaved = dao.getTotalCarbonSaved() ?: 0.0

        return NavigationStatistics(
            totalTrips = totalCount,
            greenTrips = greenTripCount,
            totalDistanceMeters = totalDistance,
            totalCarbonSavedKg = totalCarbonSaved
        )
    }

    /**
     * Convert LatLng list to JSON string (for storage)
     */
    private fun convertLatLngListToJson(points: List<LatLng>): String {
        val simplified = points.map { mapOf("lat" to it.latitude, "lng" to it.longitude) }
        return com.google.gson.Gson().toJson(simplified)
    }

    /**
     * Convert JSON string to LatLng list (for reading)
     */
    fun parseLatLngListFromJson(json: String): List<LatLng> {
        val type = object : com.google.gson.reflect.TypeToken<List<Map<String, Double>>>() {}.type
        val list: List<Map<String, Double>> = com.google.gson.Gson().fromJson(json, type)
        return list.map { LatLng(it["lat"] ?: 0.0, it["lng"] ?: 0.0) }
    }

    companion object {
        @Volatile
        private var INSTANCE: NavigationHistoryRepository? = null

        /**
         * Initialize repository (recommended to call in Application)
         */
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = NavigationHistoryRepository(context)
                    }
                }
            }
        }

        /**
         * Get repository instance
         * @throws IllegalStateException if initialize() has not been called first
         */
        fun getInstance(): NavigationHistoryRepository {
            return INSTANCE ?: throw IllegalStateException(
                "NavigationHistoryRepository must be initialized by calling initialize(context) first"
            )
        }

        /**
         * Check if initialized
         */
        fun isInitialized(): Boolean {
            return INSTANCE != null
        }
    }
}

/**
 * Navigation statistics
 */
data class NavigationStatistics(
    val totalTrips: Int,               // Total number of trips
    val greenTrips: Int,               // Number of green trips
    val totalDistanceMeters: Double,   // Total distance (meters)
    val totalCarbonSavedKg: Double     // Total carbon saved (kg)
) {
    /**
     * Total distance (km)
     */
    val totalDistanceKm: Double
        get() = totalDistanceMeters / 1000.0

    /**
     * Green trip percentage
     */
    val greenTripPercentage: Double
        get() = if (totalTrips > 0) (greenTrips.toDouble() / totalTrips * 100) else 0.0
}
