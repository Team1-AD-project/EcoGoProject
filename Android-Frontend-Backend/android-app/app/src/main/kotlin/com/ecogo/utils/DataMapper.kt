package com.ecogo.utils

import com.ecogo.data.Achievement
import com.ecogo.data.Outfit
import com.ecogo.data.ShopItem
import com.ecogo.data.dto.BadgeDto
import com.ecogo.data.dto.UserBadgeDto
import com.ecogo.data.dto.UserOutfitDto

/**
 * Data mapping utility
 */
object DataMapper {

    /**
     * Convert BadgeDto (cloth) to ShopItem
     */
    fun BadgeDto.toShopItem(): ShopItem? {
        if (category != "cloth") return null

        return ShopItem(
            id = badgeId,
            name = name?.get("en") ?: "Unknown",
            type = subCategory ?: "body",
            cost = purchaseCost ?: 0,
            owned = false,
            equipped = false
        )
    }

    /**
     * Convert BadgeDto (badge) to Achievement
     * Generates dynamic unlock conditions based on carbonThreshold
     */
    fun BadgeDto.toAchievement(): Achievement? {
        if (category != "badge") return null

        val howToUnlock = when (acquisitionMethod) {
            "achievement" -> {
                val threshold = carbonThreshold ?: 0.0
                when {
                    threshold == 0.0 -> "Start your eco journey"
                    threshold < 10 -> "Save ${threshold.toInt()}kg of carbon"
                    threshold < 100 -> "Save ${threshold.toInt()}kg of carbon by taking eco-friendly trips"
                    threshold < 1000 -> "Save ${threshold.toInt()}kg of carbon - You're a green champion!"
                    else -> "Save ${threshold.toInt()}kg of carbon - Ultimate eco warrior!"
                }
            }
            "purchase" -> "Purchase for ${purchaseCost ?: 0} pts"
            else -> "Complete special requirements"
        }

        return Achievement(
            id = badgeId,
            name = name?.get("en") ?: "Unknown Badge",
            description = description?.get("en") ?: "",
            unlocked = false,
            howToUnlock = howToUnlock
        )
    }

    /**
     * Merge clothing data
     */
    fun mergeClothData(
        shopItems: List<BadgeDto>,
        userItems: List<UserBadgeDto>
    ): List<ShopItem> {
        val userItemsMap = userItems.associateBy { it.badgeId }

        return shopItems
            .filter { it.category == "cloth" }
            .mapNotNull { badge ->
                val userItem = userItemsMap[badge.badgeId]

                ShopItem(
                    id = badge.badgeId,
                    name = badge.name?.get("en") ?: "Unknown",
                    type = badge.subCategory ?: "body",
                    cost = badge.purchaseCost ?: 0,
                    owned = userItem != null,
                    equipped = userItem?.isDisplay ?: false
                )
            }
    }

    /**
     * Merge badge data
     * Supports dynamic unlock condition text
     */
    fun mergeBadgeData(
        shopBadges: List<BadgeDto>,
        userBadges: List<UserBadgeDto>
    ): List<Achievement> {
        val userBadgesMap = userBadges.associateBy { it.badgeId }

        return shopBadges
            .filter { it.category == "badge" }
            .mapNotNull { badge ->
                val userBadge = userBadgesMap[badge.badgeId]

                val howToUnlock = when (badge.acquisitionMethod) {
                    "achievement" -> {
                        val threshold = badge.carbonThreshold ?: 0.0
                        when {
                            threshold == 0.0 -> "Start your eco journey"
                            threshold < 10 -> "Save ${threshold.toInt()}kg of carbon"
                            threshold < 100 -> "Save ${threshold.toInt()}kg of carbon by taking eco-friendly trips"
                            threshold < 1000 -> "Save ${threshold.toInt()}kg of carbon - You're a green champion!"
                            else -> "Save ${threshold.toInt()}kg of carbon - Ultimate eco warrior!"
                        }
                    }
                    "purchase" -> "Purchase for ${badge.purchaseCost ?: 0} pts"
                    else -> "Complete special requirements"
                }

                Achievement(
                    id = badge.badgeId,
                    name = badge.name?.get("en") ?: "Unknown Badge",
                    description = badge.description?.get("en") ?: "",
                    unlocked = userBadge != null,
                    howToUnlock = howToUnlock
                )
            }
    }

    /**
     * Convert to Outfit
     */
    fun UserOutfitDto.toOutfit(): Outfit {
        return Outfit(
            head = this.head ?: "none",
            face = this.face ?: "none",
            body = this.body ?: "none",
            badge = this.badge ?: "none"
        )
    }
}