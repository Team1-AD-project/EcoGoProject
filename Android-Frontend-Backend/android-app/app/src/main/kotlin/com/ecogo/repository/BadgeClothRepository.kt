package com.ecogo.repository

import com.ecogo.api.ApiResponse
import com.ecogo.api.BadgeApiService
import com.ecogo.data.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Badge & Cloth Repository
 * Unified data requests for badges and clothes
 */
class BadgeClothRepository(
    private val apiService: BadgeApiService
) {

    // ========== Common Methods ==========

    /**
     * Get shop list (includes badges and clothes)
     */
    suspend fun getShopList(): Result<List<BadgeDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getShopList()
            if (response.code == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load shop"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user's inventory (includes badges and clothes)
     */
    suspend fun getMyItems(userId: String): Result<List<UserBadgeDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMyItems(userId)
            if (response.code == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load items"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Purchase an item (badge or cloth)
     */
    suspend fun purchaseItem(userId: String, badgeId: String): Result<UserBadgeDto> = withContext(Dispatchers.IO) {
        try {
            val request = PurchaseRequest(userId)
            val response = apiService.purchaseItem(badgeId, request)
            if (response.code == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Purchase failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggle display status (badge or cloth)
     */
    suspend fun toggleDisplay(userId: String, badgeId: String, isDisplay: Boolean): Result<UserBadgeDto> = withContext(Dispatchers.IO) {
        try {
            val request = ToggleDisplayRequest(userId, isDisplay)
            val response = apiService.toggleDisplay(badgeId, request)
            if (response.code == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Toggle failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== Badge-Specific Methods ==========

    /**
     * Get badge list (badges only)
     */
    suspend fun getBadgeList(): Result<List<BadgeDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getShopList()
            if (response.code == 200 && response.data != null) {
                val badges = response.data.filter { it.category == "badge" }
                Result.success(badges)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load badges"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user's badge inventory (badges only)
     */
    suspend fun getMyBadges(userId: String): Result<List<UserBadgeDto>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMyItems(userId)
            if (response.code == 200 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message ?: "Failed to load badges"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== Cloth-Specific Methods ==========

    /**
     * Equip an item (Cloth)
     */
    suspend fun equipItem(userId: String, badgeId: String): Result<UserBadgeDto> {
        return toggleDisplay(userId, badgeId, true)
    }

    /**
     * Unequip an item (Cloth)
     */
    suspend fun unequipItem(userId: String, badgeId: String): Result<UserBadgeDto> {
        return toggleDisplay(userId, badgeId, false)
    }

    /**
     * Get user outfit info
     */
    suspend fun getUserOutfit(userId: String): Result<UserOutfitDto> = withContext(Dispatchers.IO) {
        try {
            val myItemsResult = getMyItems(userId)
            if (myItemsResult.isFailure) {
                return@withContext Result.failure(myItemsResult.exceptionOrNull()!!)
            }

            val userItems = myItemsResult.getOrNull() ?: emptyList()
            val displayedItems = userItems.filter { it.isDisplay }

            val shopResult = getShopList()
            if (shopResult.isFailure) {
                return@withContext Result.failure(shopResult.exceptionOrNull()!!)
            }

            val shopItems = shopResult.getOrNull() ?: emptyList()
            val shopMap = shopItems.associateBy { it.badgeId }

            var head: String? = null
            var face: String? = null
            var body: String? = null
            var badge: String? = null

            displayedItems.forEach { userItem ->
                val shopItem = shopMap[userItem.badgeId]
                when (shopItem?.subCategory) {
                    "head" -> head = userItem.badgeId
                    "face" -> face = userItem.badgeId
                    "body" -> body = userItem.badgeId
                    "rank" -> badge = userItem.badgeId // Note: badge's subCategory is "rank"
                }
            }

            Result.success(UserOutfitDto(head, face, body, badge))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}