package com.ecogo.mapengine.ml

import kotlin.math.sqrt

/**
 * Sensor Feature Extractor
 * Extracts statistical features from raw sensor data windows
 */
object SensorFeatureExtractor {

    /**
     * Extract features from data window
     */
    fun extractFeatures(window: SensorWindow): SensorFeatures {
        if (window.data.isEmpty()) {
            return createZeroFeatures()
        }

        // Extract per-axis data
        val accX = window.data.map { it.accelerometerX }
        val accY = window.data.map { it.accelerometerY }
        val accZ = window.data.map { it.accelerometerZ }

        val gyroX = window.data.map { it.gyroscopeX }
        val gyroY = window.data.map { it.gyroscopeY }
        val gyroZ = window.data.map { it.gyroscopeZ }

        val gpsSpeed = window.data.map { it.gpsSpeed }
        val pressure = window.data.map { it.pressure }

        // Calculate accelerometer magnitude
        val accMagnitude = window.data.map { data ->
            sqrt(data.accelerometerX * data.accelerometerX +
                 data.accelerometerY * data.accelerometerY +
                 data.accelerometerZ * data.accelerometerZ)
        }

        // Calculate gyroscope magnitude
        val gyroMagnitude = window.data.map { data ->
            sqrt(data.gyroscopeX * data.gyroscopeX +
                 data.gyroscopeY * data.gyroscopeY +
                 data.gyroscopeZ * data.gyroscopeZ)
        }

        return SensorFeatures(
            // Accelerometer X-axis features
            accXMean = accX.mean(),
            accXStd = accX.std(),
            accXMax = accX.maxOrNull() ?: 0f,
            accXMin = accX.minOrNull() ?: 0f,
            accXRange = (accX.maxOrNull() ?: 0f) - (accX.minOrNull() ?: 0f),
            accXMedian = accX.median(),
            accXSma = accX.sma(),

            // Accelerometer Y-axis features
            accYMean = accY.mean(),
            accYStd = accY.std(),
            accYMax = accY.maxOrNull() ?: 0f,
            accYMin = accY.minOrNull() ?: 0f,
            accYRange = (accY.maxOrNull() ?: 0f) - (accY.minOrNull() ?: 0f),
            accYMedian = accY.median(),
            accYSma = accY.sma(),

            // Accelerometer Z-axis features
            accZMean = accZ.mean(),
            accZStd = accZ.std(),
            accZMax = accZ.maxOrNull() ?: 0f,
            accZMin = accZ.minOrNull() ?: 0f,
            accZRange = (accZ.maxOrNull() ?: 0f) - (accZ.minOrNull() ?: 0f),
            accZMedian = accZ.median(),
            accZSma = accZ.sma(),

            // Gyroscope X-axis features
            gyroXMean = gyroX.mean(),
            gyroXStd = gyroX.std(),
            gyroXMax = gyroX.maxOrNull() ?: 0f,
            gyroXMin = gyroX.minOrNull() ?: 0f,
            gyroXRange = (gyroX.maxOrNull() ?: 0f) - (gyroX.minOrNull() ?: 0f),
            gyroXMedian = gyroX.median(),
            gyroXSma = gyroX.sma(),

            // Gyroscope Y-axis features
            gyroYMean = gyroY.mean(),
            gyroYStd = gyroY.std(),
            gyroYMax = gyroY.maxOrNull() ?: 0f,
            gyroYMin = gyroY.minOrNull() ?: 0f,
            gyroYRange = (gyroY.maxOrNull() ?: 0f) - (gyroY.minOrNull() ?: 0f),
            gyroYMedian = gyroY.median(),
            gyroYSma = gyroY.sma(),

            // Gyroscope Z-axis features
            gyroZMean = gyroZ.mean(),
            gyroZStd = gyroZ.std(),
            gyroZMax = gyroZ.maxOrNull() ?: 0f,
            gyroZMin = gyroZ.minOrNull() ?: 0f,
            gyroZRange = (gyroZ.maxOrNull() ?: 0f) - (gyroZ.minOrNull() ?: 0f),
            gyroZMedian = gyroZ.median(),
            gyroZSma = gyroZ.sma(),

            // Accelerometer magnitude features
            accMagnitudeMean = accMagnitude.mean(),
            accMagnitudeStd = accMagnitude.std(),
            accMagnitudeMax = accMagnitude.maxOrNull() ?: 0f,

            // Gyroscope magnitude features
            gyroMagnitudeMean = gyroMagnitude.mean(),
            gyroMagnitudeStd = gyroMagnitude.std(),
            gyroMagnitudeMax = gyroMagnitude.maxOrNull() ?: 0f,

            // GPS features
            gpsSpeedMean = gpsSpeed.mean(),
            gpsSpeedStd = gpsSpeed.std(),
            gpsSpeedMax = gpsSpeed.maxOrNull() ?: 0f,

            // Barometric pressure features
            pressureMean = pressure.mean(),
            pressureStd = pressure.std()
        )
    }

    /**
     * Create zero features (for empty data cases)
     */
    private fun createZeroFeatures(): SensorFeatures {
        return SensorFeatures(
            accXMean = 0f, accXStd = 0f, accXMax = 0f, accXMin = 0f, accXRange = 0f, accXMedian = 0f, accXSma = 0f,
            accYMean = 0f, accYStd = 0f, accYMax = 0f, accYMin = 0f, accYRange = 0f, accYMedian = 0f, accYSma = 0f,
            accZMean = 0f, accZStd = 0f, accZMax = 0f, accZMin = 0f, accZRange = 0f, accZMedian = 0f, accZSma = 0f,
            gyroXMean = 0f, gyroXStd = 0f, gyroXMax = 0f, gyroXMin = 0f, gyroXRange = 0f, gyroXMedian = 0f, gyroXSma = 0f,
            gyroYMean = 0f, gyroYStd = 0f, gyroYMax = 0f, gyroYMin = 0f, gyroYRange = 0f, gyroYMedian = 0f, gyroYSma = 0f,
            gyroZMean = 0f, gyroZStd = 0f, gyroZMax = 0f, gyroZMin = 0f, gyroZRange = 0f, gyroZMedian = 0f, gyroZSma = 0f,
            accMagnitudeMean = 0f, accMagnitudeStd = 0f, accMagnitudeMax = 0f,
            gyroMagnitudeMean = 0f, gyroMagnitudeStd = 0f, gyroMagnitudeMax = 0f,
            gpsSpeedMean = 0f, gpsSpeedStd = 0f, gpsSpeedMax = 0f,
            pressureMean = 0f, pressureStd = 0f
        )
    }

    // ============ Statistical Extension Functions ============

    private fun List<Float>.mean(): Float {
        return if (isEmpty()) 0f else sum() / size
    }

    private fun List<Float>.std(): Float {
        if (size < 2) return 0f
        val mean = mean()
        val variance = map { (it - mean) * (it - mean) }.sum() / size
        return sqrt(variance)
    }

    private fun List<Float>.median(): Float {
        if (isEmpty()) return 0f
        val sorted = sorted()
        return if (size % 2 == 0) {
            (sorted[size / 2 - 1] + sorted[size / 2]) / 2
        } else {
            sorted[size / 2]
        }
    }

    private fun List<Float>.sma(): Float {
        // Signal Magnitude Area
        return if (isEmpty()) 0f else map { kotlin.math.abs(it) }.sum() / size
    }
}
