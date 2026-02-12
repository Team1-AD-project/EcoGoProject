package com.ecogo.mapengine.ml

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Detect transport mode using Google Snap to Roads API
 *
 * Principle:
 * 1. Snap GPS trajectory to real road network
 * 2. Obtain road types (motorway, street, path, etc.)
 * 3. Infer transport mode combined with GPS speed
 */
class SnapToRoadsDetector {

    companion object {
        private const val TAG = "SnapToRoadsDetector"
    }

    // Track last predicted mode for hysteresis
    private var lastPredictedMode: TransportModeLabel? = null

    /**
     * Infer transport mode from GPS trajectory points and speed information
     */
    suspend fun detectTransportMode(
        gpsPoints: List<LatLng>,
        speeds: List<Float>,  // m/s
        apiKey: String
    ): TransportModePrediction? = withContext(Dispatchers.IO) {
        try {
            if (gpsPoints.size < 2) {
                Log.w(TAG, "At least 2 GPS points required for detection")
                return@withContext null
            }

            // Calculate average speed and acceleration
            val avgSpeed = speeds.average()
            val avgSpeedKmh = (avgSpeed * 3.6).toFloat()

            // Get road types (via Snap to Roads)
            val roadTypes = snapToRoads(gpsPoints, apiKey)
            
            Log.d(TAG, "Average speed: $avgSpeedKmh km/h")
            Log.d(TAG, "Road types: $roadTypes")

            // 基于道路类型和速度推断交通方式
            val prediction = inferTransportMode(roadTypes, avgSpeedKmh)
            
            Log.d(TAG, "Detected transport mode: ${prediction.mode}, confidence: ${prediction.confidence}")
            
            return@withContext prediction
        } catch (e: Exception) {
            Log.e(TAG, "Snap to Roads detection failed: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * 调用 Snap to Roads API 获取道路信息
     */
    private suspend fun snapToRoads(
        points: List<LatLng>,
        apiKey: String
    ): List<RoadInfo> = withContext(Dispatchers.IO) {
        try {
            // 使用 HTTP 客户端调用真实 API
            val snappedPoints = SnapToRoadsHttpClient.snapToRoads(points, apiKey)
            
            if (snappedPoints == null || snappedPoints.isEmpty()) {
                Log.w(TAG, "Snap to Roads API returned empty result")
                return@withContext emptyList()
            }
            
            // 从对齐后的点推断道路类型
            // 注：真实的 API 返回中可能包含 placeId，但不直接包含道路类型
            // 这里基于点的密度和分布进行启发式推断
            val roadInfoList = inferRoadTypesFromSnappedPoints(snappedPoints, points)
            
            Log.d(TAG, "Snap to Roads returned ${snappedPoints.size} snapped points, inferred ${roadInfoList.size} roads")
            
            return@withContext roadInfoList
            
        } catch (e: Exception) {
            Log.e(TAG, "Snap to Roads API call failed: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    /**
     * 从对齐后的点推断道路类型
     * 由于 Google Snap to Roads API 不直接返回详细的道路类型信息，
     * 我们通过以下启发式方法推断：
     * - 点的间距密度反映街道类型
     * - 偏离原始路径的程度反映道路质量
     */
    private fun inferRoadTypesFromSnappedPoints(
        snappedPoints: List<SnappedPoint>,
        originalPoints: List<LatLng>
    ): List<RoadInfo> {
        if (snappedPoints.isEmpty()) return emptyList()
        
        // 简化实现：将所有对齐点分组，推断为"街道"
        // 实际应用中可以通过额外的 API（如 Place Details）获得更精确的信息
        
        return listOf(
            RoadInfo(
                roadType = "aligned_street"  // 已对齐到真实街道
            )
        )
    }

    /**
     * 根据道路类型和速度推断交通方式
     * Uses hysteresis (5 km/h buffer) at speed boundaries to prevent oscillation
     */
    private fun inferTransportMode(
        roadTypes: List<RoadInfo>,
        avgSpeedKmh: Float
    ): TransportModePrediction {
        val typeDistribution = roadTypes.groupingBy { it.roadType }.eachCount()
        val hasAlignedRoad = typeDistribution.containsKey("aligned_street")

        // Hysteresis buffer: require crossing threshold by 5 km/h to change mode
        val hysteresis = 5f
        val last = lastPredictedMode

        val prediction = when {
            // Motorway + high speed → DRIVING
            typeDistribution.containsKey("motorway") &&
            avgSpeedKmh > 50 -> makePrediction(TransportModeLabel.DRIVING, 0.95f)

            // Trunk road + medium speed → BUS
            typeDistribution.containsKey("trunk") &&
            avgSpeedKmh in 15f..60f -> makePrediction(TransportModeLabel.BUS, 0.85f)

            // Cycleway → CYCLING
            typeDistribution.containsKey("cycleway") -> makePrediction(TransportModeLabel.CYCLING, 0.9f)

            // Aligned road: speed-based with hysteresis
            hasAlignedRoad -> classifyBySpeedWithHysteresis(avgSpeedKmh, last, hysteresis)

            // No road type info: pure speed-based with hysteresis
            else -> classifyBySpeedWithHysteresis(avgSpeedKmh, last, hysteresis)
        }

        lastPredictedMode = prediction.mode
        return prediction
    }

    /**
     * Classify transport mode by speed with hysteresis to prevent oscillation.
     *
     * Thresholds: Walking < 10, Cycling 10-30, Bus 30-55, Driving > 55
     * When already in a mode, require crossing threshold +/- hysteresis to change.
     */
    private fun classifyBySpeedWithHysteresis(
        speedKmh: Float,
        lastMode: TransportModeLabel?,
        hysteresis: Float
    ): TransportModePrediction {
        // Define base thresholds
        val walkCycleThreshold = 10f
        val cycleBusThreshold = 30f
        val busDriveThreshold = 55f

        // Apply hysteresis: stick with current mode unless speed clearly crosses boundary
        return when (lastMode) {
            TransportModeLabel.WALKING -> when {
                speedKmh > walkCycleThreshold + hysteresis -> classifyBySpeed(speedKmh)
                else -> makePrediction(TransportModeLabel.WALKING, 0.85f)
            }
            TransportModeLabel.CYCLING -> when {
                speedKmh < walkCycleThreshold - hysteresis -> makePrediction(TransportModeLabel.WALKING, 0.85f)
                speedKmh > cycleBusThreshold + hysteresis -> classifyBySpeed(speedKmh)
                else -> makePrediction(TransportModeLabel.CYCLING, 0.80f)
            }
            TransportModeLabel.BUS -> when {
                speedKmh < cycleBusThreshold - hysteresis -> classifyBySpeed(speedKmh)
                speedKmh > busDriveThreshold + hysteresis -> makePrediction(TransportModeLabel.DRIVING, 0.90f)
                else -> makePrediction(TransportModeLabel.BUS, 0.75f)
            }
            TransportModeLabel.DRIVING -> when {
                speedKmh < busDriveThreshold - hysteresis -> classifyBySpeed(speedKmh)
                else -> makePrediction(TransportModeLabel.DRIVING, 0.90f)
            }
            else -> classifyBySpeed(speedKmh)
        }
    }

    /**
     * Pure speed-based classification (no hysteresis, used for initial classification)
     */
    private fun classifyBySpeed(speedKmh: Float): TransportModePrediction {
        return when {
            speedKmh < 10f -> makePrediction(TransportModeLabel.WALKING, 0.85f)
            speedKmh < 30f -> makePrediction(TransportModeLabel.CYCLING, 0.80f)
            speedKmh < 55f -> makePrediction(TransportModeLabel.BUS, 0.75f)
            else -> makePrediction(TransportModeLabel.DRIVING, 0.90f)
        }
    }

    private fun makePrediction(mode: TransportModeLabel, confidence: Float): TransportModePrediction {
        val probabilities = mutableMapOf(
            TransportModeLabel.WALKING to 0f,
            TransportModeLabel.CYCLING to 0f,
            TransportModeLabel.BUS to 0f,
            TransportModeLabel.DRIVING to 0f,
            TransportModeLabel.SUBWAY to 0f,
            TransportModeLabel.UNKNOWN to 0f
        )
        probabilities[mode] = confidence
        val remaining = 1f - confidence
        val otherModes = probabilities.keys.filter { it != mode && it != TransportModeLabel.UNKNOWN }
        otherModes.forEach { probabilities[it] = remaining / otherModes.size }
        return TransportModePrediction(mode = mode, confidence = confidence, probabilities = probabilities)
    }
}

/**
 * 道路信息
 */
data class RoadInfo(
    val roadType: String,  // motorway, trunk, primary, secondary, residential, cycleway, footway
    val speedLimit: Int? = null,  // 速度限制（km/h）
    val name: String? = null
)
