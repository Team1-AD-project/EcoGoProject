package com.ecogo.mapengine.data.repository

import android.util.Log
import com.ecogo.auth.TokenManager
import com.ecogo.mapengine.data.model.*
import com.ecogo.mapengine.data.remote.RetrofitClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 行程仓库 - 整合本地存储和远程API
 *
 * 功能：
 * 1. 开始行程 -> 调用后端API + 本地记录
 * 2. 完成行程 -> 上传轨迹到后端 + 本地保存
 * 3. 获取历史记录 -> 优先本地，支持从云端同步
 *
 * 使用方法：
 * ```kotlin
 * val repo = TripRepository.getInstance()
 *
 * // 开始行程
 * val result = repo.startTrip(startLat, startLng, placeName, address)
 *
 * // 完成行程
 * val result = repo.completeTrip(tripId, endLat, endLng, ...)
 * ```
 */
class TripRepository private constructor() {

    private val tripApiService = RetrofitClient.tripApiService
    private val historyRepo by lazy { NavigationHistoryRepository.getInstance() }

    // 当前行程ID（从后端获取）
    private var currentTripId: String? = null

    // Token管理：优先从 TokenManager 获取登录 token（不再硬编码）
    private fun resolveAuthToken(): String =
        TokenManager.getAuthHeader() ?: "Bearer test_token_123"

    companion object {
        private const val TAG = "TripRepository"

        @Volatile
        private var INSTANCE: TripRepository? = null

        fun getInstance(): TripRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TripRepository().also { INSTANCE = it }
            }
        }
    }

    /**
     * 获取当前认证Token（从 TokenManager 动态读取）
     */
    fun getAuthToken(): String = resolveAuthToken()

    // ============================================================
    // 1. 开始行程
    // ============================================================

    /**
     * 开始行程
     *
     * @return Result<String> 成功返回tripId，失败返回错误信息
     */
    suspend fun startTrip(
        startLat: Double,
        startLng: Double,
        startPlaceName: String,
        startAddress: String,
        startCampusZone: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = TripStartRequest(
                startLng = startLng,
                startLat = startLat,
                startAddress = startAddress,
                startPlaceName = startPlaceName,
                startCampusZone = startCampusZone
            )

            Log.d(TAG, "Starting trip: $request")

            val response = tripApiService.startTrip(resolveAuthToken(), request)

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess && apiResponse.data != null) {
                    val tripId = apiResponse.data.tripId
                    currentTripId = tripId
                    Log.d(TAG, "Trip started successfully: tripId=$tripId")
                    Result.success(tripId)
                } else {
                    val error = "API returned error: ${apiResponse.message}"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
            } else {
                val error = "Failed to start trip: ${response.code()} ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting trip", e)
            Result.failure(e)
        }
    }

    // ============================================================
    // 2. 完成行程
    // ============================================================

    /**
     * 完成行程并上传数据
     *
     * @param tripId 行程ID（从startTrip获取）
     * @param endLat 终点纬度
     * @param endLng 终点经度
     * @param endPlaceName 终点名称
     * @param endAddress 终点地址
     * @param distance 总距离（米）
     * @param trackPoints 实际轨迹点
     * @param transportMode 主要交通方式（用户选择）
     * @param detectedMode AI检测的主要交通方式
     * @param mlConfidence ML置信度
     * @param carbonSaved 减碳量（克）
     * @param isGreenTrip 是否为绿色出行
     * @param transportModeSegments ML检测的交通方式段列表（UI显示什么就传什么）
     *
     * @return Result<TripCompleteResponse>
     */
    suspend fun completeTrip(
        tripId: String,
        endLat: Double,
        endLng: Double,
        endPlaceName: String,
        endAddress: String,
        distance: Double,
        trackPoints: List<LatLng>,
        transportMode: String,
        detectedMode: String? = null,
        mlConfidence: Double? = null,
        carbonSaved: Long = 0L,
        isGreenTrip: Boolean = false,
        transportModeSegments: List<TransportModeSegment>? = null
    ): Result<TripCompleteResponse> = withContext(Dispatchers.IO) {
        try {
            val request = buildCompleteRequest(
                endLat, endLng, endPlaceName, endAddress, distance,
                trackPoints, transportMode, detectedMode, mlConfidence,
                carbonSaved, isGreenTrip, transportModeSegments
            )

            Log.d(TAG, "Completing trip: tripId=$tripId, points=${trackPoints.size}")

            val response = tripApiService.completeTrip(tripId, resolveAuthToken(), request)

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess && apiResponse.data != null) {
                    val result = apiResponse.data
                    Log.d(TAG, "Trip completed successfully: $result")
                    clearTripIdIfMatch(tripId)
                    Result.success(result)
                } else {
                    val error = "API returned error: ${apiResponse.message}"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
            } else {
                val error = "Failed to complete trip: ${response.code()} ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error completing trip", e)
            Result.failure(e)
        }
    }

    private fun buildCompleteRequest(
        endLat: Double, endLng: Double,
        endPlaceName: String, endAddress: String,
        distance: Double, trackPoints: List<LatLng>,
        transportMode: String, detectedMode: String?,
        mlConfidence: Double?, carbonSaved: Long,
        isGreenTrip: Boolean,
        transportModeSegments: List<TransportModeSegment>?
    ): TripCompleteRequest {
        val polylinePoints = trackPoints.map {
            PolylinePoint(lng = it.longitude, lat = it.latitude)
        }
        val transportModes = if (!transportModeSegments.isNullOrEmpty()) {
            transportModeSegments
        } else {
            listOf(TransportModeSegment(mode = transportMode, subDistance = distance / 1000.0, subDuration = 0))
        }
        return TripCompleteRequest(
            endLng = endLng, endLat = endLat,
            endAddress = endAddress, endPlaceName = endPlaceName,
            distance = distance / 1000.0,
            detectedMode = detectedMode, mlConfidence = mlConfidence,
            isGreenTrip = isGreenTrip, carbonSaved = carbonSaved,
            transportModes = transportModes, polylinePoints = polylinePoints
        )
    }

    private fun clearTripIdIfMatch(tripId: String) {
        if (currentTripId == tripId) {
            currentTripId = null
        }
    }

    // ============================================================
    // 3. 取消行程
    // ============================================================

    /**
     * 取消行程
     */
    suspend fun cancelTrip(tripId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Canceling trip: tripId=$tripId")

            val response = tripApiService.cancelTrip(tripId, resolveAuthToken())

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess && apiResponse.data != null) {
                    Log.d(TAG, "Trip canceled successfully: ${apiResponse.data}")

                    // 清除当前行程ID
                    if (currentTripId == tripId) {
                        currentTripId = null
                    }

                    Result.success(apiResponse.data)
                } else {
                    val error = "API returned error: ${apiResponse.message}"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
            } else {
                val error = "Failed to cancel trip: ${response.code()} ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error canceling trip", e)
            Result.failure(e)
        }
    }

    // ============================================================
    // 4. 获取行程列表
    // ============================================================

    /**
     * 从云端获取行程列表
     */
    suspend fun getTripListFromCloud(
        page: Int? = null,
        pageSize: Int? = null,
        status: String? = null
    ): Result<List<TripDetail>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching trip list from cloud")

            val response = tripApiService.getTripList(resolveAuthToken(), page, pageSize, status)

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess && apiResponse.data != null) {
                    val trips = apiResponse.data
                    Log.d(TAG, "Fetched ${trips.size} trips from cloud")
                    Result.success(trips)
                } else {
                    val error = "API returned error: ${apiResponse.message}"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
            } else {
                val error = "Failed to fetch trips: ${response.code()} ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching trips", e)
            Result.failure(e)
        }
    }

    /**
     * 从本地获取行程列表（快速）
     */
    suspend fun getTripListFromLocal(): Result<List<com.ecogo.mapengine.data.local.entity.NavigationHistory>> {
        return try {
            val histories = historyRepo.getAllHistories()
            Result.success(histories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================================
    // 5. 获取行程详情
    // ============================================================

    /**
     * 获取行程详情
     */
    suspend fun getTripDetail(tripId: String): Result<TripDetail> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching trip detail: tripId=$tripId")

            val response = tripApiService.getTripDetail(tripId, resolveAuthToken())

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess && apiResponse.data != null) {
                    val trip = apiResponse.data
                    Log.d(TAG, "Fetched trip detail successfully")
                    Result.success(trip)
                } else {
                    val error = "API returned error: ${apiResponse.message}"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
            } else {
                val error = "Failed to fetch trip detail: ${response.code()} ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching trip detail", e)
            Result.failure(e)
        }
    }

    // ============================================================
    // 6. 获取当前追踪行程
    // ============================================================

    /**
     * 获取当前正在追踪的行程
     */
    suspend fun getCurrentTrip(): Result<TripDetail?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching current trip")

            val response = tripApiService.getCurrentTrip(resolveAuthToken())

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.isSuccess) {
                    val trip = apiResponse.data
                    if (trip != null) {
                        currentTripId = trip.tripId
                        Log.d(TAG, "Current trip found: ${trip.tripId}")
                        Result.success(trip)
                    } else {
                        Log.d(TAG, "No current trip")
                        Result.success(null)
                    }
                } else {
                    val error = "API returned error: ${apiResponse.message}"
                    Log.e(TAG, error)
                    Result.failure(Exception(error))
                }
            } else {
                val error = "Failed to fetch current trip: ${response.code()} ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching current trip", e)
            Result.failure(e)
        }
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    /**
     * 获取当前行程ID
     */
    fun getCurrentTripId(): String? = currentTripId

    /**
     * 清除当前行程ID
     */
    fun clearCurrentTripId() {
        currentTripId = null
    }
}
