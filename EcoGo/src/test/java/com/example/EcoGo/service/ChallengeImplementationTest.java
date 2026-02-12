package com.example.EcoGo.service;

import com.example.EcoGo.dto.UserChallengeProgressDTO;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.PointsService;
import com.example.EcoGo.model.Challenge;
import com.example.EcoGo.model.User;
import com.example.EcoGo.model.UserChallengeProgress;
import com.example.EcoGo.model.UserPointsLog;
import com.example.EcoGo.repository.ChallengeRepository;
import com.example.EcoGo.repository.UserChallengeProgressRepository;
import com.example.EcoGo.repository.UserRepository;
import com.example.EcoGo.repository.UserPointsLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeImplementationTest {

    @Mock private ChallengeRepository challengeRepository;
    @Mock private UserChallengeProgressRepository userChallengeProgressRepository;
    @Mock private UserRepository userRepository;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private PointsService pointsService;
    @Mock private UserPointsLogRepository userPointsLogRepository;

    @InjectMocks private ChallengeImplementation challengeService;

    // ---------- helper ----------
    private static Challenge buildChallenge(String id, String title, String type, Double target,
                                            Integer reward, String status) {
        Challenge c = new Challenge();
        c.setId(id);
        c.setTitle(title);
        c.setType(type);
        c.setTarget(target);
        c.setReward(reward);
        c.setStatus(status);
        c.setParticipants(5);
        c.setEndTime(LocalDateTime.now().plusDays(30));
        return c;
    }

    // ---------- getAllChallenges ----------
    @Test
    void getAllChallenges_shouldSetParticipantCount() {
        Challenge c1 = buildChallenge("c1", "Walk", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        Challenge c2 = buildChallenge("c2", "Carbon", "CARBON_SAVED", 5000.0, 200, "ACTIVE");
        when(challengeRepository.findAll()).thenReturn(List.of(c1, c2));
        when(mongoTemplate.count(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(3L, 7L);

        List<Challenge> result = challengeService.getAllChallenges();

        assertEquals(2, result.size());
        assertEquals(3, result.get(0).getParticipants());
        assertEquals(7, result.get(1).getParticipants());
    }

    @Test
    void getAllChallenges_emptyList() {
        when(challengeRepository.findAll()).thenReturn(List.of());

        List<Challenge> result = challengeService.getAllChallenges();

        assertTrue(result.isEmpty());
    }

    // ---------- getChallengeById ----------
    @Test
    void getChallengeById_success() {
        Challenge c = buildChallenge("c1", "Walk", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));

        Challenge result = challengeService.getChallengeById("c1");

        assertEquals("c1", result.getId());
        assertEquals("Walk", result.getTitle());
    }

    @Test
    void getChallengeById_notFound() {
        when(challengeRepository.findById("x")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> challengeService.getChallengeById("x"));
        assertEquals(ErrorCode.CHALLENGE_NOT_FOUND.getCode(), ex.getCode());
    }

    // ---------- createChallenge ----------
    @Test
    void createChallenge_success() {
        Challenge input = new Challenge();
        input.setTitle("New Challenge");
        input.setType("CARBON_SAVED");
        input.setTarget(5000.0);

        when(challengeRepository.save(any(Challenge.class))).thenAnswer(inv -> {
            Challenge c = inv.getArgument(0);
            c.setId("c3");
            return c;
        });

        Challenge result = challengeService.createChallenge(input);

        assertNotNull(result);
        assertEquals("c3", result.getId());
        assertEquals("ACTIVE", result.getStatus());
        assertEquals(0, result.getParticipants());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());
    }

    @Test
    void createChallenge_setsDefaultIconWhenNull() {
        Challenge input = new Challenge();
        input.setIcon(null);

        when(challengeRepository.save(any(Challenge.class))).thenAnswer(inv -> inv.getArgument(0));

        Challenge result = challengeService.createChallenge(input);

        assertEquals("\uD83C\uDFC6", result.getIcon());
    }

    // ---------- updateChallenge ----------
    @Test
    void updateChallenge_success() {
        Challenge existing = buildChallenge("c1", "Old Title", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(existing));
        when(challengeRepository.save(any(Challenge.class))).thenAnswer(inv -> inv.getArgument(0));

        Challenge update = new Challenge();
        update.setTitle("New Title");
        update.setTarget(20000.0);

        Challenge result = challengeService.updateChallenge("c1", update);

        assertEquals("New Title", result.getTitle());
        assertEquals(20000.0, result.getTarget());
        // Fields not in update should remain unchanged
        assertEquals("GREEN_TRIPS_DISTANCE", result.getType());
        assertEquals(100, result.getReward());
    }

    @Test
    void updateChallenge_notFound() {
        when(challengeRepository.findById("x")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> challengeService.updateChallenge("x", new Challenge()));
        assertEquals(ErrorCode.CHALLENGE_NOT_FOUND.getCode(), ex.getCode());
    }

    // ---------- deleteChallenge ----------
    @Test
    void deleteChallenge_success() {
        when(challengeRepository.existsById("c1")).thenReturn(true);

        challengeService.deleteChallenge("c1");

        verify(mongoTemplate).remove(any(Query.class), eq(UserChallengeProgress.class));
        verify(challengeRepository).deleteById("c1");
    }

    @Test
    void deleteChallenge_notFound() {
        when(challengeRepository.existsById("x")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> challengeService.deleteChallenge("x"));
        assertEquals(ErrorCode.CHALLENGE_NOT_FOUND.getCode(), ex.getCode());
    }

    // ---------- getChallengesByStatus ----------
    @Test
    void getChallengesByStatus_success() {
        Challenge c = buildChallenge("c1", "Walk", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeRepository.findByStatus("ACTIVE")).thenReturn(List.of(c));

        List<Challenge> result = challengeService.getChallengesByStatus("ACTIVE");

        assertEquals(1, result.size());
        assertEquals("ACTIVE", result.get(0).getStatus());
    }

    // ---------- getChallengesByType ----------
    @Test
    void getChallengesByType_success() {
        Challenge c = buildChallenge("c1", "Walk", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeRepository.findByType("GREEN_TRIPS_DISTANCE")).thenReturn(List.of(c));

        List<Challenge> result = challengeService.getChallengesByType("GREEN_TRIPS_DISTANCE");

        assertEquals(1, result.size());
    }

    // ---------- getChallengesByUserId ----------
    @Test
    void getChallengesByUserId_success() {
        UserChallengeProgress p = new UserChallengeProgress();
        p.setChallengeId("c1");
        when(mongoTemplate.find(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(List.of(p));

        Challenge c = buildChallenge("c1", "Walk", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeRepository.findAllById(List.of("c1"))).thenReturn(List.of(c));

        List<Challenge> result = challengeService.getChallengesByUserId("user001");

        assertEquals(1, result.size());
        assertEquals("c1", result.get(0).getId());
    }

    @Test
    void getChallengesByUserId_noProgress_returnsEmptyList() {
        when(mongoTemplate.find(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(List.of());

        List<Challenge> result = challengeService.getChallengesByUserId("user001");

        assertTrue(result.isEmpty());
        verify(challengeRepository, never()).findAllById(anyList());
    }

    // ---------- joinChallenge ----------
    @Test
    void joinChallenge_success() {
        Challenge c = buildChallenge("c1", "Walk", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));
        when(mongoTemplate.exists(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(false);
        when(userChallengeProgressRepository.save(any(UserChallengeProgress.class)))
                .thenAnswer(inv -> {
                    UserChallengeProgress p = inv.getArgument(0);
                    p.setId("prog1");
                    return p;
                });
        when(challengeRepository.save(any(Challenge.class))).thenAnswer(inv -> inv.getArgument(0));

        UserChallengeProgress result = challengeService.joinChallenge("c1", "user001");

        assertNotNull(result);
        assertEquals("c1", result.getChallengeId());
        assertEquals("user001", result.getUserId());
        assertEquals("IN_PROGRESS", result.getStatus());
        // Participants should be incremented
        assertEquals(6, c.getParticipants());
    }

    @Test
    void joinChallenge_notActive_throwsException() {
        Challenge c = buildChallenge("c1", "Walk", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "EXPIRED");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> challengeService.joinChallenge("c1", "user001"));
        assertEquals(ErrorCode.CHALLENGE_NOT_ACTIVE.getCode(), ex.getCode());
    }

    @Test
    void joinChallenge_expired_throwsException() {
        Challenge c = buildChallenge("c1", "Walk", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        c.setEndTime(LocalDateTime.now().minusDays(1)); // Already expired
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> challengeService.joinChallenge("c1", "user001"));
        assertEquals(ErrorCode.CHALLENGE_EXPIRED.getCode(), ex.getCode());
    }

    @Test
    void joinChallenge_alreadyJoined_throwsException() {
        Challenge c = buildChallenge("c1", "Walk", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));
        when(mongoTemplate.exists(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> challengeService.joinChallenge("c1", "user001"));
        assertEquals(ErrorCode.CHALLENGE_ALREADY_JOINED.getCode(), ex.getCode());
    }

    @Test
    void joinChallenge_challengeNotFound() {
        when(challengeRepository.findById("x")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> challengeService.joinChallenge("x", "user001"));
        assertEquals(ErrorCode.CHALLENGE_NOT_FOUND.getCode(), ex.getCode());
    }

    // ---------- leaveChallenge ----------
    @Test
    void leaveChallenge_success() {
        Challenge c = buildChallenge("c1", "Walk", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));

        UserChallengeProgress p = new UserChallengeProgress();
        p.setId("prog1");
        p.setChallengeId("c1");
        p.setUserId("user001");
        when(mongoTemplate.findOne(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(p);
        when(challengeRepository.save(any(Challenge.class))).thenAnswer(inv -> inv.getArgument(0));

        challengeService.leaveChallenge("c1", "user001");

        verify(mongoTemplate).remove(p);
        assertEquals(4, c.getParticipants());
    }

    @Test
    void leaveChallenge_progressNotFound_noAction() {
        Challenge c = buildChallenge("c1", "Walk", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));
        when(mongoTemplate.findOne(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(null);

        challengeService.leaveChallenge("c1", "user001");

        verify(mongoTemplate, never()).remove(any(UserChallengeProgress.class));
        // Participants should remain unchanged
        assertEquals(5, c.getParticipants());
    }

    // ---------- claimChallengeReward ----------
    @Test
    void claimChallengeReward_notCompleted_throwsException() {
        Challenge c = buildChallenge("c1", "Walk", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));

        UserChallengeProgress p = new UserChallengeProgress();
        p.setId("prog1");
        p.setChallengeId("c1");
        p.setUserId("user001");
        p.setStatus("IN_PROGRESS");
        when(mongoTemplate.findOne(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(p);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> challengeService.claimChallengeReward("c1", "user001"));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void claimChallengeReward_alreadyClaimed_throwsException() {
        Challenge c = buildChallenge("c1", "Walk", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));

        UserChallengeProgress p = new UserChallengeProgress();
        p.setId("prog1");
        p.setChallengeId("c1");
        p.setUserId("user001");
        p.setStatus("COMPLETED");
        p.setRewardClaimed(true);
        when(mongoTemplate.findOne(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(p);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> challengeService.claimChallengeReward("c1", "user001"));
        assertEquals(ErrorCode.PARAM_ERROR.getCode(), ex.getCode());
    }

    @Test
    void claimChallengeReward_challengeNotFound() {
        when(challengeRepository.findById("x")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> challengeService.claimChallengeReward("x", "user001"));
        assertEquals(ErrorCode.CHALLENGE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void claimChallengeReward_progressNotFound() {
        Challenge c = buildChallenge("c1", "Walk", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));
        when(mongoTemplate.findOne(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> challengeService.claimChallengeReward("c1", "user001"));
        assertEquals(ErrorCode.CHALLENGE_NOT_FOUND.getCode(), ex.getCode());
    }

    // ========== Tests for buildProgressDTO / calculateProgressFromTrips / getChallengeParticipantsWithProgress / getUserChallengeProgress ==========

    // Helper: mock calculateProgressFromTrips via mongoTemplate.count (GREEN_TRIPS_COUNT)
    private void mockTripsCount(long count) {
        when(mongoTemplate.count(any(Query.class), eq("trips"))).thenReturn(count);
    }

    // Helper: mock calculateProgressFromTrips via mongoTemplate.aggregate (CARBON_SAVED / GREEN_TRIPS_DISTANCE)
    @SuppressWarnings("unchecked")
    private void mockTripsAggregation(double total) {
        AggregationResults<Map> results = mock(AggregationResults.class);
        when(results.getUniqueMappedResult()).thenReturn(Map.of("total", total));
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("trips"), eq(Map.class))).thenReturn(results);
    }

    @SuppressWarnings("unchecked")
    private void mockTripsAggregationNull() {
        AggregationResults<Map> results = mock(AggregationResults.class);
        when(results.getUniqueMappedResult()).thenReturn(null);
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("trips"), eq(Map.class))).thenReturn(results);
    }

    private static UserChallengeProgress buildProgress(String id, String challengeId, String userId, String status) {
        UserChallengeProgress p = new UserChallengeProgress();
        p.setId(id);
        p.setChallengeId(challengeId);
        p.setUserId(userId);
        p.setStatus(status);
        p.setJoinedAt(LocalDateTime.now().minusDays(5));
        p.setRewardClaimed(false);
        return p;
    }

    // ---------- getUserChallengeProgress ----------
    @Test
    void getUserChallengeProgress_success_inProgress() {
        Challenge c = buildChallenge("c1", "Count Trips", "GREEN_TRIPS_COUNT", 10.0, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));

        UserChallengeProgress p = buildProgress("p1", "c1", "user001", "IN_PROGRESS");
        when(mongoTemplate.findOne(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(p);

        // calculateProgressFromTrips: GREEN_TRIPS_COUNT → mongoTemplate.count
        mockTripsCount(3L);

        // userRepository.findByUserid
        User user = new User();
        user.setUserid("user001");
        user.setNickname("Alice");
        when(userRepository.findByUserid("user001")).thenReturn(Optional.of(user));

        UserChallengeProgressDTO result = challengeService.getUserChallengeProgress("c1", "user001");

        assertNotNull(result);
        assertEquals("c1", result.getChallengeId());
        assertEquals("user001", result.getUserId());
        assertEquals("Alice", result.getUserNickname());
        assertEquals(3.0, result.getCurrent());
        assertEquals(10.0, result.getTarget());
        assertEquals(30.0, result.getProgressPercent());
        assertEquals("IN_PROGRESS", result.getStatus());
    }

    @Test
    void getUserChallengeProgress_notFound() {
        Challenge c = buildChallenge("c1", "Walk", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));
        when(mongoTemplate.findOne(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> challengeService.getUserChallengeProgress("c1", "user001"));
        assertEquals(ErrorCode.CHALLENGE_NOT_FOUND.getCode(), ex.getCode());
    }

    // ---------- getChallengeParticipantsWithProgress ----------
    @Test
    void getChallengeParticipantsWithProgress_success() {
        Challenge c = buildChallenge("c1", "Carbon Save", "CARBON_SAVED", 5000.0, 200, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));

        UserChallengeProgress p1 = buildProgress("p1", "c1", "user001", "IN_PROGRESS");
        when(mongoTemplate.find(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(List.of(p1));

        // calculateProgressFromTrips: CARBON_SAVED → aggregation
        mockTripsAggregation(2000.0);

        User user = new User();
        user.setUserid("user001");
        user.setNickname("Alice");
        when(userRepository.findByUserid("user001")).thenReturn(Optional.of(user));

        List<UserChallengeProgressDTO> result = challengeService.getChallengeParticipantsWithProgress("c1");

        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getUserNickname());
        assertEquals(2000.0, result.get(0).getCurrent());
        assertEquals(40.0, result.get(0).getProgressPercent());
    }

    @Test
    void getChallengeParticipantsWithProgress_challengeNotFound() {
        when(challengeRepository.findById("x")).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> challengeService.getChallengeParticipantsWithProgress("x"));
        assertEquals(ErrorCode.CHALLENGE_NOT_FOUND.getCode(), ex.getCode());
    }

    // ---------- buildProgressDTO: user not found → "Unknown User" ----------
    @Test
    void getUserChallengeProgress_userNotFound_showsUnknown() {
        Challenge c = buildChallenge("c1", "Count", "GREEN_TRIPS_COUNT", 10.0, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));

        UserChallengeProgress p = buildProgress("p1", "c1", "user001", "IN_PROGRESS");
        when(mongoTemplate.findOne(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(p);
        mockTripsCount(1L);
        when(userRepository.findByUserid("user001")).thenReturn(Optional.empty());

        UserChallengeProgressDTO result = challengeService.getUserChallengeProgress("c1", "user001");

        assertEquals("Unknown User", result.getUserNickname());
        assertNull(result.getUserEmail());
    }

    // ---------- buildProgressDTO: target reached, first time COMPLETED ----------
    @Test
    void getUserChallengeProgress_targetReached_firstTimeCompleted() {
        Challenge c = buildChallenge("c1", "Count", "GREEN_TRIPS_COUNT", 5.0, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));

        UserChallengeProgress p = buildProgress("p1", "c1", "user001", "IN_PROGRESS");
        when(mongoTemplate.findOne(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(p);
        mockTripsCount(5L); // exactly meets target
        when(userRepository.findByUserid("user001")).thenReturn(Optional.empty());
        when(userChallengeProgressRepository.save(any(UserChallengeProgress.class))).thenAnswer(inv -> inv.getArgument(0));

        UserChallengeProgressDTO result = challengeService.getUserChallengeProgress("c1", "user001");

        assertEquals("COMPLETED", result.getStatus());
        assertEquals(100.0, result.getProgressPercent());
        assertFalse(result.getRewardClaimed());
        assertNotNull(result.getCompletedAt());
        // progress entity should be updated
        verify(userChallengeProgressRepository).save(any(UserChallengeProgress.class));
    }

    // ---------- buildProgressDTO: target reached, rewardClaimed=true but no points log → reset ----------
    @Test
    void getUserChallengeProgress_rewardClaimedButNoLog_resetsReward() {
        Challenge c = buildChallenge("c1", "Count", "GREEN_TRIPS_COUNT", 5.0, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));

        UserChallengeProgress p = buildProgress("p1", "c1", "user001", "COMPLETED");
        p.setRewardClaimed(true);
        when(mongoTemplate.findOne(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(p);
        mockTripsCount(10L); // exceeds target
        when(userRepository.findByUserid("user001")).thenReturn(Optional.empty());

        // No matching log → should reset rewardClaimed
        when(userPointsLogRepository.findByUserIdAndSource("user001", "challenges")).thenReturn(List.of());
        when(userChallengeProgressRepository.save(any(UserChallengeProgress.class))).thenAnswer(inv -> inv.getArgument(0));

        UserChallengeProgressDTO result = challengeService.getUserChallengeProgress("c1", "user001");

        assertEquals("COMPLETED", result.getStatus());
        assertFalse(result.getRewardClaimed());
        verify(userChallengeProgressRepository).save(any(UserChallengeProgress.class));
    }

    // ---------- buildProgressDTO: target reached, rewardClaimed=true with valid log → keep ----------
    @Test
    void getUserChallengeProgress_rewardClaimedWithLog_keepsReward() {
        Challenge c = buildChallenge("c1", "Count", "GREEN_TRIPS_COUNT", 5.0, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));

        UserChallengeProgress p = buildProgress("p1", "c1", "user001", "COMPLETED");
        p.setRewardClaimed(true);
        when(mongoTemplate.findOne(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(p);
        mockTripsCount(10L);
        when(userRepository.findByUserid("user001")).thenReturn(Optional.empty());

        // Has matching log
        UserPointsLog log = new UserPointsLog();
        log.setRelatedId("c1");
        when(userPointsLogRepository.findByUserIdAndSource("user001", "challenges")).thenReturn(List.of(log));

        UserChallengeProgressDTO result = challengeService.getUserChallengeProgress("c1", "user001");

        assertEquals("COMPLETED", result.getStatus());
        // rewardClaimed should remain true (from progress entity)
        assertTrue(result.getRewardClaimed());
    }

    // ---------- buildProgressDTO: not reached target, previously COMPLETED but no reward → revert to IN_PROGRESS ----------
    @Test
    void getUserChallengeProgress_previouslyCompletedButBelowTarget_revertsToInProgress() {
        Challenge c = buildChallenge("c1", "Count", "GREEN_TRIPS_COUNT", 10.0, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));

        UserChallengeProgress p = buildProgress("p1", "c1", "user001", "COMPLETED");
        p.setRewardClaimed(false);
        when(mongoTemplate.findOne(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(p);
        mockTripsCount(3L); // below target
        when(userRepository.findByUserid("user001")).thenReturn(Optional.empty());
        when(userChallengeProgressRepository.save(any(UserChallengeProgress.class))).thenAnswer(inv -> inv.getArgument(0));

        UserChallengeProgressDTO result = challengeService.getUserChallengeProgress("c1", "user001");

        assertEquals("IN_PROGRESS", result.getStatus());
        verify(userChallengeProgressRepository).save(any(UserChallengeProgress.class));
    }

    // ---------- buildProgressDTO: target is null → progressPercent=0 ----------
    @Test
    void getUserChallengeProgress_nullTarget_zeroPercent() {
        Challenge c = buildChallenge("c1", "Count", "GREEN_TRIPS_COUNT", null, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));

        UserChallengeProgress p = buildProgress("p1", "c1", "user001", "IN_PROGRESS");
        when(mongoTemplate.findOne(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(p);
        mockTripsCount(5L);
        when(userRepository.findByUserid("user001")).thenReturn(Optional.empty());

        UserChallengeProgressDTO result = challengeService.getUserChallengeProgress("c1", "user001");

        assertEquals(0.0, result.getProgressPercent());
        assertEquals("IN_PROGRESS", result.getStatus());
    }

    // ---------- calculateProgressFromTrips: GREEN_TRIPS_DISTANCE ----------
    @Test
    void getUserChallengeProgress_greenTripsDistance() {
        Challenge c = buildChallenge("c1", "Distance", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));

        UserChallengeProgress p = buildProgress("p1", "c1", "user001", "IN_PROGRESS");
        when(mongoTemplate.findOne(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(p);
        mockTripsAggregation(5000.0);
        when(userRepository.findByUserid("user001")).thenReturn(Optional.empty());

        UserChallengeProgressDTO result = challengeService.getUserChallengeProgress("c1", "user001");

        assertEquals(5000.0, result.getCurrent());
        assertEquals(50.0, result.getProgressPercent());
    }

    // ---------- calculateProgressFromTrips: CARBON_SAVED with null result ----------
    @Test
    void getUserChallengeProgress_carbonSaved_nullResult() {
        Challenge c = buildChallenge("c1", "Carbon", "CARBON_SAVED", 1000.0, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));

        UserChallengeProgress p = buildProgress("p1", "c1", "user001", "IN_PROGRESS");
        when(mongoTemplate.findOne(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(p);
        mockTripsAggregationNull();
        when(userRepository.findByUserid("user001")).thenReturn(Optional.empty());

        UserChallengeProgressDTO result = challengeService.getUserChallengeProgress("c1", "user001");

        assertEquals(0.0, result.getCurrent());
    }

    // ---------- calculateProgressFromTrips: unknown type → 0.0 ----------
    @Test
    void getUserChallengeProgress_unknownType_zeroProgress() {
        Challenge c = buildChallenge("c1", "Unknown", "UNKNOWN_TYPE", 100.0, 50, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));

        UserChallengeProgress p = buildProgress("p1", "c1", "user001", "IN_PROGRESS");
        when(mongoTemplate.findOne(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(p);
        when(userRepository.findByUserid("user001")).thenReturn(Optional.empty());

        UserChallengeProgressDTO result = challengeService.getUserChallengeProgress("c1", "user001");

        assertEquals(0.0, result.getCurrent());
    }

    // ---------- claimChallengeReward: success path ----------
    @Test
    void claimChallengeReward_success_awardsPointsAndCallsBuildProgressDTO() {
        Challenge c = buildChallenge("c1", "Count", "GREEN_TRIPS_COUNT", 5.0, 100, "ACTIVE");
        when(challengeRepository.findById("c1")).thenReturn(Optional.of(c));

        UserChallengeProgress p = buildProgress("p1", "c1", "user001", "COMPLETED");
        p.setRewardClaimed(false);
        when(mongoTemplate.findOne(any(Query.class), eq(UserChallengeProgress.class))).thenReturn(p);

        // For pointsService.adjustPoints (returns UserPointsLog, not void)
        when(pointsService.adjustPoints(eq("user001"), eq(100L), eq("challenges"),
                anyString(), eq("c1"), isNull())).thenReturn(new UserPointsLog());

        // For buildProgressDTO called at the end
        when(userChallengeProgressRepository.save(any(UserChallengeProgress.class))).thenAnswer(inv -> inv.getArgument(0));
        mockTripsCount(10L); // exceeds target
        when(userRepository.findByUserid("user001")).thenReturn(Optional.empty());

        // rewardClaimed is now true, so buildProgressDTO checks user_points_logs
        UserPointsLog log = new UserPointsLog();
        log.setRelatedId("c1");
        when(userPointsLogRepository.findByUserIdAndSource("user001", "challenges")).thenReturn(List.of(log));

        UserChallengeProgressDTO result = challengeService.claimChallengeReward("c1", "user001");

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        verify(pointsService).adjustPoints(eq("user001"), eq(100L), eq("challenges"),
                anyString(), eq("c1"), isNull());
    }
}
