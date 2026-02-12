package com.ecogo.mapengine.ml

import android.content.Context
import android.location.Location
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.LinkedList

/**
 * Transport Mode Detector
 * Integrates sensor data collection, feature extraction, and model prediction
 */
class TransportModeDetector(private val context: Context) {

    private val sensorCollector = SensorDataCollector(context)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Prediction result flow
    private val _detectedMode = MutableStateFlow<TransportModePrediction?>(null)
    val detectedMode: StateFlow<TransportModePrediction?> = _detectedMode

    // Prediction history (for smoothing)
    private val predictionHistory = LinkedList<TransportModeLabel>()
    private val historySize = 3  // Keep the last 3 predictions

    // Whether detection is running
    private var isDetecting = false

    companion object {
        private const val TAG = "TransportModeDetector"
    }

    /**
     * Start transport mode detection
     */
    fun startDetection() {
        if (isDetecting) {
            Log.w(TAG, "Already detecting")
            return
        }

        Log.d(TAG, "Starting transport mode detection")
        isDetecting = true

        // Start collecting sensor data
        sensorCollector.startCollecting()

        // Listen to data windows
        scope.launch {
            sensorCollector.windowFlow.collect { window ->
                window?.let {
                    processWindow(it)
                }
            }
        }
    }

    /**
     * Stop detection
     */
    fun stopDetection() {
        if (!isDetecting) return

        Log.d(TAG, "Stopping transport mode detection")
        isDetecting = false

        sensorCollector.stopCollecting()
        predictionHistory.clear()
    }

    /**
     * Check if detection is running
     */
    fun isDetecting(): Boolean = isDetecting

    /**
     * Update GPS location (called externally)
     */
    fun updateLocation(location: Location) {
        sensorCollector.updateGpsSpeed(location)
    }

    /**
     * Process a data window
     */
    private suspend fun processWindow(window: SensorWindow) = withContext(Dispatchers.Default) {
        try {
            // 1. Extract features
            val features = SensorFeatureExtractor.extractFeatures(window)
            Log.d(TAG, "Extracted features: accMean=${features.accXMean}, gpsSpeed=${features.gpsSpeedMean}")

            // 2. Run model prediction
            val prediction = predictTransportMode(features)

            // 3. Smooth prediction result
            val smoothedPrediction = smoothPrediction(prediction)

            // 4. Update detection result
            _detectedMode.value = smoothedPrediction

            Log.d(TAG, "Detected mode: ${smoothedPrediction.mode} (confidence: ${smoothedPrediction.confidence})")

        } catch (e: Exception) {
            Log.e(TAG, "Error processing window", e)
        }
    }

    /**
     * Predict transport mode using model
     *
     * Uses simplified decision tree classifier
     * TODO: Replace with actual model after training Random Forest
     */
    private fun predictTransportMode(features: SensorFeatures): TransportModePrediction {
        // Convert features to array
        val featureArray = features.toFloatArray()

        // Use simplified decision tree classifier
        val (predictedClass, confidence) = SimpleDecisionTreeClassifier.predict(featureArray)

        // Convert class index to enum
        val mode = when (predictedClass) {
            0 -> TransportModeLabel.WALKING
            1 -> TransportModeLabel.CYCLING
            2 -> TransportModeLabel.BUS
            3 -> TransportModeLabel.SUBWAY
            4 -> TransportModeLabel.DRIVING
            else -> TransportModeLabel.UNKNOWN
        }

        // Get probability distribution
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
     * Smooth prediction results (using majority voting)
     */
    private fun smoothPrediction(prediction: TransportModePrediction): TransportModePrediction {
        // Add to history
        predictionHistory.add(prediction.mode)
        if (predictionHistory.size > historySize) {
            predictionHistory.removeFirst()
        }

        // If history is insufficient, return current prediction directly
        if (predictionHistory.size < historySize) {
            return prediction
        }

        // Majority voting
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
     * Clean up resources
     */
    fun cleanup() {
        stopDetection()
        sensorCollector.cleanup()
        scope.cancel()
    }
}
