package com.example.EcoGo.interfacemethods;

import com.example.EcoGo.dto.UserChallengeProgressDTO;
import com.example.EcoGo.model.Challenge;
import com.example.EcoGo.model.UserChallengeProgress;

import java.util.List;

public interface ChallengeInterface {
    // CRUD
    List<Challenge> getAllChallenges();
    Challenge getChallengeById(String id);
    Challenge createChallenge(Challenge challenge);
    Challenge updateChallenge(String id, Challenge challenge);
    void deleteChallenge(String id);

    // 挑战查询
    List<Challenge> getChallengesByStatus(String status);
    List<Challenge> getChallengesByType(String type);

    // 用户参与挑战
    List<Challenge> getChallengesByUserId(String userId);
    UserChallengeProgress joinChallenge(String challengeId, String userId);
    void leaveChallenge(String challengeId, String userId);

    // 获取挑战参与者及其进度（从Trip表实时计算）
    List<UserChallengeProgressDTO> getChallengeParticipantsWithProgress(String challengeId);

    // 获取单个用户在某挑战的进度
    UserChallengeProgressDTO getUserChallengeProgress(String challengeId, String userId);

    // 领取挑战完成奖励
    UserChallengeProgressDTO claimChallengeReward(String challengeId, String userId);
}
