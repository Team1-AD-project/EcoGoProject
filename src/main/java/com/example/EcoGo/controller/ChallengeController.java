package com.example.EcoGo.controller;

import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.dto.UserChallengeProgressDTO;
import com.example.EcoGo.interfacemethods.ChallengeInterface;
import com.example.EcoGo.model.Challenge;
import com.example.EcoGo.model.UserChallengeProgress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 挑战管理接口控制器
 * 路径规范：/api/v1/challenges
 *
 * 挑战类型(type):
 * - GREEN_TRIPS_DISTANCE: 绿色出行总距离(米)
 * - CARBON_SAVED: 碳排放减少量(克)
 * - GREEN_TRIPS_COUNT: 绿色出行次数
 *
 * 注意：用户进度从Trip表实时计算，不存储
 */
@RestController
@RequestMapping("/api/v1")
public class ChallengeController {
    private static final Logger logger = LoggerFactory.getLogger(ChallengeController.class);

    @Autowired
    private ChallengeInterface challengeService;

    // === Web Endpoints (Admin) ===

    /**
     * 获取所有挑战
     * GET /api/v1/web/challenges
     */
    @GetMapping("/web/challenges")
    public ResponseMessage<List<Challenge>> getAllWebChallenges() {
        logger.info("[WEB] Fetching all challenges");
        return ResponseMessage.success(challengeService.getAllChallenges());
    }

    /**
     * 根据ID获取挑战
     * GET /api/v1/web/challenges/{id}
     */
    @GetMapping("/web/challenges/{id}")
    public ResponseMessage<Challenge> getWebChallengeById(@PathVariable String id) {
        logger.info("[WEB] Fetching challenge by ID: {}", id);
        return ResponseMessage.success(challengeService.getChallengeById(id));
    }

    /**
     * 创建挑战
     * POST /api/v1/web/challenges
     */
    @PostMapping("/web/challenges")
    public ResponseMessage<Challenge> createWebChallenge(@RequestBody Challenge challenge) {
        logger.info("[WEB] Creating new challenge: {}", challenge.getTitle());
        return ResponseMessage.success(challengeService.createChallenge(challenge));
    }

    /**
     * 更新挑战
     * PUT /api/v1/web/challenges/{id}
     */
    @PutMapping("/web/challenges/{id}")
    public ResponseMessage<Challenge> updateWebChallenge(
            @PathVariable String id,
            @RequestBody Challenge challenge) {
        logger.info("[WEB] Updating challenge: {}", id);
        return ResponseMessage.success(challengeService.updateChallenge(id, challenge));
    }

    /**
     * 删除挑战
     * DELETE /api/v1/web/challenges/{id}
     */
    @DeleteMapping("/web/challenges/{id}")
    public ResponseMessage<Void> deleteWebChallenge(@PathVariable String id) {
        logger.info("[WEB] Deleting challenge: {}", id);
        challengeService.deleteChallenge(id);
        return ResponseMessage.success(null);
    }

    /**
     * 根据状态获取挑战
     * GET /api/v1/web/challenges/status/{status}
     */
    @GetMapping("/web/challenges/status/{status}")
    public ResponseMessage<List<Challenge>> getWebChallengesByStatus(@PathVariable String status) {
        logger.info("[WEB] Fetching challenges by status: {}", status);
        return ResponseMessage.success(challengeService.getChallengesByStatus(status));
    }

    /**
     * 根据类型获取挑战
     * GET /api/v1/web/challenges/type/{type}
     */
    @GetMapping("/web/challenges/type/{type}")
    public ResponseMessage<List<Challenge>> getWebChallengesByType(@PathVariable String type) {
        logger.info("[WEB] Fetching challenges by type: {}", type);
        return ResponseMessage.success(challengeService.getChallengesByType(type));
    }

    /**
     * 获取挑战的所有参与者及其进度（从Trip表实时计算）
     * GET /api/v1/web/challenges/{id}/participants
     */
    @GetMapping("/web/challenges/{id}/participants")
    public ResponseMessage<List<UserChallengeProgressDTO>> getWebChallengeParticipants(@PathVariable String id) {
        logger.info("[WEB] Fetching participants for challenge: {}", id);
        return ResponseMessage.success(challengeService.getChallengeParticipantsWithProgress(id));
    }

    // === Mobile Endpoints ===

    /**
     * 获取所有挑战
     * GET /api/v1/mobile/challenges
     */
    @GetMapping("/mobile/challenges")
    public ResponseMessage<List<Challenge>> getAllMobileChallenges() {
        logger.info("[Mobile] Fetching all challenges");
        return ResponseMessage.success(challengeService.getAllChallenges());
    }

    /**
     * 根据ID获取挑战
     * GET /api/v1/mobile/challenges/{id}
     */
    @GetMapping("/mobile/challenges/{id}")
    public ResponseMessage<Challenge> getMobileChallengeById(@PathVariable String id) {
        logger.info("[Mobile] Fetching challenge by ID: {}", id);
        return ResponseMessage.success(challengeService.getChallengeById(id));
    }

    /**
     * 根据状态获取挑战
     * GET /api/v1/mobile/challenges/status/{status}
     */
    @GetMapping("/mobile/challenges/status/{status}")
    public ResponseMessage<List<Challenge>> getMobileChallengesByStatus(@PathVariable String status) {
        logger.info("[Mobile] Fetching challenges by status: {}", status);
        return ResponseMessage.success(challengeService.getChallengesByStatus(status));
    }

    /**
     * 根据类型获取挑战
     * GET /api/v1/mobile/challenges/type/{type}
     */
    @GetMapping("/mobile/challenges/type/{type}")
    public ResponseMessage<List<Challenge>> getMobileChallengesByType(@PathVariable String type) {
        logger.info("[Mobile] Fetching challenges by type: {}", type);
        return ResponseMessage.success(challengeService.getChallengesByType(type));
    }

    /**
     * 获取用户参加的挑战
     * GET /api/v1/mobile/challenges/user/{userId}
     */
    @GetMapping("/mobile/challenges/user/{userId}")
    public ResponseMessage<List<Challenge>> getMobileChallengesByUserId(@PathVariable String userId) {
        logger.info("[Mobile] Fetching challenges for user: {}", userId);
        return ResponseMessage.success(challengeService.getChallengesByUserId(userId));
    }

    /**
     * 参加挑战
     * POST /api/v1/mobile/challenges/{id}/join
     */
    @PostMapping("/mobile/challenges/{id}/join")
    public ResponseMessage<UserChallengeProgress> joinMobileChallenge(
            @PathVariable String id,
            @RequestParam String userId) {
        logger.info("[Mobile] User {} joining challenge {}", userId, id);
        return ResponseMessage.success(challengeService.joinChallenge(id, userId));
    }

    /**
     * 退出挑战
     * POST /api/v1/mobile/challenges/{id}/leave
     */
    @PostMapping("/mobile/challenges/{id}/leave")
    public ResponseMessage<Void> leaveMobileChallenge(
            @PathVariable String id,
            @RequestParam String userId) {
        logger.info("[Mobile] User {} leaving challenge {}", userId, id);
        challengeService.leaveChallenge(id, userId);
        return ResponseMessage.success(null);
    }

    /**
     * 获取用户在某挑战的进度（从Trip表实时计算）
     * GET /api/v1/mobile/challenges/{id}/progress
     */
    @GetMapping("/mobile/challenges/{id}/progress")
    public ResponseMessage<UserChallengeProgressDTO> getMobileChallengeProgress(
            @PathVariable String id,
            @RequestParam String userId) {
        logger.info("[Mobile] Getting challenge {} progress for user {}", id, userId);
        return ResponseMessage.success(challengeService.getUserChallengeProgress(id, userId));
    }

    /**
     * 领取挑战完成奖励
     * POST /api/v1/mobile/challenges/{id}/claim-reward
     */
    @PostMapping("/mobile/challenges/{id}/claim-reward")
    public ResponseMessage<UserChallengeProgressDTO> claimMobileChallengeReward(
            @PathVariable String id,
            @RequestParam String userId) {
        logger.info("[Mobile] User {} claiming reward for challenge {}", userId, id);
        return ResponseMessage.success(challengeService.claimChallengeReward(id, userId));
    }
}
