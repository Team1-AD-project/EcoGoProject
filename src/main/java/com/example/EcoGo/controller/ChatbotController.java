package com.example.EcoGo.controller;

import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.dto.chatbot.BookingDetailDto;
import com.example.EcoGo.dto.chatbot.ChatRequestDto;
import com.example.EcoGo.dto.chatbot.ChatResponseDto;
import com.example.EcoGo.service.chatbot.ChatBookingService;
import com.example.EcoGo.service.chatbot.ChatOrchestratorService;
import com.example.EcoGo.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/mobile/chatbot")
public class ChatbotController {

    private static final Logger log = LoggerFactory.getLogger(ChatbotController.class);

    @Autowired
    private ChatOrchestratorService orchestratorService;

    @Autowired
    private ChatBookingService bookingService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * Main chat endpoint.
     * POST /api/v1/mobile/chatbot/chat
     */
    @PostMapping("/chat")
    public ResponseMessage<ChatResponseDto> chat(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody ChatRequestDto request) {

        String userId = extractUserId(authHeader);
        boolean isAdmin = extractIsAdmin(authHeader);

        ChatResponseDto response = orchestratorService.handleChat(
                userId, isAdmin, request.getConversationId(), request.getMessage());

        return ResponseMessage.success(response);
    }

    /**
     * Get booking detail by bookingId.
     * GET /api/v1/mobile/chatbot/bookings/{bookingId}
     */
    @GetMapping("/bookings/{bookingId}")
    public ResponseMessage<BookingDetailDto> getBooking(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String bookingId) {

        Optional<BookingDetailDto> booking = bookingService.getBooking(bookingId);
        if (booking.isEmpty()) {
            return new ResponseMessage<>(HttpStatus.NOT_FOUND.value(), "Booking not found", null);
        }
        return ResponseMessage.success(booking.get());
    }

    /**
     * List current user's bookings (newest first).
     * GET /api/v1/mobile/chatbot/bookings
     */
    @GetMapping("/bookings")
    public ResponseMessage<List<BookingDetailDto>> getUserBookings(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String userId = extractUserId(authHeader);
        List<BookingDetailDto> bookings = bookingService.getUserBookings(userId);
        return ResponseMessage.success(bookings);
    }

    /**
     * Cancel a booking.
     * POST /api/v1/mobile/chatbot/bookings/{bookingId}/cancel
     */
    @PostMapping("/bookings/{bookingId}/cancel")
    public ResponseMessage<String> cancelBooking(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable String bookingId) {

        String userId = extractUserId(authHeader);
        boolean cancelled = bookingService.cancelBooking(bookingId, userId);
        if (!cancelled) {
            return new ResponseMessage<>(HttpStatus.BAD_REQUEST.value(),
                    "Cannot cancel: booking not found, not owned, or already cancelled", null);
        }
        return ResponseMessage.success("Booking cancelled");
    }

    /**
     * Health check for chatbot module.
     * GET /api/v1/mobile/chatbot/health
     */
    @GetMapping("/health")
    public ResponseMessage<String> health() {
        return ResponseMessage.success("Chatbot module is running");
    }

    // ========== Helper methods ==========

    private String extractUserId(String authHeader) {
        if (authHeader != null && !authHeader.isBlank()) {
            try {
                String token = authHeader.replace("Bearer ", "");
                Claims claims = jwtUtils.validateToken(token);
                return claims.getSubject();
            } catch (Exception e) {
                log.debug("[CHATBOT] Invalid token; falling back to guest: {}", e.getMessage());
            }
        }
        return "guest";
    }

    private boolean extractIsAdmin(String authHeader) {
        if (authHeader != null && !authHeader.isBlank()) {
            try {
                String token = authHeader.replace("Bearer ", "");
                Claims claims = jwtUtils.validateToken(token);
                Boolean adminClaim = claims.get("isAdmin", Boolean.class);
                return Boolean.TRUE.equals(adminClaim);
            } catch (Exception ignored) {}
        }
        return false;
    }
}
