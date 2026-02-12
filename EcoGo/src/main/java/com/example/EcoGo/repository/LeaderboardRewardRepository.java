package com.example.EcoGo.repository;

import com.example.EcoGo.model.LeaderboardReward;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaderboardRewardRepository extends MongoRepository<LeaderboardReward, String> {
    boolean existsByTypeAndPeriodKey(String type, String periodKey);
    List<LeaderboardReward> findByTypeAndPeriodKey(String type, String periodKey);
}
