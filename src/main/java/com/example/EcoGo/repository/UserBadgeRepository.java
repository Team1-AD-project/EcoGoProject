package com.example.EcoGo.repository;
import com.example.EcoGo.dto.BadgePurchaseStatDto;
import com.example.EcoGo.model.UserBadge;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBadgeRepository extends MongoRepository<UserBadge, String> {
    List<UserBadge> findByUserId(String userId);
    List<UserBadge> findByBadgeId(String badgeId);
    boolean existsByUserIdAndBadgeId(String userId, String badgeId);
    Optional<UserBadge> findByUserIdAndBadgeId(String userId, String badgeId);

    List<UserBadge> findByUserIdAndIsDisplayTrueAndBadgeIdIn(String userId, List<String> badgeIds);

    // 统计 user_badges 中每个 badge_id 的购买次数
    // $group by badge_id → _id=badge_id值, purchaseCount=数量
    // DTO 通过 @Field("_id") 将 _id 映射为 badgeId
    @Aggregation(pipeline = {
        "{ '$group': { '_id': '$badge_id', 'purchaseCount': { '$sum': 1 } } }"
    })
    List<BadgePurchaseStatDto> countPurchasesByBadge();
}
