package com.example.EcoGo.interfacemethods;

import com.example.EcoGo.model.Badge;
import com.example.EcoGo.model.UserBadge;
import java.util.List;
import java.util.Map;

public interface BadgeService {

    /**
     * 购买徽章
     */
    UserBadge purchaseBadge(String userId, String badgeId);

    /**
     * 切换佩戴状态
     */
    UserBadge toggleBadgeDisplay(String userId, String badgeId, boolean isDisplay);

    /**
     * 获取商店里可购买的徽章列表
     */
    List<Badge> getShopList();

    /**
     * 获取用户拥有的徽章列表
     */
    List<UserBadge> getMyBadges(String userId);

    /**
     * 创建新徽章（管理员用）
     */
    Badge createBadge(Badge badge);

    /**
     * 修改徽章（管理员用）
     */
    Badge updateBadge(String badgeId, Badge badge);

    /**
     * 删除徽章（管理员用）
     */
    void deleteBadge(String badgeId);

    /**
     * 按子分类获取徽章列表
     */
    List<Badge> getBadgesBySubCategory(String subCategory);

    /**
     * 按获取方式获取徽章列表
     */
    List<Badge> getBadgesByAcquisitionMethod(String acquisitionMethod);

    /**
     * 获取 Badge 购买统计（管理员用）
     * 返回每个 badge 的购买次数
     */
    List<Map<String, Object>> getBadgePurchaseStats();

    /**
     * 检查并自动解锁碳减排成就徽章
     * 当用户的 totalCarbon 达到某个 badge 的 carbonThreshold 时自动解锁
     */
    List<UserBadge> checkAndUnlockCarbonBadges(String userId);

    /**
     * 获取所有徽章（管理员用）
     * @param category 可选，按大类过滤 (badge/cloth)
     */
    List<Badge> getAllBadges(String category);
}
