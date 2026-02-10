package com.example.EcoGo.service;

import com.example.EcoGo.dto.PointsDto;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.BadgeService;
import com.example.EcoGo.model.TransportMode;
import com.example.EcoGo.model.User;
import com.example.EcoGo.model.UserPointsLog;
import com.example.EcoGo.repository.TransportModeRepository;
import com.example.EcoGo.repository.UserPointsLogRepository;
import com.example.EcoGo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPointsLogRepository pointsLogRepository;

    @Mock
    private TransportModeRepository transportModeRepository;

    @Mock
    private BadgeService badgeService;

    @InjectMocks
    private PointsServiceImpl pointsService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserid("testUser");
        mockUser.setCurrentPoints(100L);
        mockUser.setTotalPoints(100L);
        mockUser.setTotalCarbon(10.0);
    }

    @Test
    void adjustPoints_add_success() {
        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(pointsLogRepository.save(any(UserPointsLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserPointsLog log = pointsService.adjustPoints("testUser", 50, "trip", "Trip points", null, null);

        assertEquals(150L, mockUser.getCurrentPoints());
        assertEquals(150L, mockUser.getTotalPoints()); // Trip adds to total
        assertEquals(50, log.getPoints());
        verify(userRepository).save(mockUser);
    }

    @Test
    void adjustPoints_deduct_success() {
        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(pointsLogRepository.save(any(UserPointsLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserPointsLog log = pointsService.adjustPoints("testUser", -50, "redeem", "Redeem", null, null);

        assertEquals(50L, mockUser.getCurrentPoints());
        assertEquals(100L, mockUser.getTotalPoints()); // Redeem doesn't change total points
        assertEquals(-50, log.getPoints());
    }

    @Test
    void adjustPoints_insufficientFunds() {
        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));

        assertThrows(BusinessException.class,
                () -> pointsService.adjustPoints("testUser", -200, "redeem", "Redeem", null, null));
    }

    @Test
    void getCurrentPoints_success() {
        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));

        PointsDto.CurrentPointsResponse response = pointsService.getCurrentPoints("testUser");

        assertEquals(100L, response.currentPoints);
        assertEquals(100L, response.totalPoints);
    }

    @Test
    void calculatePoints_success() {
        TransportMode mode = new TransportMode();
        mode.setMode("walk");
        mode.setCarbonFactor(0.0);

        when(transportModeRepository.findByMode("walk")).thenReturn(Optional.of(mode));

        // car (100) - walk (0) = 100g/km. 5km * 100 = 500g. 500 * 10 = 5000 points.
        long points = pointsService.calculatePoints("walk", 5.0);

        assertEquals(5000L, points);
    }

    @Test
    void settleTrip_success() {
        PointsDto.SettleTripRequest request = new PointsDto.SettleTripRequest();
        request.tripId = "trip1";
        request.detectedMode = "walk";
        request.distance = 2.0;
        request.carbonSaved = 200;
        request.isGreenTrip = true;

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        TransportMode mode = new TransportMode();
        mode.setMode("walk");
        mode.setCarbonFactor(0.0);
        when(transportModeRepository.findByMode("walk")).thenReturn(Optional.of(mode));
        when(pointsLogRepository.save(any(UserPointsLog.class))).thenAnswer(i -> i.getArgument(0));

        pointsService.settleTrip("testUser", request);

        // assertNotNull(result); // method is void
        // assertEquals(2000L, result.points); // cannot check return value
        verify(userRepository, times(2)).save(mockUser);
    }

    @Test
    void getFacultyTotalPoints_success() {
        mockUser.setFaculty("CS");
        User user2 = new User();
        user2.setFaculty("CS");
        user2.setTotalPoints(500L);

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(userRepository.findByFaculty("CS")).thenReturn(java.util.Arrays.asList(mockUser, user2));

        com.example.EcoGo.dto.FacultyStatsDto.PointsResponse response = pointsService.getFacultyTotalPoints("testUser");

        assertEquals("CS", response.faculty);
        assertEquals(600L, response.totalPoints); // 100 + 500
    }

    @Test
    void getPointsHistory_success() {
        UserPointsLog log1 = new UserPointsLog();
        log1.setId("log1");
        log1.setPoints(10L);
        log1.setCreatedAt(java.time.LocalDateTime.now());

        when(pointsLogRepository.findByUserIdOrderByCreatedAtDesc("testUser"))
                .thenReturn(java.util.Arrays.asList(log1));

        java.util.List<PointsDto.PointsLogResponse> response = pointsService.getPointsHistory("testUser");

        assertEquals(1, response.size());
        assertEquals("log1", response.get(0).id);
    }

    @Test
    void getAllPointsHistory_success() {
        UserPointsLog log1 = new UserPointsLog();
        log1.setId("log1");
        log1.setPoints(10L);
        log1.setCreatedAt(java.time.LocalDateTime.now());

        when(pointsLogRepository.findAll()).thenReturn(java.util.Arrays.asList(log1));

        java.util.List<PointsDto.PointsLogResponse> response = pointsService.getAllPointsHistory();

        assertEquals(1, response.size());
        assertEquals("log1", response.get(0).id);
    }

    @Test
    void getTripStats_user_success() {
        User.Stats stats = new User.Stats();
        stats.setTotalTrips(5);
        stats.setTotalPointsFromTrips(500L);
        mockUser.setStats(stats);

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));

        PointsDto.TripStatsResponse response = pointsService.getTripStats("testUser");

        assertEquals(5, response.totalTrips);
        assertEquals(500L, response.totalPointsEarned);
    }

    @Test
    void getTripStats_global_success() {
        UserPointsLog log1 = new UserPointsLog();
        log1.setPoints(100L);
        UserPointsLog log2 = new UserPointsLog();
        log2.setPoints(50L);

        when(pointsLogRepository.findBySource("trip")).thenReturn(java.util.Arrays.asList(log1, log2));

        PointsDto.TripStatsResponse response = pointsService.getTripStats(null);

        assertEquals(2, response.totalTrips);
        assertEquals(150L, response.totalPointsEarned);
    }

    @Test
    void redeemPoints_success() {
        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(pointsLogRepository.save(any(UserPointsLog.class))).thenAnswer(i -> i.getArgument(0));

        pointsService.redeemPoints("testUser", "order123", 50L);

        assertEquals(50L, mockUser.getCurrentPoints());
        verify(userRepository).save(mockUser);
    }

    @Test
    void settle_success() {
        PointsDto.SettleResult result = new PointsDto.SettleResult();
        result.points = 100L;
        result.source = "bonus";
        result.description = "Bonus points";

        when(userRepository.findByUserid("testUser")).thenReturn(Optional.of(mockUser));
        when(pointsLogRepository.save(any(UserPointsLog.class))).thenAnswer(i -> i.getArgument(0));

        pointsService.settle("testUser", result);

        assertEquals(200L, mockUser.getCurrentPoints());
        verify(userRepository).save(mockUser);
    }

    @Test
    void getAllUserPoints_success() {
        User user1 = new User();
        user1.setUserid("user1");
        user1.setCurrentPoints(100L);
        user1.setTotalPoints(200L);

        when(userRepository.findAll()).thenReturn(java.util.Arrays.asList(user1));

        java.util.List<PointsDto.CurrentPointsResponse> response = pointsService.getAllUserPoints();

        assertEquals(1, response.size());
        assertEquals("user1", response.get(0).userId);
    }

    @Test
    void formatTripDescription_strings_success() {
        String desc = pointsService.formatTripDescription("A", "B", 10.5);
        assertEquals("A -> B (10.5km)", desc);

        String descDefault = pointsService.formatTripDescription(null, "", 5.0);
        assertEquals("Unknown Start -> Unknown Destination (5.0km)", descDefault);
    }

    @Test
    void formatTripDescription_objects_success() {
        PointsDto.LocationInfo start = new PointsDto.LocationInfo();
        start.placeName = "Start";
        PointsDto.LocationInfo end = new PointsDto.LocationInfo();
        end.placeName = "End";

        String desc = pointsService.formatTripDescription(start, end, 2.0);
        assertEquals("Start -> End (2.0km)", desc);

        String descNull = pointsService.formatTripDescription((PointsDto.LocationInfo) null,
                (PointsDto.LocationInfo) null, 1.0);
        assertEquals("Unknown Start -> Unknown Destination (1.0km)", descNull);
    }

    @Test
    void formatBadgeDescription_success() {
        assertEquals("Purchased Badge: Gold", pointsService.formatBadgeDescription("Gold"));
        assertEquals("Purchased Badge: Unknown Badge", pointsService.formatBadgeDescription(null));
    }

    @Test
    void getFacultyTotalPoints_emptyFaculty() {
        User noFacultyUser = new User();
        noFacultyUser.setUserid("noFac");
        // noFacultyUser.setIsAdmin(false); // Method not found, assuming default or use
        // available setter
        noFacultyUser.setFaculty(null);
        when(userRepository.findByUserid("noFac")).thenReturn(java.util.Optional.of(noFacultyUser));

        com.example.EcoGo.dto.FacultyStatsDto.PointsResponse response = pointsService.getFacultyTotalPoints("noFac");
        assertEquals("", response.faculty);
        assertEquals(0L, response.totalPoints);
    }

    @Test
    void getAllPointsHistory_withAdminAction() {
        UserPointsLog log = new UserPointsLog();
        log.setId("log1");
        log.setPoints(10L);
        log.setCreatedAt(java.time.LocalDateTime.now());
        UserPointsLog.AdminAction action = new UserPointsLog.AdminAction("admin1", "reason", "APPROVED");
        log.setAdminAction(action);

        when(pointsLogRepository.findAll()).thenReturn(java.util.Arrays.asList(log));

        java.util.List<PointsDto.PointsLogResponse> result = pointsService.getAllPointsHistory();
        assertEquals(1, result.size());
        assertEquals("admin1", result.get(0).admin_action.operator_id);
    }

    @Test
    void adjustPoints_userNotFound() {
        when(userRepository.findByUserid("unknownUser")).thenReturn(java.util.Optional.empty());

        assertThrows(BusinessException.class,
                () -> pointsService.adjustPoints("unknownUser", 100, "trip", "test", null, null));
    }

    @Test
    void getCurrentPoints_userNotFound() {
        when(userRepository.findByUserid("unknownUser")).thenReturn(java.util.Optional.empty());

        assertThrows(BusinessException.class, () -> pointsService.getCurrentPoints("unknownUser"));
    }

    @Test
    void calculatePoints_invalidMode() {
        when(transportModeRepository.findByMode("rocket")).thenReturn(java.util.Optional.empty());

        assertThrows(BusinessException.class, () -> pointsService.calculatePoints("rocket", 10.0));
    }

    @Test
    void getTripStats_userNotFound() {
        when(userRepository.findByUserid("unknownUser")).thenReturn(java.util.Optional.empty());

        assertThrows(BusinessException.class, () -> pointsService.getTripStats("unknownUser"));
    }

    @Test
    void settleTrip_userNotFound() {
        PointsDto.SettleTripRequest request = new PointsDto.SettleTripRequest();
        request.detectedMode = "walk";
        request.distance = 1.0;

        // Mock getMode for the internal calculatePoints call
        TransportMode mode = new TransportMode();
        mode.setMode("walk");
        when(transportModeRepository.findByMode("walk")).thenReturn(java.util.Optional.of(mode));

        // Mock user not found for the actual settle part
        when(userRepository.findByUserid("unknownUser")).thenReturn(java.util.Optional.empty());

        assertThrows(BusinessException.class, () -> pointsService.settleTrip("unknownUser", request));
    }

    @Test
    void settleTrip_userNotFound_duringStatsUpdate() {
        PointsDto.SettleTripRequest request = new PointsDto.SettleTripRequest();
        request.detectedMode = "walk";
        request.distance = 1.0;

        TransportMode mode = new TransportMode();
        mode.setMode("walk");
        when(transportModeRepository.findByMode("walk")).thenReturn(java.util.Optional.of(mode));

        // First call (adjustPoints) -> Found, Second call (updateStats) -> Not Found
        when(userRepository.findByUserid("testUser"))
                .thenReturn(java.util.Optional.of(mockUser))
                .thenReturn(java.util.Optional.empty());

        // Mock save for adjustPoints
        when(pointsLogRepository.save(any(UserPointsLog.class))).thenAnswer(i -> i.getArgument(0));

        assertThrows(BusinessException.class, () -> pointsService.settleTrip("testUser", request));
    }

    @Test
    void getFacultyTotalPoints_userNotFound() {
        when(userRepository.findByUserid("unknownUser")).thenReturn(java.util.Optional.empty());

        assertThrows(BusinessException.class, () -> pointsService.getFacultyTotalPoints("unknownUser"));
    }
}
