package com.ecogo.api

import com.ecogo.data.dto.BadgeDto
import com.ecogo.data.dto.UserBadgeDto
import com.ecogo.data.dto.PurchaseRequest
import com.ecogo.data.dto.ToggleDisplayRequest
import retrofit2.http.*

/**
 * Badge & Cloth API Service
 * Unified API calls for badges and clothes
 */
interface BadgeApiService {

    /**
     * Get shop list
     * GET /api/v1/mobile/badges/shop
     */
    @GET("api/v1/mobile/badges/shop")
    suspend fun getShopList(): ApiResponse<List<BadgeDto>>

    /**
     * Get user's inventory
     * GET /api/v1/mobile/badges/user/{user_id}
     */
    @GET("api/v1/mobile/badges/user/{user_id}")
    suspend fun getMyItems(
        @Path("user_id") userId: String
    ): ApiResponse<List<UserBadgeDto>>

    /**
     * Purchase an item
     * POST /api/v1/mobile/badges/{badge_id}/purchase
     */
    @POST("api/v1/mobile/badges/{badge_id}/purchase")
    suspend fun purchaseItem(
        @Path("badge_id") badgeId: String,
        @Body request: PurchaseRequest
    ): ApiResponse<UserBadgeDto>

    /**
     * Toggle display status
     * PUT /api/v1/mobile/badges/{badge_id}/display
     */
    @PUT("api/v1/mobile/badges/{badge_id}/display")
    suspend fun toggleDisplay(
        @Path("badge_id") badgeId: String,
        @Body request: ToggleDisplayRequest
    ): ApiResponse<UserBadgeDto>

    /**
     * Query by sub-category
     * GET /api/v1/mobile/badges/sub-category/{sub_category}
     */
    @GET("api/v1/mobile/badges/sub-category/{sub_category}")
    suspend fun getItemsBySubCategory(
        @Path("sub_category") subCategory: String
    ): ApiResponse<List<BadgeDto>>
}