package com.ecogo.mapengine.ml

import android.content.Context
import android.location.Location
import android.os.Build
import android.util.Log
import com.ecogo.mapengine.ml.model.JourneyFeatures
import com.ecogo.mapengine.ml.tflite.TensorFlowLiteDetector
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.LinkedList
import kotlin.math.sqrt

/**
 * 混合交通方式检测器
 * 优先使用 Google Snap to Roads API，回退到本地传感器检测
 */
class HybridTransportModeDetector(
    private val context: Context,
    private val googleMapsApiKey: String? = null
) {

    private val sensorCollector = SensorDataCollector(context)
    private val snapToRoadsDetector = SnapToRoadsDetector()
    private val tfliteDetector = TensorFlowLiteDetector(context)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isTFLiteLoaded = false

    // 预测结果流
    private val _detectedMode = MutableStateFlow<TransportModePrediction?>(null)
    val detectedMode: StateFlow<TransportModePrediction?> = _detectedMode

    // 预测历史（用于平滑）
    private val predictionHistory = LinkedList<TransportModeLabel>()
    private val historySize = 3

    // GPS 轨迹点（用于 Snap to Roads）
    private val gpsTrajectory = LinkedList<com.google.android.gms.maps.model.LatLng>()
    private val MAX_TRAJECTORY_SIZE = 100

    // GPS 速度列表
    private val speedSequence = LinkedList<Float>()
    private val MAX_SPEED_SIZE = 100

    // 检测方式
    private var useSnapToRoads = !googleMapsApiKey.isNullOrEmpty()
    private var isDetecting = false
    private val isEmulator = detectEmulator()


    companion object {
        private const val TAG = "HybridTransportModeDetector"

        /**
         * 检测是否运行在模拟器上
         */
        private fun detectEmulator(): Boolean {
            return (Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    || Build.BRAND.startsWith("generic")
                    || Build.DEVICE.startsWith("generic")
                    || Build.HARDWARE.contains("goldfish")
                    || Build.HARDWARE.contains("ranchu")
                    || Build.PRODUCT.contains("sdk")
                    || Build.PRODUCT.contains("emulator"))
        }
    }

    /**
     * 开始检测
     *
     * 策略：
     * - 模拟器：只用 Snap-to-Roads API（模拟器无真实传感器数据）
     * - 真机：  只用 ML 本地检测（TFLite 模型 + 传感器 + GPS 速度）
     */
    fun startDetection() {
        if (isDetecting) {
            Log.w(TAG, "Already detecting")
            return
        }

        Log.d(TAG, "Starting transport mode detection (isEmulator=$isEmulator, useSnapToRoads=$useSnapToRoads)")
        isDetecting = true

        if (isEmulator) {
            // 模拟器：只用 Snap-to-Roads（基于 GPS 道路匹配）
            Log.d(TAG, "Emulator detected → Snap-to-Roads only (no sensor ML)")
            if (useSnapToRoads) {
                startSnapToRoadsDetection()
            } else {
                Log.w(TAG, "Emulator without API Key: no detection method available")
            }
        } else {
            // 真机：只用 ML 本地检测（传感器 + GPS 速度）
            Log.d(TAG, "Real device detected → ML local detection (TFLite + sensors)")
            isTFLiteLoaded = tfliteDetector.loadModel()
            Log.d(TAG, "TFLite model loaded: $isTFLiteLoaded")
            startLocalDetection()
        }
    }

    /**
     * 停止检测
     */
    fun stopDetection() {
        if (!isDetecting) return

        Log.d(TAG, "Stopping hybrid transport mode detection")
        isDetecting = false

        sensorCollector.stopCollecting()
        predictionHistory.clear()
        gpsTrajectory.clear()
        speedSequence.clear()
    }

    /**
     * 启动本地传感器检测
     */
    private fun startLocalDetection() {
        sensorCollector.startCollecting()

        scope.launch {
            sensorCollector.windowFlow.collect { window ->
                window?.let {
                    processLocalWindow(it)
                }
            }
        }
    }

    /**
     * 启动 Snap to Roads 检测
     */
    private fun startSnapToRoadsDetection() {
        // Snap to Roads 是被动检测：等待 GPS 更新时调用 API
        Log.d(TAG, "Snap to Roads detection ready (waiting for GPS updates)")
    }

    /**
     * 处理本地传感器窗口数据（真机专用）
     */
    private suspend fun processLocalWindow(window: SensorWindow) = withContext(Dispatchers.Default) {
        try {
            val features = SensorFeatureExtractor.extractFeatures(window)
            val prediction = predictTransportMode(features)
            val smoothedPrediction = smoothPrediction(prediction)

            _detectedMode.value = smoothedPrediction
            Log.d(TAG, "ML detection: ${smoothedPrediction.mode} (confidence=${smoothedPrediction.confidence})")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing local window", e)
        }
    }

    /**
     * 更新 GPS 位置（应定期调用）
     */
    suspend fun updateLocation(location: Location) {
        // 真机：更新传感器收集器的 GPS 速度（用于 ML 特征）
        if (!isEmulator) {
            sensorCollector.updateGpsSpeed(location)
        }

        // 添加速度（真机 ML 需要 GPS 速度统计）
        val speed = location.speed  // m/s
        speedSequence.add(speed)
        if (speedSequence.size > MAX_SPEED_SIZE) {
            speedSequence.removeFirst()
        }

        // 模拟器：收集 GPS 轨迹用于 Snap-to-Roads
        if (isEmulator && useSnapToRoads) {
            val latLng = com.google.android.gms.maps.model.LatLng(location.latitude, location.longitude)
            gpsTrajectory.add(latLng)
            if (gpsTrajectory.size > MAX_TRAJECTORY_SIZE) {
                gpsTrajectory.removeFirst()
            }

            // 每采集 10 个点尝试一次 Snap to Roads 检测
            if (gpsTrajectory.size >= 10 && gpsTrajectory.size % 10 == 0) {
                performSnapToRoadsDetection()
            }
        }
    }

    /**
     * 执行 Snap to Roads 检测
     */
    private suspend fun performSnapToRoadsDetection() {
        if (!isDetecting || googleMapsApiKey.isNullOrEmpty()) return

        try {
            Log.d(TAG, "Performing Snap to Roads detection with ${gpsTrajectory.size} points...")

            // 只使用最近 15 个速度点（更快响应速度变化）
            val recentSpeeds = if (speedSequence.size > 15) {
                speedSequence.toList().takeLast(15)
            } else {
                speedSequence.toList()
            }

            val prediction = snapToRoadsDetector.detectTransportMode(
                gpsPoints = gpsTrajectory.toList(),
                speeds = recentSpeeds,  // m/s（SnapToRoadsDetector 内部会转 km/h）
                apiKey = googleMapsApiKey!!
            )

            if (prediction != null) {
                _detectedMode.value = prediction
                Log.d(TAG, "Snap to Roads detection: ${prediction.mode} (${prediction.confidence})")
            } else {
                Log.w(TAG, "Snap to Roads detection returned null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Snap to Roads detection failed: ${e.message}", e)
            // 自动回退到本地检测（已在运行）
        }
    }

    /**
     * 本地预测 - 使用 TFLite 模型 (传感器 + GPS 速度)
     * 如果 TFLite 加载失败，回退到规则引擎
     */
    private fun predictTransportMode(features: SensorFeatures): TransportModePrediction {
        // 计算 GPS 速度统计
        val speeds = speedSequence.toList()
        val gpsSpeedMean = if (speeds.isNotEmpty()) speeds.average().toFloat() else 0f
        val gpsSpeedStd = if (speeds.size > 1) {
            val mean = speeds.average()
            sqrt(speeds.map { (it - mean) * (it - mean) }.average()).toFloat()
        } else 0f
        val gpsSpeedMax = speeds.maxOrNull() ?: 0f

        // 尝试 TFLite 模型
        if (isTFLiteLoaded) {
            val journeyFeatures = JourneyFeatures(
                accelMeanX = features.accXMean,
                accelMeanY = features.accYMean,
                accelMeanZ = features.accZMean,
                accelStdX = features.accXStd,
                accelStdY = features.accYStd,
                accelStdZ = features.accZStd,
                accelMagnitude = features.accMagnitudeMean,
                gyroMeanX = features.gyroXMean,
                gyroMeanY = features.gyroYMean,
                gyroMeanZ = features.gyroZMean,
                gyroStdX = features.gyroXStd,
                gyroStdY = features.gyroYStd,
                gyroStdZ = features.gyroZStd,
                journeyDuration = 120f, // 5秒窗口，归一化
                gpsSpeedMean = gpsSpeedMean,
                gpsSpeedStd = gpsSpeedStd,
                gpsSpeedMax = gpsSpeedMax,
                transportMode = ""
            )

            val tfliteResult = tfliteDetector.predict(journeyFeatures)
            if (tfliteResult != null) {
                val mode = when (tfliteResult.predictedMode) {
                    "WALKING" -> TransportModeLabel.WALKING
                    "CYCLING" -> TransportModeLabel.CYCLING
                    "BUS" -> TransportModeLabel.BUS
                    "SUBWAY" -> TransportModeLabel.SUBWAY
                    "DRIVING" -> TransportModeLabel.DRIVING
                    else -> TransportModeLabel.UNKNOWN
                }
                val probabilities = mapOf(
                    TransportModeLabel.WALKING to (tfliteResult.probabilities["WALKING"] ?: 0f),
                    TransportModeLabel.CYCLING to (tfliteResult.probabilities["CYCLING"] ?: 0f),
                    TransportModeLabel.BUS to (tfliteResult.probabilities["BUS"] ?: 0f),
                    TransportModeLabel.SUBWAY to (tfliteResult.probabilities["SUBWAY"] ?: 0f),
                    TransportModeLabel.DRIVING to (tfliteResult.probabilities["DRIVING"] ?: 0f),
                    TransportModeLabel.UNKNOWN to 0f
                )
                return TransportModePrediction(
                    mode = mode,
                    confidence = tfliteResult.confidence,
                    probabilities = probabilities
                )
            }
        }

        // 回退：规则引擎
        val featureArray = features.toFloatArray()
        val (predictedClass, confidence) = SimpleDecisionTreeClassifier.predict(featureArray)

        val mode = when (predictedClass) {
            0 -> TransportModeLabel.WALKING
            1 -> TransportModeLabel.CYCLING
            2 -> TransportModeLabel.BUS
            3 -> TransportModeLabel.SUBWAY
            4 -> TransportModeLabel.DRIVING
            else -> TransportModeLabel.UNKNOWN
        }

        val probArray = SimpleDecisionTreeClassifier.predictProba(featureArray)
        val probabilities = mapOf(
            TransportModeLabel.WALKING to probArray[0],
            TransportModeLabel.CYCLING to probArray[1],
            TransportModeLabel.BUS to probArray[2],
            TransportModeLabel.SUBWAY to probArray[3],
            TransportModeLabel.DRIVING to probArray[4],
            TransportModeLabel.UNKNOWN to 0f
        )

        return TransportModePrediction(
            mode = mode,
            confidence = confidence,
            probabilities = probabilities
        )
    }

    /**
     * 平滑预测结果
     */
    private fun smoothPrediction(prediction: TransportModePrediction): TransportModePrediction {
        predictionHistory.add(prediction.mode)
        if (predictionHistory.size > historySize) {
            predictionHistory.removeFirst()
        }

        if (predictionHistory.size < historySize) {
            return prediction
        }

        val modeCount = predictionHistory.groupingBy { it }.eachCount()
        val majorityMode = modeCount.maxByOrNull { it.value }?.key ?: prediction.mode
        val majorityConfidence = modeCount[majorityMode]!!.toFloat() / historySize

        return TransportModePrediction(
            mode = majorityMode,
            confidence = majorityConfidence,
            probabilities = prediction.probabilities
        )
    }

    /**
     * 检查是否正在检测
     */
    fun isDetecting(): Boolean = isDetecting

    /**
     * 清理资源
     */
    fun cleanup() {
        stopDetection()
        sensorCollector.cleanup()
        tfliteDetector.release()
        scope.cancel()
    }
}
