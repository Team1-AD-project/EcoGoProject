package com.ecogo.data.dto

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * Badge - Unified badge/cloth data model
 * Backend uses the same table to store badges and clothes
 */
data class BadgeDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("badgeId")
    val badgeId: String,

    @SerializedName("name")
    val name: Map<String, String>?,

    @SerializedName("description")
    val description: Map<String, String>?,

    @SerializedName("purchaseCost")
    val purchaseCost: Int?,

    @SerializedName("category")
    val category: String?,  // "badge" or "cloth"

    @SerializedName("subCategory")
    val subCategory: String?,  // cloth: head/face/body, badge: rank

    @SerializedName("acquisitionMethod")
    val acquisitionMethod: String?,

    @SerializedName("carbonThreshold")
    val carbonThreshold: Double?,

    @SerializedName("icon")
    val icon: BadgeIcon?,

    @SerializedName("isActive")
    val isActive: Boolean?,

    @SerializedName("createdAt")
    val createdAt: Date? = null
) {
    data class BadgeIcon(
        @SerializedName("url")
        val url: String?,

        @SerializedName("colorScheme")
        val colorScheme: String?
    )

    fun isCloth(): Boolean = category == "cloth"
    fun isBadge(): Boolean = category == "badge"
    fun getEnglishName(): String = name?.get("en") ?: "Unknown"
}

/**
 * UserBadge - User's owned badges/clothes
 */
data class UserBadgeDto(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("badgeId")
    val badgeId: String,

    @SerializedName("unlockedAt")
    val unlockedAt: Date?,

    @SerializedName(value = "isDisplay", alternate = ["display"])
    val isDisplay: Boolean,

    @SerializedName("createdAt")
    val createdAt: Date? = null
)

/**
 * Purchase request
 */
data class PurchaseRequest(
    @SerializedName("user_id")
    val userId: String
)

/**
 * Toggle display request
 */
data class ToggleDisplayRequest(
    @SerializedName("user_id")
    val userId: String,

    @SerializedName("is_display")
    val isDisplay: Boolean
)

/**
 * User outfit info
 */
data class UserOutfitDto(
    val head: String? = null,
    val face: String? = null,
    val body: String? = null,
    val badge: String? = null
)