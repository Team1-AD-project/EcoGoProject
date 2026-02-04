package com.example.EcoGo.service;

import com.example.EcoGo.interfacemethods.BadgeService;
import com.example.EcoGo.model.Badge;
import com.example.EcoGo.model.User;
import com.example.EcoGo.model.UserBadge;
import com.example.EcoGo.model.UserPointsLog;
import com.example.EcoGo.repository.BadgeRepository;
import com.example.EcoGo.repository.UserBadgeRepository;
import com.example.EcoGo.repository.UserRepository;
import com.example.EcoGo.repository.UserPointsLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class BadgeServiceImpl implements BadgeService {

    @Autowired private BadgeRepository badgeRepository;
    @Autowired private UserBadgeRepository userBadgeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserPointsLogRepository userPointsLogRepository;

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

        Integer cost = badge.getPurchaseCost();
        if (cost == null || cost <= 0) throw new RuntimeException("该徽章不可购买");

        User user = userRepository.findByUserid(userId)
        .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getTotalPoints() < cost) {
            throw new RuntimeException("积分不足");
        }

        long newBalance = user.getTotalPoints() - cost;
        user.setTotalPoints(newBalance);
        userRepository.save(user);

        // 记录积分消费日志
        UserPointsLog log = new UserPointsLog();
        log.setUserId(user.getId()); // 使用 UUID
        log.setChangeType("deduct");
        log.setPoints(-cost); // 负数表示扣除
        log.setSource("badge_purchase");
        log.setRelatedId(badgeId); // 关联购买的 badge ID
        log.setBalanceAfter(newBalance);
        userPointsLogRepository.save(log);

        UserBadge newBadge = new UserBadge();
        newBadge.setUserId(userId);
        newBadge.setBadgeId(badgeId);
        newBadge.setUnlockedAt(new Date());
        newBadge.setDisplay(false); // 默认不佩戴
        newBadge.setCreatedAt(new Date());

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
        
        // A. 查当前徽章的分类
        Badge targetBadgeDef = badgeRepository.findByBadgeId(badgeId)
                .orElseThrow(() -> new RuntimeException("徽章定义不存在"));
        
        String category = targetBadgeDef.getCategory(); // ✅ 现在这里不会报错了

        if (category != null && !category.isEmpty()) {
            // B. 找出该分类下所有的徽章ID (比如所有 RANK 类的)
            List<Badge> sameCategoryBadges = badgeRepository.findByCategory(category);
            List<String> sameCategoryBadgeIds = sameCategoryBadges.stream()
                    .map(Badge::getBadgeId)
                    .toList();

            // C. 找出用户身上正在戴着的、且属于该分类的旧徽章
            List<UserBadge> conflictingBadges = userBadgeRepository
                    .findByUserIdAndIsDisplayTrueAndBadgeIdIn(userId, sameCategoryBadgeIds);

            // D. 统统卸下
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
    public List<Badge> getShopList() { return badgeRepository.findByIsActive(true); }
    public List<UserBadge> getMyBadges(String userId) { return userBadgeRepository.findByUserId(userId); }
    public Badge createBadge(Badge badge) { return badgeRepository.save(badge); }

    /**
     * 3. 获取 Badge 购买统计
     * 返回每个 badge 的购买次数，供管理员查看
     */
    public List<Map<String, Object>> getBadgePurchaseStats() {
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

        return badgeRepository.save(existingBadge);
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
}