package com.ecogo.mapengine.ml


object SimpleDecisionTreeClassifier {

    /**
     * Predict transport mode
     *
     * @param features 53-dimensional feature array
     * @return Predicted class index (0=WALKING, 1=CYCLING, 2=BUS, 3=SUBWAY, 4=DRIVING)
     */
    fun predict(features: FloatArray): Pair<Int, Float> {
        // Extract key features
        // Feature index reference from SensorFeatures.toFloatArray():
        // [42-44] accMagnitude(Mean/Std/Max), [45-47] gyroMagnitude(Mean/Std/Max),
        // [48-50] gpsSpeed(Mean/Std/Max), [51-52] pressure(Mean/Std)
        val accMagnitudeMean = features[42]
        val accMagnitudeStd = features[43]
        val accMagnitudeMax = features[44]

        val gyroMagnitudeMean = features[45]
        val gyroMagnitudeStd = features[46]

        val gpsSpeedMean = features[48] * 3.6f  // m/s -> km/h
        val gpsSpeedStd = features[49] * 3.6f
        val gpsSpeedMax = features[50] * 3.6f

        val pressureStd = features[52]

        // ====================================================================
        // This is a temporary rule-based classifier
        // Replace with model decision logic after training Random Forest
        // ====================================================================

        return when {
            // Walking: low speed + noticeable regular vibration
            gpsSpeedMean < 7f && accMagnitudeMean > 1.0f && accMagnitudeStd > 0.4f -> {
                0 to calculateConfidence(gpsSpeedMean, 0f, 7f, accMagnitudeMean, 1.0f, 3.0f)
            }

            // Cycling: low-medium speed + moderate or higher vibration (body bounces more on bike than in vehicle)
            // Removed gpsSpeedStd < 4 constraint: cyclists hitting red lights cause frequent stop-start speed fluctuation
            // Key differentiator: cycling accMagnitudeMean > 0.3 (body bounce), bus < 0.5 (smooth ride inside)
            gpsSpeedMean in 7f..25f && accMagnitudeMean > 0.3f -> {
                1 to calculateConfidence(gpsSpeedMean, 7f, 25f, accMagnitudeMean, 0.3f, 1.2f)
            }

            // Bus: medium-high speed + frequent stop-start + low vibration (passenger seated inside, body stable)
            // Raised speed lower bound to 20 km/h, lowered accMagnitudeMean threshold to 0.5
            // Prevents misclassifying cycling as bus
            gpsSpeedMean in 20f..60f && gpsSpeedStd > 4f && accMagnitudeMean < 0.5f -> {
                2 to calculateConfidence(gpsSpeedStd, 4f, 15f, gpsSpeedMean, 20f, 60f)
            }

            // Subway: high speed + significant pressure change + smooth
            gpsSpeedMean > 25f && pressureStd > 3f && accMagnitudeMean < 0.6f -> {
                3 to calculateConfidence(gpsSpeedMean, 25f, 80f, pressureStd, 3f, 15f)
            }

            // Driving: medium-high speed + smooth + relatively stable speed
            gpsSpeedMean > 25f && accMagnitudeMean < 0.7f && gpsSpeedStd < 10f -> {
                4 to calculateConfidence(gpsSpeedMean, 25f, 100f, accMagnitudeMean, 0f, 0.7f)
            }

            // Default: determine most likely class based on speed
            else -> {
                when {
                    gpsSpeedMean < 7f -> 0 to 0.5f  // Likely walking
                    gpsSpeedMean < 25f -> 1 to 0.5f  // Likely cycling
                    gpsSpeedMean < 60f -> 2 to 0.5f  // Likely bus
                    else -> 4 to 0.5f  // Likely driving
                }
            }
        }
    }

    /**
     * Calculate confidence
     * Based on how well the feature values fall within expected ranges
     */
    private fun calculateConfidence(
        value1: Float, min1: Float, max1: Float,
        value2: Float, min2: Float, max2: Float
    ): Float {
        // Normalize to [0, 1]
        val score1 = ((value1 - min1) / (max1 - min1)).coerceIn(0f, 1f)
        val score2 = ((value2 - min2) / (max2 - min2)).coerceIn(0f, 1f)

        // Average score, mapped to [0.6, 0.95] range
        val avgScore = (score1 + score2) / 2f
        return 0.6f + avgScore * 0.35f
    }

    /**
     * Predict probability distribution (simplified version)
     *
     * TODO: Use predict_proba() results after training Random Forest
     */
    fun predictProba(features: FloatArray): FloatArray {
        val (predictedClass, confidence) = predict(features)

        // Create probability array
        val probabilities = FloatArray(5) { 0f }

        // Assign confidence to the predicted class
        probabilities[predictedClass] = confidence

        // Distribute remaining probability equally among other classes
        val remainingProb = 1f - confidence
        val probPerOther = remainingProb / 4

        for (i in probabilities.indices) {
            if (i != predictedClass) {
                probabilities[i] = probPerOther
            }
        }

        return probabilities
    }
}

/**
 * Usage example:
 *
 * ```kotlin
 * val features = SensorFeatureExtractor.extractFeatures(window)
 * val featureArray = features.toFloatArray()
 *
 * val (predictedClass, confidence) = SimpleDecisionTreeClassifier.predict(featureArray)
 *
 * val mode = when (predictedClass) {
 *     0 -> TransportModeLabel.WALKING
 *     1 -> TransportModeLabel.CYCLING
 *     2 -> TransportModeLabel.BUS
 *     3 -> TransportModeLabel.SUBWAY
 *     4 -> TransportModeLabel.DRIVING
 *     else -> TransportModeLabel.UNKNOWN
 * }
 *
 * println("Prediction: $mode, Confidence: $confidence")
 * ```
 */
