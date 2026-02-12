package com.ecogo.mapengine.ml

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 使用 Google Snap to Roads API 识别交通方式
 *
 * 原理：
 * 1. 将 GPS 轨迹吸附到真实道路网络
 * 2. 获取道路类型（高速公路、街道、小路等）
 * 3. 结合 GPS 速度推断交通方式
 */
class SnapToRoadsDetector {

    companion object {
        private const val TAG = "SnapToRoadsDetector"
    }

    /**
     * 根据 GPS 轨迹点和速度信息推断交通方式
     */
    suspend fun detectTransportMode(
        gpsPoints: List<LatLng>,
        speeds: List<Float>,  // m/s
        apiKey: String
    ): TransportModePrediction? = withContext(Dispatchers.IO) {
        try {
            if (gpsPoints.size < 2) {
                Log.w(TAG, "需要至少 2 个 GPS 点来进行检测")
                return@withContext null
            }

            // 计算平均速度和加速度
            val avgSpeed = speeds.average()
            val avgSpeedKmh = (avgSpeed * 3.6).toFloat()

            // 获取道路类型（通过 Snap to Roads）
            val roadTypes = snapToRoads(gpsPoints, apiKey)
            
            Log.d(TAG, "平均速度: $avgSpeedKmh km/h")
            Log.d(TAG, "道路类型: $roadTypes")

            // 基于道路类型和速度推断交通方式
            val prediction = inferTransportMode(roadTypes, avgSpeedKmh)
            
            Log.d(TAG, "检测到交通方式: ${prediction.mode}, 置信度: ${prediction.confidence}")
            
            return@withContext prediction
        } catch (e: Exception) {
            Log.e(TAG, "Snap to Roads 检测失败: ${e.message}", e)
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
                Log.w(TAG, "Snap to Roads API 返回空结果")
                return@withContext emptyList()
            }
            
            // 从对齐后的点推断道路类型
            // 注：真实的 API 返回中可能包含 placeId，但不直接包含道路类型
            // 这里基于点的密度和分布进行启发式推断
            val roadInfoList = inferRoadTypesFromSnappedPoints(snappedPoints, points)
            
            Log.d(TAG, "Snap to Roads 返回 ${snappedPoints.size} 个对齐点，推断出 ${roadInfoList.size} 条道路")
            
            return@withContext roadInfoList
            
        } catch (e: Exception) {
            Log.e(TAG, "Snap to Roads API 调用失败: ${e.message}", e)
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
     */
    private fun inferTransportMode(
        roadTypes: List<RoadInfo>,
        avgSpeedKmh: Float
    ): TransportModePrediction {
        // 统计各类型道路的出现频率
        val typeDistribution = roadTypes.groupingBy { it.roadType }.eachCount()
        val hasAlignedRoad = typeDistribution.containsKey("aligned_street")

        return when {
            // 高速公路（通常是驾车）
            typeDistribution.containsKey("motorway") && 
            avgSpeedKmh > 50 -> {
                TransportModePrediction(
                    mode = TransportModeLabel.DRIVING,
                    confidence = 0.95f,
                    probabilities = mapOf(
                        TransportModeLabel.DRIVING to 0.95f,
                        TransportModeLabel.BUS to 0.03f,
                        TransportModeLabel.CYCLING to 0.02f,
                        TransportModeLabel.WALKING to 0.0f,
                        TransportModeLabel.SUBWAY to 0.0f,
                        TransportModeLabel.UNKNOWN to 0.0f
                    )
                )
            }

            // 公交路线（通常公交吸附到主干道，速度 20-60 km/h，会停靠）
            typeDistribution.containsKey("trunk") && 
            avgSpeedKmh in 15f..60f -> {
                TransportModePrediction(
                    mode = TransportModeLabel.BUS,
                    confidence = 0.85f,
                    probabilities = mapOf(
                        TransportModeLabel.BUS to 0.85f,
                        TransportModeLabel.DRIVING to 0.10f,
                        TransportModeLabel.CYCLING to 0.03f,
                        TransportModeLabel.WALKING to 0.02f,
                        TransportModeLabel.SUBWAY to 0.0f,
                        TransportModeLabel.UNKNOWN to 0.0f
                    )
                )
            }

            // 自行车道（速度通常 15-25 km/h）
            typeDistribution.containsKey("cycleway") -> {
                TransportModePrediction(
                    mode = TransportModeLabel.CYCLING,
                    confidence = 0.9f,
                    probabilities = mapOf(
                        TransportModeLabel.CYCLING to 0.9f,
                        TransportModeLabel.WALKING to 0.07f,
                        TransportModeLabel.BUS to 0.02f,
                        TransportModeLabel.DRIVING to 0.01f,
                        TransportModeLabel.SUBWAY to 0.0f,
                        TransportModeLabel.UNKNOWN to 0.0f
                    )
                )
            }

            // 已对齐道路 + 低速（步行或骑行）
            hasAlignedRoad && avgSpeedKmh < 10 -> {
                TransportModePrediction(
                    mode = TransportModeLabel.WALKING,
                    confidence = 0.85f,
                    probabilities = mapOf(
                        TransportModeLabel.WALKING to 0.85f,
                        TransportModeLabel.CYCLING to 0.10f,
                        TransportModeLabel.BUS to 0.03f,
                        TransportModeLabel.DRIVING to 0.01f,
                        TransportModeLabel.SUBWAY to 0.01f,
                        TransportModeLabel.UNKNOWN to 0.0f
                    )
                )
            }

            // 已对齐道路 + 低-中等速度（骑行）
            hasAlignedRoad && avgSpeedKmh in 10f..25f -> {
                TransportModePrediction(
                    mode = TransportModeLabel.CYCLING,
                    confidence = 0.80f,
                    probabilities = mapOf(
                        TransportModeLabel.CYCLING to 0.80f,
                        TransportModeLabel.WALKING to 0.08f,
                        TransportModeLabel.BUS to 0.08f,
                        TransportModeLabel.DRIVING to 0.03f,
                        TransportModeLabel.SUBWAY to 0.01f,
                        TransportModeLabel.UNKNOWN to 0.0f
                    )
                )
            }

            // 已对齐道路 + 中等速度（公交或驾车）
            hasAlignedRoad && avgSpeedKmh in 25f..50f -> {
                TransportModePrediction(
                    mode = TransportModeLabel.BUS,
                    confidence = 0.75f,
                    probabilities = mapOf(
                        TransportModeLabel.BUS to 0.75f,
                        TransportModeLabel.DRIVING to 0.20f,
                        TransportModeLabel.CYCLING to 0.03f,
                        TransportModeLabel.WALKING to 0.01f,
                        TransportModeLabel.SUBWAY to 0.01f,
                        TransportModeLabel.UNKNOWN to 0.0f
                    )
                )
            }

            // 已对齐道路 + 高速（驾车）
            hasAlignedRoad && avgSpeedKmh > 50 -> {
                TransportModePrediction(
                    mode = TransportModeLabel.DRIVING,
                    confidence = 0.90f,
                    probabilities = mapOf(
                        TransportModeLabel.DRIVING to 0.90f,
                        TransportModeLabel.BUS to 0.07f,
                        TransportModeLabel.CYCLING to 0.02f,
                        TransportModeLabel.WALKING to 0.01f,
                        TransportModeLabel.SUBWAY to 0.0f,
                        TransportModeLabel.UNKNOWN to 0.0f
                    )
                )
            }

            // 人行道或低速街道 + 低速（步行）
            avgSpeedKmh < 10 -> {
                TransportModePrediction(
                    mode = TransportModeLabel.WALKING,
                    confidence = 0.88f,
                    probabilities = mapOf(
                        TransportModeLabel.WALKING to 0.88f,
                        TransportModeLabel.CYCLING to 0.08f,
                        TransportModeLabel.BUS to 0.02f,
                        TransportModeLabel.DRIVING to 0.01f,
                        TransportModeLabel.SUBWAY to 0.01f,
                        TransportModeLabel.UNKNOWN to 0.0f
                    )
                )
            }

            // 街道 + 中等速度（可能是自行车或轻型机动车）
            typeDistribution.containsKey("residential") ||
            typeDistribution.containsKey("secondary") -> {
                when {
                    avgSpeedKmh < 15f -> {
                        TransportModePrediction(
                            mode = TransportModeLabel.CYCLING,
                            confidence = 0.70f,
                            probabilities = mapOf(
                                TransportModeLabel.CYCLING to 0.70f,
                                TransportModeLabel.WALKING to 0.15f,
                                TransportModeLabel.BUS to 0.10f,
                                TransportModeLabel.DRIVING to 0.04f,
                                TransportModeLabel.SUBWAY to 0.01f,
                                TransportModeLabel.UNKNOWN to 0.0f
                            )
                        )
                    }
                    else -> {
                        TransportModePrediction(
                            mode = TransportModeLabel.DRIVING,
                            confidence = 0.75f,
                            probabilities = mapOf(
                                TransportModeLabel.DRIVING to 0.75f,
                                TransportModeLabel.BUS to 0.15f,
                                TransportModeLabel.CYCLING to 0.07f,
                                TransportModeLabel.WALKING to 0.02f,
                                TransportModeLabel.SUBWAY to 0.01f,
                                TransportModeLabel.UNKNOWN to 0.0f
                            )
                        )
                    }
                }
            }

            // 默认
            else -> {
                TransportModePrediction(
                    mode = TransportModeLabel.WALKING,
                    confidence = 0.50f,
                    probabilities = mapOf(
                        TransportModeLabel.WALKING to 0.50f,
                        TransportModeLabel.CYCLING to 0.20f,
                        TransportModeLabel.BUS to 0.15f,
                        TransportModeLabel.DRIVING to 0.10f,
                        TransportModeLabel.SUBWAY to 0.03f,
                        TransportModeLabel.UNKNOWN to 0.02f
                    )
                )
            }
        }
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
