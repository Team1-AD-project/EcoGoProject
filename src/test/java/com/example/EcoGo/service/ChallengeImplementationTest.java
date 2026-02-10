package com.example.EcoGo.service;

import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.PointsService;
import com.example.EcoGo.model.Challenge;
import com.example.EcoGo.model.UserChallengeProgress;
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
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.List;
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
}
