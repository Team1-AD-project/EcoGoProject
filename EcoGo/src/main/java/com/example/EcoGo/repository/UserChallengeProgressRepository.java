package com.example.EcoGo.repository;

import com.example.EcoGo.model.UserChallengeProgress;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserChallengeProgressRepository extends MongoRepository<UserChallengeProgress, String> {
    // 根据挑战ID查找所有参与记录
    List<UserChallengeProgress> findByChallengeId(String challengeId);

    // 根据用户ID查找所有参与记录
    List<UserChallengeProgress> findByUserId(String userId);

    // 根据挑战ID和用户ID查找记录
    Optional<UserChallengeProgress> findByChallengeIdAndUserId(String challengeId, String userId);

    // 根据状态查找
    List<UserChallengeProgress> findByStatus(String status);

    // 根据挑战ID和状态查找
    List<UserChallengeProgress> findByChallengeIdAndStatus(String challengeId, String status);

    // 统计挑战参与人数
    long countByChallengeId(String challengeId);

    // 检查用户是否已参与挑战
    boolean existsByChallengeIdAndUserId(String challengeId, String userId);

    // 删除挑战的所有参与记录
    void deleteByChallengeId(String challengeId);
}
