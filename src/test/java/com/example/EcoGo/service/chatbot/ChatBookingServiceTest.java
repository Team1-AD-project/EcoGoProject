package com.example.EcoGo.service.chatbot;

import com.example.EcoGo.dto.TripDto;
import com.example.EcoGo.dto.chatbot.BookingDetailDto;
import com.example.EcoGo.interfacemethods.TripService;
import com.example.EcoGo.model.ChatBooking;
import com.example.EcoGo.model.Trip;
import com.example.EcoGo.repository.ChatBookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatBookingServiceTest {

    @Mock private ChatBookingRepository bookingRepository;
    @Mock private TripService tripService;

    @InjectMocks private ChatBookingService chatBookingService;

    @BeforeEach
    void setUp() {
        lenient().when(bookingRepository.save(any(ChatBooking.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- createBooking ----------
    @Test
    void createBooking_tripServiceSuccess_shouldReturnConfirmedBooking() {
        Trip trip = new Trip();
        trip.setId("trip_001");
        trip.setCarbonStatus("low");
        when(tripService.startTrip(eq("u_001"), any(TripDto.StartTripRequest.class))).thenReturn(trip);

        ChatBookingService.BookingResult result = chatBookingService.createBooking(
                "u_001", "COM3", "UTown", "2026-02-11T10:00:00", 2);

        assertNotNull(result);
        assertNotNull(result.bookingId());
        assertTrue(result.bookingId().startsWith("bk_"));
        assertEquals("trip_001", result.tripId());
        assertTrue(result.deeplink().contains("trip/trip_001"));

        verify(bookingRepository).save(argThat(b ->
                "confirmed".equals(b.getStatus()) && b.getTripId().equals("trip_001")));
    }

    @Test
    void createBooking_tripServiceFails_shouldFallbackToPendingBooking() {
        when(tripService.startTrip(eq("u_001"), any(TripDto.StartTripRequest.class)))
                .thenThrow(new RuntimeException("Trip service down"));

        ChatBookingService.BookingResult result = chatBookingService.createBooking(
                "u_001", "PGP", "CLB", "2026-02-11T09:00:00", 1);

        assertNotNull(result);
        assertNotNull(result.bookingId());
        assertNull(result.tripId());
        assertTrue(result.deeplink().contains("booking/"));

        verify(bookingRepository).save(argThat(b -> "pending".equals(b.getStatus())));
    }

    @Test
    void createBooking_shouldPassCorrectCoordinates_knownLocation() {
        Trip trip = new Trip();
        trip.setId("trip_002");
        when(tripService.startTrip(eq("u_001"), any(TripDto.StartTripRequest.class))).thenReturn(trip);

        chatBookingService.createBooking("u_001", "COM3", "UTown", "2026-02-11T10:00:00", 1);

        verify(tripService).startTrip(eq("u_001"), argThat(req ->
                "COM3".equals(req.startPlaceName)));
    }

    // ---------- getBooking ----------
    @Test
    void getBooking_found_shouldReturnDto() {
        ChatBooking booking = new ChatBooking();
        booking.setBookingId("bk_123");
        booking.setTripId("trip_1");
        booking.setFromName("PGP");
        booking.setToName("CLB");
        booking.setDepartAt("2026-02-11T10:00:00");
        booking.setPassengers(2);
        booking.setStatus("confirmed");
        booking.setCreatedAt(Instant.now());

        when(bookingRepository.findByBookingId("bk_123")).thenReturn(Optional.of(booking));

        Optional<BookingDetailDto> result = chatBookingService.getBooking("bk_123");

        assertTrue(result.isPresent());
        assertEquals("bk_123", result.get().getBookingId());
        assertEquals("PGP", result.get().getFromName());
        assertEquals("CLB", result.get().getToName());
        assertEquals(2, result.get().getPassengers());
    }

    @Test
    void getBooking_notFound_shouldReturnEmpty() {
        when(bookingRepository.findByBookingId("bk_xxx")).thenReturn(Optional.empty());

        Optional<BookingDetailDto> result = chatBookingService.getBooking("bk_xxx");

        assertTrue(result.isEmpty());
    }

    // ---------- getUserBookings ----------
    @Test
    void getUserBookings_shouldReturnMappedDtos() {
        ChatBooking b1 = new ChatBooking();
        b1.setBookingId("bk_1");
        b1.setUserId("u_001");
        b1.setFromName("COM3");
        b1.setToName("UTown");
        b1.setDepartAt("2026-02-11T10:00:00");
        b1.setPassengers(1);
        b1.setStatus("confirmed");
        b1.setCreatedAt(Instant.now());

        ChatBooking b2 = new ChatBooking();
        b2.setBookingId("bk_2");
        b2.setUserId("u_001");
        b2.setFromName("PGP");
        b2.setToName("CLB");
        b2.setDepartAt("2026-02-11T11:00:00");
        b2.setPassengers(3);
        b2.setStatus("pending");
        b2.setCreatedAt(Instant.now());

        when(bookingRepository.findByUserIdOrderByCreatedAtDesc("u_001")).thenReturn(List.of(b1, b2));

        List<BookingDetailDto> result = chatBookingService.getUserBookings("u_001");

        assertEquals(2, result.size());
        assertEquals("bk_1", result.get(0).getBookingId());
        assertEquals("bk_2", result.get(1).getBookingId());
    }

    @Test
    void getUserBookings_noBookings_shouldReturnEmptyList() {
        when(bookingRepository.findByUserIdOrderByCreatedAtDesc("u_999")).thenReturn(List.of());

        List<BookingDetailDto> result = chatBookingService.getUserBookings("u_999");

        assertTrue(result.isEmpty());
    }

    // ---------- cancelBooking ----------
    @Test
    void cancelBooking_validOwner_shouldCancelAndReturnTrue() {
        ChatBooking booking = new ChatBooking();
        booking.setBookingId("bk_1");
        booking.setUserId("u_001");
        booking.setTripId("trip_1");
        booking.setStatus("confirmed");

        when(bookingRepository.findByBookingId("bk_1")).thenReturn(Optional.of(booking));

        boolean result = chatBookingService.cancelBooking("bk_1", "u_001");

        assertTrue(result);
        verify(bookingRepository).save(argThat(b -> "cancelled".equals(b.getStatus())));
        verify(tripService).cancelTrip("u_001", "trip_1");
    }

    @Test
    void cancelBooking_notFound_shouldReturnFalse() {
        when(bookingRepository.findByBookingId("bk_xxx")).thenReturn(Optional.empty());

        boolean result = chatBookingService.cancelBooking("bk_xxx", "u_001");

        assertFalse(result);
    }

    @Test
    void cancelBooking_notOwned_shouldReturnFalse() {
        ChatBooking booking = new ChatBooking();
        booking.setBookingId("bk_1");
        booking.setUserId("u_002");
        booking.setStatus("confirmed");

        when(bookingRepository.findByBookingId("bk_1")).thenReturn(Optional.of(booking));

        boolean result = chatBookingService.cancelBooking("bk_1", "u_001");

        assertFalse(result);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelBooking_alreadyCancelled_shouldReturnFalse() {
        ChatBooking booking = new ChatBooking();
        booking.setBookingId("bk_1");
        booking.setUserId("u_001");
        booking.setStatus("cancelled");

        when(bookingRepository.findByBookingId("bk_1")).thenReturn(Optional.of(booking));

        boolean result = chatBookingService.cancelBooking("bk_1", "u_001");

        assertFalse(result);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void cancelBooking_noLinkedTrip_shouldStillCancel() {
        ChatBooking booking = new ChatBooking();
        booking.setBookingId("bk_1");
        booking.setUserId("u_001");
        booking.setTripId(null);
        booking.setStatus("pending");

        when(bookingRepository.findByBookingId("bk_1")).thenReturn(Optional.of(booking));

        boolean result = chatBookingService.cancelBooking("bk_1", "u_001");

        assertTrue(result);
        verify(bookingRepository).save(argThat(b -> "cancelled".equals(b.getStatus())));
        verify(tripService, never()).cancelTrip(anyString(), anyString());
    }

    @Test
    void cancelBooking_tripCancelFails_shouldStillCancelBooking() {
        ChatBooking booking = new ChatBooking();
        booking.setBookingId("bk_1");
        booking.setUserId("u_001");
        booking.setTripId("trip_1");
        booking.setStatus("confirmed");

        when(bookingRepository.findByBookingId("bk_1")).thenReturn(Optional.of(booking));
        doThrow(new RuntimeException("Trip cancel failed")).when(tripService).cancelTrip("u_001", "trip_1");

        boolean result = chatBookingService.cancelBooking("bk_1", "u_001");

        assertTrue(result);
        verify(bookingRepository).save(argThat(b -> "cancelled".equals(b.getStatus())));
    }
}
