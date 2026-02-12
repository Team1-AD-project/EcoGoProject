package com.example.EcoGo.service;

import com.example.EcoGo.dto.PointsDto;
import com.example.EcoGo.dto.TripDto;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.interfacemethods.PointsService;
import com.example.EcoGo.interfacemethods.TripService;
import com.example.EcoGo.interfacemethods.VipSwitchService;
import com.example.EcoGo.model.TransportMode;
import com.example.EcoGo.model.Trip;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.TransportModeRepository;
import com.example.EcoGo.repository.TripRepository;
import com.example.EcoGo.repository.UserRepository;
import com.example.EcoGo.utils.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class TripServiceImpl implements TripService {

    private static final Logger log = LoggerFactory.getLogger(TripServiceImpl.class);

    private static final String STATUS_TRACKING = "tracking";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_CANCELED = "canceled";
    private static final String VIP_SWITCH_DOUBLE_POINTS = "Double_points";

    @Autowired
    private VipSwitchService vipSwitchService;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TransportModeRepository transportModeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointsService pointsService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Trip startTrip(String userId, TripDto.StartTripRequest request) {
        // Verify user exists
        userRepository.findByUserid(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Trip trip = new Trip();
        trip.setId(UUID.randomUUID().toString());
        trip.setUserId(userId);
        trip.setStartPoint(new Trip.GeoPoint(request.startLng, request.startLat));
        trip.setStartLocation(new Trip.LocationDetail(
                request.startAddress, request.startPlaceName, request.startCampusZone));
        trip.setStartTime(LocalDateTime.now());
        trip.setCarbonStatus(STATUS_TRACKING);
        trip.setCreatedAt(LocalDateTime.now());

        return tripRepository.save(trip);
    }

    /**
     * SonarQube Cognitive Complexity fix: split completeTrip into small helpers.
     * (Problem line around "Convert transport segments and calculate carbonSaved")
     */
    @Override
    public Trip completeTrip(String userId, String tripId, TripDto.CompleteTripRequest request) {
        Trip trip = loadTripOrThrow(tripId);
        validateTripOwnershipAndStatus(trip, userId);

        fillTripEndData(trip, request);

        double carbonSaved = calculateAndSetSegmentsAndCarbon(trip, request);

        setPolylinePointsIfPresent(trip, request);

        long pointsGained = settleTripPoints(userId, trip, request, carbonSaved);

        trip.setPointsGained(pointsGained);
        trip.setCarbonStatus(STATUS_COMPLETED);

        updateUserTotalCarbonIfNeeded(userId, carbonSaved);

        return tripRepository.save(trip);
    }

    // =========================
    // completeTrip helpers
    // =========================
    private Trip loadTripOrThrow(String tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
    }

    private void validateTripOwnershipAndStatus(Trip trip, String userId) {
        if (!trip.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }
        if (!STATUS_TRACKING.equals(trip.getCarbonStatus())) {
            throw new BusinessException(ErrorCode.TRIP_STATUS_ERROR, trip.getCarbonStatus());
        }
    }

    private void fillTripEndData(Trip trip, TripDto.CompleteTripRequest request) {
        trip.setEndPoint(new Trip.GeoPoint(request.endLng, request.endLat));
        trip.setEndLocation(new Trip.LocationDetail(
                request.endAddress, request.endPlaceName, request.endCampusZone));
        trip.setEndTime(LocalDateTime.now());
        trip.setDistance(request.distance);
        trip.setDetectedMode(request.detectedMode);
        trip.setMlConfidence(request.mlConfidence);
        trip.setGreenTrip(request.isGreenTrip);
    }

    /**
     * Convert segments, set them into trip, and compute carbonSaved (kg, 2
     * decimals).
     */
    private double calculateAndSetSegmentsAndCarbon(Trip trip, TripDto.CompleteTripRequest request) {
        double carbonSaved = calculateCarbonSavedFromSegments(request);
        trip.setCarbonSaved(carbonSaved);

        // Set segments on trip (keep original behavior)
        if (request.transportModes != null) {
            List<Trip.TransportSegment> segments = request.transportModes.stream()
                    .map(s -> new Trip.TransportSegment(s.mode, s.subDistance, s.subDuration))
                    .collect(Collectors.toList());
            trip.setTransportModes(segments);
        }
        return carbonSaved;
    }

    /**
     * carbonSaved = Σ((carCarbon - modeCarbonFactor) × subDistance) for each
     * segment
     * Convert g -> kg-like display unit (/100 in your original code) and round to 2
     * decimals.
     */
    private double calculateCarbonSavedFromSegments(TripDto.CompleteTripRequest request) {
        double carCarbon = 100.0; // g/km benchmark
        double carbonSavedG = 0.0;

        if (request.transportModes == null) {
            return 0.0;
        }

        for (TripDto.TransportSegmentDto seg : request.transportModes) {
            double savingPerKm = calculateSavingPerKm(seg, carCarbon);
            if (savingPerKm > 0) {
                carbonSavedG += savingPerKm * seg.subDistance;
            }
        }

        double display = carbonSavedG / 100.0;
        return round2(display);
    }

    private double calculateSavingPerKm(TripDto.TransportSegmentDto seg, double carCarbon) {
        TransportMode mode = transportModeRepository.findByMode(seg.mode).orElse(null);
        if (mode == null)
            return 0.0;
        return carCarbon - mode.getCarbonFactor();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private void setPolylinePointsIfPresent(Trip trip, TripDto.CompleteTripRequest request) {
        if (request.polylinePoints == null)
            return;

        List<Trip.GeoPoint> points = request.polylinePoints.stream()
                .map(p -> new Trip.GeoPoint(p.lng, p.lat))
                .collect(Collectors.toList());
        trip.setPolylinePoints(points);
    }

    /**
     * Points = round(carbonSaved * 100), VIP double if switch enabled, and then
     * settle().
     * Returns pointsGained.
     */
    private long settleTripPoints(String userId, Trip trip, TripDto.CompleteTripRequest request, double carbonSaved) {
        long basePoints = Math.round(carbonSaved * 100);

        User user = userRepository.findByUserid(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean isVip = user.getVip() != null && user.getVip().isActive();
        boolean isEnabled = vipSwitchService.isSwitchEnabled(VIP_SWITCH_DOUBLE_POINTS);

        long pointsGained = (isVip && isEnabled) ? basePoints * 2 : basePoints;

        String description = pointsService.formatTripDescription(
                trip.getStartLocation() != null ? trip.getStartLocation().getPlaceName() : null,
                request.endPlaceName,
                request.distance);

        PointsDto.SettleResult settleResult = new PointsDto.SettleResult();
        settleResult.points = pointsGained;
        settleResult.source = "trip";
        settleResult.description = description;
        settleResult.relatedId = trip.getId();

        pointsService.settle(userId, settleResult);
        return pointsGained;
    }

    private void updateUserTotalCarbonIfNeeded(String userId, double carbonSaved) {
        if (carbonSaved <= 0)
            return;

        User user = userRepository.findByUserid(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        double newTotal = user.getTotalCarbon() + carbonSaved / 10.0;
        user.setTotalCarbon(round2(newTotal));
        userRepository.save(user);
    }

    @Override
    public void cancelTrip(String userId, String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));

        if (!trip.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }
        if (!STATUS_TRACKING.equals(trip.getCarbonStatus())) {
            throw new BusinessException(ErrorCode.TRIP_STATUS_ERROR, trip.getCarbonStatus());
        }

        trip.setCarbonStatus(STATUS_CANCELED);
        trip.setEndTime(LocalDateTime.now());
        tripRepository.save(trip);
    }

    @Override
    public TripDto.TripResponse getTripById(String userId, String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));

        if (!trip.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        return convertToResponse(trip);
    }

    @Override
    public List<TripDto.TripSummaryResponse> getUserTrips(String userId) {
        List<Trip> trips = tripRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return trips.stream().map(this::convertToSummary).collect(Collectors.toList());
    }

    @Override
    public TripDto.TripResponse getCurrentTrip(String userId) {
        List<Trip> trackingTrips = tripRepository.findByUserIdAndCarbonStatus(userId, STATUS_TRACKING);
        if (trackingTrips.isEmpty()) {
            return null;
        }
        return convertToResponse(trackingTrips.get(0));
    }

    @Override
    public List<TripDto.TripSummaryResponse> getAllTrips() {
        List<Document> rawDocs;
        try {
            rawDocs = mongoTemplate.findAll(Document.class, "trips");
        } catch (Exception e) {
            log.error("[getAllTrips] Failed to fetch trips from DB: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
        log.info("[getAllTrips] Found {} trip documents in DB", rawDocs.size());
        List<TripDto.TripSummaryResponse> result = new ArrayList<>();
        int failCount = 0;
        for (Document doc : rawDocs) {
            try {
                Trip trip = mongoTemplate.getConverter().read(Trip.class, doc);
                result.add(convertToSummary(trip));
            } catch (Exception e) {
                failCount++;
                log.error("[getAllTrips] Skipping corrupted trip _id={}: {}", doc.get("_id"), e.getMessage());
            }
        }
        if (failCount > 0) {
            log.warn("[getAllTrips] Skipped {} corrupted trip records out of {}", failCount, rawDocs.size());
        }
        return result;
    }

    @Override
    public List<TripDto.TripResponse> getTripsByUser(String userId) {
        Query query = new Query(Criteria.where("user_id").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "created_at"));
        List<Document> rawDocs;
        try {
            rawDocs = mongoTemplate.find(query, Document.class, "trips");
        } catch (Exception e) {
            log.error("[getTripsByUser] Failed to fetch trips for userId={}: {}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
        log.info("[getTripsByUser] Found {} trips for userId={}", rawDocs.size(), LogSanitizer.sanitize(userId));
        List<TripDto.TripResponse> result = new ArrayList<>();
        int failCount = 0;
        for (Document doc : rawDocs) {
            try {
                Trip trip = mongoTemplate.getConverter().read(Trip.class, doc);
                result.add(convertToResponse(trip));
            } catch (Exception e) {
                failCount++;
                log.error("[getTripsByUser] Skipping corrupted trip _id={} for userId={}: {}",
                        doc.get("_id"), userId, e.getMessage());
            }
        }
        if (failCount > 0) {
            log.warn("[getTripsByUser] Skipped {} corrupted trips out of {} for userId={}",
                    failCount, rawDocs.size(), LogSanitizer.sanitize(userId));
        }
        return result;
    }

    // --- Converters ---

    private TripDto.TripResponse convertToResponse(Trip trip) {
        TripDto.TripResponse resp = new TripDto.TripResponse();
        resp.id = trip.getId();
        resp.userId = trip.getUserId();
        resp.startTime = trip.getStartTime();
        resp.endTime = trip.getEndTime();
        resp.detectedMode = trip.getDetectedMode();
        resp.mlConfidence = trip.getMlConfidence();
        resp.isGreenTrip = trip.isGreenTrip();
        resp.distance = trip.getDistance(); // km
        resp.carbonSaved = trip.getCarbonSaved(); // already in kg
        resp.pointsGained = trip.getPointsGained();
        resp.carbonStatus = trip.getCarbonStatus();
        resp.createdAt = trip.getCreatedAt();

        if (trip.getStartPoint() != null) {
            TripDto.GeoPointDto sp = new TripDto.GeoPointDto();
            sp.lng = trip.getStartPoint().getLng();
            sp.lat = trip.getStartPoint().getLat();
            resp.startPoint = sp;
        }
        if (trip.getEndPoint() != null) {
            TripDto.GeoPointDto ep = new TripDto.GeoPointDto();
            ep.lng = trip.getEndPoint().getLng();
            ep.lat = trip.getEndPoint().getLat();
            resp.endPoint = ep;
        }
        if (trip.getStartLocation() != null) {
            TripDto.LocationDetailDto sl = new TripDto.LocationDetailDto();
            sl.address = trip.getStartLocation().getAddress();
            sl.placeName = trip.getStartLocation().getPlaceName();
            sl.campusZone = trip.getStartLocation().getCampusZone();
            resp.startLocation = sl;
        }
        if (trip.getEndLocation() != null) {
            TripDto.LocationDetailDto el = new TripDto.LocationDetailDto();
            el.address = trip.getEndLocation().getAddress();
            el.placeName = trip.getEndLocation().getPlaceName();
            el.campusZone = trip.getEndLocation().getCampusZone();
            resp.endLocation = el;
        }
        if (trip.getTransportModes() != null) {
            resp.transportModes = trip.getTransportModes().stream().map(s -> {
                TripDto.TransportSegmentDto dto = new TripDto.TransportSegmentDto();
                dto.mode = s.getMode();
                dto.subDistance = s.getSubDistance();
                dto.subDuration = s.getSubDuration();
                return dto;
            }).collect(Collectors.toList());
        }
        if (trip.getPolylinePoints() != null) {
            resp.polylinePoints = trip.getPolylinePoints().stream().map(p -> {
                TripDto.GeoPointDto dto = new TripDto.GeoPointDto();
                dto.lng = p.getLng();
                dto.lat = p.getLat();
                return dto;
            }).collect(Collectors.toList());
        }

        return resp;
    }

    private TripDto.TripSummaryResponse convertToSummary(Trip trip) {
        TripDto.TripSummaryResponse resp = new TripDto.TripSummaryResponse();
        resp.id = trip.getId();
        resp.userId = trip.getUserId();
        resp.detectedMode = trip.getDetectedMode();
        resp.distance = trip.getDistance(); // km
        resp.carbonSaved = trip.getCarbonSaved(); // already in kg
        resp.pointsGained = trip.getPointsGained();
        resp.isGreenTrip = trip.isGreenTrip();
        resp.carbonStatus = trip.getCarbonStatus();
        resp.startTime = trip.getStartTime();
        resp.endTime = trip.getEndTime();

        if (trip.getStartPoint() != null) {
            TripDto.GeoPointDto sp = new TripDto.GeoPointDto();
            sp.lng = trip.getStartPoint().getLng();
            sp.lat = trip.getStartPoint().getLat();
            resp.startPoint = sp;
        }
        if (trip.getEndPoint() != null) {
            TripDto.GeoPointDto ep = new TripDto.GeoPointDto();
            ep.lng = trip.getEndPoint().getLng();
            ep.lat = trip.getEndPoint().getLat();
            resp.endPoint = ep;
        }

        if (trip.getStartLocation() != null) {
            resp.startPlaceName = trip.getStartLocation().getPlaceName();
        }
        if (trip.getEndLocation() != null) {
            resp.endPlaceName = trip.getEndLocation().getPlaceName();
        }

        return resp;
    }
}
