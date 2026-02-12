package com.example.EcoGo.controller;

import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.dto.TripDto;
import com.example.EcoGo.interfacemethods.TripService;
import com.example.EcoGo.model.TransportMode;
import com.example.EcoGo.model.Trip;
import com.example.EcoGo.repository.TransportModeRepository;
import com.example.EcoGo.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TripControllerTest {

    private TripService tripService;
    private TransportModeRepository transportModeRepository;
    private JwtUtils jwtUtils;
    private TripController controller;

    private static final String AUTH_HEADER = "Bearer test-jwt-token";
    private static final String USER_ID = "user001";

    @BeforeEach
    void setUp() throws Exception {
        tripService = mock(TripService.class);
        transportModeRepository = mock(TransportModeRepository.class);
        jwtUtils = mock(JwtUtils.class);
        controller = new TripController();

        injectField("tripService", tripService);
        injectField("transportModeRepository", transportModeRepository);
        injectField("jwtUtils", jwtUtils);

        when(jwtUtils.getUserIdFromToken("test-jwt-token")).thenReturn(USER_ID);
    }

    private void injectField(String fieldName, Object value) throws Exception {
        Field f = TripController.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(controller, value);
    }

    // ---------- helpers ----------

    private static Trip buildTrip(String id, String userId, String status) {
        Trip trip = new Trip();
        trip.setId(id);
        trip.setUserId(userId);
        trip.setCarbonStatus(status);
        trip.setStartTime(LocalDateTime.now());
        trip.setCreatedAt(LocalDateTime.now());
        trip.setStartPoint(new Trip.GeoPoint(116.0, 39.0));
        trip.setStartLocation(new Trip.LocationDetail("Address A", "Place A", "Zone A"));
        return trip;
    }

    private static TripDto.TripResponse buildTripResponse(String id, String userId, String status) {
        TripDto.TripResponse resp = new TripDto.TripResponse();
        resp.id = id;
        resp.userId = userId;
        resp.carbonStatus = status;
        resp.startTime = LocalDateTime.now();
        resp.createdAt = LocalDateTime.now();
        resp.distance = 2.5;
        resp.carbonSaved = 0.5;
        resp.pointsGained = 50;
        return resp;
    }

    private static TripDto.TripSummaryResponse buildSummary(String id, String userId, String status) {
        TripDto.TripSummaryResponse s = new TripDto.TripSummaryResponse();
        s.id = id;
        s.userId = userId;
        s.carbonStatus = status;
        s.distance = 2.5;
        s.carbonSaved = 0.5;
        s.pointsGained = 50;
        s.startTime = LocalDateTime.now();
        return s;
    }

    // ========== startTrip ==========

    @Test
    void startTrip_success() {
        Trip trip = buildTrip("trip1", USER_ID, "tracking");
        TripDto.TripResponse tripResp = buildTripResponse("trip1", USER_ID, "tracking");

        when(tripService.startTrip(eq(USER_ID), any(TripDto.StartTripRequest.class))).thenReturn(trip);
        when(tripService.getTripById(USER_ID, "trip1")).thenReturn(tripResp);

        TripDto.StartTripRequest request = new TripDto.StartTripRequest();
        request.startLng = 116.0;
        request.startLat = 39.0;
        request.startAddress = "Address A";
        request.startPlaceName = "Place A";
        request.startCampusZone = "Zone A";

        ResponseMessage<TripDto.TripResponse> resp = controller.startTrip(AUTH_HEADER, request);

        assertEquals(200, resp.getCode());
        assertEquals("trip1", resp.getData().id);
        assertEquals("tracking", resp.getData().carbonStatus);
        verify(tripService).startTrip(eq(USER_ID), any(TripDto.StartTripRequest.class));
    }

    // ========== completeTrip ==========

    @Test
    void completeTrip_success() {
        Trip trip = buildTrip("trip1", USER_ID, "completed");
        TripDto.TripResponse tripResp = buildTripResponse("trip1", USER_ID, "completed");

        when(tripService.completeTrip(eq(USER_ID), eq("trip1"), any(TripDto.CompleteTripRequest.class))).thenReturn(trip);
        when(tripService.getTripById(USER_ID, "trip1")).thenReturn(tripResp);

        TripDto.CompleteTripRequest request = new TripDto.CompleteTripRequest();
        request.endLng = 116.1;
        request.endLat = 39.1;
        request.endAddress = "Address B";
        request.endPlaceName = "Place B";
        request.distance = 2.5;
        request.isGreenTrip = true;

        ResponseMessage<TripDto.TripResponse> resp = controller.completeTrip(AUTH_HEADER, "trip1", request);

        assertEquals(200, resp.getCode());
        assertEquals("trip1", resp.getData().id);
        assertEquals("completed", resp.getData().carbonStatus);
    }

    // ========== cancelTrip ==========

    @Test
    void cancelTrip_success() {
        doNothing().when(tripService).cancelTrip(USER_ID, "trip1");

        ResponseMessage<String> resp = controller.cancelTrip(AUTH_HEADER, "trip1");

        assertEquals(200, resp.getCode());
        assertEquals("Trip canceled", resp.getData());
        verify(tripService).cancelTrip(USER_ID, "trip1");
    }

    // ========== getTripDetail ==========

    @Test
    void getTripDetail_success() {
        TripDto.TripResponse tripResp = buildTripResponse("trip1", USER_ID, "completed");
        when(tripService.getTripById(USER_ID, "trip1")).thenReturn(tripResp);

        ResponseMessage<TripDto.TripResponse> resp = controller.getTripDetail(AUTH_HEADER, "trip1");

        assertEquals(200, resp.getCode());
        assertEquals("trip1", resp.getData().id);
    }

    // ========== getMyTrips ==========

    @Test
    void getMyTrips_success() {
        TripDto.TripSummaryResponse s1 = buildSummary("trip1", USER_ID, "completed");
        TripDto.TripSummaryResponse s2 = buildSummary("trip2", USER_ID, "tracking");
        when(tripService.getUserTrips(USER_ID)).thenReturn(List.of(s1, s2));

        ResponseMessage<List<TripDto.TripSummaryResponse>> resp = controller.getMyTrips(AUTH_HEADER);

        assertEquals(200, resp.getCode());
        assertEquals(2, resp.getData().size());
    }

    @Test
    void getMyTrips_empty() {
        when(tripService.getUserTrips(USER_ID)).thenReturn(List.of());

        ResponseMessage<List<TripDto.TripSummaryResponse>> resp = controller.getMyTrips(AUTH_HEADER);

        assertEquals(200, resp.getCode());
        assertTrue(resp.getData().isEmpty());
    }

    // ========== getCurrentTrip ==========

    @Test
    void getCurrentTrip_exists() {
        TripDto.TripResponse tripResp = buildTripResponse("trip1", USER_ID, "tracking");
        when(tripService.getCurrentTrip(USER_ID)).thenReturn(tripResp);

        ResponseMessage<TripDto.TripResponse> resp = controller.getCurrentTrip(AUTH_HEADER);

        assertEquals(200, resp.getCode());
        assertEquals("trip1", resp.getData().id);
        assertEquals("tracking", resp.getData().carbonStatus);
    }

    @Test
    void getCurrentTrip_none() {
        when(tripService.getCurrentTrip(USER_ID)).thenReturn(null);

        ResponseMessage<TripDto.TripResponse> resp = controller.getCurrentTrip(AUTH_HEADER);

        assertEquals(200, resp.getCode());
        assertNull(resp.getData());
    }

    // ========== getAllTrips (admin) ==========

    @Test
    void getAllTrips_success() {
        TripDto.TripSummaryResponse s1 = buildSummary("trip1", "userA", "completed");
        TripDto.TripSummaryResponse s2 = buildSummary("trip2", "userB", "completed");
        when(tripService.getAllTrips()).thenReturn(List.of(s1, s2));

        ResponseMessage<List<TripDto.TripSummaryResponse>> resp = controller.getAllTrips();

        assertEquals(200, resp.getCode());
        assertEquals(2, resp.getData().size());
    }

    @Test
    void getAllTrips_empty() {
        when(tripService.getAllTrips()).thenReturn(List.of());

        ResponseMessage<List<TripDto.TripSummaryResponse>> resp = controller.getAllTrips();

        assertEquals(200, resp.getCode());
        assertTrue(resp.getData().isEmpty());
    }

    // ========== getTripsByUser (admin) ==========

    @Test
    void getTripsByUser_success() {
        TripDto.TripResponse r1 = buildTripResponse("trip1", "userA", "completed");
        when(tripService.getTripsByUser("userA")).thenReturn(List.of(r1));

        ResponseMessage<List<TripDto.TripResponse>> resp = controller.getTripsByUser("userA");

        assertEquals(200, resp.getCode());
        assertEquals(1, resp.getData().size());
    }

    @Test
    void getTripsByUser_empty() {
        when(tripService.getTripsByUser("userX")).thenReturn(List.of());

        ResponseMessage<List<TripDto.TripResponse>> resp = controller.getTripsByUser("userX");

        assertEquals(200, resp.getCode());
        assertTrue(resp.getData().isEmpty());
    }

    // ========== getAllTransportModes ==========

    @Test
    void getAllTransportModes_success() {
        TransportMode m1 = new TransportMode("1", "walk", "Walking", 0.2, "icon1", 1, true);
        TransportMode m2 = new TransportMode("2", "bike", "Cycling", 0.1, "icon2", 2, true);
        when(transportModeRepository.findAll()).thenReturn(List.of(m1, m2));

        ResponseMessage<List<String>> resp = controller.getAllTransportModes();

        assertEquals(200, resp.getCode());
        assertEquals(2, resp.getData().size());
        assertTrue(resp.getData().contains("walk"));
        assertTrue(resp.getData().contains("bike"));
    }

    @Test
    void getAllTransportModes_empty() {
        when(transportModeRepository.findAll()).thenReturn(List.of());

        ResponseMessage<List<String>> resp = controller.getAllTransportModes();

        assertEquals(200, resp.getCode());
        assertTrue(resp.getData().isEmpty());
    }

    // ========== getMyTripHistory ==========

    @Test
    void getMyTripHistory_success() {
        TripDto.TripResponse r1 = buildTripResponse("trip1", USER_ID, "completed");
        TripDto.TripResponse r2 = buildTripResponse("trip2", USER_ID, "completed");
        when(tripService.getTripsByUser(USER_ID)).thenReturn(List.of(r1, r2));

        ResponseMessage<List<TripDto.TripResponse>> resp = controller.getMyTripHistory(AUTH_HEADER);

        assertEquals(200, resp.getCode());
        assertEquals(2, resp.getData().size());
        verify(tripService).getTripsByUser(USER_ID);
    }

    @Test
    void getMyTripHistory_empty() {
        when(tripService.getTripsByUser(USER_ID)).thenReturn(List.of());

        ResponseMessage<List<TripDto.TripResponse>> resp = controller.getMyTripHistory(AUTH_HEADER);

        assertEquals(200, resp.getCode());
        assertTrue(resp.getData().isEmpty());
    }
}
