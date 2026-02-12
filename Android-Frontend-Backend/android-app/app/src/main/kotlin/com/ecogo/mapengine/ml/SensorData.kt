package com.ecogo.mapengine.ml

/**
 * Sensor raw data
 */
data class SensorRawData(
    val timestamp: Long,
    val accelerometerX: Float,
    val accelerometerY: Float,
    val accelerometerZ: Float,
    val gyroscopeX: Float,
    val gyroscopeY: Float,
    val gyroscopeZ: Float,
    val gpsSpeed: Float,  // Unit: m/s
    val pressure: Float   // Barometric pressure, unit: hPa
)

/**
 * Sensor data window (5-second window)
 */
data class SensorWindow(
    val startTime: Long,
    val endTime: Long,
    val data: List<SensorRawData>,
    val label: TransportModeLabel? = null  // Label for training
)

/**
 * Transport mode label (for data annotation)
 */
enum class TransportModeLabel {
    WALKING,
    CYCLING,
    BUS,
    SUBWAY,
    DRIVING,
    UNKNOWN
}

/**
 * Extracted features
 */
data class SensorFeatures(
    // Accelerometer statistical features (3 axes x 7 features = 21)
    val accXMean: Float,
    val accXStd: Float,
    val accXMax: Float,
    val accXMin: Float,
    val accXRange: Float,
    val accXMedian: Float,
    val accXSma: Float,  // Signal Magnitude Area

    val accYMean: Float,
    val accYStd: Float,
    val accYMax: Float,
    val accYMin: Float,
    val accYRange: Float,
    val accYMedian: Float,
    val accYSma: Float,

    val accZMean: Float,
    val accZStd: Float,
    val accZMax: Float,
    val accZMin: Float,
    val accZRange: Float,
    val accZMedian: Float,
    val accZSma: Float,

    // Gyroscope statistical features (3 axes x 7 features = 21)
    val gyroXMean: Float,
    val gyroXStd: Float,
    val gyroXMax: Float,
    val gyroXMin: Float,
    val gyroXRange: Float,
    val gyroXMedian: Float,
    val gyroXSma: Float,

    val gyroYMean: Float,
    val gyroYStd: Float,
    val gyroYMax: Float,
    val gyroYMin: Float,
    val gyroYRange: Float,
    val gyroYMedian: Float,
    val gyroYSma: Float,

    val gyroZMean: Float,
    val gyroZStd: Float,
    val gyroZMax: Float,
    val gyroZMin: Float,
    val gyroZRange: Float,
    val gyroZMedian: Float,
    val gyroZSma: Float,

    // Combined features
    val accMagnitudeMean: Float,      // Mean of sqrt(x² + y² + z²)
    val accMagnitudeStd: Float,
    val accMagnitudeMax: Float,

    val gyroMagnitudeMean: Float,
    val gyroMagnitudeStd: Float,
    val gyroMagnitudeMax: Float,

    // GPS features
    val gpsSpeedMean: Float,
    val gpsSpeedStd: Float,
    val gpsSpeedMax: Float,

    // Barometric pressure features
    val pressureMean: Float,
    val pressureStd: Float,

    // Frequency domain features (optional, not implemented yet)
    // val dominantFrequency: Float,
    // val spectralEntropy: Float
) {
    /**
     * Convert to float array (for model input)
     */
    fun toFloatArray(): FloatArray {
        return floatArrayOf(
            accXMean, accXStd, accXMax, accXMin, accXRange, accXMedian, accXSma,
            accYMean, accYStd, accYMax, accYMin, accYRange, accYMedian, accYSma,
            accZMean, accZStd, accZMax, accZMin, accZRange, accZMedian, accZSma,
            gyroXMean, gyroXStd, gyroXMax, gyroXMin, gyroXRange, gyroXMedian, gyroXSma,
            gyroYMean, gyroYStd, gyroYMax, gyroYMin, gyroYRange, gyroYMedian, gyroYSma,
            gyroZMean, gyroZStd, gyroZMax, gyroZMin, gyroZRange, gyroZMedian, gyroZSma,
            accMagnitudeMean, accMagnitudeStd, accMagnitudeMax,
            gyroMagnitudeMean, gyroMagnitudeStd, gyroMagnitudeMax,
            gpsSpeedMean, gpsSpeedStd, gpsSpeedMax,
            pressureMean, pressureStd
        )
    }

    companion object {
        const val FEATURE_SIZE = 53  // Total feature count
    }
}

/**
 * Prediction result
 */
data class TransportModePrediction(
    val mode: TransportModeLabel,
    val confidence: Float,  // Confidence 0-1
    val probabilities: Map<TransportModeLabel, Float>  // Probability for each class
)
