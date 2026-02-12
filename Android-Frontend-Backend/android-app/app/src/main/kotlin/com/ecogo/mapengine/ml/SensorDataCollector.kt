package com.ecogo.mapengine.ml

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Sensor Data Collector
 * Responsible for collecting accelerometer, gyroscope, GPS, and barometer data
 */
class SensorDataCollector(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Sensors
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    // Current sensor readings
    private var currentAccX = 0f
    private var currentAccY = 0f
    private var currentAccZ = 0f
    private var currentGyroX = 0f
    private var currentGyroY = 0f
    private var currentGyroZ = 0f
    private var currentPressure = 1013.25f  // Standard atmospheric pressure
    private var currentGpsSpeed = 0f

    // Data buffer (for storing 5-second window data)
    private val dataBuffer = ConcurrentLinkedQueue<SensorRawData>()

    // Window parameters
    private val windowSizeMs = 5000L  // 5-second window
    private val samplingIntervalMs = 50L  // 20 Hz sampling rate
    private val slidingStepMs = 2500L  // 2.5-second sliding step

    // Coroutines
    private var collectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Data window flow (externally subscribable)
    private val _windowFlow = MutableStateFlow<SensorWindow?>(null)
    val windowFlow: StateFlow<SensorWindow?> = _windowFlow

    // Whether collection is active
    private var isCollecting = false

    companion object {
        private const val TAG = "SensorDataCollector"
    }

    /**
     * Start collecting sensor data
     */
    fun startCollecting() {
        if (isCollecting) {
            Log.w(TAG, "Already collecting data")
            return
        }

        Log.d(TAG, "Starting sensor data collection")
        isCollecting = true

        // Register sensor listeners
        accelerometer?.let {
            sensorManager.registerListener(
                this, it,
                SensorManager.SENSOR_DELAY_GAME  // ~20ms = 50Hz
            )
        } ?: Log.w(TAG, "Accelerometer not available")

        gyroscope?.let {
            sensorManager.registerListener(
                this, it,
                SensorManager.SENSOR_DELAY_GAME
            )
        } ?: Log.w(TAG, "Gyroscope not available")

        pressure?.let {
            sensorManager.registerListener(
                this, it,
                SensorManager.SENSOR_DELAY_NORMAL
            )
        } ?: Log.w(TAG, "Pressure sensor not available")

        // Start data sampling coroutine
        collectJob = scope.launch {
            while (isActive && isCollecting) {
                // Record current sensor readings every 50ms
                val data = SensorRawData(
                    timestamp = System.currentTimeMillis(),
                    accelerometerX = currentAccX,
                    accelerometerY = currentAccY,
                    accelerometerZ = currentAccZ,
                    gyroscopeX = currentGyroX,
                    gyroscopeY = currentGyroY,
                    gyroscopeZ = currentGyroZ,
                    gpsSpeed = currentGpsSpeed,
                    pressure = currentPressure
                )
                dataBuffer.offer(data)

                // Clean up old data (keep the last 5 seconds)
                val now = System.currentTimeMillis()
                while (dataBuffer.peek()?.timestamp?.let { now - it > windowSizeMs } == true) {
                    dataBuffer.poll()
                }

                // Generate a window every 2.5 seconds
                if (dataBuffer.size >= windowSizeMs / samplingIntervalMs) {
                    generateWindow()
                }

                delay(samplingIntervalMs)
            }
        }
    }

    /**
     * Stop collecting sensor data
     */
    fun stopCollecting() {
        if (!isCollecting) return

        Log.d(TAG, "Stopping sensor data collection")
        isCollecting = false

        // Unregister sensors
        sensorManager.unregisterListener(this)

        // Cancel coroutine
        collectJob?.cancel()
        collectJob = null

        // Clear buffer
        dataBuffer.clear()
    }

    /**
     * Update GPS speed (called from external LocationManager)
     */
    fun updateGpsSpeed(location: Location) {
        currentGpsSpeed = location.speed  // m/s
    }

    /**
     * Generate data window
     */
    private fun generateWindow() {
        val windowData = dataBuffer.toList()
        if (windowData.isEmpty()) return

        val window = SensorWindow(
            startTime = windowData.first().timestamp,
            endTime = windowData.last().timestamp,
            data = windowData
        )

        Log.d(TAG, "Generated window with ${windowData.size} samples")
        _windowFlow.value = window
    }

    /**
     * Sensor event callback
     */
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                currentAccX = event.values[0]
                currentAccY = event.values[1]
                currentAccZ = event.values[2]
            }
            Sensor.TYPE_GYROSCOPE -> {
                currentGyroX = event.values[0]
                currentGyroY = event.values[1]
                currentGyroZ = event.values[2]
            }
            Sensor.TYPE_PRESSURE -> {
                currentPressure = event.values[0]
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No need to handle accuracy changes
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopCollecting()
        scope.cancel()
    }

    /**
     * Save data window locally (for training data collection)
     */
    fun saveWindowForTraining(window: SensorWindow, label: TransportModeLabel) {
        // TODO: Save window data to local file or database
        // Format: CSV or JSON
        // For subsequent Random Forest model training
        Log.d(TAG, "Saving window with label: $label")
    }
}
