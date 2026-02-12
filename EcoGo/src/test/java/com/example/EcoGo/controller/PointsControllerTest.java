package com.example.EcoGo.controller;

import com.example.EcoGo.dto.PointsDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.PointsService;
import com.example.EcoGo.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointsControllerTest {

    @Mock
    private PointsService pointsService;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private PointsController pointsController;

    private String mockToken;
    private String mockUserId;

    @BeforeEach
    void setUp() {
        mockToken = "Bearer mock-token";
        mockUserId = "user123";
    }

    @Test
    void getCurrentPoints_success() {
        when(jwtUtils.getUserIdFromToken("mock-token")).thenReturn(mockUserId);
        PointsDto.CurrentPointsResponse response = new PointsDto.CurrentPointsResponse(mockUserId, 100L, 200L);
        when(pointsService.getCurrentPoints(mockUserId)).thenReturn(response);

        ResponseMessage<PointsDto.CurrentPointsResponse> result = pointsController.getCurrentPoints(mockToken);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        assertEquals(100L, result.getData().currentPoints);
        verify(pointsService).getCurrentPoints(mockUserId);
    }

    @Test
    void getHistory_success() {
        when(jwtUtils.getUserIdFromToken("mock-token")).thenReturn(mockUserId);
        when(pointsService.getPointsHistory(mockUserId)).thenReturn(Collections.emptyList());

        ResponseMessage<List<PointsDto.PointsLogResponse>> result = pointsController.getHistory(mockToken);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(pointsService).getPointsHistory(mockUserId);
    }

    @Test
    void calculatePoints_success() {
        when(pointsService.calculatePoints("walk", 5.0)).thenReturn(50L);

        ResponseMessage<Long> result = pointsController.calculatePoints("walk", 5.0);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        assertEquals(50L, result.getData());
        verify(pointsService).calculatePoints("walk", 5.0);
    }

    @Test
    void getMyTripStats_success() {
        when(jwtUtils.getUserIdFromToken("mock-token")).thenReturn(mockUserId);
        PointsDto.TripStatsResponse response = new PointsDto.TripStatsResponse(10, 500);
        when(pointsService.getTripStats(mockUserId)).thenReturn(response);

        ResponseMessage<PointsDto.TripStatsResponse> result = pointsController.getMyTripStats(mockToken);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        assertEquals(10, result.getData().totalTrips);
        verify(pointsService).getTripStats(mockUserId);
    }

    @Test
    void getAllUserPoints_success() {
        when(pointsService.getAllUserPoints()).thenReturn(Collections.emptyList());

        ResponseMessage<List<PointsDto.CurrentPointsResponse>> result = pointsController.getAllUserPoints();

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(pointsService).getAllUserPoints();
    }

    @Test
    void getAllPointsHistory_success() {
        when(pointsService.getAllPointsHistory()).thenReturn(Collections.emptyList());

        ResponseMessage<List<PointsDto.PointsLogResponse>> result = pointsController.getAllPointsHistory();

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(pointsService).getAllPointsHistory();
    }

    @Test
    void getGlobalTripStats_success() {
        PointsDto.TripStatsResponse response = new PointsDto.TripStatsResponse(100, 5000);
        when(pointsService.getTripStats(null)).thenReturn(response);

        ResponseMessage<PointsDto.TripStatsResponse> result = pointsController.getGlobalTripStats();

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        assertEquals(100, result.getData().totalTrips);
        verify(pointsService).getTripStats(null);
    }

    @Test
    void getUserTripStats_success() {
        PointsDto.TripStatsResponse response = new PointsDto.TripStatsResponse(5, 200);
        when(pointsService.getTripStats("userId")).thenReturn(response);

        ResponseMessage<PointsDto.TripStatsResponse> result = pointsController.getUserTripStats("userId");

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(pointsService).getTripStats("userId");
    }

    @Test
    void adjustPointsAdmin_success() {
        when(jwtUtils.getUserIdFromToken("mock-token")).thenReturn("adminId");
        PointsDto.AdjustPointsRequest request = new PointsDto.AdjustPointsRequest();
        request.points = 100;
        request.reason = "Bonus";

        ResponseMessage<String> result = pointsController.adjustPointsAdmin(mockToken, "userId", request);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(pointsService).adjustPoints(eq("userId"), eq(100L), any(), any(), any(), any());
    }

    @Test
    void getFacultyPoints_success() {
        when(jwtUtils.getUserIdFromToken("mock-token")).thenReturn(mockUserId);
        com.example.EcoGo.dto.FacultyStatsDto.PointsResponse response = new com.example.EcoGo.dto.FacultyStatsDto.PointsResponse(
                "CS", 1000L);
        when(pointsService.getFacultyTotalPoints(mockUserId)).thenReturn(response);

        ResponseMessage<com.example.EcoGo.dto.FacultyStatsDto.PointsResponse> result = pointsController
                .getFacultyPoints(mockToken);

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        assertEquals("CS", result.getData().faculty);
        verify(pointsService).getFacultyTotalPoints(mockUserId);
    }

    @Test
    void getAdminUserBalance_success() {
        PointsDto.CurrentPointsResponse response = new PointsDto.CurrentPointsResponse("userId", 100L, 200L);
        when(pointsService.getCurrentPoints("userId")).thenReturn(response);

        ResponseMessage<PointsDto.CurrentPointsResponse> result = pointsController.getAdminUserBalance("userId");

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        assertEquals(100L, result.getData().currentPoints);
        verify(pointsService).getCurrentPoints("userId");
    }

    @Test
    void getAdminUserHistory_success() {
        when(pointsService.getPointsHistory("userId")).thenReturn(Collections.emptyList());

        ResponseMessage<List<PointsDto.PointsLogResponse>> result = pointsController.getAdminUserHistory("userId");

        assertEquals(ErrorCode.SUCCESS.getCode(), result.getCode());
        verify(pointsService).getPointsHistory("userId");
    }
}
