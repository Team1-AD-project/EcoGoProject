package com.example.EcoGo.repository;
import com.example.EcoGo.model.UserBadge;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface UserBadgeRepository extends MongoRepository<UserBadge, String> {
    List<UserBadge> findByUserId(String userId);
    List<UserBadge> findByBadgeId(String badgeId);
    boolean existsByUserIdAndBadgeId(String userId, String badgeId);
    Optional<UserBadge> findByUserIdAndBadgeId(String userId, String badgeId);

    // ✅ 必须有这个方法，Service 才能查出用户身上正在戴着的冲突徽章
    List<UserBadge> findByUserIdAndIsDisplayTrueAndBadgeIdIn(String userId, List<String> badgeIds);

    // 统计每个 badge 的购买次数
    @Aggregation(pipeline = {
        "{ '$group': { '_id': '$badge_id', 'purchaseCount': { '$sum': 1 } } }",
        "{ '$project': { 'badgeId': '$_id', 'purchaseCount': 1, '_id': 0 } }"
    })
    List<Map<String, Object>> countPurchasesByBadge();
}
