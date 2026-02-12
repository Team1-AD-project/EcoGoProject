package com.ecogo.mapengine.ui.map

import com.ecogo.data.Advertisement
import com.ecogo.mapengine.data.model.TransportMode
import com.ecogo.mapengine.data.model.TransportModeSegment
import com.ecogo.mapengine.ml.TransportModeLabel

/**
 * MapActivity 纯业务逻辑提取类
 * 将不依赖 Android 框架的计算/映射/格式化逻辑抽取到此处以便单元测试
 */
object MapActivityHelper {

    /**
     * 交通方式段记录（用于行程中按段记录交通方式）
     */
    data class ModeSegment(
        val mode: TransportModeLabel,
        val startTime: Long,
        var endTime: Long = startTime
    )

    /**
     * ML 标签 → transport_modes_dict 的值映射
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
     * 判断是否为绿色出行
     * walk/bike/bus/subway → true，car → false
     */
    fun isGreenMode(dictMode: String): Boolean {
        return dictMode != "car"
    }

    /**
     * 获取行程中持续时间最长的交通方式
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
     * 将记录的交通方式段转换为 API 所需的 TransportModeSegment 列表
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
     * 计算实时碳排放减少量（单位：克）
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
     * 计算环保评级（星级）
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
     * 生成鼓励消息
     */
    fun generateEncouragementMessage(distanceMeters: Float, mode: TransportMode?): String {
        val carbonSavedGrams = calculateRealTimeCarbonSaved(distanceMeters, mode)

        return when (mode) {
            TransportMode.WALKING, TransportMode.CYCLING -> {
                if (carbonSavedGrams >= 1) {
                    String.format("已减碳 %.0f g | 继续加油 💪", carbonSavedGrams)
                } else {
                    "绿色出行 | 继续加油 💪"
                }
            }
            TransportMode.BUS, TransportMode.SUBWAY -> {
                if (carbonSavedGrams >= 1) {
                    String.format("绿色出行进行中 🚌 | 已减碳 %.0f g", carbonSavedGrams)
                } else {
                    "绿色出行进行中 🚌"
                }
            }
            else -> {
                String.format("已行进: %.2f 公里", distanceMeters / 1000f)
            }
        }
    }

    /**
     * 生成里程碑消息
     */
    fun generateMilestoneMessage(milestoneMeters: Float, mode: TransportMode?): String {
        val carbonSavedGrams = calculateRealTimeCarbonSaved(milestoneMeters, mode)

        return when (mode) {
            TransportMode.WALKING -> {
                String.format("恭喜！您已步行 %.0f 米，减碳 %.0f g 🎉", milestoneMeters, carbonSavedGrams)
            }
            TransportMode.CYCLING -> {
                String.format("恭喜！您已骑行 %.0f 米，减碳 %.0f g 🚴", milestoneMeters, carbonSavedGrams)
            }
            TransportMode.BUS, TransportMode.SUBWAY -> {
                String.format("恭喜！您已出行 %.0f 米，减碳 %.0f g 🌱", milestoneMeters, carbonSavedGrams)
            }
            else -> {
                String.format("恭喜！您已出行 %.0f 米", milestoneMeters)
            }
        }
    }

    /**
     * 格式化计时器显示
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
     * 检查是否达到新里程碑
     * @return 达到的里程碑值，未达到返回 null
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
     * 获取交通方式图标
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
     * 获取交通方式文本
     */
    fun getModeText(mode: TransportModeLabel): String {
        return when (mode) {
            TransportModeLabel.WALKING -> "步行"
            TransportModeLabel.CYCLING -> "骑行"
            TransportModeLabel.BUS -> "公交"
            TransportModeLabel.SUBWAY -> "地铁"
            TransportModeLabel.DRIVING -> "驾车"
            else -> "未知"
        }
    }

    /**
     * 获取路线类型显示文本
     */
    fun getRouteTypeText(routeType: String?): String {
        return when (routeType) {
            "low_carbon" -> "低碳路线"
            "balanced" -> "平衡路线"
            else -> "推荐路线"
        }
    }

    /**
     * 获取碳排放颜色编码
     * @return 颜色的十六进制字符串
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
     * 格式化碳减排显示文本
     */
    fun formatCarbonSavedText(carbonSaved: Double, totalCarbon: Double): String {
        return if (carbonSaved > 0) {
            String.format("🌍 比驾车减少 %.2f kg 碳排放", carbonSaved)
        } else {
            String.format("碳排放: %.2f kg", totalCarbon)
        }
    }

    // ===== Extracted from MapActivity =====

    /**
     * 检测是否运行在模拟器上
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
     * 获取交通步骤对应的颜色名称（不依赖 Context）
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
     * 分析路线步骤是否包含公交步骤和步骤级polyline
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
     * 生成追踪UI状态文本
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
                buttonText = "正在开始...",
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
                buttonText = "正在结束...",
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
     * 准备行程完成数据
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
     * 构建路线信息文本
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
        val headerText = "$routeTypeText  环保指数: $ecoRating"
        val durationMinutes = estimatedDuration.takeIf { it > 0 } ?: duration ?: 0
        val durationText = "预计: $durationMinutes 分钟"
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
     * 确定行程记录模式
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
     * 处理交通方式检测结果
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
            navigationModeText = "$icon 当前交通: $text ($pct%)",
            trackingModeText = "$icon 检测到: $text ($pct%)"
        )
    }

    /**
     * 计算起止点是否都已设置
     */
    fun shouldShowStartButton(hasOrigin: Boolean, hasDestination: Boolean): Boolean {
        return hasOrigin && hasDestination
    }

    /**
     * 里程碑列表常量
     */
    val MILESTONES = listOf(1000f, 2000f, 3000f, 5000f, 10000f)

    /**
     * 过滤有效广告
     */
    fun filterActiveAds(ads: List<Advertisement>?): List<Advertisement> {
        return ads?.filter { it.position == "banner" && it.status == "Active" } ?: emptyList()
    }

    /**
     * 判断是否为VIP用户
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
     * 生成行程完成消息
     */
    fun generateTripCompletionMessage(isGreenTrip: Boolean, carbonSaved: Double, greenPoints: Int): String {
        val carbonSavedStr = String.format("%.2f", carbonSaved)
        return if (isGreenTrip) {
            "🎉 绿色出行完成！减碳 $carbonSavedStr kg，获得 $greenPoints 积分"
        } else {
            "行程完成，碳排放 $carbonSavedStr kg"
        }
    }

    /**
     * 验证tripId是否有效
     */
    fun isValidTripId(tripId: String?): Boolean {
        return tripId != null && !tripId.startsWith("MOCK_") && tripId != "restored-trip"
    }

    /**
     * 获取路线绘制宽度
     */
    fun getRouteWidth(mode: TransportMode?): Float {
        return if (mode == TransportMode.WALKING) 8f else 12f
    }

    /**
     * 判断是否使用虚线
     */
    fun shouldUseDashedLine(mode: TransportMode?): Boolean {
        return mode == TransportMode.WALKING
    }

    /**
     * 计算回退模式下的每步骤分配点数
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
     * 格式化导航信息文本
     */
    fun formatNavigationInfoText(traveledMeters: Float, remainingMeters: Float): Pair<String, String> {
        val remainingKm = remainingMeters / 1000f
        val durationText = String.format("剩余: %.2f 公里", remainingKm)
        return Pair("navigation", durationText)
    }

    /**
     * 格式化追踪信息文本
     */
    fun formatTrackingInfoText(distanceMeters: Float): Pair<String, String> {
        return Pair("tracking", "实时记录GPS轨迹")
    }
}
