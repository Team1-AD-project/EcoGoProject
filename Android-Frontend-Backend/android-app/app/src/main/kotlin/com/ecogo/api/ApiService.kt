package com.ecogo.api

import com.ecogo.data.Activity
import com.ecogo.data.BusRoute
import com.ecogo.data.CarbonFootprint
import com.ecogo.data.Challenge
import com.ecogo.data.ChatRequest
import com.ecogo.data.ChatResponse
import com.ecogo.data.CheckIn
import com.ecogo.data.CheckInResponse
import com.ecogo.data.CheckInStatus
import com.ecogo.data.DailyGoal
import com.ecogo.data.Faculty
import com.ecogo.data.FacultyCarbonData
import com.ecogo.data.Friend
import com.ecogo.data.Advertisement
import com.ecogo.data.FriendActivity
import com.ecogo.data.HistoryItem
import com.ecogo.data.MobileOrderHistoryData
import com.ecogo.data.Notification
import com.ecogo.data.Product
import com.ecogo.data.Ranking
import com.ecogo.data.RecommendationRequest
import com.ecogo.data.RecommendationResponse
import com.ecogo.data.RedeemRequest
import com.ecogo.data.RedeemResponse
import com.ecogo.data.UserVoucher
import com.ecogo.data.UserChallengeProgress
import com.ecogo.data.Voucher
import com.ecogo.data.VoucherRedeemRequest
import com.ecogo.data.WalkingRoute
import com.ecogo.data.Weather
import com.google.gson.annotations.SerializedName
import retrofit2.http.*
import retrofit2.http.GET
import com.ecogo.api.ApiResponse
import com.ecogo.data.PointsCurrentData
import com.ecogo.data.LeaderboardStatsData
import com.ecogo.data.TripDto

/**
 * API Service Interface
 * Defines all backend API endpoints (fully matching backend Controllers)
 */
interface ApiService {

    // ==================== Challenge Related ====================

    /**
     * Get all challenges (Mobile)
     * GET /api/v1/mobile/challenges
     */
    @GET("api/v1/mobile/challenges")
    suspend fun getAllChallenges(): ApiResponse<List<Challenge>>

    /**
     * Get challenge by ID (Mobile)
     * GET /api/v1/mobile/challenges/{id}
     */
    @GET("api/v1/mobile/challenges/{id}")
    suspend fun getChallengeById(@Path("id") id: String): ApiResponse<Challenge>

    /**
     * Get challenges by status (Mobile)
     * GET /api/v1/mobile/challenges/status/{status}
     */
    @GET("api/v1/mobile/challenges/status/{status}")
    suspend fun getChallengesByStatus(@Path("status") status: String): ApiResponse<List<Challenge>>

    /**
     * Get challenges by type (Mobile)
     * GET /api/v1/mobile/challenges/type/{type}
     */
    @GET("api/v1/mobile/challenges/type/{type}")
    suspend fun getChallengesByType(@Path("type") type: String): ApiResponse<List<Challenge>>

    /**
     * Get user's joined challenges (Mobile)
     * GET /api/v1/mobile/challenges/user/{userId}
     */
    @GET("api/v1/mobile/challenges/user/{userId}")
    suspend fun getUserChallenges(@Path("userId") userId: String): ApiResponse<List<Challenge>>

    /**
     * Join a challenge (Mobile)
     * POST /api/v1/mobile/challenges/{id}/join?userId={userId}
     */
    @POST("api/v1/mobile/challenges/{id}/join")
    suspend fun joinChallenge(
        @Path("id") challengeId: String,
        @Query("userId") userId: String
    ): ApiResponse<UserChallengeProgress>

    /**
     * Leave a challenge (Mobile)
     * POST /api/v1/mobile/challenges/{id}/leave?userId={userId}
     */
    @POST("api/v1/mobile/challenges/{id}/leave")
    suspend fun leaveChallenge(
        @Path("id") challengeId: String,
        @Query("userId") userId: String
    ): ApiResponse<Unit>

    /**
     * Get user's progress in a challenge (calculated in real-time from Trip table)
     * GET /api/v1/mobile/challenges/{id}/progress?userId={userId}
     */
    @GET("api/v1/mobile/challenges/{id}/progress")
    suspend fun getChallengeProgress(
        @Path("id") challengeId: String,
        @Query("userId") userId: String
    ): ApiResponse<UserChallengeProgress>

    /**
     * Claim challenge completion reward
     * POST /api/v1/mobile/challenges/{id}/claim-reward?userId={userId}
     */
    @POST("api/v1/mobile/challenges/{id}/claim-reward")
    suspend fun claimChallengeReward(
        @Path("id") challengeId: String,
        @Query("userId") userId: String
    ): ApiResponse<UserChallengeProgress>

    // ==================== Activity Related ====================

    /**
     * Get all activities (Mobile)
     * GET /api/v1/mobile/activities
     */
    @GET("api/v1/mobile/activities")
    suspend fun getAllActivities(): ApiResponse<List<Activity>>
    
    /**
     * Get activity by ID (Mobile)
     * GET /api/v1/mobile/activities/{id}
     */
    @GET("api/v1/mobile/activities/{id}")
    suspend fun getActivityById(@Path("id") id: String): ApiResponse<Activity>
    
    /**
     * Create an activity
     * POST /api/v1/activities
     */
    @POST("api/v1/activities")
    suspend fun createActivity(@Body activity: Activity): ApiResponse<Activity>
    
    /**
     * Update an activity
     * PUT /api/v1/activities/{id}
     */
    @PUT("api/v1/activities/{id}")
    suspend fun updateActivity(
        @Path("id") id: String,
        @Body activity: Activity
    ): ApiResponse<Activity>
    
    /**
     * Delete an activity
     * DELETE /api/v1/activities/{id}
     */
    @DELETE("api/v1/activities/{id}")
    suspend fun deleteActivity(@Path("id") id: String): ApiResponse<Unit>
    
    /**
     * Get activities by status
     * GET /api/v1/activities/status/{status}
     */
    @GET("api/v1/activities/status/{status}")
    suspend fun getActivitiesByStatus(@Path("status") status: String): ApiResponse<List<Activity>>
    
    /**
     * Join an activity (Mobile)
     * POST /api/v1/mobile/activities/{id}/join?userId={userId}
     */
    @POST("api/v1/mobile/activities/{id}/join")
    suspend fun joinActivity(
        @Path("id") activityId: String,
        @Query("userId") userId: String
    ): ApiResponse<Activity>

    /**
     * Leave an activity (Mobile)
     * POST /api/v1/mobile/activities/{id}/leave?userId={userId}
     */
    @POST("api/v1/mobile/activities/{id}/leave")
    suspend fun leaveActivity(
        @Path("id") activityId: String,
        @Query("userId") userId: String
    ): ApiResponse<Activity>
    
    // ==================== Leaderboard Related ====================

    /**
     * Get faculty monthly carbon emission statistics ranking
     * GET /api/v1/mobile/faculties/stats/carbon/monthly
     */
    @GET("api/v1/mobile/faculties/stats/carbon/monthly")
    suspend fun getFacultyMonthlyCarbonStats(): ApiResponse<List<FacultyCarbonData>>

    /**
     * Get individual leaderboard (Mobile, limited to today/this month)
     * GET /api/v1/mobile/leaderboards/rankings?type=DAILY&name=&page=0&size=50
     */
    @GET("api/v1/mobile/leaderboards/rankings")
    suspend fun getMobileLeaderboardRankings(
        @Query("type") type: String,
        @Query("name") name: String = "",
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): ApiResponse<LeaderboardStatsData>
    
    // ==================== Product Related ====================
    /**
     * Get user trip history (Web trips endpoint)
     * GET /api/v1/web/trips/user/{userId}
     */
    @GET("api/v1/web/trips/user/{userId}")
    suspend fun getUserTripsWeb(
        @Path("userId") userId: String
    ): ApiResponse<List<TripDto>>

    @GET("api/v1/mobile/trips/history")
    suspend fun getMyTripHistory(): ApiResponse<List<TripDto>>



    @GET("api/v1/support/churn/me")
    suspend fun getMyChurnRisk(
        @Query("userId") userId: String
    ): ApiResponse<ChurnRiskDTO>


    @GET("/api/v1/mobile/points/current")
    suspend fun getCurrentPoints(): ApiResponse<PointsCurrentData>

    @GET("api/v1/orders/mobile/user/{userId}")
    suspend fun getUserOrderHistoryMobile(
        @Path("userId") userId: String,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResponse<MobileOrderHistoryData>


    /**
     * Get all products
     * GET /api/v1/goods?page=1&size=20
     */
    @GET("api/v1/goods")
    suspend fun getAllGoods(
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20,
        @Query("category") category: String? = null,
        @Query("keyword") keyword: String? = null,
        @Query("isForRedemption") isForRedemption: Boolean? = null,
        // Added: aligned with backend GoodsController parameter name
        @Query("isVipActive") isVipActive: Boolean? = null
    ): ApiResponse<GoodsResponse>


    /**
     * Get product details
     * GET /api/v1/goods/{id}
     */
    @GET("api/v1/goods/{id}")
    suspend fun getGoodsById(@Path("id") id: String): ApiResponse<GoodsDto>
    
    /**
     * Mobile - Get redeemable products
     * GET /api/v1/goods/mobile/redemption?vipLevel={vipLevel}
     */
    @GET("api/v1/goods/mobile/redemption")
    suspend fun getRedemptionGoods(@Query("vipLevel") vipLevel: Int? = null): ApiResponse<List<GoodsDto>>

    // ==================== Voucher Related ====================

    /**
     * Get visible coupon list in the Marketplace
     * Backend endpoint: GET /api/v1/goods/coupons
     *
     * @param isVipActive Whether the user is VIP (true = can see VIP vouchers)
     */
    @GET("api/v1/goods/coupons")
    suspend fun getGoodsCoupons(
        @Query("isVipActive") isVipActive: Boolean? = null
    ): ApiResponse<List<Voucher>>

    @GET("api/v1/vouchers")
    suspend fun getUserVouchers(
        @Query("userId") userId: String,
        @Query("tab") tab: String
    ): ApiResponse<List<UserVoucher>>

    @GET("api/v1/vouchers/{id}")
    suspend fun getUserVoucherById(@Path("id") id: String): ApiResponse<UserVoucher>


    // ==================== Order Related ====================

    /**
     * Create an order
     * POST /api/v1/orders
     */
    @POST("api/v1/orders")
    suspend fun createOrder(@Body order: OrderCreateRequest): ApiResponse<OrderDto>
    
    /**
     * Create a redemption order
     * POST /api/v1/orders/redemption
     */
    @POST("api/v1/orders/redemption")
    suspend fun createRedemptionOrder(@Body order: OrderCreateRequest): ApiResponse<OrderDto>
    
    /**
     * Get user order history (Mobile)
     * GET /api/v1/orders/mobile/user/{userId}?status={status}&page=1&size=10
     */
    @GET("api/v1/orders/mobile/user/{userId}")
    suspend fun getUserOrderHistory(
        @Path("userId") userId: String,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 10
    ): ApiResponse<OrderHistoryResponse>
    
    /**
     * Update order status
     * PUT /api/v1/orders/{id}/status?status={status}
     */
    @PUT("api/v1/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") orderId: String,
        @Query("status") status: String
    ): ApiResponse<OrderDto>
    
    // ==================== Badge Related ====================

    /**
     * Purchase a badge
     * POST /api/v1/mobile/badges/{badge_id}/purchase
     */
    @POST("api/v1/mobile/badges/{badge_id}/purchase")
    suspend fun purchaseBadge(
        @Path("badge_id") badgeId: String,
        @Body request: Map<String, String>
    ): ApiResponse<BadgeDto>
    
    /**
     * Toggle badge display status
     * PUT /api/v1/mobile/badges/{badge_id}/display
     */
    @PUT("api/v1/mobile/badges/{badge_id}/display")
    suspend fun toggleBadgeDisplay(
        @Path("badge_id") badgeId: String,
        @Body request: Map<String, Any>
    ): ApiResponse<BadgeDto>
    
    /**
     * Get shop list
     * GET /api/v1/mobile/badges/shop
     */
    @GET("api/v1/mobile/badges/shop")
    suspend fun getBadgeShopList(): ApiResponse<List<BadgeDto>>

    /**
     * Get my badge inventory
     * GET /api/v1/mobile/badges/user/{user_id}
     */
    @GET("api/v1/mobile/badges/user/{user_id}")
    suspend fun getMyBadges(@Path("user_id") userId: String): ApiResponse<List<BadgeDto>>
    
    // ==================== Statistics Related ====================

    /**
     * Get dashboard statistics
     * GET /api/v1/statistics/dashboard
     */
    @GET("api/v1/statistics/dashboard")
    suspend fun getDashboardStats(): ApiResponse<DashboardStatsDto>

    // ==================== Routes & Map Related ====================

    /**
     * Get bus routes
     * GET /api/v1/routes
     */
    @GET("api/v1/routes")
    suspend fun getBusRoutes(): ApiResponse<List<BusRoute>>

    /**
     * Get walking routes
     * GET /api/v1/walking-routes
     */
    @GET("api/v1/walking-routes")
    suspend fun getWalkingRoutes(): ApiResponse<List<WalkingRoute>>

    /**
     * Get faculty map data
     * GET /api/v1/mobile/faculties
     */
    @GET("api/v1/mobile/faculties")
    suspend fun getFaculties(): ApiResponse<List<Faculty>>



    /**
     * Redeem a voucher
     * POST /api/v1/vouchers/redeem
     */
    @POST("api/v1/vouchers/redeem")
    suspend fun redeemVoucher(@Body request: VoucherRedeemRequest): ApiResponse<String>

    /**
     * Get history records
     * GET /api/v1/history?userId={userId}
     */
    @GET("api/v1/history")
    suspend fun getHistory(@Query("userId") userId: String? = null): ApiResponse<List<HistoryItem>>

    /**
     * Get travel recommendations
     * POST /api/v1/recommendations
     */
    @POST("api/v1/recommendations")
    suspend fun getRecommendation(@Body request: RecommendationRequest): ApiResponse<RecommendationResponse>

    /**
     * Send a chat message (AI Assistant)
     * POST /api/v1/mobile/chatbot/chat
     */
    @POST("api/v1/mobile/chatbot/chat")
    suspend fun sendChat(@Body request: ChatRequest): ApiResponse<ChatResponse>

    /**
     * Get chat booking details
     * GET /api/v1/mobile/chatbot/bookings/{bookingId}
     */
    @GET("api/v1/mobile/chatbot/bookings/{bookingId}")
    suspend fun getBookingDetail(@Path("bookingId") bookingId: String): ApiResponse<com.ecogo.data.BookingDetail>

    /**
     * Get user's chat booking list
     * GET /api/v1/mobile/chatbot/bookings
     */
    @GET("api/v1/mobile/chatbot/bookings")
    suspend fun getUserBookings(): ApiResponse<List<com.ecogo.data.BookingDetail>>

    /**
     * Get trip details
     * GET /api/v1/mobile/trips/{tripId}
     */
    @GET("api/v1/mobile/trips/{tripId}")
    suspend fun getTripDetail(@Path("tripId") tripId: String): ApiResponse<com.ecogo.data.TripDetail>
    
    // ==================== Check-in Related ====================

    /**
     * Perform check-in
     * POST /api/v1/checkin?userId={userId}
     */
    @POST("api/v1/checkin")
    suspend fun performCheckIn(@Query("userId") userId: String): ApiResponse<CheckInResponse>
    
    /**
     * Get check-in status
     * GET /api/v1/checkin/status/{userId}
     */
    @GET("api/v1/checkin/status/{userId}")
    suspend fun getCheckInStatus(@Path("userId") userId: String): ApiResponse<CheckInStatus>
    
    /**
     * Get check-in history
     * GET /api/v1/checkin/history/{userId}
     */
    @GET("api/v1/checkin/history/{userId}")
    suspend fun getCheckInHistory(@Path("userId") userId: String): ApiResponse<List<CheckIn>>
    
    // ==================== Daily Goal Related ====================

    /**
     * Get today's goal
     * GET /api/v1/goals/daily/{userId}
     */
    @GET("api/v1/goals/daily/{userId}")
    suspend fun getDailyGoal(@Path("userId") userId: String): ApiResponse<DailyGoal>
    
    /**
     * Update today's goal
     * PUT /api/v1/goals/daily/{userId}
     */
    @PUT("api/v1/goals/daily/{userId}")
    suspend fun updateDailyGoal(
        @Path("userId") userId: String,
        @Body updates: Map<String, Any>
    ): ApiResponse<DailyGoal>
    
    // ==================== Carbon Footprint Related ====================

    /**
     * Get carbon footprint data
     * GET /api/v1/carbon/{userId}?period={period}
     */
    @GET("api/v1/carbon/{userId}")
    suspend fun getCarbonFootprint(
        @Path("userId") userId: String,
        @Query("period") period: String = "monthly"
    ): ApiResponse<CarbonFootprint>
    
    /**
     * Record a trip
     * POST /api/v1/carbon/record
     */
    @POST("api/v1/carbon/record")
    suspend fun recordTrip(@Body request: Map<String, Any>): ApiResponse<CarbonFootprint>

    /**
     * Get faculty total carbon footprint (SoC Score)
     * GET /api/v1/carbon-records/faculty/total
     */
    @GET("api/v1/carbon-records/faculty/total")
    suspend fun getFacultyTotalCarbon(): ApiResponse<FacultyCarbonData>
    
    // ==================== Notification Related ====================

    /**
     * Get user notifications
     * GET /api/v1/notifications/{userId}
     */
    @GET("api/v1/notifications/{userId}")
    suspend fun getNotifications(@Path("userId") userId: String): ApiResponse<List<Notification>>
    
    /**
     * Get unread notifications
     * GET /api/v1/notifications/{userId}/unread
     */
    @GET("api/v1/notifications/{userId}/unread")
    suspend fun getUnreadNotifications(@Path("userId") userId: String): ApiResponse<List<Notification>>
    
    /**
     * Mark notification as read
     * POST /api/v1/notifications/{notificationId}/read
     */
    @POST("api/v1/notifications/{notificationId}/read")
    suspend fun markNotificationAsRead(@Path("notificationId") notificationId: String): ApiResponse<Notification>
    
    /**
     * Mark all notifications as read
     * POST /api/v1/notifications/{userId}/read-all
     */
    @POST("api/v1/notifications/{userId}/read-all")
    suspend fun markAllNotificationsAsRead(@Path("userId") userId: String): ApiResponse<Unit>
    
    // ==================== Friend Related ====================

    /**
     * Get friends list
     * GET /api/v1/friends/{userId}
     */
    @GET("api/v1/friends/{userId}")
    suspend fun getFriends(@Path("userId") userId: String): ApiResponse<List<Friend>>
    
    /**
     * Add a friend
     * POST /api/v1/friends/add
     */
    @POST("api/v1/friends/add")
    suspend fun addFriend(@Body request: Map<String, String>): ApiResponse<Friend>
    
    /**
     * Remove a friend
     * DELETE /api/v1/friends/{userId}/{friendId}
     */
    @DELETE("api/v1/friends/{userId}/{friendId}")
    suspend fun removeFriend(
        @Path("userId") userId: String,
        @Path("friendId") friendId: String
    ): ApiResponse<Unit>
    
    /**
     * Get friend requests
     * GET /api/v1/friends/requests/{userId}
     */
    @GET("api/v1/friends/requests/{userId}")
    suspend fun getFriendRequests(@Path("userId") userId: String): ApiResponse<List<Friend>>
    
    /**
     * Accept a friend request
     * POST /api/v1/friends/accept
     */
    @POST("api/v1/friends/accept")
    suspend fun acceptFriendRequest(@Body request: Map<String, String>): ApiResponse<Friend>
    
    /**
     * Get friend activities
     * GET /api/v1/friends/{userId}/activities
     */
    @GET("api/v1/friends/{userId}/activities")
    suspend fun getFriendActivities(@Path("userId") userId: String): ApiResponse<List<FriendActivity>>
    
    // ==================== Weather Related (optional, uses third-party API) ====================

    /**
     * Get weather data
     * GET /api/v1/weather
     */
    @GET("api/v1/weather")
    suspend fun getWeather(): ApiResponse<Weather>
    
    // ==================== Shop Related ====================

    /**
     * Get all products
     * GET /api/v1/shop/products
     */
    @GET("api/v1/shop/products")
    suspend fun getShopProducts(
        @Query("type") type: String? = null,  // "voucher", "goods", "all"
        @Query("category") category: String? = null,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 20
    ): ApiResponse<ProductsResponse>
    
    /**
     * Get single product details
     * GET /api/v1/shop/products/{id}
     */
    @GET("api/v1/shop/products/{id}")
    suspend fun getProductById(@Path("id") id: String): ApiResponse<Product>
    
    /**
     * Redeem with points
     * POST /api/v1/shop/redeem
     */
    @POST("api/v1/shop/redeem")
    suspend fun redeemProduct(@Body request: RedeemRequest): ApiResponse<RedeemResponse>
    
    // ==================== Advertisement Related ====================

    /**
     * Get active advertisements
     * GET /api/v1/advertisements/active
     */
    @GET("api/v1/advertisements/active")
    suspend fun getAdvertisements(): ApiResponse<List<Advertisement>>

    // ==================== Trip/Transport Related ====================

    /**
     * Get transport modes list
     * GET /api/v1/trips/transport-modes
     */
    @GET("api/v1/trips/transport-modes")
    suspend fun getTransportModes(): ApiResponse<List<String>>

    /**
     * Create PaymentIntent
     * POST /api/v1/shop/payment-intent
     */
    @POST("api/v1/shop/payment-intent")
    suspend fun createPaymentIntent(@Body request: PaymentIntentRequest): ApiResponse<PaymentIntentResponse>
    
    /**
     * Confirm payment
     * POST /api/v1/shop/confirm-payment
     */
    @POST("api/v1/shop/confirm-payment")
    suspend fun confirmPayment(@Body request: ConfirmPaymentRequest): ApiResponse<OrderDto>
    /**
     * Mobile user login
     * POST /api/v1/mobile/users/login
     */
    @POST("api/v1/mobile/users/login")
    suspend fun login(@Body request: MobileLoginRequest): ApiResponse<MobileLoginResponse>

    /**
     * Update internal user profile (no Token required, used by registration wizard)
     * PUT /internal/users/{userid}/profile
     */
    @PUT("api/v1/internal/users/{userid}/profile")
    suspend fun updateInternalUserProfile(
        @Path("userid") userId: String,
        @Body request: UpdateProfileRequest
    ): ApiResponse<Any>

    /**
     * Get user info (includes VIP status)
     * GET /api/v1/mobile/users/{id}
     */
    @GET("api/v1/mobile/users/{id}")
    suspend fun getUserProfile(@Path("id") userId: String): ApiResponse<UserInfo>

    /**
     * Get mobile user detailed profile
     * GET /api/v1/mobile/users/profile
     */
    @GET("api/v1/mobile/users/profile")
    suspend fun getMobileUserProfile(): ApiResponse<MobileProfileResponse>


    /**
     * Get user points history
     * GET /api/v1/mobile/points/history
     */
    @GET("api/v1/mobile/points/history")
    suspend fun getMobilePointsHistory(): ApiResponse<List<PointHistoryItem>>


    /**
     * Get faculty points / SoC Score
     * GET /api/v1/mobile/points/stats/faculty
     */
    @GET("api/v1/mobile/points/stats/faculty")
    suspend fun getFacultyPointsStats(): ApiResponse<Int>


    @POST("api/v1/mobile/users/register")
    suspend fun register(@Body request: MobileRegisterRequest): ApiResponse<MobileRegisterData>

    /**
     * Update user profile (Authenticated)
     * PUT /api/v1/mobile/users/profile
     */
    @PUT("api/v1/mobile/users/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): ApiResponse<Any>
}

// ==================== DTO Data Classes ====================
data class ChurnRiskDTO(
    val userId: String,
    val riskLevel: String
)


/**
 * Mobile login request
 */
data class MobileLoginRequest(
    val userid: String,
    val password: Any 
)

/**
 * Mobile login response
 */
data class MobileLoginResponse(
    val token: String,
    @SerializedName("user_info") val userInfo: UserInfo
)

data class MobileProfileResponse(
    @SerializedName("user_info") val userInfo: UserInfo,
    @SerializedName("vip_info") val vipInfo: VipInfo?,
    val stats: UserStats?
)

data class UserInfo(
    val id: String,
    val userid: String,
    val email: String?,
    val phone: String?,
    val nickname: String,
    val avatar: String?,
    val vip: VipInfo? = null,
    val stats: UserStats? = null,
    val preferences: UserPreferences? = null,
    val activityMetrics: ActivityMetrics? = null,
    val totalCarbon: Double = 0.0,
    val totalPoints: Int = 0,
    val currentPoints: Int = 0,
    val lastLoginAt: String?,
    val createdAt: String?,
    val isAdmin: Boolean = false,
    val faculty: String? = null,
    val mascotOutfit: MascotOutfitDto? = null,
    val inventory: List<String>? = null
)

data class UserStats(
    val totalTrips: Int,
    val totalDistance: Double,
    val greenDays: Int,
    val weeklyRank: Int,
    val monthlyRank: Int,
    val totalPointsFromTrips: Int
)

data class UserPreferences(
    val preferredTransport: List<String>?,
    val enablePush: Boolean,
    val enableEmail: Boolean,
    val enableBusReminder: Boolean,
    val language: String,
    val theme: String,
    val shareLocation: Boolean,
    val showOnLeaderboard: Boolean,
    val shareAchievements: Boolean,
    val dormitoryOrResidence: String?,
    val mainTeachingBuilding: String?,
    val favoriteStudySpot: String?,
    val interests: List<String>?,
    val weeklyGoals: Int,
    val newChallenges: Boolean,
    val activityReminders: Boolean,
    val friendActivity: Boolean

)

data class ActivityMetrics(
    val activeDays7d: Int,
    val activeDays30d: Int,
    val lastTripDays: Int,
    val loginFrequency7d: Int

)

data class VipInfo(
    val active: Boolean,
    val plan: String?,
    val expiryDate: String?,
    val autoRenew: Boolean = false,
    val pointsMultiplier: Double = 1.0
)

/**
 * Mobile register request
 */
data class MobileRegisterRequest(
    val userid: String,
    val password: String,
    val nickname: String,
    val repassword: String,
    val email: String
)

/**
 * Mobile register response
 */
data class MobileRegisterData(
    val id: String,
    val userid: String,
    val nickname: String,
    @SerializedName("created_at") val createdAt: String
)

/**
 * Update profile request (supports partial updates)
 */

data class UpdateProfileRequest(
    val nickname: String? = null,
    val phone: String? = null,
    val faculty: String? = null,

    val preferences: UpdatePreferencesWrapper? = null,
    val mascotOutfit: MascotOutfitDto? = null,
    val inventory: List<String>? = null
)

data class UpdatePreferencesWrapper(
    val preferredTransport: List<String>? = null,
    val dormitoryOrResidence: String? = null,
    val mainTeachingBuilding: String? = null,
    val favoriteStudySpot: String? = null,
    val interests: List<String>? = null,
    val weeklyGoals: Int? = null,
    val newChallenges: Boolean? = null,
    val activityReminders: Boolean? = null,

    val friendActivity: Boolean? = null,
    val mascotOutfit: MascotOutfitDto? = null,
    val inventory: List<String>? = null
)

data class TransportPreferencesWrapper(
    val preferredTransport: List<String>
)

/**
 * Mascot outfit DTO
 */
data class MascotOutfitDto(
    val head: String = "none",
    val face: String = "none",
    val body: String = "none",
    val badge: String = "none"

)


/**
 * Products response (with pagination)
 */
data class GoodsResponse(
    val items: List<GoodsDto>,
    val pagination: PaginationDto
)

/**
 * Pagination info
 */
data class PaginationDto(
    val page: Int,
    val size: Int,
    val total: Int,
    val totalPages: Int
)

/**
 * Product DTO
 */
data class GoodsDto(
    val id: String,
    val name: String,
    val description: String?,
    val price: Double?,
    val stock: Int,
    val category: String?,
    val brand: String?,
    val imageUrl: String?,
    val isActive: Boolean = true,
    val isForRedemption: Boolean = false,
    val redemptionPoints: Int = 0,
    val vipLevelRequired: Int = 0
)

/**
 * Order creation request
 */
data class OrderCreateRequest(
    val userId: String,
    val items: List<OrderItemRequest>,
    val shippingAddress: String? = null,
    val recipientName: String? = null,
    val recipientPhone: String? = null,
    val remark: String? = null
)

/**
 * Order item request
 */
data class OrderItemRequest(
    val goodsId: String,
    val goodsName: String,
    val quantity: Int,
    val price: Double,
    val subtotal: Double
)

/**
 * Order DTO
 */
data class OrderDto(
    val id: String,
    val orderNumber: String,
    val userId: String,
    val status: String,
    val totalAmount: Double,
    val shippingFee: Double,
    val finalAmount: Double,
    val paymentStatus: String,
    val createdAt: String,
    val updatedAt: String,
    val trackingNumber: String? = null,
    val carrier: String? = null,
    val isRedemptionOrder: Boolean = false
)

/**
 * Order history response (with pagination)
 */
data class OrderHistoryResponse(
    val data: List<OrderSummaryDto>,
    val pagination: PaginationDto
)

/**
 * Order summary DTO (simplified version for list display)
 */
data class OrderSummaryDto(
    val id: String,
    val orderNumber: String,
    val status: String,
    val finalAmount: Double,
    val createdAt: String,
    val itemCount: Int,
    val isRedemption: Boolean,
    val trackingNumber: String?,
    val carrier: String?
)

/**
 * Badge DTO
 */
data class BadgeDto(
    val id: String,
    val badgeId: String,
    val name: Map<String, String>,
    val description: Map<String, String>,
    val purchaseCost: Int,
    val category: String,
    val icon: BadgeIcon,
    val isActive: Boolean
)

/**
 * Badge icon
 */
data class BadgeIcon(
    val url: String,
    val colorScheme: String
)

/**
 * Dashboard statistics DTO
 */
data class DashboardStatsDto(
    val totalUsers: Long,
    val activeUsers: Long,
    val totalAdvertisements: Long,
    val activeAdvertisements: Long,
    val totalActivities: Long,
    val ongoingActivities: Long,
    val totalCarbonCredits: Long,
    val totalCarbonReduction: Long,
    val redemptionVolume: Long
)

/**
 * Current points response
 */
data class CurrentPointsResponse(
    val userId: String,
    val currentPoints: Long,
    val totalPoints: Long
)

/**
 * Products response (with pagination)
 */
data class ProductsResponse(
    val data: List<Product>,
    val pagination: PaginationDto
)

/**
 * Payment intent request
 */
data class PaymentIntentRequest(
    val userId: String,
    val productId: String
)

/**
 * Payment intent response
 */
data class PaymentIntentResponse(
    val clientSecret: String,
    val publishableKey: String
)

data class PointHistoryItem(
    val id: String,
    @SerializedName("change_type") val changeType: String,
    val points: Int,
    val source: String,
    val description: String?,
    @SerializedName("balance_after") val balanceAfter: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("admin_action") val adminAction: AdminAction?
)

data class AdminAction(
    @SerializedName("operator_id") val operatorId: String,
    val reason: String,
    @SerializedName("approval_status") val approvalStatus: String
)

/**
 * Confirm payment request
 */
data class ConfirmPaymentRequest(
    val userId: String,
    val productId: String,
    val paymentIntentId: String
)
