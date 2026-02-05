package com.ecogo.app.data.remote

import com.ecogo.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * 行程API服务接口
 * Base URL: http://47.129.124.55:8090/api/v1
 */
interface TripApiService {

    /**
     * 1. 开始行程
     * POST /mobile/trips/start
     */
    @POST("mobile/trips/start")
    suspend fun startTrip(
        @Header("Authorization") authorization: String,
        @Body request: TripStartRequest
    ): Response<TripStartResponse>

    /**
     * 2. 完成行程
     * POST /mobile/trips/{tripId}/complete
     */
    @POST("mobile/trips/{tripId}/complete")
    suspend fun completeTrip(
        @Path("tripId") tripId: String,
        @Header("Authorization") authorization: String,
        @Body request: TripCompleteRequest
    ): Response<TripCompleteResponse>

    /**
     * 3. 取消行程
     * POST /mobile/trips/{tripId}/cancel
     */
    @POST("mobile/trips/{tripId}/cancel")
    suspend fun cancelTrip(
        @Path("tripId") tripId: String,
        @Header("Authorization") authorization: String
    ): Response<TripCancelResponse>

    /**
     * 4. 获取行程列表
     * GET /mobile/trips
     */
    @GET("mobile/trips")
    suspend fun getTripList(
        @Header("Authorization") authorization: String,
        @Query("page") page: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
        @Query("status") status: String? = null
    ): Response<TripListResponse>

    /**
     * 5. 获取行程详情
     * GET /mobile/trips/{tripId}
     */
    @GET("mobile/trips/{tripId}")
    suspend fun getTripDetail(
        @Path("tripId") tripId: String,
        @Header("Authorization") authorization: String
    ): Response<TripDetail>

    /**
     * 6. 获取当前追踪行程
     * GET /mobile/trips/current
     */
    @GET("mobile/trips/current")
    suspend fun getCurrentTrip(
        @Header("Authorization") authorization: String
    ): Response<CurrentTripResponse>

    /**
     * 7. [Admin] 获取所有行程
     * GET /web/trips/all
     */
    @GET("web/trips/all")
    suspend fun getAllTrips(
        @Header("Authorization") authorization: String
    ): Response<TripListResponse>

    /**
     * 8. [Admin] 获取指定用户的行程
     * GET /web/trips/user/{userid}
     */
    @GET("web/trips/user/{userid}")
    suspend fun getUserTrips(
        @Path("userid") userId: String,
        @Header("Authorization") authorization: String
    ): Response<TripListResponse>
}
