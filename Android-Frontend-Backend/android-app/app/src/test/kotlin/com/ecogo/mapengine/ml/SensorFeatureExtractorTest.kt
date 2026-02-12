package com.ecogo.mapengine.ml

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class SensorFeatureExtractorTest {

    private fun makeSensorData(
        accX: Float = 0f, accY: Float = 0f, accZ: Float = 0f,
        gyroX: Float = 0f, gyroY: Float = 0f, gyroZ: Float = 0f,
        gpsSpeed: Float = 0f, pressure: Float = 1013f
    ) = SensorRawData(
        timestamp = System.currentTimeMillis(),
        accelerometerX = accX, accelerometerY = accY, accelerometerZ = accZ,
        gyroscopeX = gyroX, gyroscopeY = gyroY, gyroscopeZ = gyroZ,
        gpsSpeed = gpsSpeed, pressure = pressure
    )

    @Test
    fun `extractFeatures with empty window returns all zeros`() {
        val window = SensorWindow(startTime = 0, endTime = 100, data = emptyList())
        val features = SensorFeatureExtractor.extractFeatures(window)

        assertEquals(0f, features.accXMean, 0.001f)
        assertEquals(0f, features.gpsSpeedMean, 0.001f)
        assertEquals(0f, features.gyroMagnitudeMean, 0.001f)

        val array = features.toFloatArray()
        assertEquals(SensorFeatures.FEATURE_SIZE, array.size)
        assertTrue(array.all { it == 0f })
    }

    @Test
    fun `extractFeatures with single data point`() {
        val data = listOf(makeSensorData(accX = 3f, accY = 4f, accZ = 0f))
        val window = SensorWindow(startTime = 0, endTime = 100, data = data)
        val features = SensorFeatureExtractor.extractFeatures(window)

        // Mean should equal the single value
        assertEquals(3f, features.accXMean, 0.001f)
        assertEquals(4f, features.accYMean, 0.001f)

        // Std should be 0 for single value
        assertEquals(0f, features.accXStd, 0.001f)

        // Magnitude: sqrt(3^2 + 4^2 + 0^2) = 5
        assertEquals(5f, features.accMagnitudeMean, 0.001f)
    }

    @Test
    fun `extractFeatures computes mean correctly for multiple data points`() {
        val data = listOf(
            makeSensorData(accX = 1f),
            makeSensorData(accX = 2f),
            makeSensorData(accX = 3f)
        )
        val window = SensorWindow(startTime = 0, endTime = 300, data = data)
        val features = SensorFeatureExtractor.extractFeatures(window)

        assertEquals(2f, features.accXMean, 0.001f)
    }

    @Test
    fun `extractFeatures computes std correctly`() {
        // values: 2, 4, 4, 4, 5, 5, 7, 9
        val values = listOf(2f, 4f, 4f, 4f, 5f, 5f, 7f, 9f)
        val data = values.map { makeSensorData(accX = it) }
        val window = SensorWindow(startTime = 0, endTime = 800, data = data)
        val features = SensorFeatureExtractor.extractFeatures(window)

        // Population std: mean = 5, variance = 4, std = 2
        assertEquals(2f, features.accXStd, 0.01f)
    }

    @Test
    fun `extractFeatures computes median correctly for odd count`() {
        val data = listOf(
            makeSensorData(accX = 5f),
            makeSensorData(accX = 1f),
            makeSensorData(accX = 3f)
        )
        val window = SensorWindow(startTime = 0, endTime = 300, data = data)
        val features = SensorFeatureExtractor.extractFeatures(window)

        // Sorted: 1, 3, 5 => median = 3
        assertEquals(3f, features.accXMedian, 0.001f)
    }

    @Test
    fun `extractFeatures computes median correctly for even count`() {
        val data = listOf(
            makeSensorData(accX = 1f),
            makeSensorData(accX = 2f),
            makeSensorData(accX = 3f),
            makeSensorData(accX = 4f)
        )
        val window = SensorWindow(startTime = 0, endTime = 400, data = data)
        val features = SensorFeatureExtractor.extractFeatures(window)

        // Sorted: 1, 2, 3, 4 => median = (2+3)/2 = 2.5
        assertEquals(2.5f, features.accXMedian, 0.001f)
    }

    @Test
    fun `extractFeatures computes range correctly`() {
        val data = listOf(
            makeSensorData(accX = -3f),
            makeSensorData(accX = 7f)
        )
        val window = SensorWindow(startTime = 0, endTime = 200, data = data)
        val features = SensorFeatureExtractor.extractFeatures(window)

        assertEquals(10f, features.accXRange, 0.001f)
    }

    @Test
    fun `extractFeatures computes SMA correctly`() {
        // SMA = mean of absolute values
        val data = listOf(
            makeSensorData(accX = -2f),
            makeSensorData(accX = 4f)
        )
        val window = SensorWindow(startTime = 0, endTime = 200, data = data)
        val features = SensorFeatureExtractor.extractFeatures(window)

        // SMA = (|-2| + |4|) / 2 = (2+4)/2 = 3
        assertEquals(3f, features.accXSma, 0.001f)
    }

    @Test
    fun `toFloatArray has correct size`() {
        val data = listOf(makeSensorData())
        val window = SensorWindow(startTime = 0, endTime = 100, data = data)
        val features = SensorFeatureExtractor.extractFeatures(window)
        assertEquals(53, features.toFloatArray().size)
    }

    @Test
    fun `extractFeatures computes GPS speed features`() {
        val data = listOf(
            makeSensorData(gpsSpeed = 5f),
            makeSensorData(gpsSpeed = 15f)
        )
        val window = SensorWindow(startTime = 0, endTime = 200, data = data)
        val features = SensorFeatureExtractor.extractFeatures(window)

        assertEquals(10f, features.gpsSpeedMean, 0.001f)
        assertEquals(15f, features.gpsSpeedMax, 0.001f)
    }
}
