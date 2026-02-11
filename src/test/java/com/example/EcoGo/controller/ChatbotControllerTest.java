package com.example.EcoGo.controller;

import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.dto.chatbot.BookingDetailDto;
import com.example.EcoGo.dto.chatbot.ChatRequestDto;
import com.example.EcoGo.dto.chatbot.ChatResponseDto;
import com.example.EcoGo.service.chatbot.ChatBookingService;
import com.example.EcoGo.service.chatbot.ChatOrchestratorService;
import com.example.EcoGo.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatbotControllerTest {

    private ChatOrchestratorService orchestratorService;
    private ChatBookingService bookingService;
    private JwtUtils jwtUtils;
    private ChatbotController controller;

    @BeforeEach
    void setUp() throws Exception {
        orchestratorService = mock(ChatOrchestratorService.class);
        bookingService = mock(ChatBookingService.class);
        jwtUtils = mock(JwtUtils.class);
        controller = new ChatbotController();

        Field f1 = ChatbotController.class.getDeclaredField("orchestratorService");
        f1.setAccessible(true);
        f1.set(controller, orchestratorService);

        Field f2 = ChatbotController.class.getDeclaredField("bookingService");
        f2.setAccessible(true);
        f2.set(controller, bookingService);

        Field f3 = ChatbotController.class.getDeclaredField("jwtUtils");
        f3.setAccessible(true);
        f3.set(controller, jwtUtils);
    }

    // ---------- chat ----------
    @Test
    void chat_withNoAuthHeader_shouldUseGuestAndNotAdmin() {
        ChatRequestDto request = new ChatRequestDto();
        request.setConversationId("c1");
        request.setMessage("hello");

        ChatResponseDto mockResp = new ChatResponseDto("c1", "Hi there!");
        when(orchestratorService.handleChat("guest", false, "c1", "hello")).thenReturn(mockResp);

        ResponseMessage<ChatResponseDto> resp = controller.chat(null, request);

        assertEquals(HttpStatus.OK.value(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals("c1", resp.getData().getConversationId());
        verify(orchestratorService).handleChat("guest", false, "c1", "hello");
    }

    @Test
    void chat_withValidToken_shouldExtractUserIdAndAdminFlag() {
        ChatRequestDto request = new ChatRequestDto();
        request.setConversationId("c2");
        request.setMessage("book a trip");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("u_001");
        when(claims.get("isAdmin", Boolean.class)).thenReturn(true);
        when(jwtUtils.validateToken("valid-token")).thenReturn(claims);

        ChatResponseDto mockResp = new ChatResponseDto("c2", "Let me help you book!");
        when(orchestratorService.handleChat("u_001", true, "c2", "book a trip")).thenReturn(mockResp);

        ResponseMessage<ChatResponseDto> resp = controller.chat("Bearer valid-token", request);

        assertEquals(HttpStatus.OK.value(), resp.getCode());
        assertNotNull(resp.getData());
        verify(orchestratorService).handleChat("u_001", true, "c2", "book a trip");
    }

    @Test
    void chat_withInvalidToken_shouldFallbackToGuest() {
        ChatRequestDto request = new ChatRequestDto();
        request.setConversationId("c3");
        request.setMessage("hi");

        when(jwtUtils.validateToken("bad-token")).thenThrow(new RuntimeException("invalid token"));

        ChatResponseDto mockResp = new ChatResponseDto("c3", "Hello!");
        when(orchestratorService.handleChat("guest", false, "c3", "hi")).thenReturn(mockResp);

        ResponseMessage<ChatResponseDto> resp = controller.chat("Bearer bad-token", request);

        assertEquals(HttpStatus.OK.value(), resp.getCode());
        verify(orchestratorService).handleChat("guest", false, "c3", "hi");
    }

    // ---------- getBooking ----------
    @Test
    void getBooking_found_shouldReturnSuccess() {
        BookingDetailDto dto = new BookingDetailDto("bk_123", "trip_1", "COM3", "UTown",
                "2026-02-11T10:00:00", 2, "confirmed", Instant.now());
        when(bookingService.getBooking("bk_123")).thenReturn(Optional.of(dto));

        ResponseMessage<BookingDetailDto> resp = controller.getBooking(null, "bk_123");

        assertEquals(HttpStatus.OK.value(), resp.getCode());
        assertNotNull(resp.getData());
        assertEquals("bk_123", resp.getData().getBookingId());
    }

    @Test
    void getBooking_notFound_shouldReturn404() {
        when(bookingService.getBooking("bk_xxx")).thenReturn(Optional.empty());

        ResponseMessage<BookingDetailDto> resp = controller.getBooking(null, "bk_xxx");

        assertEquals(HttpStatus.NOT_FOUND.value(), resp.getCode());
        assertNull(resp.getData());
    }

    // ---------- getUserBookings ----------
    @Test
    void getUserBookings_noAuth_shouldUseGuest() {
        when(bookingService.getUserBookings("guest")).thenReturn(List.of());

        ResponseMessage<List<BookingDetailDto>> resp = controller.getUserBookings(null);

        assertEquals(HttpStatus.OK.value(), resp.getCode());
        assertNotNull(resp.getData());
        assertTrue(resp.getData().isEmpty());
        verify(bookingService).getUserBookings("guest");
    }

    @Test
    void getUserBookings_withAuth_shouldReturnBookings() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("u_001");
        when(jwtUtils.validateToken("tok")).thenReturn(claims);

        BookingDetailDto dto = new BookingDetailDto("bk_1", null, "PGP", "CLB",
                "2026-02-11T09:00:00", 1, "confirmed", Instant.now());
        when(bookingService.getUserBookings("u_001")).thenReturn(List.of(dto));

        ResponseMessage<List<BookingDetailDto>> resp = controller.getUserBookings("Bearer tok");

        assertEquals(HttpStatus.OK.value(), resp.getCode());
        assertEquals(1, resp.getData().size());
        assertEquals("bk_1", resp.getData().get(0).getBookingId());
    }

    // ---------- cancelBooking ----------
    @Test
    void cancelBooking_success_shouldReturnSuccess() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("u_001");
        when(jwtUtils.validateToken("tok")).thenReturn(claims);
        when(bookingService.cancelBooking("bk_1", "u_001")).thenReturn(true);

        ResponseMessage<String> resp = controller.cancelBooking("Bearer tok", "bk_1");

        assertEquals(HttpStatus.OK.value(), resp.getCode());
        assertEquals("Booking cancelled", resp.getData());
    }

    @Test
    void cancelBooking_failed_shouldReturnBadRequest() {
        when(bookingService.cancelBooking("bk_xxx", "guest")).thenReturn(false);

        ResponseMessage<String> resp = controller.cancelBooking(null, "bk_xxx");

        assertEquals(HttpStatus.BAD_REQUEST.value(), resp.getCode());
        assertNull(resp.getData());
    }

    // ---------- health ----------
    @Test
    void health_shouldReturnRunningMessage() {
        ResponseMessage<String> resp = controller.health();

        assertEquals(HttpStatus.OK.value(), resp.getCode());
        assertEquals("Chatbot module is running", resp.getData());
    }
}
