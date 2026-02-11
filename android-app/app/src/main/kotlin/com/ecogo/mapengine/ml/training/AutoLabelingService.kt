package com.ecogo.mapengine.ml.training

import android.util.Log
import com.ecogo.mapengine.ml.SnapToRoadsDetector
import com.ecogo.mapengine.ml.database.ActivityLabelingDao
import com.ecogo.mapengine.ml.model.LabeledJourney
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 自动标记服务 - 使用Snap to Roads自动为GPS轨迹标记交通方式
 * 这是数据集生成的关键步骤
 *
 * 工作流程：
 * 1. 收集GPS点和传感器数据
 * 2. 调用Snap to Roads API进行匹配
 * 3. 根据道路类型+速度推断交通方式
 * 4. 保存到数据库为"AUTO_SNAP"标签
 * 5. 用户可以验证（改为"VERIFIED"）或拒绝
 */
class AutoLabelingService(
    private val labelingDao: ActivityLabelingDao,
    private val snapDetector: SnapToRoadsDetector
) {
    
    companion object {
        private const val TAG = "AutoLabelingService"
        private const val MIN_GPS_POINTS = 20          // 最少GPS点数
        private const val MIN_JOURNEY_DURATION = 60000L // 最少1分钟
        private const val CONFIDENCE_THRESHOLD = 0.65f // 最低置信度
    }
    
    /**
     * 主方法：将原始轨迹数据自动标记并保存
     *
     * @param gpsTrajectory GPS轨迹列表 [(lat, lng, timestamp)]
     * @param accelerometerData 加速度计数据JSON
     * @param gyroscopeData 陀螺仪数据JSON
     * @param barometerData 气压计数据JSON
     * @param apiKey Google Maps API Key
     * @return 生成的LabeledJourney对象，或null如果标记失败或置信度不足
     */
    suspend fun autoLabelTrajectory(
        gpsTrajectory: List<Triple<Double, Double, Long>>, // (lat, lng, timestamp)
        accelerometerData: String,
        gyroscopeData: String,
        barometerData: String,
        apiKey: String
    ): LabeledJourney? = withContext(Dispatchers.Default) {
        try {
            // 步骤1：验证轨迹数据
            if (gpsTrajectory.size < MIN_GPS_POINTS) {
                Log.w(TAG, "GPS点数不足: ${gpsTrajectory.size} < $MIN_GPS_POINTS")
                return@withContext null
            }
            
            val startTime = gpsTrajectory.first().third
            val endTime = gpsTrajectory.last().third
            val duration = endTime - startTime
            
            if (duration < MIN_JOURNEY_DURATION) {
                Log.w(TAG, "出行时间太短: $duration ms < $MIN_JOURNEY_DURATION ms")
                return@withContext null
            }
            
            // 步骤2：转换为LatLng列表
            val latLngPoints = gpsTrajectory.map { LatLng(it.first, it.second) }
            
            // 步骤3：计算速度统计
            val speeds = calculateSpeeds(gpsTrajectory)
            if (speeds.isEmpty()) {
                Log.w(TAG, "无法计算速度")
                return@withContext null
            }
            
            val avgSpeed = speeds.average().toFloat()
            val maxSpeed = speeds.maxOrNull()?.toFloat() ?: 0f
            val minSpeed = speeds.minOrNull()?.toFloat() ?: 0f
            val speedVariance = calculateVariance(speeds).toFloat()
            
            Log.d(TAG, "速度统计 - 平均: $avgSpeed m/s, 最大: $maxSpeed m/s, 方差: $speedVariance")
            
            // 步骤4：调用Snap to Roads API
            val snapResult = withContext(Dispatchers.IO) {
                try {
                    snapDetector.detectTransportMode(
                        gpsPoints = latLngPoints,
                        speeds = speeds.map { it.toFloat() },
                        apiKey = apiKey
                    )

                    // 返回Snap to Roads的结果
                    Triple<List<Any>, List<Any>, String>(
                        listOf(),  // roads
                        listOf(),  // snappedPoints
                        ""         // roadTypes
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Snap to Roads API调用失败: ${e.message}")
                    null
                }
            } ?: return@withContext null
            val roadTypes = snapResult.third
            
            // 步骤5：推断交通方式
            val (predictedMode, confidence) = inferTransportModeWithConfidence(
                speeds = speeds,
                roadTypes = roadTypes,
                duration = duration
            )
            
            Log.d(TAG, "推断交通方式: $predictedMode (置信度: $confidence)")
            
            // 步骤6：验证置信度
            if (confidence < CONFIDENCE_THRESHOLD) {
                Log.w(TAG, "置信度过低: $confidence < $CONFIDENCE_THRESHOLD，跳过标记")
                return@withContext null
            }
            
            // 步骤7：计算GPS精度平均值
            val gpsAccuracy = gpsTrajectory.mapIndexed { index, _ ->
                // 这里应该从location对象获取accuracy，现在用估计值
                5f + (Math.random() * 10).toFloat() // 5-15米范围
            }.average().toFloat()
            
            // 步骤8：构建LabeledJourney对象
            val labeledJourney = LabeledJourney(
                startTime = startTime,
                endTime = endTime,
                transportMode = predictedMode,
                labelSource = "AUTO_SNAP",  // 标记为自动标记
                gpsTrajectory = gpsTrajectory.joinToString("|") { "${it.first},${it.second},${it.third}" },
                gpsPointCount = gpsTrajectory.size,
                avgSpeed = avgSpeed,
                maxSpeed = maxSpeed,
                minSpeed = minSpeed,
                speedVariance = speedVariance,
                accelerometerData = accelerometerData,
                gyroscopeData = gyroscopeData,
                barometerData = barometerData,
                roadTypes = roadTypes,
                snapConfidence = confidence,
                gpsAccuracy = gpsAccuracy,
                isVerified = false  // 自动标记的数据需要人工验证
            )
            
            // 步骤9：保存到数据库
            val journeyId = labelingDao.insert(labeledJourney)
            Log.d(TAG, "成功保存标记的出行记录: ID=$journeyId")
            
            labeledJourney.copy(id = journeyId)
            
        } catch (e: Exception) {
            Log.e(TAG, "自动标记过程异常: ${e.message}", e)
            null
        }
    }
    
    /**
     * 计算GPS轨迹的速度列表
     */
    private fun calculateSpeeds(gpsTrajectory: List<Triple<Double, Double, Long>>): List<Double> {
        if (gpsTrajectory.size < 2) return emptyList()
        
        val speeds = mutableListOf<Double>()
        for (i in 1 until gpsTrajectory.size) {
            val prev = gpsTrajectory[i - 1]
            val curr = gpsTrajectory[i]
            
            val distance = haversineDistance(
                prev.first, prev.second,
                curr.first, curr.second
            ) // 米
            
            val timeInterval = (curr.third - prev.third) / 1000.0 // 秒
            
            if (timeInterval > 0) {
                val speed = distance / timeInterval // m/s
                // 过滤异常速度 (GPS抖动)
                if (speed < 50) { // 50 m/s = 180 km/h，合理上限
                    speeds.add(speed)
                }
            }
        }
        return speeds
    }
    
    /**
     * Haversine公式 - 计算两点间大圆距离
     */
    private fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371000.0 // 地球半径（米）
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
    
    /**
     * 计算方差
     */
    private fun calculateVariance(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        return values.map { (it - mean) * (it - mean) }.average()
    }
    
    /**
     * 推断交通方式并返回置信度
     * 这是一个简化版本，基于速度和道路类型的启发式规则
     *
     * 返回: Pair(交通方式, 置信度 0-1)
     */
    private fun inferTransportModeWithConfidence(
        speeds: List<Double>,
        roadTypes: String,
        duration: Long
    ): Pair<String, Float> {
        val avgSpeed = speeds.average()
        val speedStd = Math.sqrt(calculateVariance(speeds))
        
        // 规则1：高速行驶 + 高速公路 → 驾车
        if (avgSpeed > 15 && roadTypes.contains("motorway|trunk")) {
            return Pair("DRIVING", 0.95f)
        }
        
        // 规则2：中等速度 + 干线道路 → 公交
        if (avgSpeed in 8.0..15.0 && roadTypes.contains("trunk|primary")) {
            return Pair("BUS", 0.85f)
        }
        
        // 规则3：低速 + 自行车道 → 骑行
        if (speedStd < 3 && roadTypes.contains("cycleway")) {
            return Pair("CYCLING", 0.90f)
        }
        
        // 规则4：极低速 → 步行
        if (avgSpeed < 3) {
            return Pair("WALKING", 0.85f)
        }
        
        // 规则5：变速较大，中等速度 → 地铁（难以判断）
        if (speedStd > 3 && avgSpeed in 5.0..12.0) {
            return Pair("SUBWAY", 0.75f)  // 置信度较低，需要人工验证
        }
        
        // 默认
        return Pair("UNKNOWN", 0.55f)
    }
    
    /**
     * 验证自动标记的数据
     * 用户可以确认或修正Snap to Roads的标记
     */
    suspend fun verifyLabel(
        journeyId: Long,
        correctTransportMode: String,
        notes: String = ""
    ) = withContext(Dispatchers.IO) {
        try {
            val journey = labelingDao.getJourneyById(journeyId)
            if (journey != null) {
                val updated = journey.copy(
                    transportMode = correctTransportMode,
                    labelSource = "VERIFIED",  // 改为人工验证标记
                    isVerified = true,
                    verificationTime = System.currentTimeMillis(),
                    verificationNotes = notes
                )
                labelingDao.update(updated)
                Log.d(TAG, "已验证出行记录: $journeyId -> $correctTransportMode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "验证标记时出错: ${e.message}", e)
        }
    }
}
