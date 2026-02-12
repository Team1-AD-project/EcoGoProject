package com.ecogo.mapengine.ml

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Transport Mode Detector Integration Example
 * Demonstrates how to use sensor collection and transport mode detection in MapActivity
 */
class TransportModeDetectorIntegration(
    private val context: Context,
    private val onModeDetected: (TransportModePrediction) -> Unit
) : LifecycleObserver {

    private val detector = TransportModeDetector(context)
    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        private const val TAG = "TMDetectorIntegration"
    }

    /**
     * Start detection (called when navigation starts)
     */
    fun start() {
        Log.d(TAG, "Starting transport mode detection")
        detector.startDetection()

        // Listen for detection results
        scope.launch {
            detector.detectedMode.collect { prediction ->
                prediction?.let {
                    Log.d(TAG, "Mode detected: ${it.mode} (${it.confidence})")
                    onModeDetected(it)
                }
            }
        }
    }

    /**
     * Stop detection (called when navigation ends)
     */
    fun stop() {
        Log.d(TAG, "Stopping transport mode detection")
        detector.stopDetection()
    }

    /**
     * Update location (called when LocationManager updates)
     */
    fun updateLocation(location: Location) {
        detector.updateLocation(location)
    }

    /**
     * Clean up resources
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun cleanup() {
        detector.cleanup()
    }
}

/**
 * Usage example in MapActivity:
 *
 * ```kotlin
 * class MapActivity : AppCompatActivity() {
 *
 *     private lateinit var modeDetector: TransportModeDetectorIntegration
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         // Initialize transport mode detector
 *         modeDetector = TransportModeDetectorIntegration(this) { prediction ->
 *             // Transport mode change detected
 *             handleModeChange(prediction)
 *         }
 *
 *         // Bind detector to lifecycle
 *         lifecycle.addObserver(modeDetector)
 *     }
 *
 *     private fun startLocationTracking() {
 *         // ... existing location logic ...
 *
 *         // Start transport mode detection
 *         modeDetector.start()
 *     }
 *
 *     private fun stopLocationTracking() {
 *         // ... existing location logic ...
 *
 *         // Stop transport mode detection
 *         modeDetector.stop()
 *     }
 *
 *     private fun onLocationChanged(location: Location) {
 *         // ... existing location update logic ...
 *
 *         // Update detector location (for GPS speed)
 *         modeDetector.updateLocation(location)
 *     }
 *
 *     private fun handleModeChange(prediction: TransportModePrediction) {
 *         val currentMode = viewModel.selectedTransportMode.value
 *         val detectedMode = prediction.mode
 *
 *         // Check if detected mode differs from user's selected mode
 *         if (isModeMismatch(currentMode, detectedMode) && prediction.confidence > 0.7f) {
 *             // Show dialog asking user whether to switch
 *             showModeSwitchDialog(detectedMode)
 *         }
 *
 *         // Update real-time carbon calculation (using detected transport mode)
 *         updateRealTimeCarbonCalculation(detectedMode)
 *     }
 *
 *     private fun isModeMismatch(
 *         userSelected: TransportMode?,
 *         detected: TransportModeLabel
 *     ): Boolean {
 *         // Determine if there is a clear mismatch
 *         return when {
 *             userSelected == TransportMode.WALKING && detected == TransportModeLabel.DRIVING -> true
 *             userSelected == TransportMode.CYCLING && detected == TransportModeLabel.BUS -> true
 *             userSelected == TransportMode.DRIVING && detected == TransportModeLabel.WALKING -> true
 *             else -> false
 *         }
 *     }
 *
 *     private fun showModeSwitchDialog(detectedMode: TransportModeLabel) {
 *         AlertDialog.Builder(this)
 *             .setTitle("Transport Mode Change Detected")
 *             .setMessage("It appears you are using ${detectedMode.displayName()}. Would you like to switch?")
 *             .setPositiveButton("Switch") { _, _ ->
 *                 switchToDetectedMode(detectedMode)
 *             }
 *             .setNegativeButton("Keep Current", null)
 *             .show()
 *     }
 *
 *     private fun switchToDetectedMode(mode: TransportModeLabel) {
 *         // Update transport mode in ViewModel
 *         val transportMode = when (mode) {
 *             TransportModeLabel.WALKING -> TransportMode.WALKING
 *             TransportModeLabel.CYCLING -> TransportMode.CYCLING
 *             TransportModeLabel.BUS -> TransportMode.BUS
 *             TransportModeLabel.DRIVING -> TransportMode.DRIVING
 *             else -> return
 *         }
 *         viewModel.updateSelectedTransportMode(transportMode)
 *     }
 * }
 *
 * // Helper extension function
 * fun TransportModeLabel.displayName(): String {
 *     return when (this) {
 *         TransportModeLabel.WALKING -> "Walking"
 *         TransportModeLabel.CYCLING -> "Cycling"
 *         TransportModeLabel.BUS -> "Bus"
 *         TransportModeLabel.SUBWAY -> "Subway"
 *         TransportModeLabel.DRIVING -> "Driving"
 *         TransportModeLabel.UNKNOWN -> "Unknown"
 *     }
 * }
 * ```
 */
