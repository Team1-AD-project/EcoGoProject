package com.ecogo.mapengine.data.model

import com.google.gson.annotations.SerializedName

/**
 * ========================================
 * 行程API相关模型（对接后端API）
 * Base URL: http://47.129.124.55:8090/api/v1
 * ========================================
 */

// ============================================================
// 1. 开始行程 POST /mobile/trips/start
// ============================================================

/**
 * 开始行程请求
 */
data class TripStartRequest(
    @SerializedName("startLng")
    val startLng: Double,

    @SerializedName("startLat")
    val startLat: Double,

    @SerializedName("startAddress")
    val startAddress: String,

    @SerializedName("startPlaceName")
    val startPlaceName: String,

    @SerializedName("startCampusZone")
    val startCampusZone: String? = null
)

/**
 * 开始行程响应 - 实际返回TripResponse对象
 */
typealias TripStartResponse = TripDetail

// ============================================================
// 2. 完成行程 POST /mobile/trips/{tripId}/complete
// ============================================================

/**
 * 完成行程请求
 */
data class TripCompleteRequest(
    @SerializedName("endLng")
    val endLng: Double,

    @SerializedName("endLat")
    val endLat: Double,

    @SerializedName("endAddress")
    val endAddress: String,

    @SerializedName("endPlaceName")
    val endPlaceName: String,

    @SerializedName("endCampusZone")
    val endCampusZone: String? = null,

    @SerializedName("distance")
    val distance: Double,

    @SerializedName("detectedMode")
    val detectedMode: String? = null,

    @SerializedName("mlConfidence")
    val mlConfidence: Double? = null,

    @SerializedName("isGreenTrip")
    val isGreenTrip: Boolean,

    @SerializedName("carbonSaved")
    val carbonSaved: Long,  // 单位：克(g)

    @SerializedName("transportModes")
    val transportModes: List<TransportModeSegment>,

    @SerializedName("polylinePoints")
    val polylinePoints: List<PolylinePoint>
)

/**
 * 交通方式段
 */
data class TransportModeSegment(
    @SerializedName("mode")
    val mode: String,

    @SerializedName("subDistance")
    val subDistance: Double,

    @SerializedName("subDuration")
    val subDuration: Int
)

/**
 * 路线点
 */
data class PolylinePoint(
    @SerializedName("lng")
    val lng: Double,

    @SerializedName("lat")
    val lat: Double
)

/**
 * 完成行程响应 - 实际返回TripResponse对象
 */
typealias TripCompleteResponse = TripDetail

// ============================================================
// 3. 取消行程 POST /mobile/trips/{tripId}/cancel
// ============================================================

/**
 * 取消行程响应
 */
data class TripCancelResponse(
    @SerializedName("tripId")
    val tripId: String,

    @SerializedName("status")
    val status: String,

    @SerializedName("cancelTime")
    val cancelTime: String,

    @SerializedName("message")
    val message: String? = null
)

// ============================================================
// 4. 获取行程列表 GET /mobile/trips
// ============================================================

/**
 * 行程列表响应
 */
data class TripListResponse(
    @SerializedName("trips")
    val trips: List<TripDetail>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("page")
    val page: Int? = null,

    @SerializedName("pageSize")
    val pageSize: Int? = null
)

// ============================================================
// 5. 获取行程详情 GET /mobile/trips/{tripId}
// ============================================================

/**
 * 行程详情
 */
/**
 * 后端返回的嵌套坐标对象 {"lng": ..., "lat": ...}
 */
data class GeoPointObj(
    @SerializedName("lng") val lng: Double = 0.0,
    @SerializedName("lat") val lat: Double = 0.0
)

/**
 * 后端返回的嵌套地点对象 {"address": ..., "placeName": ..., "campusZone": ...}
 */
data class LocationObj(
    @SerializedName("address") val address: String? = null,
    @SerializedName("placeName") val placeName: String? = null,
    @SerializedName("campusZone") val campusZone: String? = null
)

data class TripDetail(
    @SerializedName(value = "id", alternate = ["tripId"])
    val tripId: String = "",

    @SerializedName("userId")
    val userId: String? = null,

    @SerializedName("startTime")
    val startTime: String = "",

    @SerializedName("endTime")
    val endTime: String? = null,

    @SerializedName(value = "carbonStatus", alternate = ["status"])
    val status: String = "",

    // 后端返回嵌套对象 startPoint: {lng, lat}
    @SerializedName("startPoint")
    val startPoint: GeoPointObj? = null,

    // 后端返回嵌套对象 startLocation: {address, placeName, campusZone}
    @SerializedName("startLocation")
    val startLocation: LocationObj? = null,

    // 后端返回嵌套对象 endPoint: {lng, lat}
    @SerializedName("endPoint")
    val endPoint: GeoPointObj? = null,

    // 后端返回嵌套对象 endLocation: {address, placeName, campusZone}
    @SerializedName("endLocation")
    val endLocation: LocationObj? = null,

    @SerializedName("distance")
    val distance: Double? = null,

    @SerializedName("detectedMode")
    val detectedMode: String? = null,

    @SerializedName("mlConfidence")
    val mlConfidence: Double? = null,

    @SerializedName("isGreenTrip")
    val isGreenTrip: Boolean? = null,

    @SerializedName("carbonSaved")
    val carbonSaved: Long? = null,  // 单位：克(g)

    @SerializedName("pointsGained")
    val pointsGained: Long? = null,

    @SerializedName("transportModes")
    val transportModes: List<TransportModeSegment>? = null,

    @SerializedName("polylinePoints")
    val polylinePoints: List<PolylinePoint>? = null
) {
    // 兼容旧代码的便捷属性
    val startLng: Double get() = startPoint?.lng ?: 0.0
    val startLat: Double get() = startPoint?.lat ?: 0.0
    val startAddress: String get() = startLocation?.address ?: ""
    val startPlaceName: String get() = startLocation?.placeName ?: ""
    val endLng: Double? get() = endPoint?.lng
    val endLat: Double? get() = endPoint?.lat
    val endAddress: String? get() = endLocation?.address
    val endPlaceName: String? get() = endLocation?.placeName
}

// ============================================================
// 6. 获取当前追踪行程 GET /mobile/trips/current
// ============================================================

/**
 * 当前行程响应
 */
data class CurrentTripResponse(
    @SerializedName("hasCurrentTrip")
    val hasCurrentTrip: Boolean,

    @SerializedName("trip")
    val trip: TripDetail? = null
)

// ============================================================
// 通用响应包装 - 使用 ApiResponse.kt 中的定义
// ============================================================

/**
 * API错误响应
 */
data class ApiError(
    @SerializedName("code")
    val code: Int,

    @SerializedName("message")
    val message: String,

    @SerializedName("details")
    val details: String? = null
)
