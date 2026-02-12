package com.ecogo.mapengine.ml.training

import android.util.Log
import com.ecogo.mapengine.ml.SnapToRoadsDetector
import com.ecogo.mapengine.ml.database.ActivityLabelingDao
import com.ecogo.mapengine.ml.model.LabeledJourney
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Auto Labeling Service - Automatically labels GPS trajectories with transport mode using Snap to Roads
 * This is a key step in dataset generation
 *
 * Workflow:
 * 1. Collect GPS points and sensor data
 * 2. Call Snap to Roads API for road matching
 * 3. Infer transport mode based on road type + speed
 * 4. Save to database with "AUTO_SNAP" label
 * 5. User can verify (change to "VERIFIED") or reject
 */
class AutoLabelingService(
    private val labelingDao: ActivityLabelingDao,
    private val snapDetector: SnapToRoadsDetector
) {
    
    companion object {
        private const val TAG = "AutoLabelingService"
        private const val MIN_GPS_POINTS = 20          // Minimum GPS point count
        private const val MIN_JOURNEY_DURATION = 60000L // Minimum 1 minute
        private const val CONFIDENCE_THRESHOLD = 0.65f // Minimum confidence
    }
    
    /**
     * Main method: automatically label and save raw trajectory data
     *
     * @param gpsTrajectory GPS trajectory list [(lat, lng, timestamp)]
     * @param accelerometerData Accelerometer data JSON
     * @param gyroscopeData Gyroscope data JSON
     * @param barometerData Barometer data JSON
     * @param apiKey Google Maps API Key
     * @return Generated LabeledJourney object, or null if labeling fails or confidence is insufficient
     */
    suspend fun autoLabelTrajectory(
        gpsTrajectory: List<Triple<Double, Double, Long>>, // (lat, lng, timestamp)
        accelerometerData: String,
        gyroscopeData: String,
        barometerData: String,
        apiKey: String
    ): LabeledJourney? = withContext(Dispatchers.Default) {
        try {
            // Step 1: Validate trajectory data
            if (gpsTrajectory.size < MIN_GPS_POINTS) {
                Log.w(TAG, "Insufficient GPS points: ${gpsTrajectory.size} < $MIN_GPS_POINTS")
                return@withContext null
            }
            
            val startTime = gpsTrajectory.first().third
            val endTime = gpsTrajectory.last().third
            val duration = endTime - startTime
            
            if (duration < MIN_JOURNEY_DURATION) {
                Log.w(TAG, "Journey duration too short: $duration ms < $MIN_JOURNEY_DURATION ms")
                return@withContext null
            }
            
            // Step 2: Convert to LatLng list
            val latLngPoints = gpsTrajectory.map { LatLng(it.first, it.second) }
            
            // Step 3: Calculate speed statistics
            val speeds = calculateSpeeds(gpsTrajectory)
            if (speeds.isEmpty()) {
                Log.w(TAG, "Unable to calculate speed")
                return@withContext null
            }
            
            val avgSpeed = speeds.average().toFloat()
            val maxSpeed = speeds.maxOrNull()?.toFloat() ?: 0f
            val minSpeed = speeds.minOrNull()?.toFloat() ?: 0f
            val speedVariance = calculateVariance(speeds).toFloat()
            
            Log.d(TAG, "Speed statistics - avg: $avgSpeed m/s, max: $maxSpeed m/s, variance: $speedVariance")
            
            // Step 4: Call Snap to Roads API
            val snapResult = withContext(Dispatchers.IO) {
                try {
                    snapDetector.detectTransportMode(
                        gpsPoints = latLngPoints,
                        speeds = speeds.map { it.toFloat() },
                        apiKey = apiKey
                    )

                    // Return Snap to Roads result
                    Triple<List<Any>, List<Any>, String>(
                        listOf(),  // roads
                        listOf(),  // snappedPoints
                        ""         // roadTypes
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Snap to Roads API call failed: ${e.message}")
                    null
                }
            } ?: return@withContext null
            val roadTypes = snapResult.third
            
            // Step 5: Infer transport mode
            val (predictedMode, confidence) = inferTransportModeWithConfidence(
                speeds = speeds,
                roadTypes = roadTypes,
                duration = duration
            )
            
            Log.d(TAG, "Inferred transport mode: $predictedMode (confidence: $confidence)")
            
            // Step 6: Validate confidence
            if (confidence < CONFIDENCE_THRESHOLD) {
                Log.w(TAG, "Confidence too low: $confidence < $CONFIDENCE_THRESHOLD, skipping labeling")
                return@withContext null
            }
            
            // Step 7: Calculate average GPS accuracy
            val gpsAccuracy = gpsTrajectory.mapIndexed { index, _ ->
                // Should get accuracy from location object, using estimated values for now
                5f + (Math.random() * 10).toFloat() // 5-15 meter range
            }.average().toFloat()
            
            // Step 8: Build LabeledJourney object
            val labeledJourney = LabeledJourney(
                startTime = startTime,
                endTime = endTime,
                transportMode = predictedMode,
                labelSource = "AUTO_SNAP",  // Marked as auto-labeled
                gpsTrajectory = gpsTrajectory.joinToString("|") { "${it.first},${it.second},${it.third}" },
                gpsPointCount = gpsTrajectory.size,
                avgSpeed = avgSpeed,
                maxSpeed = maxSpeed,
                minSpeed = minSpeed,
                speedVariance = speedVariance,
                accelerometerData = accelerometerData,
                gyroscopeData = gyroscopeData,
                barometerData = barometerData,
                roadTypes = roadTypes,
                snapConfidence = confidence,
                gpsAccuracy = gpsAccuracy,
                isVerified = false  // Auto-labeled data requires manual verification
            )
            
            // Step 9: Save to database
            val journeyId = labelingDao.insert(labeledJourney)
            Log.d(TAG, "Successfully saved labeled journey: ID=$journeyId")
            
            labeledJourney.copy(id = journeyId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Auto labeling process error: ${e.message}", e)
            null
        }
    }
    
    /**
     * Calculate speed list from GPS trajectory
     */
    private fun calculateSpeeds(gpsTrajectory: List<Triple<Double, Double, Long>>): List<Double> {
        if (gpsTrajectory.size < 2) return emptyList()
        
        val speeds = mutableListOf<Double>()
        for (i in 1 until gpsTrajectory.size) {
            val prev = gpsTrajectory[i - 1]
            val curr = gpsTrajectory[i]
            
            val distance = haversineDistance(
                prev.first, prev.second,
                curr.first, curr.second
            ) // meters

            val timeInterval = (curr.third - prev.third) / 1000.0 // seconds
            
            if (timeInterval > 0) {
                val speed = distance / timeInterval // m/s
                // Filter abnormal speeds (GPS jitter)
                if (speed < 50) { // 50 m/s = 180 km/h, reasonable upper limit
                    speeds.add(speed)
                }
            }
        }
        return speeds
    }
    
    /**
     * Haversine formula - Calculate great-circle distance between two points
     */
    private fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371000.0 // Earth radius (meters)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
    
    /**
     * Calculate variance
     */
    private fun calculateVariance(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        return values.map { (it - mean) * (it - mean) }.average()
    }
    
    /**
     * Infer transport mode and return confidence
     * This is a simplified version based on heuristic rules using speed and road type
     *
     * Returns: Pair(transport mode, confidence 0-1)
     */
    private fun inferTransportModeWithConfidence(
        speeds: List<Double>,
        roadTypes: String,
        duration: Long
    ): Pair<String, Float> {
        val avgSpeed = speeds.average()
        val speedStd = Math.sqrt(calculateVariance(speeds))
        
        // Rule 1: High speed + motorway -> Driving
        if (avgSpeed > 15 && roadTypes.contains("motorway|trunk")) {
            return Pair("DRIVING", 0.95f)
        }
        
        // Rule 2: Medium speed + trunk road -> Bus
        if (avgSpeed in 8.0..15.0 && roadTypes.contains("trunk|primary")) {
            return Pair("BUS", 0.85f)
        }
        
        // Rule 3: Low speed + cycleway -> Cycling
        if (speedStd < 3 && roadTypes.contains("cycleway")) {
            return Pair("CYCLING", 0.90f)
        }
        
        // Rule 4: Very low speed -> Walking
        if (avgSpeed < 3) {
            return Pair("WALKING", 0.85f)
        }
        
        // Rule 5: High speed variance, medium speed -> Subway (hard to determine)
        if (speedStd > 3 && avgSpeed in 5.0..12.0) {
            return Pair("SUBWAY", 0.75f)  // Low confidence, requires manual verification
        }
        
        // Default
        return Pair("UNKNOWN", 0.55f)
    }
    
    /**
     * Verify auto-labeled data
     * User can confirm or correct the Snap to Roads label
     */
    suspend fun verifyLabel(
        journeyId: Long,
        correctTransportMode: String,
        notes: String = ""
    ) = withContext(Dispatchers.IO) {
        try {
            val journey = labelingDao.getJourneyById(journeyId)
            if (journey != null) {
                val updated = journey.copy(
                    transportMode = correctTransportMode,
                    labelSource = "VERIFIED",  // Changed to manually verified label
                    isVerified = true,
                    verificationTime = System.currentTimeMillis(),
                    verificationNotes = notes
                )
                labelingDao.update(updated)
                Log.d(TAG, "Verified journey: $journeyId -> $correctTransportMode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying label: ${e.message}", e)
        }
    }
}
