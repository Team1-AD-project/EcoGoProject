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
 * Hybrid Transport Mode Detector
 * Prioritizes Google Snap to Roads API, falls back to local sensor detection
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

    // Prediction result flow
    private val _detectedMode = MutableStateFlow<TransportModePrediction?>(null)
    val detectedMode: StateFlow<TransportModePrediction?> = _detectedMode

    // Prediction history (for smoothing)
    private val predictionHistory = LinkedList<TransportModeLabel>()
    private val historySize = 5

    // GPS trajectory points (for Snap to Roads)
    private val gpsTrajectory = LinkedList<com.google.android.gms.maps.model.LatLng>()
    private val MAX_TRAJECTORY_SIZE = 100

    // GPS speed list
    private val speedSequence = LinkedList<Float>()
    private val MAX_SPEED_SIZE = 100

    // Detection mode
    private var useSnapToRoads = !googleMapsApiKey.isNullOrEmpty()
    private var isDetecting = false
    private val isEmulator = detectEmulator()


    companion object {
        private const val TAG = "HybridTransportModeDetector"

        /**
         * Detect if running on an emulator
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
     * Start detection
     *
     * Strategy:
     * - Emulator: Snap-to-Roads API only (emulator has no real sensor data)
     * - Real device: ML local detection only (TFLite model + sensors + GPS speed)
     */
    fun startDetection() {
        if (isDetecting) {
            Log.w(TAG, "Already detecting")
            return
        }

        Log.d(TAG, "Starting transport mode detection (isEmulator=$isEmulator, useSnapToRoads=$useSnapToRoads)")
        isDetecting = true

        if (isEmulator) {
            // Emulator: Snap-to-Roads only (GPS road matching)
            Log.d(TAG, "Emulator detected → Snap-to-Roads only (no sensor ML)")
            if (useSnapToRoads) {
                startSnapToRoadsDetection()
            } else {
                Log.w(TAG, "Emulator without API Key: no detection method available")
            }
        } else {
            // Real device: ML local detection only (sensors + GPS speed)
            Log.d(TAG, "Real device detected → ML local detection (TFLite + sensors)")
            isTFLiteLoaded = tfliteDetector.loadModel()
            Log.d(TAG, "TFLite model loaded: $isTFLiteLoaded")
            startLocalDetection()
        }
    }

    /**
     * Stop detection
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
     * Start local sensor detection
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
     * Start Snap to Roads detection
     */
    private fun startSnapToRoadsDetection() {
        // Snap to Roads is passive detection: calls API when GPS updates arrive
        Log.d(TAG, "Snap to Roads detection ready (waiting for GPS updates)")
    }

    /**
     * Process local sensor window data (real device only)
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
     * Update GPS location (should be called periodically)
     */
    suspend fun updateLocation(location: Location) {
        // Real device: update sensor collector GPS speed (for ML features)
        if (!isEmulator) {
            sensorCollector.updateGpsSpeed(location)
        }

        // Add speed (real device ML needs GPS speed statistics)
        val speed = location.speed  // m/s
        speedSequence.add(speed)
        if (speedSequence.size > MAX_SPEED_SIZE) {
            speedSequence.removeFirst()
        }

        // Emulator: collect GPS trajectory for Snap-to-Roads
        if (isEmulator && useSnapToRoads) {
            val latLng = com.google.android.gms.maps.model.LatLng(location.latitude, location.longitude)
            gpsTrajectory.add(latLng)
            if (gpsTrajectory.size > MAX_TRAJECTORY_SIZE) {
                gpsTrajectory.removeFirst()
            }

            // Attempt Snap to Roads detection every 10 collected points
            if (gpsTrajectory.size >= 10 && gpsTrajectory.size % 10 == 0) {
                performSnapToRoadsDetection()
            }
        }
    }

    /**
     * Perform Snap to Roads detection
     */
    private suspend fun performSnapToRoadsDetection() {
        if (!isDetecting || googleMapsApiKey.isNullOrEmpty()) return

        try {
            Log.d(TAG, "Performing Snap to Roads detection with ${gpsTrajectory.size} points...")

            // Use only the most recent 15 speed points (faster response to speed changes)
            val recentSpeeds = if (speedSequence.size > 15) {
                speedSequence.toList().takeLast(15)
            } else {
                speedSequence.toList()
            }

            val prediction = snapToRoadsDetector.detectTransportMode(
                gpsPoints = gpsTrajectory.toList(),
                speeds = recentSpeeds,  // m/s (SnapToRoadsDetector converts to km/h internally)
                apiKey = googleMapsApiKey!!
            )

            if (prediction != null) {
                val smoothed = smoothPrediction(prediction)
                _detectedMode.value = smoothed
                Log.d(TAG, "Snap to Roads detection: raw=${prediction.mode}, smoothed=${smoothed.mode} (${smoothed.confidence})")
            } else {
                Log.w(TAG, "Snap to Roads detection returned null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Snap to Roads detection failed: ${e.message}", e)
            // Automatically fall back to local detection (already running)
        }
    }

    /**
     * Local prediction - using TFLite model (sensors + GPS speed)
     * Falls back to rule engine if TFLite loading fails
     */
    private fun predictTransportMode(features: SensorFeatures): TransportModePrediction {
        // Calculate GPS speed statistics
        val speeds = speedSequence.toList()
        val gpsSpeedMean = if (speeds.isNotEmpty()) speeds.average().toFloat() else 0f
        val gpsSpeedStd = if (speeds.size > 1) {
            val mean = speeds.average()
            sqrt(speeds.map { (it - mean) * (it - mean) }.average()).toFloat()
        } else 0f
        val gpsSpeedMax = speeds.maxOrNull() ?: 0f

        // Try TFLite model
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
                journeyDuration = 120f, // 5-second window, normalized
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

        // Fallback: rule engine
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
     * Smooth prediction results
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
     * Check if detection is running
     */
    fun isDetecting(): Boolean = isDetecting

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopDetection()
        sensorCollector.cleanup()
        tfliteDetector.release()
        scope.cancel()
    }
}
