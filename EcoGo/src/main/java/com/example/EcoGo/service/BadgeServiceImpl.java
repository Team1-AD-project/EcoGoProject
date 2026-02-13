package com.example.EcoGo.service;

import com.example.EcoGo.dto.BadgePurchaseStatDto;
import com.example.EcoGo.dto.PointsDto;
import com.example.EcoGo.interfacemethods.BadgeService;
import com.example.EcoGo.interfacemethods.PointsService;
import com.example.EcoGo.model.Badge;
import com.example.EcoGo.model.User;
import com.example.EcoGo.model.UserBadge;
import com.example.EcoGo.repository.BadgeRepository;
import com.example.EcoGo.repository.UserBadgeRepository;
import com.example.EcoGo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BadgeServiceImpl implements BadgeService {

    @Autowired
    private BadgeRepository badgeRepository;
    @Autowired
    private UserBadgeRepository userBadgeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    @Lazy
    private PointsService pointsService;

    @Autowired
    @Lazy
    private BadgeService self;

    /**
     * 1. 购买徽章
     */
    @Transactional
    public UserBadge purchaseBadge(String userId, String badgeId) {
        if (userBadgeRepository.existsByUserIdAndBadgeId(userId, badgeId)) {
            throw new RuntimeException("您已拥有该徽章");
        }

        Badge badge = badgeRepository.findByBadgeId(badgeId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        // 检查获取方式是否为 purchase
        String method = badge.getAcquisitionMethod();
        if (method != null && !method.isEmpty() && !"purchase".equalsIgnoreCase(method)) {
            throw new RuntimeException("该徽章不可通过购买获得");
        }

        Integer cost = badge.getPurchaseCost();
        if (cost == null || cost <= 0)
            throw new RuntimeException("该徽章不可购买");

        // 使用 PointsService 统一处理积分扣除和日志记录
        String badgeName = badge.getName() != null ? badge.getName().getOrDefault("en", "Unknown Badge")
                : "Unknown Badge";
        PointsDto.SettleResult settleResult = new PointsDto.SettleResult();
        settleResult.points = -cost;
        settleResult.source = "badge";
        settleResult.description = pointsService.formatBadgeDescription(badgeName);
        settleResult.relatedId = badgeId;
        pointsService.settle(userId, settleResult);

        UserBadge newBadge = new UserBadge();
        newBadge.setUserId(userId);
        newBadge.setBadgeId(badgeId);
        newBadge.setUnlockedAt(new Date());
        newBadge.setDisplay(false); // 默认不佩戴
        newBadge.setCreatedAt(new Date());
        newBadge.setCategory(badge.getCategory());
        newBadge.setSubcategory(badge.getSubCategory());

        return userBadgeRepository.save(newBadge);
    }

    /**
     * 2. 切换佩戴状态 (支持同类互斥)
     * 逻辑：如果佩戴的是 "RANK" 类，系统会自动卸下用户身上已有的其他 "RANK" 类徽章
     */
    @Transactional
    public UserBadge toggleBadgeDisplay(String userId, String badgeId, boolean isDisplay) {

        UserBadge targetUserBadge = userBadgeRepository.findByUserIdAndBadgeId(userId, badgeId)
                .orElseThrow(() -> new RuntimeException("您还没有获得这个徽章"));

        // 如果是“取下”，直接操作，无需互斥
        if (!isDisplay) {
            targetUserBadge.setDisplay(false);
            return userBadgeRepository.save(targetUserBadge);
        }

        // ================= 同类互斥核心逻辑 =================
        // 按 subCategory 做互斥：head/face/body/rank 各只能装备一件

        Badge targetBadgeDef = badgeRepository.findByBadgeId(badgeId)
                .orElseThrow(() -> new RuntimeException("徽章定义不存在"));

        String subCategory = targetBadgeDef.getSubCategory();

        if (subCategory != null && !subCategory.isEmpty()) {
            // A. 找出同 subCategory 下所有的徽章ID (比如所有 head 类的)
            List<Badge> sameSubCategoryBadges = badgeRepository.findBySubCategory(subCategory);
            List<String> sameSubCategoryBadgeIds = sameSubCategoryBadges.stream()
                    .map(Badge::getBadgeId)
                    .toList();

            // B. 找出用户身上正在戴着的、且属于同 subCategory 的旧徽章
            List<UserBadge> conflictingBadges = userBadgeRepository
                    .findByUserIdAndIsDisplayTrueAndBadgeIdIn(userId, sameSubCategoryBadgeIds);

            // C. 统统卸下
            for (UserBadge conflict : conflictingBadges) {
                if (!conflict.getBadgeId().equals(badgeId)) {
                    conflict.setDisplay(false);
                    userBadgeRepository.save(conflict);
                }
            }
        }
        // ====================================================

        targetUserBadge.setDisplay(true);
        return userBadgeRepository.save(targetUserBadge);
    }

    // ... 其他 getter 方法 (getShopList, getMyBadges) 保持不变 ...
    public List<Badge> getShopList() {

        return badgeRepository.findByIsActive(true);
    }

    public List<UserBadge> getMyBadges(String userId) {
        self.checkAndUnlockCarbonBadges(userId);
        return userBadgeRepository.findByUserId(userId);
    }

    public Badge createBadge(Badge badge) {
        return badgeRepository.save(badge);
    }

    /**
     * 按子分类获取徽章列表
     */
    public List<Badge> getBadgesBySubCategory(String subCategory) {
        return badgeRepository.findBySubCategory(subCategory);
    }

    /**
     * 按获取方式获取徽章列表
     */
    public List<Badge> getBadgesByAcquisitionMethod(String acquisitionMethod) {
        return badgeRepository.findByAcquisitionMethod(acquisitionMethod);
    }

    /**
     * 3. 获取 Badge 购买统计
     * 返回每个 badge 的购买次数，供管理员查看
     */
    public List<BadgePurchaseStatDto> getBadgePurchaseStats() {
        return userBadgeRepository.countPurchasesByBadge();
    }

    /**
     * 4. 修改徽章（管理员用）
     */
    @Transactional
    public Badge updateBadge(String badgeId, Badge updatedBadge) {
        Badge existingBadge = badgeRepository.findByBadgeId(badgeId)
                .orElseThrow(() -> new RuntimeException("徽章不存在"));

        // 更新字段（只更新非空字段）
        if (updatedBadge.getName() != null) {
            existingBadge.setName(updatedBadge.getName());
        }
        if (updatedBadge.getDescription() != null) {
            existingBadge.setDescription(updatedBadge.getDescription());
        }
        if (updatedBadge.getPurchaseCost() != null) {
            existingBadge.setPurchaseCost(updatedBadge.getPurchaseCost());
        }
        if (updatedBadge.getCategory() != null) {
            existingBadge.setCategory(updatedBadge.getCategory());
        }
        if (updatedBadge.getIcon() != null) {
            existingBadge.setIcon(updatedBadge.getIcon());
        }
        if (updatedBadge.getIsActive() != null) {
            existingBadge.setIsActive(updatedBadge.getIsActive());
        }
        if (updatedBadge.getSubCategory() != null) {
            existingBadge.setSubCategory(updatedBadge.getSubCategory());
        }
        if (updatedBadge.getAcquisitionMethod() != null) {
            existingBadge.setAcquisitionMethod(updatedBadge.getAcquisitionMethod());
        }
        if (updatedBadge.getCarbonThreshold() != null) {
            existingBadge.setCarbonThreshold(updatedBadge.getCarbonThreshold());
        }

        return badgeRepository.save(existingBadge);
    }

    /**
     * 检查并自动解锁碳减排成就徽章
     * 查找所有 acquisitionMethod="achievement" 且 carbonThreshold <= 用户 totalCarbon 的徽章，
     * 如果用户尚未拥有则自动解锁。
     */
    @Transactional
    public List<UserBadge> checkAndUnlockCarbonBadges(String userId) {
        User user = userRepository.findByUserid(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        double userCarbon = user.getTotalCarbon();

        // 查找所有已启用的、achievement 类型的、用户碳减排已达标的徽章
        List<Badge> qualifiedBadges = badgeRepository
                .findByIsActiveTrueAndAcquisitionMethodAndCarbonThresholdLessThanEqual("achievement", userCarbon);

        if (qualifiedBadges.isEmpty()) {
            return List.of();
        }

        // 获取用户已拥有的徽章 ID 集合
        List<UserBadge> ownedBadges = userBadgeRepository.findByUserId(userId);
        Set<String> ownedBadgeIds = ownedBadges.stream()
                .map(UserBadge::getBadgeId)
                .collect(Collectors.toSet());

        // 过滤出用户尚未拥有的徽章，逐个解锁
        List<UserBadge> newlyUnlocked = new ArrayList<>();
        for (Badge badge : qualifiedBadges) {
            if (!ownedBadgeIds.contains(badge.getBadgeId())) {
                UserBadge newBadge = new UserBadge();
                newBadge.setUserId(userId);
                newBadge.setBadgeId(badge.getBadgeId());
                newBadge.setUnlockedAt(new Date());
                newBadge.setDisplay(false);
                newBadge.setCreatedAt(new Date());
                newBadge.setCategory(badge.getCategory());
                newBadge.setSubcategory(badge.getSubCategory());
                newlyUnlocked.add(userBadgeRepository.save(newBadge));
            }
        }

        return newlyUnlocked;
    }

    /**
     * 5. 删除徽章（管理员用）
     */
    @Transactional
    public void deleteBadge(String badgeId) {
        Badge badge = badgeRepository.findByBadgeId(badgeId)
                .orElseThrow(() -> new RuntimeException("徽章不存在"));

        // 删除徽章本身
        badgeRepository.delete(badge);

        // 可选：同时删除所有用户持有的该徽章
        // List<UserBadge> userBadges = userBadgeRepository.findByBadgeId(badgeId);
        // userBadgeRepository.deleteAll(userBadges);
    }

    /**
     * 6. 获取所有徽章（管理员用）
     * 
     * @param category 可选，按大类过滤 (badge/cloth)
     */
    public List<Badge> getAllBadges(String category) {
        if (category != null && !category.isEmpty()) {
            return badgeRepository.findByCategory(category);
        }
        return badgeRepository.findAll();
    }
}