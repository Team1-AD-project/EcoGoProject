package com.ecogo.mapengine.ui.map

import com.ecogo.data.Advertisement
import com.ecogo.mapengine.data.model.TransportMode
import com.ecogo.mapengine.data.model.TransportModeSegment
import com.ecogo.mapengine.ml.TransportModeLabel

/**
 * MapActivity pure business logic extraction class
 * Extracts computation/mapping/formatting logic independent of Android framework for unit testing
 */
object MapActivityHelper {

    /**
     * Transport mode segment record (for recording transport mode per segment during a trip)
     */
    data class ModeSegment(
        val mode: TransportModeLabel,
        val startTime: Long,
        var endTime: Long = startTime
    )

    /**
     * ML label → transport_modes_dict value mapping
     */
    fun mlLabelToDictMode(label: TransportModeLabel): String {
        return when (label) {
            TransportModeLabel.WALKING -> "walk"
            TransportModeLabel.CYCLING -> "bike"
            TransportModeLabel.BUS -> "bus"
            TransportModeLabel.SUBWAY -> "subway"
            TransportModeLabel.DRIVING -> "car"
            else -> "walk"
        }
    }

    /**
     * Determine if it's a green travel mode
     * walk/bike/bus/subway → true, car → false
     */
    fun isGreenMode(dictMode: String): Boolean {
        return dictMode != "car"
    }

    /**
     * Get the transport mode with the longest duration in a trip
     */
    fun getDominantMode(modeSegments: List<ModeSegment>): TransportModeLabel {
        if (modeSegments.isEmpty()) {
            return TransportModeLabel.WALKING
        }
        return modeSegments.groupBy { it.mode }
            .mapValues { (_, segs) -> segs.sumOf { (it.endTime - it.startTime) } }
            .maxByOrNull { it.value }?.key
            ?: TransportModeLabel.WALKING
    }

    /**
     * Convert recorded transport mode segments to the TransportModeSegment list required by the API
     */
    fun buildTransportModeSegments(
        modeSegments: List<ModeSegment>,
        totalDistanceMeters: Double
    ): List<TransportModeSegment> {
        if (modeSegments.isEmpty()) return emptyList()

        val totalDurationMs = modeSegments.sumOf { it.endTime - it.startTime }.coerceAtLeast(1L)

        return modeSegments.map { seg ->
            val segDurationMs = (seg.endTime - seg.startTime).coerceAtLeast(0L)
            val ratio = segDurationMs.toDouble() / totalDurationMs
            TransportModeSegment(
                mode = mlLabelToDictMode(seg.mode),
                subDistance = (totalDistanceMeters / 1000.0) * ratio,
                subDuration = (segDurationMs / 1000).toInt()
            )
        }
    }

    /**
     * Calculate real-time carbon emission reduction (in grams)
     */
    fun calculateRealTimeCarbonSaved(distanceMeters: Float, mode: TransportMode?): Double {
        val distanceKm = distanceMeters / 1000.0

        val emissionFactor = when (mode) {
            TransportMode.WALKING, TransportMode.CYCLING -> 0.0
            TransportMode.BUS, TransportMode.SUBWAY -> 0.05
            else -> 0.15
        }

        val currentModeCarbon = distanceKm * emissionFactor
        val drivingCarbon = distanceKm * 0.15
        val carbonSaved = (drivingCarbon - currentModeCarbon) * 1000

        return carbonSaved.coerceAtLeast(0.0)
    }

    /**
     * Calculate eco rating (star rating)
     */
    fun calculateEcoRating(totalCarbon: Double, distance: Double): String {
        val carbonPerKm = if (distance > 0) totalCarbon / distance else totalCarbon

        return when {
            carbonPerKm == 0.0 -> "⭐⭐⭐⭐⭐"
            carbonPerKm < 0.03 -> "⭐⭐⭐⭐"
            carbonPerKm < 0.06 -> "⭐⭐⭐"
            carbonPerKm < 0.10 -> "⭐⭐"
            else -> "⭐"
        }
    }

    /**
     * Generate encouragement message
     */
    fun generateEncouragementMessage(distanceMeters: Float, mode: TransportMode?): String {
        val carbonSavedGrams = calculateRealTimeCarbonSaved(distanceMeters, mode)

        return when (mode) {
            TransportMode.WALKING, TransportMode.CYCLING -> {
                if (carbonSavedGrams >= 1) {
                    String.format("Carbon reduced %.0f g | Keep it up 💪", carbonSavedGrams)
                } else {
                    "Green travel | Keep it up 💪"
                }
            }
            TransportMode.BUS, TransportMode.SUBWAY -> {
                if (carbonSavedGrams >= 1) {
                    String.format("Green travel in progress 🚌 | Carbon reduced %.0f g", carbonSavedGrams)
                } else {
                    "Green travel in progress 🚌"
                }
            }
            else -> {
                String.format("Traveled: %.2f km", distanceMeters / 1000f)
            }
        }
    }

    /**
     * Generate milestone message
     */
    fun generateMilestoneMessage(milestoneMeters: Float, mode: TransportMode?): String {
        val carbonSavedGrams = calculateRealTimeCarbonSaved(milestoneMeters, mode)

        return when (mode) {
            TransportMode.WALKING -> {
                String.format("Congrats! You've walked %.0f m, carbon reduced %.0f g 🎉", milestoneMeters, carbonSavedGrams)
            }
            TransportMode.CYCLING -> {
                String.format("Congrats! You've cycled %.0f m, carbon reduced %.0f g 🚴", milestoneMeters, carbonSavedGrams)
            }
            TransportMode.BUS, TransportMode.SUBWAY -> {
                String.format("Congrats! You've traveled %.0f m, carbon reduced %.0f g 🌱", milestoneMeters, carbonSavedGrams)
            }
            else -> {
                String.format("Congrats! You've traveled %.0f m", milestoneMeters)
            }
        }
    }

    /**
     * Format timer display
     */
    fun formatElapsedTime(elapsedMs: Long): String {
        val seconds = (elapsedMs / 1000) % 60
        val minutes = (elapsedMs / 1000 / 60) % 60
        val hours = elapsedMs / 1000 / 3600
        return if (hours > 0)
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        else
            String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * Check if a new milestone has been reached
     * @return the milestone value reached, or null if none
     */
    fun checkMilestone(
        distanceMeters: Float,
        milestones: List<Float>,
        reachedMilestones: Set<Float>
    ): Float? {
        for (milestone in milestones) {
            if (distanceMeters >= milestone && !reachedMilestones.contains(milestone)) {
                return milestone
            }
        }
        return null
    }

    /**
     * Get transport mode icon
     */
    fun getModeIcon(mode: TransportModeLabel): String {
        return when (mode) {
            TransportModeLabel.WALKING -> "🚶"
            TransportModeLabel.CYCLING -> "🚴"
            TransportModeLabel.BUS -> "🚌"
            TransportModeLabel.SUBWAY -> "🚇"
            TransportModeLabel.DRIVING -> "🚗"
            else -> "❓"
        }
    }

    /**
     * Get transport mode text
     */
    fun getModeText(mode: TransportModeLabel): String {
        return when (mode) {
            TransportModeLabel.WALKING -> "Walking"
            TransportModeLabel.CYCLING -> "Cycling"
            TransportModeLabel.BUS -> "Bus"
            TransportModeLabel.SUBWAY -> "Subway"
            TransportModeLabel.DRIVING -> "Driving"
            else -> "Unknown"
        }
    }

    /**
     * Get route type display text
     */
    fun getRouteTypeText(routeType: String?): String {
        return when (routeType) {
            "low_carbon" -> "Low Carbon Route"
            "balanced" -> "Balanced Route"
            else -> "Recommended Route"
        }
    }

    /**
     * Get carbon emission color code
     * @return hex color string
     */
    fun getCarbonColorHex(totalCarbon: Double): String {
        return when {
            totalCarbon == 0.0 -> "#4CAF50"
            totalCarbon < 0.5 -> "#8BC34A"
            totalCarbon < 1.5 -> "#FFC107"
            else -> "#FF5722"
        }
    }

    /**
     * Format carbon reduction display text
     */
    fun formatCarbonSavedText(carbonSaved: Double, totalCarbon: Double): String {
        return if (carbonSaved > 0) {
            String.format("🌍 Reduced %.2f kg CO₂ vs driving", carbonSaved)
        } else {
            String.format("Carbon emission: %.2f kg", totalCarbon)
        }
    }

    // ===== Extracted from MapActivity =====

    /**
     * Detect if running on an emulator
     */
    fun isRunningOnEmulator(): Boolean {
        return (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk" == android.os.Build.PRODUCT)
    }

    /**
     * Get color name for transit step (Context-independent)
     */
    fun getTransitStepColorName(travelMode: String, vehicleType: String?): String {
        return when (travelMode) {
            "WALKING" -> "walking"
            "TRANSIT" -> {
                when (vehicleType?.uppercase()) {
                    "SUBWAY", "METRO_RAIL" -> "subway"
                    "BUS", "INTERCITY_BUS", "TROLLEYBUS" -> "bus"
                    "RAIL", "HEAVY_RAIL", "COMMUTER_TRAIN", "HIGH_SPEED_TRAIN", "LONG_DISTANCE_TRAIN" -> "rail"
                    "TRAM", "MONORAIL" -> "tram"
                    else -> "bus"
                }
            }
            "DRIVING" -> "driving"
            "BICYCLING" -> "cycling"
            else -> "remaining"
        }
    }

    /**
     * Analyze whether route steps contain transit steps and step-level polylines
     */
    data class RouteAnalysis(
        val hasTransitSteps: Boolean,
        val hasStepPolylines: Boolean
    )

    fun analyzeRouteSteps(steps: List<com.ecogo.mapengine.data.model.RouteStep>?): RouteAnalysis {
        val hasTransit = steps?.any { it.travel_mode == "TRANSIT" } == true
        val hasPolylines = steps?.any { !it.polyline_points.isNullOrEmpty() } == true
        return RouteAnalysis(hasTransit, hasPolylines)
    }

    /**
     * Generate tracking UI state text
     */
    data class TrackingUIState(
        val buttonText: String,
        val buttonEnabled: Boolean,
        val chipGroupVisible: Boolean,
        val searchVisible: Boolean,
        val routeInfoVisible: Boolean,
        val hideTimer: Boolean,
        val isIdle: Boolean
    )

    fun getTrackingUIState(
        state: TripState,
        isNavigationMode: Boolean = false,
        remainingKm: Float = 0f
    ): TrackingUIState {
        return when (state) {
            is TripState.Idle -> TrackingUIState(
                buttonText = "start_tracking",
                buttonEnabled = true,
                chipGroupVisible = true,
                searchVisible = true,
                routeInfoVisible = false,
                hideTimer = true,
                isIdle = true
            )
            is TripState.Starting -> TrackingUIState(
                buttonText = "Starting...",
                buttonEnabled = false,
                chipGroupVisible = false,
                searchVisible = false,
                routeInfoVisible = false,
                hideTimer = false,
                isIdle = false
            )
            is TripState.Tracking -> TrackingUIState(
                buttonText = "stop_tracking",
                buttonEnabled = true,
                chipGroupVisible = false,
                searchVisible = false,
                routeInfoVisible = true,
                hideTimer = false,
                isIdle = false
            )
            is TripState.Stopping -> TrackingUIState(
                buttonText = "Stopping...",
                buttonEnabled = false,
                chipGroupVisible = false,
                searchVisible = false,
                routeInfoVisible = false,
                hideTimer = false,
                isIdle = false
            )
            is TripState.Completed -> TrackingUIState(
                buttonText = "start_tracking",
                buttonEnabled = true,
                chipGroupVisible = true,
                searchVisible = true,
                routeInfoVisible = false,
                hideTimer = true,
                isIdle = false
            )
        }
    }

    /**
     * Prepare trip completion data
     */
    data class TripCompletionData(
        val detectedMode: String?,
        val userSelectedMode: String,
        val isGreenTrip: Boolean,
        val carbonSavedGrams: Long,
        val mlConfidence: Double?
    )

    fun prepareTripCompletionData(
        modeSegments: List<ModeSegment>,
        lastMlConfidence: Float,
        userSelectedModeValue: String?,
        distanceMeters: Double,
        selectedTransportMode: TransportMode?
    ): TripCompletionData {
        val detectedMode = if (modeSegments.isNotEmpty()) {
            mlLabelToDictMode(getDominantMode(modeSegments))
        } else {
            null
        }

        val userSelectedMode = userSelectedModeValue ?: "walk"
        val greenTrip = isGreenMode(userSelectedMode)
        val carbonSavedGrams = calculateRealTimeCarbonSaved(distanceMeters.toFloat(), selectedTransportMode).toLong()
        val confidence = if (modeSegments.isNotEmpty() && lastMlConfidence > 0f) {
            lastMlConfidence.toDouble()
        } else {
            null
        }

        return TripCompletionData(
            detectedMode = detectedMode,
            userSelectedMode = userSelectedMode,
            isGreenTrip = greenTrip,
            carbonSavedGrams = carbonSavedGrams,
            mlConfidence = confidence
        )
    }

    /**
     * Build route info text
     */
    data class RouteInfoTexts(
        val routeTypeText: String,
        val carbonSavedText: String,
        val carbonColorHex: String,
        val ecoRating: String,
        val headerText: String,
        val durationText: String,
        val showCumulativeImpact: Boolean,
        val showRouteOptions: Boolean,
        val showRouteSteps: Boolean
    )

    fun buildRouteInfoTexts(
        routeType: String?,
        carbonSaved: Double,
        totalCarbon: Double,
        totalDistance: Double,
        estimatedDuration: Int,
        duration: Int?,
        hasAlternatives: Boolean,
        hasTransitSteps: Boolean
    ): RouteInfoTexts {
        val routeTypeText = getRouteTypeText(routeType)
        val carbonSavedText = formatCarbonSavedText(carbonSaved, totalCarbon)
        val carbonColorHex = getCarbonColorHex(totalCarbon)
        val ecoRating = calculateEcoRating(totalCarbon, totalDistance)
        val headerText = "$routeTypeText  Eco Rating: $ecoRating"
        val durationMinutes = estimatedDuration.takeIf { it > 0 } ?: duration ?: 0
        val durationText = "Estimated: $durationMinutes min"
        val showCumulative = carbonSaved > 0

        return RouteInfoTexts(
            routeTypeText = routeTypeText,
            carbonSavedText = carbonSavedText,
            carbonColorHex = carbonColorHex,
            ecoRating = ecoRating,
            headerText = headerText,
            durationText = durationText,
            showCumulativeImpact = showCumulative,
            showRouteOptions = hasAlternatives,
            showRouteSteps = hasTransitSteps
        )
    }

    /**
     * Determine trip recording mode
     */
    data class TrackingMode(
        val isNavigationMode: Boolean,
        val hasTransitWithPolylines: Boolean,
        val hasTransitFallback: Boolean
    )

    fun determineTrackingMode(
        routePoints: List<Any>?,
        steps: List<com.ecogo.mapengine.data.model.RouteStep>?
    ): TrackingMode {
        val hasRoutePoints = !routePoints.isNullOrEmpty()
        val analysis = analyzeRouteSteps(steps)

        return TrackingMode(
            isNavigationMode = hasRoutePoints,
            hasTransitWithPolylines = analysis.hasTransitSteps && analysis.hasStepPolylines,
            hasTransitFallback = analysis.hasTransitSteps && !analysis.hasStepPolylines && !steps.isNullOrEmpty() && hasRoutePoints
        )
    }

    /**
     * Process transport mode detection result
     */
    data class TransportModeUpdate(
        val detectedTransportMode: String,
        val modeIcon: String,
        val modeText: String,
        val confidencePercent: Int,
        val navigationModeText: String,
        val trackingModeText: String
    )

    fun processTransportModeDetection(
        mode: TransportModeLabel,
        confidence: Float
    ): TransportModeUpdate {
        val dictMode = mlLabelToDictMode(mode)
        val icon = getModeIcon(mode)
        val text = getModeText(mode)
        val pct = (confidence * 100).toInt()

        return TransportModeUpdate(
            detectedTransportMode = dictMode,
            modeIcon = icon,
            modeText = text,
            confidencePercent = pct,
            navigationModeText = "$icon Current transport: $text ($pct%)",
            trackingModeText = "$icon Detected: $text ($pct%)"
        )
    }

    /**
     * Check if both origin and destination are set
     */
    fun shouldShowStartButton(hasOrigin: Boolean, hasDestination: Boolean): Boolean {
        return hasOrigin && hasDestination
    }

    /**
     * Milestone list constants
     */
    val MILESTONES = listOf(1000f, 2000f, 3000f, 5000f, 10000f)

    /**
     * Filter active advertisements
     */
    fun filterActiveAds(ads: List<Advertisement>?): List<Advertisement> {
        return ads?.filter { it.position == "banner" && it.status == "Active" } ?: emptyList()
    }

    /**
     * Determine if user is VIP
     */
    fun isVipFromProfile(
        vipInfoActive: Boolean?,
        userVipActive: Boolean?,
        vipInfoPlan: String?,
        userVipPlan: String?,
        isAdmin: Boolean?
    ): Boolean {
        return (vipInfoActive == true) ||
                (userVipActive == true) ||
                (vipInfoPlan != null) ||
                (userVipPlan != null) ||
                (isAdmin == true)
    }

    /**
     * Generate trip completion message
     */
    fun generateTripCompletionMessage(isGreenTrip: Boolean, carbonSaved: Double, greenPoints: Int): String {
        val carbonSavedStr = String.format("%.2f", carbonSaved)
        return if (isGreenTrip) {
            "🎉 Green trip completed! Carbon reduced $carbonSavedStr kg, earned $greenPoints points"
        } else {
            "Trip completed, carbon emission $carbonSavedStr kg"
        }
    }

    /**
     * Validate if tripId is valid
     */
    fun isValidTripId(tripId: String?): Boolean {
        return tripId != null && !tripId.startsWith("MOCK_") && tripId != "restored-trip"
    }

    /**
     * Get route drawing width
     */
    fun getRouteWidth(mode: TransportMode?): Float {
        return if (mode == TransportMode.WALKING) 8f else 12f
    }

    /**
     * Determine if dashed line should be used
     */
    fun shouldUseDashedLine(mode: TransportMode?): Boolean {
        return mode == TransportMode.WALKING
    }

    /**
     * Calculate allocated points per step in fallback mode
     */
    fun calculateFallbackPointsPerStep(
        totalPoints: Int,
        stepDistance: Double,
        totalStepDistance: Double,
        isLastStep: Boolean,
        currentPointIndex: Int
    ): Int {
        return if (isLastStep) {
            totalPoints - currentPointIndex
        } else {
            val ratio = stepDistance / totalStepDistance
            (totalPoints * ratio).toInt().coerceAtLeast(2)
        }
    }

    /**
     * Format navigation info text
     */
    fun formatNavigationInfoText(traveledMeters: Float, remainingMeters: Float): Pair<String, String> {
        val remainingKm = remainingMeters / 1000f
        val durationText = String.format("Remaining: %.2f km", remainingKm)
        return Pair("navigation", durationText)
    }

    /**
     * Format tracking info text
     */
    fun formatTrackingInfoText(distanceMeters: Float): Pair<String, String> {
        return Pair("tracking", "Recording GPS track in real time")
    }
}
