package com.ecogo.mapengine.data.repository

import com.ecogo.mapengine.data.model.*
import com.ecogo.mapengine.data.remote.ApiService
import com.ecogo.mapengine.data.remote.RetrofitClient
import com.ecogo.mapengine.service.DirectionsService
import com.ecogo.mapengine.utils.MapUtils
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 地图数据仓库
 * 负责处理行程追踪和路线推荐的数据逻辑
 * 实现 IMapRepository 接口，调用真实的后端 API
 */
class MapRepository(
    private val apiService: ApiService = RetrofitClient.apiService
) : IMapRepository {

    companion object {
        private const val ERR_EMPTY_RESPONSE = "响应数据为空"
    }

    // ========================================
    // 行程追踪相关
    // ========================================

    /**
     * 开始行程追踪
     */
    override suspend fun startTripTracking(
        userId: String,
        startPoint: GeoPoint,
        startLocation: LocationInfo?
    ): Result<TripTrackData> = withContext(Dispatchers.IO) {
        try {
            val request = TripTrackRequest(
                user_id = userId,
                start_point = startPoint,
                start_location = startLocation
            )
            val response = apiService.startTripTracking(request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception(ERR_EMPTY_RESPONSE))
            } else {
                Result.failure(Exception(response.body()?.msg ?: "开启行程失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 取消行程追踪
     */
    override suspend fun cancelTripTracking(
        tripId: String,
        userId: String,
        reason: String?
    ): Result<TripCancelData> = withContext(Dispatchers.IO) {
        try {
            val request = TripCancelRequest(
                user_id = userId,
                cancel_reason = reason
            )
            val response = apiService.cancelTripTracking(tripId, request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception(ERR_EMPTY_RESPONSE))
            } else {
                Result.failure(Exception(response.body()?.msg ?: "取消行程失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取实时地图数据
     */
    override suspend fun getTripMap(
        tripId: String,
        userId: String
    ): Result<TripMapData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTripMap(tripId, userId)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception(ERR_EMPTY_RESPONSE))
            } else {
                Result.failure(Exception(response.body()?.msg ?: "获取地图数据失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 保存行程
     */
    override suspend fun saveTrip(
        tripId: String,
        userId: String,
        endPoint: GeoPoint,
        endLocation: LocationInfo?,
        distance: Double,
        endTime: String
    ): Result<TripSaveData> = withContext(Dispatchers.IO) {
        try {
            val request = TripSaveRequest(
                trip_id = tripId,
                user_id = userId,
                end_point = endPoint,
                end_location = endLocation,
                distance = distance,
                end_time = endTime
            )
            val response = apiService.saveTrip(request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception(ERR_EMPTY_RESPONSE))
            } else {
                Result.failure(Exception(response.body()?.msg ?: "保存行程失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 计算碳足迹
     */
    override suspend fun calculateCarbon(
        tripId: String,
        transportModes: List<String>
    ): Result<CarbonCalculateData> = withContext(Dispatchers.IO) {
        try {
            val request = CarbonCalculateRequest(
                trip_id = tripId,
                transport_modes = transportModes
            )
            val response = apiService.calculateCarbon(request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception(ERR_EMPTY_RESPONSE))
            } else {
                Result.failure(Exception(response.body()?.msg ?: "计算碳足迹失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================================
    // 路线推荐相关
    // ========================================

    /**
     * 获取最低碳排路线
     */
    override suspend fun getLowestCarbonRoute(
        userId: String,
        startPoint: GeoPoint,
        endPoint: GeoPoint
    ): Result<RouteRecommendData> = withContext(Dispatchers.IO) {
        try {
            val request = RouteRecommendRequest(
                user_id = userId,
                start_point = startPoint,
                end_point = endPoint
            )
            val response = apiService.getLowestCarbonRoute(request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception(ERR_EMPTY_RESPONSE))
            } else {
                Result.failure(Exception(response.body()?.msg ?: "获取路线失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取平衡路线 (时间-碳排平衡)
     */
    override suspend fun getBalancedRoute(
        userId: String,
        startPoint: GeoPoint,
        endPoint: GeoPoint
    ): Result<RouteRecommendData> = withContext(Dispatchers.IO) {
        try {
            val request = RouteRecommendRequest(
                user_id = userId,
                start_point = startPoint,
                end_point = endPoint
            )
            val response = apiService.getBalancedRoute(request)

            if (response.isSuccessful && response.body()?.isSuccess == true) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception(ERR_EMPTY_RESPONSE))
            } else {
                Result.failure(Exception(response.body()?.msg ?: "获取路线失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据交通方式获取路线
     * 直接调用 Google Directions API，不经过后端
     */
    override suspend fun getRouteByTransportMode(
        userId: String,
        startPoint: GeoPoint,
        endPoint: GeoPoint,
        transportMode: TransportMode
    ): Result<RouteRecommendData> = withContext(Dispatchers.IO) {
        try {
            val origin = LatLng(startPoint.lat, startPoint.lng)
            val destination = LatLng(endPoint.lat, endPoint.lng)

            // TransportMode → Google Directions API mode 参数
            val googleMode = when (transportMode) {
                TransportMode.WALKING -> "walking"
                TransportMode.CYCLING -> "bicycling"
                TransportMode.BUS, TransportMode.SUBWAY -> "transit"
                TransportMode.DRIVING -> "driving"
            }

            val result = DirectionsService.getRoute(origin, destination, googleMode)
                ?: return@withContext Result.failure(Exception("无法获取路线，请检查网络连接"))

            val distanceKm = result.distanceMeters / 1000.0
            val durationMinutes = result.durationSeconds / 60

            // 碳排放计算
            val totalCarbon = MapUtils.estimateCarbonEmission(distanceKm, transportMode.value)
            val carbonSaved = MapUtils.calculateCarbonSaved(distanceKm, transportMode.value)

            val routePoints = result.points.map { GeoPoint(lng = it.longitude, lat = it.latitude) }

            Result.success(RouteRecommendData(
                route_id = "directions_${System.currentTimeMillis()}",
                route_type = transportMode.value,
                total_distance = distanceKm,
                estimated_duration = durationMinutes,
                total_carbon = totalCarbon,
                carbon_saved = carbonSaved,
                route_points = routePoints,
                route_steps = result.steps
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
