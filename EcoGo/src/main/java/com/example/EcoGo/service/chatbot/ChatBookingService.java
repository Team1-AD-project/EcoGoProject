package com.example.EcoGo.service.chatbot;

import com.example.EcoGo.dto.TripDto;
import com.example.EcoGo.dto.chatbot.BookingDetailDto;
import com.example.EcoGo.interfacemethods.TripService;
import com.example.EcoGo.model.ChatBooking;
import com.example.EcoGo.model.Trip;
import com.example.EcoGo.repository.ChatBookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChatBookingService {

    private static final Logger log = LoggerFactory.getLogger(ChatBookingService.class);
    private static final String DEBUG_LOG = "c:\\Users\\Cleveland\\Desktop\\AD_Mergy_version\\.cursor\\debug.log";

    // Default coordinates: NUS campus center
    private static final double DEFAULT_LNG = 103.7764;
    private static final double DEFAULT_LAT = 1.2966;

    /** Known location coordinates for NUS campus and Singapore landmarks. */
    private static final Map<String, double[]> LOCATION_COORDS = new LinkedHashMap<>();
    static {
        // NUS campus (lat, lng)
        LOCATION_COORDS.put("COM1", new double[]{1.2950, 103.7737});
        LOCATION_COORDS.put("COM2", new double[]{1.2942, 103.7743});
        LOCATION_COORDS.put("COM3", new double[]{1.2952, 103.7745});
        LOCATION_COORDS.put("PGP", new double[]{1.2916, 103.7806});
        LOCATION_COORDS.put("UTown", new double[]{1.3048, 103.7726});
        LOCATION_COORDS.put("大学城", new double[]{1.3048, 103.7726});
        LOCATION_COORDS.put("CLB", new double[]{1.2966, 103.7725});
        LOCATION_COORDS.put("中央图书馆", new double[]{1.2966, 103.7725});
        LOCATION_COORDS.put("BIZ2", new double[]{1.2935, 103.7750});
        LOCATION_COORDS.put("KR MRT", new double[]{1.2937, 103.7845});
        LOCATION_COORDS.put("肯特岗地铁站", new double[]{1.2937, 103.7845});
        LOCATION_COORDS.put("YIH", new double[]{1.2989, 103.7749});
        LOCATION_COORDS.put("TCOMS", new double[]{1.2935, 103.7772});
        LOCATION_COORDS.put("BG MRT", new double[]{1.3224, 103.8153});
        LOCATION_COORDS.put("植物园地铁站", new double[]{1.3224, 103.8153});
        // Singapore landmarks
        LOCATION_COORDS.put("乌节路", new double[]{1.3006, 103.8368});
        LOCATION_COORDS.put("Orchard", new double[]{1.3006, 103.8368});
        LOCATION_COORDS.put("滨海湾", new double[]{1.2816, 103.8636});
        LOCATION_COORDS.put("Marina Bay", new double[]{1.2816, 103.8636});
        LOCATION_COORDS.put("莱佛士坊", new double[]{1.2832, 103.8513});
        LOCATION_COORDS.put("Raffles Place", new double[]{1.2832, 103.8513});
        LOCATION_COORDS.put("牛车水", new double[]{1.2833, 103.8443});
        LOCATION_COORDS.put("Chinatown", new double[]{1.2833, 103.8443});
        LOCATION_COORDS.put("Clementi", new double[]{1.3150, 103.7649});
        LOCATION_COORDS.put("金文泰", new double[]{1.3150, 103.7649});
    }

    private final ChatBookingRepository bookingRepository;
    private final TripService tripService;

    public ChatBookingService(ChatBookingRepository bookingRepository,
                               TripService tripService) {
        this.bookingRepository = bookingRepository;
        this.tripService = tripService;
    }

    /**
     * Create a booking via chat, start a real Trip via TripService, and return a deeplink.
     */
    public BookingResult createBooking(String userId, String fromName, String toName,
                                        String departAt, int passengers) {
        String bookingId = "bk_" + UUID.randomUUID().toString().substring(0, 11);

        // #region agent log
        debugLog("ChatBookingService:createBooking:entry", "createBooking called",
                String.format("{\"userId\":\"%s\",\"fromName\":\"%s\",\"toName\":\"%s\",\"bookingId\":\"%s\"}",
                        userId, fromName, toName, bookingId), "A");
        // #endregion

        // 1. Try to create a real Trip via TripService
        String tripId = null;
        String deeplink;
        try {
            double[] fromCoords = resolveCoordinates(fromName);
            TripDto.StartTripRequest tripReq = new TripDto.StartTripRequest();
            tripReq.startLng = fromCoords[1];
            tripReq.startLat = fromCoords[0];
            tripReq.startAddress = fromName;
            tripReq.startPlaceName = fromName;
            tripReq.startCampusZone = "NUS";

            // #region agent log
            debugLog("ChatBookingService:createBooking:beforeStartTrip", "Calling tripService.startTrip",
                    String.format("{\"userId\":\"%s\",\"startPlaceName\":\"%s\"}", userId, fromName), "A");
            // #endregion

            Trip trip = tripService.startTrip(userId, tripReq);
            tripId = trip.getId();

            // #region agent log
            debugLog("ChatBookingService:createBooking:tripCreated", "Trip created successfully",
                    String.format("{\"tripId\":\"%s\",\"carbonStatus\":\"%s\"}", tripId, trip.getCarbonStatus()), "A");
            // #endregion

            deeplink = "ecogo://trip/" + tripId;
        } catch (Exception e) {
            // #region agent log
            debugLog("ChatBookingService:createBooking:tripFailed", "TripService.startTrip failed, falling back to booking-only",
                    String.format("{\"error\":\"%s\",\"userId\":\"%s\"}", e.getMessage(), userId), "A");
            // #endregion

            log.warn("[BOOKING] Failed to create Trip for user {}: {}. Falling back to booking-only deeplink.",
                    userId, e.getMessage());
            deeplink = "ecogo://booking/" + bookingId;
        }

        // 2. Save local booking record with tripId link
        ChatBooking booking = new ChatBooking();
        booking.setBookingId(bookingId);
        booking.setTripId(tripId);
        booking.setUserId(userId);
        booking.setFromName(fromName);
        booking.setToName(toName);
        booking.setDepartAt(departAt);
        booking.setPassengers(passengers);
        booking.setStatus(tripId != null ? "confirmed" : "pending");
        booking.setCreatedAt(Instant.now());
        bookingRepository.save(booking);

        // #region agent log
        debugLog("ChatBookingService:createBooking:exit", "Booking saved",
                String.format("{\"bookingId\":\"%s\",\"tripId\":\"%s\",\"deeplink\":\"%s\",\"status\":\"%s\"}",
                        bookingId, tripId, deeplink, booking.getStatus()), "A");
        // #endregion

        return new BookingResult(bookingId, tripId, deeplink);
    }

    /**
     * Resolve location name to [lat, lng] coordinates.
     * Tries exact, case-insensitive, and partial matching against known locations.
     * Falls back to NUS campus center.
     */
    private double[] resolveCoordinates(String locationName) {
        if (locationName == null || locationName.isBlank()) {
            return new double[]{DEFAULT_LAT, DEFAULT_LNG};
        }
        // Exact match
        if (LOCATION_COORDS.containsKey(locationName)) return LOCATION_COORDS.get(locationName);
        // Case-insensitive
        for (Map.Entry<String, double[]> e : LOCATION_COORDS.entrySet()) {
            if (e.getKey().equalsIgnoreCase(locationName)) return e.getValue();
        }
        // Partial match
        for (Map.Entry<String, double[]> e : LOCATION_COORDS.entrySet()) {
            if (e.getKey().toLowerCase().contains(locationName.toLowerCase())
                    || locationName.toLowerCase().contains(e.getKey().toLowerCase())) {
                return e.getValue();
            }
        }
        return new double[]{DEFAULT_LAT, DEFAULT_LNG};
    }

    /**
     * Get a single booking by bookingId.
     */
    public Optional<BookingDetailDto> getBooking(String bookingId) {
        return bookingRepository.findByBookingId(bookingId)
                .map(this::toDto);
    }

    /**
     * Get all bookings for a user, newest first.
     */
    public List<BookingDetailDto> getUserBookings(String userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Cancel a booking. Also cancels the linked Trip if one exists.
     * Returns true if cancelled, false if not found or not owned.
     */
    public boolean cancelBooking(String bookingId, String userId) {
        Optional<ChatBooking> opt = bookingRepository.findByBookingId(bookingId);
        if (opt.isEmpty()) return false;

        ChatBooking booking = opt.get();
        if (!booking.getUserId().equals(userId)) return false;
        if ("cancelled".equals(booking.getStatus())) return false;

        // Cancel linked Trip if exists
        if (booking.getTripId() != null) {
            try {
                tripService.cancelTrip(userId, booking.getTripId());
                // #region agent log
                debugLog("ChatBookingService:cancelBooking:tripCancelled", "Linked Trip cancelled",
                        String.format("{\"tripId\":\"%s\"}", booking.getTripId()), "D");
                // #endregion
            } catch (Exception e) {
                log.warn("[BOOKING] Failed to cancel linked Trip {}: {}", booking.getTripId(), e.getMessage());
            }
        }

        booking.setStatus("cancelled");
        bookingRepository.save(booking);
        return true;
    }

    private BookingDetailDto toDto(ChatBooking b) {
        return new BookingDetailDto(
                b.getBookingId(), b.getTripId(), b.getFromName(), b.getToName(),
                b.getDepartAt(), b.getPassengers(), b.getStatus(), b.getCreatedAt());
    }

    private void debugLog(String location, String message, String dataJson, String hypothesisId) {
        try (FileWriter fw = new FileWriter(DEBUG_LOG, true)) {
            fw.write(String.format("{\"location\":\"%s\",\"message\":\"%s\",\"data\":%s,\"timestamp\":%d,\"hypothesisId\":\"%s\"}\n",
                    location, message, dataJson, System.currentTimeMillis(), hypothesisId));
        } catch (Exception ignored) {}
    }

    public record BookingResult(String bookingId, String tripId, String deeplink) {}
}
