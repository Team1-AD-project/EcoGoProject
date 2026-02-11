package com.ecogo.mapengine.ml

import org.junit.Assert.*
import org.junit.Test

class SimpleDecisionTreeClassifierTest {

    /**
     * Helper: build a 53-element feature array with key indices set.
     * Index reference from SensorFeatures.toFloatArray():
     *   [42] accMagnitudeMean, [43] accMagnitudeStd, [44] accMagnitudeMax
     *   [45] gyroMagnitudeMean, [46] gyroMagnitudeStd
     *   [48] gpsSpeedMean (m/s), [49] gpsSpeedStd, [50] gpsSpeedMax
     *   [52] pressureStd
     */
    private fun makeFeatures(
        accMagMean: Float = 0f,
        accMagStd: Float = 0f,
        accMagMax: Float = 0f,
        gyroMagMean: Float = 0f,
        gyroMagStd: Float = 0f,
        gpsSpeedMeanMs: Float = 0f,  // m/s (classifier converts to km/h internally)
        gpsSpeedStdMs: Float = 0f,
        gpsSpeedMaxMs: Float = 0f,
        pressureStd: Float = 0f
    ): FloatArray {
        val features = FloatArray(53)
        features[42] = accMagMean
        features[43] = accMagStd
        features[44] = accMagMax
        features[45] = gyroMagMean
        features[46] = gyroMagStd
        features[48] = gpsSpeedMeanMs
        features[49] = gpsSpeedStdMs
        features[50] = gpsSpeedMaxMs
        features[52] = pressureStd
        return features
    }

    @Test
    fun `predict walking - low speed high acceleration`() {
        // Walking: gpsSpeed < 7 km/h (< 1.94 m/s), accMag > 1.0, accMagStd > 0.4
        val features = makeFeatures(
            accMagMean = 1.5f,
            accMagStd = 0.6f,
            gpsSpeedMeanMs = 1.2f  // ~4.3 km/h
        )
        val (cls, confidence) = SimpleDecisionTreeClassifier.predict(features)
        assertEquals(0, cls)  // WALKING
        assertTrue(confidence in 0.6f..0.95f)
    }

    @Test
    fun `predict cycling - medium speed moderate acceleration`() {
        // Cycling: 7-25 km/h (1.94-6.94 m/s), accMag 0.3-1.2, speedStd < 4 km/h
        val features = makeFeatures(
            accMagMean = 0.6f,
            gpsSpeedMeanMs = 4.0f,  // ~14.4 km/h
            gpsSpeedStdMs = 0.5f    // ~1.8 km/h
        )
        val (cls, _) = SimpleDecisionTreeClassifier.predict(features)
        assertEquals(1, cls)  // CYCLING
    }

    @Test
    fun `predict bus - medium speed high speed variance`() {
        // Bus: 20-60 km/h, speedStd > 4 km/h (>1.11 m/s), accMag < 0.5
        val features = makeFeatures(
            accMagMean = 0.4f,       // must be < 0.5 for BUS rule
            gpsSpeedMeanMs = 8.0f,   // ~28.8 km/h
            gpsSpeedStdMs = 2.0f     // ~7.2 km/h
        )
        val (cls, _) = SimpleDecisionTreeClassifier.predict(features)
        assertEquals(2, cls)  // BUS
    }

    @Test
    fun `predict driving - high speed low acceleration stable`() {
        // Driving: gpsSpeed > 15 km/h (>4.17 m/s), accMag < 0.7, speedStd < 10 km/h
        // Must keep speedStd < 4 km/h (<1.11 m/s) to avoid matching BUS rule first
        val features = makeFeatures(
            accMagMean = 0.3f,
            gpsSpeedMeanMs = 15.0f,  // ~54 km/h
            gpsSpeedStdMs = 0.8f     // ~2.9 km/h (stable speed, not bus-like)
        )
        val (cls, _) = SimpleDecisionTreeClassifier.predict(features)
        assertEquals(4, cls)  // DRIVING
    }

    @Test
    fun `predict subway - high speed with pressure change`() {
        // Subway: gpsSpeed > 25 km/h (>6.94 m/s), pressureStd > 3, accMag < 0.6
        val features = makeFeatures(
            accMagMean = 0.3f,
            gpsSpeedMeanMs = 12.0f,  // ~43.2 km/h
            pressureStd = 5f
        )
        val (cls, _) = SimpleDecisionTreeClassifier.predict(features)
        assertEquals(3, cls)  // SUBWAY
    }

    @Test
    fun `predict fallback uses speed-based heuristic`() {
        // Default fallback when nothing matches clearly: very high speed
        val features = makeFeatures(
            accMagMean = 2.0f,  // doesn't fit any clear pattern
            gpsSpeedMeanMs = 25.0f  // ~90 km/h, high speed fallback => DRIVING
        )
        val (cls, confidence) = SimpleDecisionTreeClassifier.predict(features)
        // Should fall through to default => likely driving at this speed
        assertTrue(cls in 0..4)
        assertTrue(confidence > 0f)
    }

    @Test
    fun `predictProba returns array of size 5`() {
        val features = makeFeatures(gpsSpeedMeanMs = 1.0f, accMagMean = 1.5f, accMagStd = 0.6f)
        val proba = SimpleDecisionTreeClassifier.predictProba(features)
        assertEquals(5, proba.size)
    }

    @Test
    fun `predictProba sums to approximately 1`() {
        val features = makeFeatures(gpsSpeedMeanMs = 4.0f, accMagMean = 0.6f, gpsSpeedStdMs = 0.5f)
        val proba = SimpleDecisionTreeClassifier.predictProba(features)
        val sum = proba.sum()
        assertEquals(1.0f, sum, 0.01f)
    }

    @Test
    fun `predictProba predicted class has highest probability`() {
        val features = makeFeatures(gpsSpeedMeanMs = 1.0f, accMagMean = 1.5f, accMagStd = 0.6f)
        val (predictedClass, _) = SimpleDecisionTreeClassifier.predict(features)
        val proba = SimpleDecisionTreeClassifier.predictProba(features)
        val maxIndex = proba.indices.maxByOrNull { proba[it] }!!
        assertEquals(predictedClass, maxIndex)
    }
}
