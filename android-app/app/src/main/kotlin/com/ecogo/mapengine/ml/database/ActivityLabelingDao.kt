package com.ecogo.mapengine.ml.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ecogo.mapengine.ml.model.LabeledJourney
import kotlinx.coroutines.flow.Flow

/**
 * Room数据库DAO - 管理标记出行数据
 */
@Dao
interface ActivityLabelingDao {
    
    // 插入操作
    @Insert
    suspend fun insert(journey: LabeledJourney): Long
    
    @Insert
    suspend fun insertAll(journeys: List<LabeledJourney>)
    
    // 更新操作
    @Update
    suspend fun update(journey: LabeledJourney)
    
    // 删除操作
    @Delete
    suspend fun delete(journey: LabeledJourney)
    
    @Query("DELETE FROM labeled_journeys WHERE id = :journeyId")
    suspend fun deleteById(journeyId: Long)
    
    // 查询操作
    @Query("SELECT * FROM labeled_journeys WHERE id = :journeyId")
    suspend fun getJourneyById(journeyId: Long): LabeledJourney?
    
    @Query("SELECT * FROM labeled_journeys ORDER BY startTime DESC")
    fun getAllJourneys(): Flow<List<LabeledJourney>>
    
    @Query("SELECT * FROM labeled_journeys WHERE isVerified = 1 ORDER BY startTime DESC")
    fun getVerifiedJourneys(): Flow<List<LabeledJourney>>
    
    @Query("SELECT * FROM labeled_journeys WHERE isVerified = 0 ORDER BY startTime DESC")
    fun getUnverifiedJourneys(): Flow<List<LabeledJourney>>
    
    @Query("SELECT * FROM labeled_journeys WHERE labelSource = :source ORDER BY startTime DESC")
    fun getJourneysBySource(source: String): Flow<List<LabeledJourney>>
    
    @Query("SELECT * FROM labeled_journeys WHERE transportMode = :mode ORDER BY startTime DESC")
    fun getJourneysByTransportMode(mode: String): Flow<List<LabeledJourney>>
    
    @Query("SELECT * FROM labeled_journeys WHERE startTime BETWEEN :startTime AND :endTime ORDER BY startTime DESC")
    fun getJourneysInTimeRange(startTime: Long, endTime: Long): Flow<List<LabeledJourney>>
    
    // 统计查询
    @Query("SELECT COUNT(*) FROM labeled_journeys")
    suspend fun getTotalCount(): Int
    
    @Query("SELECT COUNT(*) FROM labeled_journeys WHERE isVerified = 1")
    suspend fun getVerifiedCount(): Int
    
    @Query("SELECT COUNT(*) FROM labeled_journeys WHERE transportMode = :mode")
    suspend fun getCountByTransportMode(mode: String): Int
    
    @Query("SELECT transportMode, COUNT(*) as count FROM labeled_journeys GROUP BY transportMode ORDER BY count DESC")
    suspend fun getTransportModeDistribution(): List<TransportModeCount>
    
    @Query("SELECT labelSource, COUNT(*) as count FROM labeled_journeys GROUP BY labelSource")
    suspend fun getLabelSourceDistribution(): List<LabelSourceCount>
    
    @Query("SELECT AVG(avgSpeed) FROM labeled_journeys WHERE transportMode = :mode")
    suspend fun getAverageSpeedForMode(mode: String): Float?
    
    // 导出操作
    @Query("SELECT * FROM labeled_journeys ORDER BY startTime DESC LIMIT :limit")
    suspend fun getJourneysForExport(limit: Int = 1000): List<LabeledJourney>
    
    @Query("SELECT * FROM labeled_journeys WHERE isVerified = 1 ORDER BY startTime DESC LIMIT :limit")
    suspend fun getVerifiedJourneysForExport(limit: Int = 1000): List<LabeledJourney>
}

/**
 * 交通方式分布查询结果
 */
data class TransportModeCount(
    val transportMode: String,
    val count: Int
)

/**
 * 标签来源分布查询结果
 */
data class LabelSourceCount(
    val labelSource: String,
    val count: Int
)
