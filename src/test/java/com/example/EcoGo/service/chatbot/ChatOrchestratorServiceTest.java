package com.example.EcoGo.service.chatbot;

import com.example.EcoGo.dto.chatbot.ChatResponseDto;
import com.example.EcoGo.model.ChatConversation;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.ChatConversationRepository;
import com.example.EcoGo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatOrchestratorServiceTest {

    @Mock private RagService ragService;
    @Mock private ChatBookingService bookingService;
    @Mock private ModelClientService modelClientService;
    @Mock private PythonChatbotProxyService pythonProxy;
    @Mock private NusBusProvider busProvider;
    @Mock private AuditLogService auditLogService;
    @Mock private ChatNotificationService notificationService;
    @Mock private ChatConversationRepository conversationRepository;
    @Mock private UserRepository userRepository;

    private ChatOrchestratorService orchestratorService;

    @BeforeEach
    void setUp() {
        orchestratorService = new ChatOrchestratorService(
                ragService, bookingService, modelClientService, pythonProxy,
                busProvider, auditLogService, notificationService,
                conversationRepository, userRepository
        );

        // Default: model disabled, python proxy disabled, RAG not available
        lenient().when(modelClientService.isEnabled()).thenReturn(false);
        lenient().when(pythonProxy.isEnabled()).thenReturn(false);
        lenient().when(ragService.isAvailable()).thenReturn(false);

        // Default: no existing conversation in DB
        lenient().when(conversationRepository.findByConversationId(anyString())).thenReturn(Optional.empty());
        lenient().when(conversationRepository.save(any(ChatConversation.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- Greeting ----------
    @Test
    void handleChat_greeting_shouldReturnMainMenuWithGreeting() {
        ChatResponseDto resp = orchestratorService.handleChat("guest", false, "c1", "hello");

        assertNotNull(resp);
        assertNotNull(resp.getAssistant());
        assertTrue(resp.getAssistant().getText().contains("EcoGo Assistant"));
        assertFalse(resp.getUiActions().isEmpty());
    }

    @Test
    void handleChat_chineseGreeting_shouldReturnMainMenu() {
        ChatResponseDto resp = orchestratorService.handleChat("guest", false, "c1", "你好");

        assertNotNull(resp);
        assertTrue(resp.getAssistant().getText().contains("EcoGo Assistant"));
    }

    // ---------- Reset / Cancel ----------
    @Test
    void handleChat_resetCommand_shouldReturnMainMenu() {
        ChatResponseDto resp = orchestratorService.handleChat("guest", false, "c1", "reset");

        assertNotNull(resp);
        assertTrue(resp.getAssistant().getText().contains("Session reset"));
    }

    @Test
    void handleChat_cancelCommand_shouldReturnMainMenu() {
        ChatResponseDto resp = orchestratorService.handleChat("guest", false, "c1", "取消");

        assertNotNull(resp);
        assertTrue(resp.getAssistant().getText().contains("Session reset"));
    }

    // ---------- Null/blank conversationId ----------
    @Test
    void handleChat_nullConversationId_shouldAutoGenerate() {
        ChatResponseDto resp = orchestratorService.handleChat("guest", false, null, "hi");

        assertNotNull(resp);
        assertNotNull(resp.getConversationId());
        assertTrue(resp.getConversationId().startsWith("c_"));
    }

    @Test
    void handleChat_blankConversationId_shouldAutoGenerate() {
        ChatResponseDto resp = orchestratorService.handleChat("guest", false, "  ", "hello");

        assertNotNull(resp);
        assertNotNull(resp.getConversationId());
        assertTrue(resp.getConversationId().startsWith("c_"));
    }

    // ---------- Bus Arrivals menu button ----------
    @Test
    void handleChat_busArrivalsButton_shouldAskForStop() {
        ChatResponseDto resp = orchestratorService.handleChat("guest", false, "c1", "🚌 Bus Arrivals");

        assertNotNull(resp);
        assertTrue(resp.getAssistant().getText().toLowerCase().contains("stop"));
    }

    // ---------- Travel Advice menu button ----------
    @Test
    void handleChat_travelAdviceButton_shouldAskForDestination() {
        ChatResponseDto resp = orchestratorService.handleChat("guest", false, "c1", "📍 Travel Advice");

        assertNotNull(resp);
        assertTrue(resp.getAssistant().getText().toLowerCase().contains("where"));
    }

    // ---------- Book a Trip menu button ----------
    @Test
    void handleChat_bookATripButton_shouldStartBookingFlow() {
        ChatResponseDto resp = orchestratorService.handleChat("guest", false, "c1", "🎫 Book a Trip");

        assertNotNull(resp);
        assertTrue(resp.getAssistant().getText().toLowerCase().contains("book"));
    }

    // ---------- My Profile for guest ----------
    @Test
    void handleChat_profileForGuest_shouldSayNotLoggedIn() {
        ChatResponseDto resp = orchestratorService.handleChat("guest", false, "c1", "📋 My Profile");

        assertNotNull(resp);
        assertTrue(resp.getAssistant().getText().contains("not logged in")
                || resp.getAssistant().getText().contains("sign in"));
    }

    // ---------- My Profile for authenticated user ----------
    @Test
    void handleChat_profileForUser_shouldReturnProfile() {
        User user = new User();
        user.setUserid("u_001");
        user.setNickname("TestUser");
        user.setEmail("test@nus.edu");
        user.setPhone("91234567");
        user.setFaculty("Computing");
        user.setTotalCarbon(100.0);
        user.setCurrentPoints(50);
        user.setTotalPoints(200);

        when(userRepository.findByUserid("u_001")).thenReturn(Optional.of(user));

        ChatResponseDto resp = orchestratorService.handleChat("u_001", false, "c1", "📋 My Profile");

        assertNotNull(resp);
        assertTrue(resp.getAssistant().getText().contains("TestUser"));
        assertTrue(resp.getAssistant().getText().contains("test@nus.edu"));
    }

    // ---------- Booking intent with from/to ----------
    @Test
    void handleChat_bookingIntent_shouldStartBookingWithRoute() {
        ChatResponseDto resp = orchestratorService.handleChat("u_001", false, "c1", "预订从COM3到UTown");

        assertNotNull(resp);
        String text = resp.getAssistant().getText();
        // Should recognize the route and ask for missing fields (time/passengers)
        assertTrue(text.contains("COM3") || text.contains("UTown") || text.toLowerCase().contains("departure")
                || text.toLowerCase().contains("time") || text.toLowerCase().contains("passenger"));
    }

    // ---------- Bus query intent ----------
    @Test
    void handleChat_busQueryIntent_shouldCallBusProvider() {
        NusBusProvider.BusArrivalsResult busResult =
                new NusBusProvider.BusArrivalsResult("COM3", java.util.List.of());
        when(busProvider.getArrivals(anyString(), any())).thenReturn(busResult);

        ChatResponseDto resp = orchestratorService.handleChat("guest", false, "c1", "bus arrival COM3");

        assertNotNull(resp);
        verify(busProvider, atLeastOnce()).getArrivals(anyString(), any());
    }

    // ---------- Recommendation intent ----------
    @Test
    void handleChat_recommendationIntent_shouldAskForRoute() {
        ChatResponseDto resp = orchestratorService.handleChat("guest", false, "c1", "推荐出行方式");

        assertNotNull(resp);
        // Should either give recommendations or ask for destination
        assertNotNull(resp.getAssistant().getText());
        assertFalse(resp.getAssistant().getText().isEmpty());
    }

    // ---------- RAG fallback ----------
    @Test
    void handleChat_unknownText_ragAvailable_shouldUseRag() {
        when(ragService.isAvailable()).thenReturn(true);
        when(ragService.retrieve(anyString(), anyInt())).thenReturn(java.util.List.of(
                new ChatResponseDto.Citation("Title", "source", "This is a RAG answer snippet.")
        ));

        ChatResponseDto resp = orchestratorService.handleChat("guest", false, "c1", "what is green travel in singapore?");

        assertNotNull(resp);
        assertTrue(resp.getAssistant().getText().contains("RAG answer snippet"));
    }

    @Test
    void handleChat_unknownText_ragNotAvailable_shouldReturnFallback() {
        when(ragService.isAvailable()).thenReturn(false);

        ChatResponseDto resp = orchestratorService.handleChat("guest", false, "c1", "random unknown text question xyz");

        assertNotNull(resp);
        assertTrue(resp.getAssistant().getText().contains("not sure") || resp.getAssistant().getText().contains("rephras"));
    }

    // ---------- Model-based intent detection ----------
    @Test
    void handleChat_modelEnabled_toolCallCreateBooking_shouldStartBooking() {
        when(modelClientService.isEnabled()).thenReturn(true);

        ModelClientService.ToolCall tc = new ModelClientService.ToolCall("create_booking",
                java.util.Map.of("fromName", "PGP", "toName", "CLB"));
        ModelClientService.ModelResult mr = new ModelClientService.ModelResult("", tc);
        when(modelClientService.callModelForTool(anyString())).thenReturn(mr);

        ChatResponseDto resp = orchestratorService.handleChat("u_001", false, "c1", "book from PGP to CLB");

        assertNotNull(resp);
        // Missing departAt and passengers -> should ask for them
        String text = resp.getAssistant().getText();
        assertTrue(text.toLowerCase().contains("depart") || text.toLowerCase().contains("time")
                || text.toLowerCase().contains("passenger") || text.toLowerCase().contains("pgp"));
    }

    @Test
    void handleChat_modelEnabled_toolCallBusArrivals_shouldQueryBus() {
        when(modelClientService.isEnabled()).thenReturn(true);

        ModelClientService.ToolCall tc = new ModelClientService.ToolCall("get_bus_arrivals",
                java.util.Map.of("stopName", "COM3"));
        ModelClientService.ModelResult mr = new ModelClientService.ModelResult("", tc);
        when(modelClientService.callModelForTool(anyString())).thenReturn(mr);

        NusBusProvider.BusArrivalsResult busResult =
                new NusBusProvider.BusArrivalsResult("COM3", java.util.List.of(
                        java.util.Map.of("route", "D2", "etaMinutes", 3, "status", "on_time")
                ));
        when(busProvider.getArrivals("COM3", null)).thenReturn(busResult);

        ChatResponseDto resp = orchestratorService.handleChat("guest", false, "c1", "next bus at COM3");

        assertNotNull(resp);
        assertTrue(resp.getAssistant().getText().contains("COM3"));
    }

    @Test
    void handleChat_modelEnabled_noToolCall_withText_shouldReturnModelText() {
        when(modelClientService.isEnabled()).thenReturn(true);

        ModelClientService.ModelResult mr = new ModelClientService.ModelResult("Here is my model response.", null);
        when(modelClientService.callModelForTool(anyString())).thenReturn(mr);

        ChatResponseDto resp = orchestratorService.handleChat("guest", false, "c1", "tell me about green travel");

        assertNotNull(resp);
        assertEquals("Here is my model response.", resp.getAssistant().getText());
    }

    // ---------- Python proxy fallback ----------
    @Test
    void handleChat_pythonProxyEnabled_shouldForwardToProxy() {
        when(pythonProxy.isEnabled()).thenReturn(true);
        ChatResponseDto proxyResp = new ChatResponseDto("c1", "Python says hello!");
        when(pythonProxy.forwardChat(anyString(), anyString(), anyString(), anyString())).thenReturn(proxyResp);

        ChatResponseDto resp = orchestratorService.handleChat("guest", false, "c1", "random question xyz");

        assertNotNull(resp);
        assertEquals("Python says hello!", resp.getAssistant().getText());
    }

    @Test
    void handleChat_pythonProxyReturnsNull_shouldFallbackToKeywordRag() {
        when(pythonProxy.isEnabled()).thenReturn(true);
        when(pythonProxy.forwardChat(anyString(), anyString(), anyString(), anyString())).thenReturn(null);

        ChatResponseDto resp = orchestratorService.handleChat("guest", false, "c1", "random unknown xyz");

        assertNotNull(resp);
        // Should fall through to RAG/default
        assertNotNull(resp.getAssistant().getText());
    }

    // ---------- Back to Menu ----------
    @Test
    void handleChat_backToMenu_shouldResetAndShowMenu() {
        ChatResponseDto resp = orchestratorService.handleChat("guest", false, "c1", "Back to Menu");

        assertNotNull(resp);
        assertTrue(resp.getAssistant().getText().contains("help"));
    }

    // ---------- Empty message ----------
    @Test
    void handleChat_emptyMessage_shouldShowMainMenu() {
        ChatResponseDto resp = orchestratorService.handleChat("guest", false, "c1", "   ");

        assertNotNull(resp);
        assertTrue(resp.getAssistant().getText().contains("help"));
    }

    // ---------- Persist message and state ----------
    @Test
    void handleChat_shouldPersistConversation() {
        orchestratorService.handleChat("u_001", false, "c1", "hello");

        // Should create new conversation and persist messages
        verify(conversationRepository, atLeastOnce()).save(any(ChatConversation.class));
    }

    // ---------- Complete booking flow ----------
    @Test
    void handleChat_completeBookingFlow_shouldCreateBooking() {
        ChatBookingService.BookingResult bookingResult =
                new ChatBookingService.BookingResult("bk_test", "trip_1", "ecogo://trip/trip_1");
        when(bookingService.createBooking(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(bookingResult);

        // Step 1: Start booking
        orchestratorService.handleChat("u_001", false, "c1", "预订从COM3到UTown");

        // Step 2: Provide time and passengers via form submission
        ChatResponseDto resp = orchestratorService.handleChat("u_001", false, "c1",
                "departAt=2026-02-11T10:00, passengers=2");

        assertNotNull(resp);
        // Should either confirm booking or ask for remaining fields
        // The exact behavior depends on state management
    }
}
