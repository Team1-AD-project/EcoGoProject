package com.example.EcoGo.controller;

import com.example.EcoGo.dto.LeaderboardRankingDto;
import com.example.EcoGo.dto.LeaderboardStatsDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.LeaderboardInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LeaderboardControllerTest {

    private LeaderboardInterface leaderboardService;
    private LeaderboardController controller;

    @BeforeEach
    void setUp() throws Exception {
        leaderboardService = mock(LeaderboardInterface.class);
        controller = new LeaderboardController();

        Field f = LeaderboardController.class.getDeclaredField("leaderboardService");
        f.setAccessible(true);
        f.set(controller, leaderboardService);
    }

    // ---------- helper ----------
    private static LeaderboardStatsDto buildStatsDto() {
        LeaderboardRankingDto ranking = new LeaderboardRankingDto("user001", "Alice", 1, 150.0, true, "MONTHLY");
        Page<LeaderboardRankingDto> page = new PageImpl<>(List.of(ranking), PageRequest.of(0, 10), 1);
        return new LeaderboardStatsDto(page, 150L, 1L, 0L);
    }

    // ---------- getWebRankings ----------
    @Test
    void getWebRankings_success_monthly() {
        LeaderboardStatsDto statsDto = buildStatsDto();
        when(leaderboardService.getRankings("MONTHLY", "2026-02", "", 0, 10)).thenReturn(statsDto);

        ResponseMessage<LeaderboardStatsDto> resp = controller.getWebRankings("MONTHLY", "2026-02", "", 0, 10);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals(150L, resp.getData().getTotalCarbonSaved());
        assertEquals(1L, resp.getData().getTotalVipUsers());
        verify(leaderboardService).getRankings("MONTHLY", "2026-02", "", 0, 10);
    }

    @Test
    void getWebRankings_success_daily() {
        LeaderboardStatsDto statsDto = buildStatsDto();
        when(leaderboardService.getRankings("DAILY", "2026-02-07", "alice", 0, 5)).thenReturn(statsDto);

        ResponseMessage<LeaderboardStatsDto> resp = controller.getWebRankings("DAILY", "2026-02-07", "alice", 0, 5);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        verify(leaderboardService).getRankings("DAILY", "2026-02-07", "alice", 0, 5);
    }

    @Test
    void getWebRankings_emptyResult() {
        Page<LeaderboardRankingDto> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        LeaderboardStatsDto emptyDto = new LeaderboardStatsDto(emptyPage, 0L, 0L, 0L);
        when(leaderboardService.getRankings("MONTHLY", "", "", 0, 10)).thenReturn(emptyDto);

        ResponseMessage<LeaderboardStatsDto> resp = controller.getWebRankings("MONTHLY", "", "", 0, 10);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertEquals(0L, resp.getData().getTotalCarbonSaved());
        assertEquals(0, resp.getData().getRankingsPage().getTotalElements());
    }

    // ---------- getMobileRankings ----------
    @Test
    void getMobileRankings_success() {
        LeaderboardStatsDto statsDto = buildStatsDto();
        // Mobile passes empty string for date
        when(leaderboardService.getRankings("MONTHLY", "", "", 0, 10)).thenReturn(statsDto);

        ResponseMessage<LeaderboardStatsDto> resp = controller.getMobileRankings("MONTHLY", "", 0, 10);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        assertNotNull(resp.getData());
        verify(leaderboardService).getRankings("MONTHLY", "", "", 0, 10);
    }

    @Test
    void getMobileRankings_withNameSearch() {
        LeaderboardStatsDto statsDto = buildStatsDto();
        when(leaderboardService.getRankings("DAILY", "", "bob", 0, 10)).thenReturn(statsDto);

        ResponseMessage<LeaderboardStatsDto> resp = controller.getMobileRankings("DAILY", "bob", 0, 10);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        verify(leaderboardService).getRankings("DAILY", "", "bob", 0, 10);
    }

    @Test
    void getMobileRankings_pagination() {
        LeaderboardStatsDto statsDto = buildStatsDto();
        when(leaderboardService.getRankings("MONTHLY", "", "", 2, 5)).thenReturn(statsDto);

        ResponseMessage<LeaderboardStatsDto> resp = controller.getMobileRankings("MONTHLY", "", 2, 5);

        assertEquals(ErrorCode.SUCCESS.getCode(), resp.getCode());
        verify(leaderboardService).getRankings("MONTHLY", "", "", 2, 5);
    }
}
