package com.example.EcoGo.controller;

import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.dto.TripDto;
import com.example.EcoGo.interfacemethods.TripService;
import com.example.EcoGo.model.TransportMode;
import com.example.EcoGo.model.Trip;
import com.example.EcoGo.repository.TransportModeRepository;
import com.example.EcoGo.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class TripController {

    @Autowired
    private TripService tripService;

    @Autowired
    private TransportModeRepository transportModeRepository;

    @Autowired
    private JwtUtils jwtUtils;

    // ========== Mobile Endpoints ==========

    /**
     * Start a new trip
     * POST /api/v1/mobile/trips/start
     */
    @PostMapping("/api/v1/mobile/trips/start")
    public ResponseMessage<TripDto.TripResponse> startTrip(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody TripDto.StartTripRequest request) {
        String userId = extractUserId(authHeader);
        Trip trip = tripService.startTrip(userId, request);
        // Return the created trip detail
        return ResponseMessage.success(tripService.getTripById(userId, trip.getId()));
    }

    /**
     * Complete a trip (fill end data, settle points)
     * POST /api/v1/mobile/trips/{tripId}/complete
     */
    @PostMapping("/api/v1/mobile/trips/{tripId}/complete")
    public ResponseMessage<TripDto.TripResponse> completeTrip(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String tripId,
            @RequestBody TripDto.CompleteTripRequest request) {
        String userId = extractUserId(authHeader);
        Trip trip = tripService.completeTrip(userId, tripId, request);
        return ResponseMessage.success(tripService.getTripById(userId, trip.getId()));
    }

    /**
     * Cancel a trip
     * POST /api/v1/mobile/trips/{tripId}/cancel
     */
    @PostMapping("/api/v1/mobile/trips/{tripId}/cancel")
    public ResponseMessage<String> cancelTrip(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String tripId) {
        String userId = extractUserId(authHeader);
        tripService.cancelTrip(userId, tripId);
        return ResponseMessage.success("Trip canceled");
    }

    /**
     * Get trip detail
     * GET /api/v1/mobile/trips/{tripId}
     */
    @GetMapping("/api/v1/mobile/trips/{tripId}")
    public ResponseMessage<TripDto.TripResponse> getTripDetail(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String tripId) {
        String userId = extractUserId(authHeader);
        return ResponseMessage.success(tripService.getTripById(userId, tripId));
    }

    /**
     * Get user's trip list (summaries)
     * GET /api/v1/mobile/trips
     */
    @GetMapping("/api/v1/mobile/trips")
    public ResponseMessage<List<TripDto.TripSummaryResponse>> getMyTrips(
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        return ResponseMessage.success(tripService.getUserTrips(userId));
    }

    /**
     * Get current tracking trip
     * GET /api/v1/mobile/trips/current
     */
    @GetMapping("/api/v1/mobile/trips/current")
    public ResponseMessage<TripDto.TripResponse> getCurrentTrip(
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        return ResponseMessage.success(tripService.getCurrentTrip(userId));
    }

    // ========== Web / Admin Endpoints ==========

    /**
     * Admin: Get all trips
     * GET /api/v1/web/trips/all
     */
    @GetMapping("/api/v1/web/trips/all")
    public ResponseMessage<List<TripDto.TripSummaryResponse>> getAllTrips() {
        return ResponseMessage.success(tripService.getAllTrips());
    }

    /**
     * Admin: Get trips by user (full details)
     * GET /api/v1/web/trips/user/{userid}
     */
    @GetMapping("/api/v1/web/trips/user/{userid}")
    public ResponseMessage<List<TripDto.TripResponse>> getTripsByUser(
            @PathVariable String userid) {
        return ResponseMessage.success(tripService.getTripsByUser(userid));
    }

    /**
     * Get all available transport mode strings
     * GET /api/v1/mobile/trips/transport-modes
     */
    @GetMapping("/api/v1/trips/transport-modes")
    public ResponseMessage<List<String>> getAllTransportModes() {
        List<String> modes = transportModeRepository.findAll().stream()
                .map(TransportMode::getMode)
                .collect(Collectors.toList());
        return ResponseMessage.success(modes);
    }

    /**
    * Mobile: Get my trip history (full details)
    * GET /api/v1/mobile/trips/history
    */
    @GetMapping("/api/v1/mobile/trips/history")
    public ResponseMessage<List<TripDto.TripResponse>> getMyTripHistory(
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader); // sub = userid
        return ResponseMessage.success(tripService.getTripsByUser(userId));
    }


    // ========== Helper ==========

    private String extractUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtils.getUserIdFromToken(token);
    }
}
