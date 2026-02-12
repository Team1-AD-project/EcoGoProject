package com.ecogo.mapengine.data.local.dao

import androidx.room.*
import com.ecogo.mapengine.data.local.entity.NavigationHistory
import com.ecogo.mapengine.data.local.entity.NavigationHistorySummary
import kotlinx.coroutines.flow.Flow

/**
 * Navigation history data access object
 * Provides database CRUD operations
 */
@Dao
interface NavigationHistoryDao {

    /**
     * Insert a navigation history record
     * @return ID of the inserted record
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: NavigationHistory): Long

    /**
     * Insert multiple navigation history records
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(histories: List<NavigationHistory>)

    /**
     * Update navigation history record
     */
    @Update
    suspend fun update(history: NavigationHistory)

    /**
     * Delete navigation history record
     */
    @Delete
    suspend fun delete(history: NavigationHistory)

    /**
     * Delete record by ID
     */
    @Query("DELETE FROM navigation_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Delete all history records
     */
    @Query("DELETE FROM navigation_history")
    suspend fun deleteAll()

    /**
     * Get a single record by ID
     */
    @Query("SELECT * FROM navigation_history WHERE id = :id")
    suspend fun getById(id: Long): NavigationHistory?

    /**
     * Get all navigation history records (sorted by time descending)
     */
    @Query("SELECT * FROM navigation_history ORDER BY startTime DESC")
    suspend fun getAll(): List<NavigationHistory>

    /**
     * Get all navigation history records (Flow, for real-time updates)
     */
    @Query("SELECT * FROM navigation_history ORDER BY startTime DESC")
    fun getAllFlow(): Flow<List<NavigationHistory>>

    /**
     * Get simplified history record list (for list display)
     */
    @Query("""
        SELECT id, startTime, originName, destinationName, totalDistance,
               transportMode, carbonSaved, durationSeconds
        FROM navigation_history
        ORDER BY startTime DESC
    """)
    suspend fun getAllSummaries(): List<NavigationHistorySummary>

    /**
     * Query by time range
     */
    @Query("""
        SELECT * FROM navigation_history
        WHERE startTime >= :startTime AND startTime <= :endTime
        ORDER BY startTime DESC
    """)
    suspend fun getByTimeRange(startTime: Long, endTime: Long): List<NavigationHistory>

    /**
     * Query by transport mode
     */
    @Query("""
        SELECT * FROM navigation_history
        WHERE transportMode = :mode
        ORDER BY startTime DESC
    """)
    suspend fun getByTransportMode(mode: String): List<NavigationHistory>

    /**
     * Query green trip records
     */
    @Query("""
        SELECT * FROM navigation_history
        WHERE isGreenTrip = 1
        ORDER BY startTime DESC
    """)
    suspend fun getGreenTrips(): List<NavigationHistory>

    /**
     * Get the most recent N records
     */
    @Query("""
        SELECT * FROM navigation_history
        ORDER BY startTime DESC
        LIMIT :limit
    """)
    suspend fun getRecent(limit: Int): List<NavigationHistory>

    /**
     * Get total record count
     */
    @Query("SELECT COUNT(*) FROM navigation_history")
    suspend fun getCount(): Int

    /**
     * Get total carbon saved
     */
    @Query("SELECT SUM(carbonSaved) FROM navigation_history WHERE isGreenTrip = 1")
    suspend fun getTotalCarbonSaved(): Double?

    /**
     * Get total travel distance
     */
    @Query("SELECT SUM(totalDistance) FROM navigation_history")
    suspend fun getTotalDistance(): Double?

    /**
     * Get green trip count
     */
    @Query("SELECT COUNT(*) FROM navigation_history WHERE isGreenTrip = 1")
    suspend fun getGreenTripCount(): Int

    /**
     * Query by user ID
     */
    @Query("""
        SELECT * FROM navigation_history
        WHERE userId = :userId
        ORDER BY startTime DESC
    """)
    suspend fun getByUserId(userId: String): List<NavigationHistory>

    /**
     * Search records (by origin or destination name)
     */
    @Query("""
        SELECT * FROM navigation_history
        WHERE originName LIKE '%' || :keyword || '%'
           OR destinationName LIKE '%' || :keyword || '%'
        ORDER BY startTime DESC
    """)
    suspend fun search(keyword: String): List<NavigationHistory>
}
