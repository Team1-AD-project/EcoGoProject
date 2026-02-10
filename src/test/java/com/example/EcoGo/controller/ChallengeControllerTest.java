package com.example.EcoGo.controller;

import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.dto.UserChallengeProgressDTO;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.ChallengeInterface;
import com.example.EcoGo.model.Challenge;
import com.example.EcoGo.model.UserChallengeProgress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChallengeControllerTest {

    private ChallengeInterface challengeService;
    private ChallengeController controller;

    @BeforeEach
    void setUp() throws Exception {
        challengeService = mock(ChallengeInterface.class);
        controller = new ChallengeController();

        Field f = ChallengeController.class.getDeclaredField("challengeService");
        f.setAccessible(true);
        f.set(controller, challengeService);
    }

    // ---------- helper ----------
    private static Challenge buildChallenge(String id, String title, String type, Double target, Integer reward, String status) {
        Challenge c = new Challenge();
        c.setId(id);
        c.setTitle(title);
        c.setType(type);
        c.setTarget(target);
        c.setReward(reward);
        c.setStatus(status);
        c.setParticipants(5);
        return c;
    }

    private static UserChallengeProgress buildProgress(String id, String challengeId, String userId) {
        UserChallengeProgress p = new UserChallengeProgress();
        p.setId(id);
        p.setChallengeId(challengeId);
        p.setUserId(userId);
        p.setStatus("IN_PROGRESS");
        p.setJoinedAt(LocalDateTime.now());
        return p;
    }

    private static UserChallengeProgressDTO buildProgressDTO(String challengeId, String userId) {
        UserChallengeProgressDTO dto = new UserChallengeProgressDTO();
        dto.setId("prog001");
        dto.setChallengeId(challengeId);
        dto.setUserId(userId);
        dto.setStatus("IN_PROGRESS");
        dto.setCurrent(50.0);
        dto.setTarget(100.0);
        dto.setProgressPercent(50.0);
        return dto;
    }

    // ========== Web Endpoints ==========

    // ---------- getAllWebChallenges ----------
    @Test
    void getAllWebChallenges_success() {
        Challenge c1 = buildChallenge("c1", "Walk 10km", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        Challenge c2 = buildChallenge("c2", "Save Carbon", "CARBON_SAVED", 5000.0, 200, "ACTIVE");
        when(challengeService.getAllChallenges()).thenReturn(List.of(c1, c2));

        ResponseMessage<List<Challenge>> resp = controller.getAllWebChallenges();

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(2, resp.getData().size());
        verify(challengeService).getAllChallenges();
    }

    @Test
    void getAllWebChallenges_emptyList() {
        when(challengeService.getAllChallenges()).thenReturn(List.of());

        ResponseMessage<List<Challenge>> resp = controller.getAllWebChallenges();

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertTrue(resp.getData().isEmpty());
    }

    // ---------- getWebChallengeById ----------
    @Test
    void getWebChallengeById_success() {
        Challenge c = buildChallenge("c1", "Walk 10km", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeService.getChallengeById("c1")).thenReturn(c);

        ResponseMessage<Challenge> resp = controller.getWebChallengeById("c1");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals("c1", resp.getData().getId());
        assertEquals("Walk 10km", resp.getData().getTitle());
    }

    // ---------- createWebChallenge ----------
    @Test
    void createWebChallenge_success() {
        Challenge input = buildChallenge(null, "New Challenge", "CARBON_SAVED", 5000.0, 200, "ACTIVE");
        Challenge created = buildChallenge("c3", "New Challenge", "CARBON_SAVED", 5000.0, 200, "ACTIVE");
        when(challengeService.createChallenge(any(Challenge.class))).thenReturn(created);

        ResponseMessage<Challenge> resp = controller.createWebChallenge(input);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals("c3", resp.getData().getId());
        verify(challengeService).createChallenge(input);
    }

    // ---------- updateWebChallenge ----------
    @Test
    void updateWebChallenge_success() {
        Challenge input = new Challenge();
        input.setTitle("Updated Title");
        Challenge updated = buildChallenge("c1", "Updated Title", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeService.updateChallenge(eq("c1"), any(Challenge.class))).thenReturn(updated);

        ResponseMessage<Challenge> resp = controller.updateWebChallenge("c1", input);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals("Updated Title", resp.getData().getTitle());
    }

    // ---------- deleteWebChallenge ----------
    @Test
    void deleteWebChallenge_success() {
        doNothing().when(challengeService).deleteChallenge("c1");

        ResponseMessage<Void> resp = controller.deleteWebChallenge("c1");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        verify(challengeService).deleteChallenge("c1");
    }

    // ---------- getWebChallengesByStatus ----------
    @Test
    void getWebChallengesByStatus_success() {
        Challenge c = buildChallenge("c1", "Walk 10km", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeService.getChallengesByStatus("ACTIVE")).thenReturn(List.of(c));

        ResponseMessage<List<Challenge>> resp = controller.getWebChallengesByStatus("ACTIVE");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(1, resp.getData().size());
    }

    // ---------- getWebChallengesByType ----------
    @Test
    void getWebChallengesByType_success() {
        Challenge c = buildChallenge("c1", "Walk 10km", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeService.getChallengesByType("GREEN_TRIPS_DISTANCE")).thenReturn(List.of(c));

        ResponseMessage<List<Challenge>> resp = controller.getWebChallengesByType("GREEN_TRIPS_DISTANCE");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(1, resp.getData().size());
    }

    // ---------- getWebChallengeParticipants ----------
    @Test
    void getWebChallengeParticipants_success() {
        UserChallengeProgressDTO dto = buildProgressDTO("c1", "user001");
        when(challengeService.getChallengeParticipantsWithProgress("c1")).thenReturn(List.of(dto));

        ResponseMessage<List<UserChallengeProgressDTO>> resp = controller.getWebChallengeParticipants("c1");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(1, resp.getData().size());
        assertEquals("user001", resp.getData().get(0).getUserId());
    }

    // ========== Mobile Endpoints ==========

    // ---------- getAllMobileChallenges ----------
    @Test
    void getAllMobileChallenges_success() {
        Challenge c = buildChallenge("c1", "Walk 10km", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeService.getAllChallenges()).thenReturn(List.of(c));

        ResponseMessage<List<Challenge>> resp = controller.getAllMobileChallenges();

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(1, resp.getData().size());
    }

    // ---------- getMobileChallengeById ----------
    @Test
    void getMobileChallengeById_success() {
        Challenge c = buildChallenge("c1", "Walk 10km", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeService.getChallengeById("c1")).thenReturn(c);

        ResponseMessage<Challenge> resp = controller.getMobileChallengeById("c1");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals("c1", resp.getData().getId());
    }

    // ---------- getMobileChallengesByStatus ----------
    @Test
    void getMobileChallengesByStatus_success() {
        when(challengeService.getChallengesByStatus("EXPIRED")).thenReturn(List.of());

        ResponseMessage<List<Challenge>> resp = controller.getMobileChallengesByStatus("EXPIRED");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertTrue(resp.getData().isEmpty());
    }

    // ---------- getMobileChallengesByType ----------
    @Test
    void getMobileChallengesByType_success() {
        Challenge c = buildChallenge("c2", "Save Carbon", "CARBON_SAVED", 5000.0, 200, "ACTIVE");
        when(challengeService.getChallengesByType("CARBON_SAVED")).thenReturn(List.of(c));

        ResponseMessage<List<Challenge>> resp = controller.getMobileChallengesByType("CARBON_SAVED");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(1, resp.getData().size());
    }

    // ---------- getMobileChallengesByUserId ----------
    @Test
    void getMobileChallengesByUserId_success() {
        Challenge c = buildChallenge("c1", "Walk 10km", "GREEN_TRIPS_DISTANCE", 10000.0, 100, "ACTIVE");
        when(challengeService.getChallengesByUserId("user001")).thenReturn(List.of(c));

        ResponseMessage<List<Challenge>> resp = controller.getMobileChallengesByUserId("user001");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(1, resp.getData().size());
    }

    // ---------- joinMobileChallenge ----------
    @Test
    void joinMobileChallenge_success() {
        UserChallengeProgress progress = buildProgress("prog1", "c1", "user001");
        when(challengeService.joinChallenge("c1", "user001")).thenReturn(progress);

        ResponseMessage<UserChallengeProgress> resp = controller.joinMobileChallenge("c1", "user001");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals("c1", resp.getData().getChallengeId());
        assertEquals("user001", resp.getData().getUserId());
    }

    // ---------- leaveMobileChallenge ----------
    @Test
    void leaveMobileChallenge_success() {
        doNothing().when(challengeService).leaveChallenge("c1", "user001");

        ResponseMessage<Void> resp = controller.leaveMobileChallenge("c1", "user001");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        verify(challengeService).leaveChallenge("c1", "user001");
    }

    // ---------- getMobileChallengeProgress ----------
    @Test
    void getMobileChallengeProgress_success() {
        UserChallengeProgressDTO dto = buildProgressDTO("c1", "user001");
        when(challengeService.getUserChallengeProgress("c1", "user001")).thenReturn(dto);

        ResponseMessage<UserChallengeProgressDTO> resp = controller.getMobileChallengeProgress("c1", "user001");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(50.0, resp.getData().getCurrent());
        assertEquals(50.0, resp.getData().getProgressPercent());
    }

    // ---------- claimMobileChallengeReward ----------
    @Test
    void claimMobileChallengeReward_success() {
        UserChallengeProgressDTO dto = buildProgressDTO("c1", "user001");
        dto.setStatus("COMPLETED");
        dto.setRewardClaimed(true);
        when(challengeService.claimChallengeReward("c1", "user001")).thenReturn(dto);

        ResponseMessage<UserChallengeProgressDTO> resp = controller.claimMobileChallengeReward("c1", "user001");

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals("COMPLETED", resp.getData().getStatus());
        assertTrue(resp.getData().getRewardClaimed());
    }
}
