package com.example.EcoGo.service.chatbot;

import com.example.EcoGo.dto.chatbot.ChatResponseDto;
import com.example.EcoGo.dto.chatbot.ChatResponseDto.AssistantMessage;
import com.example.EcoGo.dto.chatbot.ChatResponseDto.Citation;
import com.example.EcoGo.dto.chatbot.ChatResponseDto.UiAction;
import com.example.EcoGo.model.ChatConversation;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.ChatConversationRepository;
import com.example.EcoGo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main chat orchestrator — ported from Python chatbot/backend/app/services/orchestrator.py
 *
 * Handles:
 * - Multi-turn conversation state management (in-memory + MongoDB persistence)
 * - Intent detection (keyword-based + optional model-based)
 * - Routing to: booking, bus query, user update, RAG knowledge Q&A
 */
@Service
public class ChatOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(ChatOrchestratorService.class);

    private final RagService ragService;
    private final ChatBookingService bookingService;
    private final ModelClientService modelClientService;
    private final PythonChatbotProxyService pythonProxy;
    private final NusBusProvider busProvider;
    private final AuditLogService auditLogService;
    private final ChatNotificationService notificationService;
    private final ChatConversationRepository conversationRepository;
    private final UserRepository userRepository;

    // In-memory conversation state cache (backed by MongoDB) with TTL
    private final Map<String, ConversationState> conversations = new ConcurrentHashMap<>();
    private static final long CONVERSATION_TTL_MS = 30 * 60 * 1000L; // 30 minutes

    // === Main menu buttons ===
    private static final List<String> MAIN_MENU = List.of("🚌 Bus Arrivals", "📍 Travel Advice", "🎫 Book a Trip", "📋 My Profile");
    private static final List<String> POST_ACTION = List.of("🚌 Bus Arrivals", "📍 Travel Advice", "🎫 Book a Trip");
    private static final List<String> BUS_STOP_OPTIONS = List.of("COM3", "UTOWN", "KR-MRT", "PGP", "CLB", "BIZ2");

    public ChatOrchestratorService(RagService ragService,
                                    ChatBookingService bookingService,
                                    ModelClientService modelClientService,
                                    PythonChatbotProxyService pythonProxy,
                                    NusBusProvider busProvider,
                                    AuditLogService auditLogService,
                                    ChatNotificationService notificationService,
                                    ChatConversationRepository conversationRepository,
                                    UserRepository userRepository) {
        this.ragService = ragService;
        this.bookingService = bookingService;
        this.modelClientService = modelClientService;
        this.pythonProxy = pythonProxy;
        this.busProvider = busProvider;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
    }

    public ChatResponseDto handleChat(String userId, boolean isAdmin, String conversationId, String messageText) {
        log.info("[ORCHESTRATOR] User: {}, isAdmin: {}, Message: {}", userId, isAdmin, messageText);

        // Resolve or create conversation
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = "c_" + System.currentTimeMillis();
        }

        ConversationState state = getOrCreateState(conversationId, userId);
        String text = messageText.trim();
        String role = isAdmin ? "admin" : "user";

        log.info("[ORCHESTRATOR] ConversationId: {}, State.intent: {}", conversationId, state.intent);

        // Persist user message to MongoDB
        persistMessage(conversationId, userId, "user", text);

        // --- Handle reset/cancel ---
        ChatResponseDto response;
        if (isResetCommand(text)) {
            state.reset();
            response = buildMainMenu(conversationId, "Session reset. How can I help you?");
        }
        // --- Handle confirmation for user_update ---
        else if (isConfirmCommand(text) && "user_update".equals(state.intent) && state.pendingUserUpdate != null) {
            response = handleUserUpdateConfirmation(conversationId, userId, role, state);
        }
        // --- Continue booking flow ---
        else if ("booking".equals(state.intent)) {
            response = handleBookingFlow(conversationId, userId, state, text);
        }
        // --- Continue awaiting bus stop / destination flows ---
        else if ("awaiting_bus_stop".equals(state.intent)) {
            response = handleBusQueryWithStop(conversationId, state, text);
        }
        else if ("awaiting_destination".equals(state.intent)) {
            response = handleRecommendWithDestination(conversationId, state, text);
        }
        // --- Quick-action buttons (must be handled before model-based detection) ---
        else if (text.equalsIgnoreCase("Show more") || text.equalsIgnoreCase("查看更多班次")) {
            response = handleBusQueryExpanded(conversationId, text);
        }
        else if (text.contains("Change stop") || text.contains("换个站点")
                || text.equalsIgnoreCase("Try another stop") || text.equalsIgnoreCase("换个站点查询")) {
            response = buildBusStopPrompt(conversationId, state);
        }
        // --- Model-based intent detection ---
        else if (modelClientService.isEnabled()) {
            ModelClientService.ModelResult mr = modelClientService.callModelForTool(text);
            if (mr != null && mr.toolCall() != null) {
                response = handleToolCall(conversationId, userId, role, state, mr);
            } else if (mr != null && mr.text() != null && !mr.text().isBlank()) {
                response = new ChatResponseDto(conversationId, mr.text())
                        .withSuggestions(POST_ACTION);
            } else {
                // Model returned nothing useful, try Python chatbot proxy
                response = tryPythonProxyOrKeyword(conversationId, userId, role, state, text);
            }
        } else {
            // Model disabled, try Python chatbot proxy then keyword/RAG
            response = tryPythonProxyOrKeyword(conversationId, userId, role, state, text);
        }

        // Persist assistant response to MongoDB
        persistMessage(conversationId, userId, "assistant",
                response.getAssistant() != null ? response.getAssistant().getText() : "");

        // Save conversation state to MongoDB
        persistState(conversationId, userId, state);

        return response;
    }

    // ========== Python proxy fallback ==========

    /**
     * Try forwarding to the Python chatbot backend first.
     * If that fails (disabled or unreachable), fall back to keyword/RAG handling.
     */
    private ChatResponseDto tryPythonProxyOrKeyword(String convId, String userId, String role,
                                                     ConversationState state, String text) {
        if (pythonProxy.isEnabled()) {
            ChatResponseDto proxyResponse = pythonProxy.forwardChat(userId, role, convId, text);
            if (proxyResponse != null) {
                log.info("[ORCHESTRATOR] Got response from Python chatbot proxy");
                return proxyResponse;
            }
            log.debug("[ORCHESTRATOR] Python proxy unavailable, falling back to keyword/RAG");
        }
        return handleKeywordOrRag(convId, userId, role, state, text);
    }

    // ========== Keyword / RAG fallback router ==========

    private ChatResponseDto handleKeywordOrRag(String convId, String userId, String role,
                                                ConversationState state, String text) {
        // === 0. Empty message / emoji only → show main menu ===
        if (text.isBlank() || text.matches("[\\s\\p{So}\\p{Cn}]+")) {
            return buildMainMenu(convId, "How can I help you?");
        }

        // === 1. Greeting → friendly reply + main menu ===
        if (isGreeting(text)) {
            return buildMainMenu(convId, "Hi there! I'm EcoGo Assistant 😊\nHow can I help you today?");
        }

        // === 2. Menu button clicks (match button text) ===
        if (text.contains("Bus Arrivals") || text.contains("查公交")) {
            return buildBusStopPrompt(convId, state);
        }
        if (text.contains("Travel Advice") || text.contains("出行推荐")) {
            return buildRecommendPrompt(convId, state);
        }
        if (text.contains("Book a Trip") || text.contains("预订行程")) {
            state.intent = "booking";
            return new ChatResponseDto(convId, "Sure, let me help you book a trip 🎫\n\nWhere would you like to go?\ne.g. from NUS to Changi Airport")
                    .withSuggestions(List.of("NUS to Marina Bay", "PGP to UTown", "Back to Menu"));
        }
        if (text.contains("My Profile") || text.contains("我的资料")) {
            return handleProfileQuery(convId, userId);
        }
        if (text.equalsIgnoreCase("Back to Menu") || text.equals("返回主菜单") || text.equals("主菜单")) {
            state.reset();
            return buildMainMenu(convId, "Sure! What else can I help you with?");
        }

        // === 2a. "Book X to Y" buttons from recommendation follow-ups ===
        if (text.toLowerCase().contains("book ") && !text.toLowerCase().contains("book a trip")) {
            String routePart = text.replaceAll("(?i).*\\bbook\\s+", "").trim();
            // Remove emoji prefix if present
            routePart = routePart.replaceAll("^[\\p{So}\\p{Cn}\\s]+", "").trim();
            String[] ft = extractFromToLooser(routePart);
            if (ft != null) {
                state.intent = "booking";
                state.partialData.put("fromName", ft[0]);
                state.partialData.put("toName", ft[1]);
                return buildMissingFieldsResponse(convId, state);
            }
        }

        // === 2b. Bus follow-up buttons ===
        if (text.equalsIgnoreCase("Show more") || text.equalsIgnoreCase("查看更多班次")) {
            // Re-run full bus query but show more results
            return handleBusQueryExpanded(convId, text);
        }
        if (text.contains("Change stop") || text.contains("换个站点")) {
            return buildBusStopPrompt(convId, state);
        }
        if (text.equalsIgnoreCase("Try another stop") || text.equalsIgnoreCase("换个站点查询")) {
            return buildBusStopPrompt(convId, state);
        }

        // === 3. Booking intent ===
        if (isBookingIntent(text)) {
            state.intent = "booking";
            String[] fromTo = extractFromTo(text);
            if (fromTo != null) {
                state.partialData.put("fromName", fromTo[0]);
                state.partialData.put("toName", fromTo[1]);
            }
            Integer passengers = extractPassengers(text);
            if (passengers != null) {
                state.partialData.put("passengers", passengers);
            }
            return buildMissingFieldsResponse(convId, state);
        }

        // === 4. Bus intent (specific keyword combinations) ===
        if (isBusQueryIntent(text)) {
            return handleBusQuery(convId, text);
        }

        // === 5. Profile QUERY ===
        if (containsAny(text.toLowerCase(), "my profile", "my info", "my account", "view profile")
                || (containsAny(text, "查询", "查看", "看看", "我的资料", "个人信息", "我的信息") && !containsAny(text, "修改", "更新", "改"))) {
            return handleProfileQuery(convId, userId);
        }

        // === 6. User update ===
        if ((containsAny(text, "修改", "更新", "资料", "用户") && (text.contains("u_") || text.contains("我的")))
                || (containsAny(text.toLowerCase(), "update my", "change my", "edit my") && containsAny(text.toLowerCase(), "profile", "nickname", "email", "phone", "faculty"))) {
            return handleUserUpdateRequest(convId, userId, role, state, text);
        }

        // === 7. Recommendation intent (free-form) ===
        if (isRecommendationIntent(text)) {
            return handleRecommendation(convId, text);
        }

        // === 8. Default: RAG knowledge Q&A (精简版) ===
        return handleRagQuery(convId, text);
    }

    // ========== 仿人工对话 - 引导式交互 ==========

    /** 判断是否为问候语 */
    private boolean isGreeting(String text) {
        String lower = text.toLowerCase().trim();
        return Set.of("你好", "hi", "hello", "嗨", "hey", "早上好", "下午好", "晚上好",
                "在吗", "在不在", "有人吗", "good morning", "good afternoon", "good evening",
                "hi there", "hey there", "howdy", "greetings").contains(lower)
                || lower.matches("^(你好|hi|hello|嗨|hey|howdy).*");
    }

    /** 构建主菜单响应 */
    private ChatResponseDto buildMainMenu(String convId, String greeting) {
        return new ChatResponseDto(convId, greeting)
                .withSuggestions(MAIN_MENU);
    }

    /** Build bus stop selection prompt */
    private ChatResponseDto buildBusStopPrompt(String convId, ConversationState state) {
        state.intent = "awaiting_bus_stop";
        return new ChatResponseDto(convId,
                "Sure, let me check bus arrivals 🚌\n\nWhich stop would you like to check? Type a stop name or pick one below:")
                .withSuggestions(BUS_STOP_OPTIONS);
    }

    /** Handle user selecting a bus stop */
    private ChatResponseDto handleBusQueryWithStop(String convId, ConversationState state, String text) {
        state.reset();
        return handleBusQuery(convId, text);
    }

    /** Build travel recommendation prompt — ask origin/destination first */
    private ChatResponseDto buildRecommendPrompt(String convId, ConversationState state) {
        state.intent = "awaiting_destination";
        return new ChatResponseDto(convId,
                "Sure, let me recommend a travel option 📍\n\nWhere would you like to go?\ne.g. COM3 to UTown")
                .withSuggestions(List.of("COM3 to UTown", "PGP to CLB", "KR MRT to BIZ2", "Back to Menu"));
    }

    /** Handle user replying with origin/destination */
    private ChatResponseDto handleRecommendWithDestination(String convId, ConversationState state, String text) {
        state.reset();

        String[] fromTo = extractFromTo(text);
        if (fromTo == null) fromTo = extractFromToLooser(text);

        if (fromTo != null) {
            return buildSmartRecommendation(convId, fromTo[0], fromTo[1]);
        }

        // Try treating the whole text as a destination
        String dest = text.trim();
        if (!dest.isEmpty() && dest.length() < 30) {
            return buildSmartRecommendation(convId, null, dest);
        }

        // Cannot parse, ask again
        return new ChatResponseDto(convId,
                "Sorry, I couldn't identify the origin and destination.\nPlease use the format: A to B, e.g. COM3 to UTown")
                .withSuggestions(List.of("COM3 to UTown", "PGP to CLB", "Back to Menu"));
    }

    /** Generate smart recommendation based on origin/destination */
    private ChatResponseDto buildSmartRecommendation(String convId, String from, String to) {
        String queryText = from != null ? from + " to " + to : "to " + to;

        // Retrieve top-1 RAG result
        String ragTip = "";
        List<Citation> citations = Collections.emptyList();
        if (ragService.isAvailable()) {
            try {
                citations = ragService.retrieve("green travel " + queryText, 1);
                if (!citations.isEmpty()) {
                    String snippet = citations.get(0).getSnippet();
                    if (snippet.length() > 80) snippet = snippet.substring(0, 80) + "...";
                    ragTip = "\n\n💡 " + snippet;
                }
            } catch (Exception ignored) {}
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📍 Travel advice for ").append(queryText).append(":\n\n");

        boolean isNusCampus = isNusCampusLocation(from) || isNusCampusLocation(to);

        if (isNusCampus) {
            sb.append("🚌 **Campus Shuttle**: Take the free NUS shuttle bus — check real-time arrivals\n");
            sb.append("🚶 **Walk**: Most campus routes are 10-15 min on foot — zero emissions!\n");
            sb.append("🚇 **MRT Link**: For off-campus destinations, connect at KR MRT station\n");
        } else {
            sb.append("🚇 **MRT**: Fastest and low-carbon option\n");
            sb.append("🚌 **Public Bus**: Wide coverage, affordable fares\n");
            sb.append("🚶 **Walk / Cycle**: Best for short distances — zero emissions\n");
        }

        sb.append(ragTip);

        ChatResponseDto response = new ChatResponseDto();
        response.setConversationId(convId);
        response.setAssistant(new AssistantMessage(sb.toString().trim(), citations));
        response.setServerTimestamp(Instant.now());

        List<String> followUp = new ArrayList<>();
        if (isNusCampus) followUp.add("🚌 Bus Arrivals");
        if (from != null) followUp.add("🎫 Book " + queryText);
        followUp.add("Back to Menu");
        response.withSuggestions(followUp);

        return response;
    }

    /** 判断是否为NUS校园内的地点 */
    private boolean isNusCampusLocation(String location) {
        if (location == null) return false;
        String lower = location.toLowerCase();
        return Set.of("com3", "com2", "pgp", "pgpr", "utown", "clb", "yih", "biz2",
                "kr-mrt", "kr mrt", "it", "museum", "raffles", "kv", "lt13", "lt27",
                "as5", "s17", "uhc", "uhall", "krb", "tcoms", "nuss", "nus").contains(lower)
                || lower.contains("nus") || lower.contains("utown");
    }

    /**
     * Detect booking intent. Requires:
     * - Explicit booking verb ("预订", "预定", "订票", "book") OR
     * - "从...到..." route pattern (strongly implies booking) OR
     * - "预订" combined with "行程"/"路线"
     * Does NOT trigger on vague mentions of "行程" or "路线" alone.
     */
    private boolean isBookingIntent(String text) {
        String lower = text.toLowerCase();
        // Strong booking verbs (Chinese + English)
        if (containsAny(text, "预订", "预定", "订票", "帮我订", "我要订", "我想订")) return true;
        if (lower.contains("book a trip") || lower.contains("book trip") || lower.contains("book a ride")) return true;
        if (lower.contains("i want to book") || lower.contains("i'd like to book")) return true;
        // "从...到..." pattern strongly implies booking
        if (text.contains("从") && (text.contains("到") || text.contains("去"))) {
            return extractFromTo(text) != null;
        }
        // "book" + trip context
        if (lower.contains("book") && containsAny(lower, "trip", "ride", "travel", "行程", "路线")) return true;
        return false;
    }

    /**
     * Detect bus query intent. Requires specific bus-related keyword combinations
     * to avoid false positives on general messages that casually mention "公交".
     *
     * Triggers on:
     * - "公交到站" / "到站时间" / "下一班" / "几分钟到"
     * - NUS route name + "线" / "路" (e.g., "D2线", "A1路")
     * - Explicit "查询公交" / "bus arrival"
     */
    private boolean isBusQueryIntent(String text) {
        String lower = text.toLowerCase();
        // Strong bus-query combinations (Chinese + English)
        if (lower.contains("到站时间") || lower.contains("到站信息")) return true;
        if (lower.contains("公交到站") || lower.contains("公交查询")) return true;
        if (lower.contains("下一班") || lower.contains("几分钟到")) return true;
        if (lower.contains("bus arrival") || lower.contains("next bus") || lower.contains("bus schedule")) return true;
        if (lower.contains("when is the") && lower.contains("bus")) return true;
        if (lower.contains("check bus") || lower.contains("bus eta") || lower.contains("bus time")) return true;
        if (lower.contains("shuttle") && containsAny(text, "when", "time", "到站", "arrival", "next")) return true;

        // NUS route pattern + bus context (e.g., "D2线公交", "A1到站")
        if (lower.matches(".*[a-z]\\d.*") && containsAny(text, "线", "路", "到站", "公交", "bus")) {
            // Verify it's a known NUS route
            java.util.regex.Matcher rm = java.util.regex.Pattern.compile("(?i)([A-Z]\\d[A-Z]?)").matcher(text);
            if (rm.find() && NusBusProvider.isRouteName(rm.group(1))) {
                return true;
            }
        }

        // "查询/查看" + "公交/巴士" (but not "查询公交系统" type general question)
        if (containsAny(text, "查询", "查看", "查") && containsAny(text, "公交", "巴士", "shuttle")) {
            // Only if message is short enough (not a general knowledge question)
            return text.length() < 30;
        }

        return false;
    }

    /**
     * Detect recommendation/suggestion intent.
     * 只有明确需要路线推荐时才触发，一般性知识问题走 RAG。
     * - "怎么去XX" → 推荐
     * - "推荐一下出行方式" → 推荐（有"推荐"动词）
     * - "新加坡有哪些出行方式" → 不触发（一般性问题，走 RAG）
     */
    private boolean isRecommendationIntent(String text) {
        String lower = text.toLowerCase();
        // Strong recommendation keywords (Chinese + English)
        if (containsAny(text, "推荐", "建议一下", "suggest", "recommend")) return true;
        if (containsAny(text, "怎么去", "怎样去", "如何去")) return true;
        if (lower.contains("how to get") || lower.contains("how do i get") || lower.contains("best way to")) return true;
        if (lower.contains("best route") || containsAny(text, "最佳路线", "最好的路线")) return true;
        if (lower.contains("travel advice") || lower.contains("travel tip")) return true;
        // "出行方式/交通方式" only for short messages
        if (containsAny(text, "出行方式", "交通方式")) {
            return text.length() < 15 && !containsAny(text, "哪些", "什么", "有哪", "有什么");
        }
        return false;
    }

    /**
     * Handle travel recommendation queries (free-form text).
     * 如果有从...到...则直接推荐，否则引导用户给出目的地。
     */
    private ChatResponseDto handleRecommendation(String convId, String text) {
        String[] fromTo = extractFromTo(text);
        if (fromTo == null) fromTo = extractFromToLooser(text);

        if (fromTo != null) {
            return buildSmartRecommendation(convId, fromTo[0], fromTo[1]);
        }

        // No route extracted, guide the user
        return new ChatResponseDto(convId,
                "Want travel advice? Tell me your **origin and destination** 😊\ne.g. COM3 to UTown")
                .withSuggestions(List.of("COM3 to UTown", "PGP to CLB", "Back to Menu"));
    }

    // ========== Conversation Persistence ==========

    private ConversationState getOrCreateState(String conversationId, String userId) {
        // Periodically clean up expired conversations from memory
        cleanupExpiredConversations();

        // Try in-memory cache first
        ConversationState cached = conversations.get(conversationId);
        if (cached != null) {
            cached.lastAccessedAt = System.currentTimeMillis();
            return cached;
        }

        // Try loading from MongoDB
        Optional<ChatConversation> existing = conversationRepository.findByConversationId(conversationId);
        ConversationState state;
        if (existing.isPresent()) {
            ChatConversation conv = existing.get();
            state = new ConversationState();
            ChatConversation.ConversationState dbState = conv.getState();
            if (dbState != null) {
                state.intent = dbState.getIntent();
                if (dbState.getPartialData() != null) {
                    state.partialData.putAll(dbState.getPartialData());
                }
            }
        } else {
            state = new ConversationState();
            // Create new conversation in MongoDB
            ChatConversation conv = new ChatConversation();
            conv.setConversationId(conversationId);
            conv.setUserId(userId);
            conv.setCreatedAt(Instant.now());
            conv.setUpdatedAt(Instant.now());
            conversationRepository.save(conv);
        }

        conversations.put(conversationId, state);
        return state;
    }

    private void persistMessage(String conversationId, String userId, String role, String text) {
        try {
            Optional<ChatConversation> opt = conversationRepository.findByConversationId(conversationId);
            if (opt.isPresent()) {
                ChatConversation conv = opt.get();
                ChatConversation.Message msg = new ChatConversation.Message(role, text);
                conv.getMessages().add(msg);
                conv.setUpdatedAt(Instant.now());
                conversationRepository.save(conv);
            }
        } catch (Exception e) {
            log.warn("[ORCHESTRATOR] Failed to persist message: {}", e.getMessage());
        }
    }

    private void persistState(String conversationId, String userId, ConversationState state) {
        try {
            Optional<ChatConversation> opt = conversationRepository.findByConversationId(conversationId);
            if (opt.isPresent()) {
                ChatConversation conv = opt.get();
                ChatConversation.ConversationState dbState = new ChatConversation.ConversationState();
                dbState.setIntent(state.intent);
                dbState.setPartialData(new HashMap<>(state.partialData));
                conv.setState(dbState);
                conv.setUpdatedAt(Instant.now());
                conversationRepository.save(conv);
            }
        } catch (Exception e) {
            log.warn("[ORCHESTRATOR] Failed to persist conversation state: {}", e.getMessage());
        }
    }

    // ========== Intent Handlers ==========

    private ChatResponseDto handleBookingFlow(String convId, String userId, ConversationState state, String text) {
        String fromName = (String) state.partialData.get("fromName");
        String toName = (String) state.partialData.get("toName");
        String departAt = (String) state.partialData.get("departAt");
        Object passObj = state.partialData.get("passengers");
        Integer passengers = passObj instanceof Number ? ((Number) passObj).intValue() : null;

        // Support form submissions from Android as key=value pairs:
        // e.g. "route=从A到B, departAt=2026-02-10T11:30, passengers=2"
        if (text.contains("=")) {
            Map<String, String> kv = parseKeyValuePairs(text);

            // route -> from/to
            if ((fromName == null || toName == null) && kv.containsKey("route")) {
                String route = kv.get("route");
                String[] ft = extractFromTo(route);
                if (ft == null) ft = extractFromToLooser(route);

                if (ft != null) {
                    state.partialData.put("fromName", ft[0]);
                    state.partialData.put("toName", ft[1]);
                    fromName = ft[0];
                    toName = ft[1];
                }
            }
            // departAt
            if (departAt == null && kv.containsKey("departAt")) {
                String norm = normalizeDepartAt(kv.get("departAt"));
                if (norm != null) {
                    state.partialData.put("departAt", norm);
                    departAt = norm;
                }
            }
            // passengers
            if (passengers == null && kv.containsKey("passengers")) {
                Integer p = parsePassengers(kv.get("passengers"));
                if (p != null) {
                    state.partialData.put("passengers", p);
                    passengers = p;
                }
            }

            // Fallback: if key parsing is flaky, but user submitted a passengers field,
            // try parsing directly from full text (e.g. "passengers=2").
            if (passengers == null && text.toLowerCase(Locale.ROOT).contains("passengers")) {
                Integer p2 = parsePassengers(text);
                if (p2 != null) {
                    state.partialData.put("passengers", p2);
                    passengers = p2;
                }
            }
        }

        // Try to extract missing fields from the new message
        if (fromName == null || toName == null) {
            String[] ft = extractFromTo(text);
            if (ft == null) ft = extractFromToLooser(text);  // fallback: "A to B" without "from"
            if (ft != null) {
                state.partialData.put("fromName", ft[0]);
                state.partialData.put("toName", ft[1]);
                fromName = ft[0];
                toName = ft[1];
            }
        }

        if (passengers == null) {
            Integer p = extractPassengers(text);
            if (p != null) {
                state.partialData.put("passengers", p);
                passengers = p;
            }
        }

        if (departAt == null) {
            String dt = extractDepartAt(text);
            if (dt != null) {
                state.partialData.put("departAt", dt);
                departAt = dt;
            }
        }

        // Check if we have everything
        if (fromName != null && toName != null && departAt != null && passengers != null) {
            ChatBookingService.BookingResult result = bookingService.createBooking(
                    userId, fromName, toName, departAt, passengers);
            state.reset();
            return buildBookingCardResponse(convId, result, fromName, toName, departAt, passengers);
        }

        // Still missing fields
        return buildMissingFieldsResponse(convId, state);
    }

    private ChatResponseDto buildBookingCardResponse(String convId,
                                                      ChatBookingService.BookingResult result,
                                                      String fromName, String toName,
                                                      String departAt, int passengers) {
        Map<String, Object> cardPayload = new HashMap<>();
        cardPayload.put("bookingId", result.bookingId());
        cardPayload.put("fromName", fromName);
        cardPayload.put("toName", toName);
        cardPayload.put("departAt", departAt);
        cardPayload.put("passengers", passengers);
        cardPayload.put("status", "confirmed");
        if (result.tripId() != null) {
            cardPayload.put("tripId", result.tripId());
        }

        return new ChatResponseDto(convId, "Booking confirmed! 🎉")
                .withUiAction(new UiAction("BOOKING_CARD", cardPayload))
                .withSuggestions(List.of("📋 My Profile", "🚌 Bus Arrivals", "Back to Menu"));
    }

    private ChatResponseDto handleBusQuery(String convId, String text) {
        return handleBusQueryInternal(convId, text, 3);
    }

    /** "Show more" handler — re-queries the last stop and shows all results */
    private ChatResponseDto handleBusQueryExpanded(String convId, String text) {
        // Try to retrieve the last-queried stop from conversation state
        ConversationState state = conversations.get(convId);
        String lastStop = state != null ? (String) state.partialData.get("_lastBusStop") : null;
        String lastRoute = state != null ? (String) state.partialData.get("_lastBusRoute") : null;

        if (lastStop != null) {
            NusBusProvider.BusArrivalsResult result = busProvider.getArrivals(lastStop, lastRoute);
            if (result.arrivals() != null && !result.arrivals().isEmpty()) {
                return buildBusResultResponse(convId, result, result.arrivals().size());
            }
        }
        // Fallback: ask user to pick a stop again
        if (state != null) state.intent = "awaiting_bus_stop";
        return new ChatResponseDto(convId,
                "Which stop would you like to check? Pick one below:")
                .withSuggestions(BUS_STOP_OPTIONS);
    }

    private ChatResponseDto handleBusQueryInternal(String convId, String text, int maxShow) {
        // Strip common prefix keywords so we can extract the actual stop/route name
        String cleaned = text.replaceAll("(?i)(查询|查看|查|公交|到站|信息|时间|bus|arrival[s]?|stop|shuttle)", "").trim();

        // Step 1: Extract route name
        String route = null;
        Matcher routeMatcher = Pattern.compile("(?i)([A-Z]\\d[A-Z]?|[A-Z]{1,3})").matcher(text);
        while (routeMatcher.find()) {
            String candidate = routeMatcher.group(1).toUpperCase();
            if (NusBusProvider.isRouteName(candidate)) {
                route = candidate;
                break;
            }
        }

        // Step 2: Extract stop name
        String stop = null;
        Matcher codeMatcher = Pattern.compile("(?i)([A-Z][A-Z0-9\\-]{1,10})").matcher(text);
        while (codeMatcher.find()) {
            String candidate = codeMatcher.group(1).trim();
            if (NusBusProvider.isRouteName(candidate)) continue;
            if (candidate.matches("(?i)(bus|stop|arrival|shuttle|line|ETA)")) continue;
            stop = candidate;
            break;
        }

        if (stop == null) {
            Matcher cnMatcher = Pattern.compile("([\\u4e00-\\u9fa5]{2,10})(站)?").matcher(cleaned);
            while (cnMatcher.find()) {
                String candidate = cnMatcher.group(1).trim();
                boolean isKeyword = candidate.matches(".*(几分钟|时间|线路|下一班|到站|查询|公交|巴士|信息|分钟到).*");
                if (!isKeyword && candidate.length() >= 2) {
                    stop = candidate;
                    break;
                }
            }
        }

        log.info("[BUS_QUERY] Extracted: stop={}, route={} from text: {}", stop, route, text);

        NusBusProvider.BusArrivalsResult result = busProvider.getArrivals(stop, route);

        // Save the last queried stop/route for "Show more" functionality
        ConversationState state = conversations.get(convId);
        if (state != null) {
            state.partialData.put("_lastBusStop", stop != null ? stop : result.stopName());
            state.partialData.put("_lastBusRoute", route);
        }

        if (result.arrivals() == null || result.arrivals().isEmpty()) {
            return new ChatResponseDto(convId,
                    String.format("🚌 %s: No arrivals at the moment.\n\nThis may be outside operating hours. Try again later!", result.stopName()))
                    .withSuggestions(List.of("Try another stop", "🚌 Bus Arrivals", "Back to Menu"));
        }

        return buildBusResultResponse(convId, result, maxShow);
    }

    /** Build bus arrival result response with configurable max display count */
    private ChatResponseDto buildBusResultResponse(String convId, NusBusProvider.BusArrivalsResult result, int maxShow) {
        StringBuilder sb = new StringBuilder();
        sb.append("🚌 ").append(result.stopName()).append(" — Next arrivals:\n\n");

        int shown = 0;
        for (Map<String, Object> arrival : result.arrivals()) {
            if (shown >= maxShow) break;

            String statusStr = String.valueOf(arrival.get("status"));
            String statusIcon = switch (statusStr) {
                case "arriving" -> "🟢";
                case "on_time" -> "🔵";
                default -> "⏳";
            };

            sb.append(String.format("%s Route %s — %d min\n",
                    statusIcon, arrival.get("route"), arrival.get("etaMinutes")));
            shown++;
        }

        int total = result.arrivals().size();
        List<String> buttons = new ArrayList<>();
        if (total > shown) {
            sb.append(String.format("\n%d more services available", total - shown));
            buttons.add("Show more");
        }
        buttons.add("🚌 Change stop");
        buttons.add("Back to Menu");

        return new ChatResponseDto(convId, sb.toString().trim())
                .withSuggestions(buttons);
    }

    private ChatResponseDto handleProfileQuery(String convId, String userId) {
        if ("guest".equals(userId)) {
            return new ChatResponseDto(convId, "You're not logged in. Please sign in to view your profile.")
                    .withSuggestions(POST_ACTION);
        }

        Optional<User> userOpt = userRepository.findByUserid(userId);
        if (userOpt.isEmpty()) {
            return new ChatResponseDto(convId, "Account not found (userId=" + userId + ").")
                    .withSuggestions(POST_ACTION);
        }

        User user = userOpt.get();
        StringBuilder sb = new StringBuilder();
        sb.append("📋 Your Profile:\n\n");

        sb.append("👤 Nickname: ").append(nvl(user.getNickname(), "Not set")).append("\n");
        sb.append("📧 Email: ").append(nvl(user.getEmail(), "Not set")).append("\n");
        sb.append("📱 Phone: ").append(nvl(user.getPhone(), "Not set")).append("\n");
        sb.append("🏫 Faculty: ").append(nvl(user.getFaculty(), "Not set")).append("\n");
        sb.append("\n");

        // Stats
        User.Stats stats = user.getStats();
        if (stats != null) {
            sb.append("📊 Statistics:\n");
            sb.append("  • Total Trips: ").append(stats.getTotalTrips()).append("\n");
            sb.append("  • Total Distance: ").append(String.format("%.1f km", stats.getTotalDistance())).append("\n");
            sb.append("  • Green Travel Days: ").append(stats.getGreenDays()).append("\n");
            if (stats.getWeeklyRank() > 0)
                sb.append("  • Weekly Rank: #").append(stats.getWeeklyRank()).append("\n");
            sb.append("\n");
        }

        // Points & Carbon
        sb.append("🌿 Eco Impact:\n");
        sb.append("  • Carbon Saved: ").append(String.format("%.1f g", user.getTotalCarbon())).append("\n");
        sb.append("  • Current Points: ").append(user.getCurrentPoints()).append("\n");
        sb.append("  • Total Points: ").append(user.getTotalPoints()).append("\n");

        // VIP
        User.Vip vip = user.getVip();
        if (vip != null && vip.isActive()) {
            sb.append("\n⭐ VIP: ").append(nvl(vip.getPlan(), "active"));
            if (vip.getExpiryDate() != null)
                sb.append(" (expires: ").append(vip.getExpiryDate().toLocalDate()).append(")");
            sb.append("\n");
        }

        return new ChatResponseDto(convId, sb.toString())
                .withSuggestions(List.of("Update my nickname", "🎫 Book a Trip", "Back to Menu"));
    }

    /** Null-safe value: returns fallback if val is null or blank. */
    private static String nvl(String val, String fallback) {
        return (val != null && !val.isBlank()) ? val : fallback;
    }

    private ChatResponseDto handleUserUpdateRequest(String convId, String userId, String role,
                                                      ConversationState state, String text) {
        String targetUserId = extractUserId(text);
        if (targetUserId == null) targetUserId = userId;

        // Extract profile field patches from text
        ProfilePatch patch = extractProfilePatch(text);

        // Permission check: regular users cannot modify others
        if (!targetUserId.equals(userId) && "user".equals(role)) {
            return new ChatResponseDto(convId, "You don't have permission to modify another user's profile.")
                    .withSuggestions(List.of("📋 My Profile", "🎫 Book a Trip"));
        }

        // Cross-user modification needs confirmation
        if (!targetUserId.equals(userId)) {
            state.intent = "user_update";
            state.pendingUserUpdate = new PendingUserUpdate(targetUserId, patch);
            return new ChatResponseDto(convId, "You're modifying another user's profile. Please confirm to proceed.")
                    .withShowConfirm("Confirm profile update",
                            String.format("This will update user %s's profile. Reply \"confirm\" to continue.", targetUserId));
        }

        // Self-modification: execute immediately
        return executeUserUpdate(convId, userId, role, targetUserId, patch, state);
    }

    private ChatResponseDto handleUserUpdateConfirmation(String convId, String userId, String role,
                                                          ConversationState state) {
        PendingUserUpdate pending = state.pendingUserUpdate;
        ChatResponseDto response = executeUserUpdate(convId, userId, role,
                pending.targetUserId, pending.patch, state);
        state.reset();
        return response;
    }

    private ChatResponseDto executeUserUpdate(String convId, String actorUserId, String actorRole,
                                               String targetUserId, ProfilePatch patch, ConversationState state) {
        // Find user in MongoDB
        Optional<User> userOpt = userRepository.findByUserid(targetUserId);
        if (userOpt.isEmpty()) {
            return new ChatResponseDto(convId, "User not found.");
        }

        User user = userOpt.get();
        boolean updated = false;
        Map<String, Object> patchDetails = new HashMap<>();

        // Apply profile patches to actual EcoGo User model fields
        if (patch.nickname != null) {
            user.setNickname(patch.nickname);
            patchDetails.put("nickname", patch.nickname);
            updated = true;
        }
        if (patch.email != null) {
            user.setEmail(patch.email);
            patchDetails.put("email", patch.email);
            updated = true;
        }
        if (patch.phone != null) {
            user.setPhone(patch.phone);
            patchDetails.put("phone", patch.phone);
            updated = true;
        }
        if (patch.faculty != null) {
            user.setFaculty(patch.faculty);
            patchDetails.put("faculty", patch.faculty);
            updated = true;
        }

        if (updated) {
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("[ORCHESTRATOR] Updated user {} fields: {}", targetUserId, patchDetails.keySet());
        } else {
            return new ChatResponseDto(convId,
                    "No fields to update detected. Supported fields: nickname, email, phone, faculty.\n" +
                    "Example: update my nickname=NewName")
                    .withSuggestions(POST_ACTION);
        }

        // Create audit log if modifying another user
        String auditId = null;
        String notifId = null;
        if (!actorUserId.equals(targetUserId)) {
            auditId = auditLogService.createAuditLog(
                    actorUserId, "update_user", targetUserId, patchDetails);
            notifId = notificationService.createNotification(
                    targetUserId, "profile_updated", "Your profile has been updated",
                    String.format("%s(%s) modified your profile.", actorRole, actorUserId));
        }

        String responseText = auditId != null
                ? String.format("Profile updated (%s). Audit ID: %s. User %s notified (notification=%s).",
                patchDetails.keySet(), auditId, targetUserId, notifId)
                : String.format("Your profile has been updated (%s).", patchDetails.keySet());

        state.reset();
        return new ChatResponseDto(convId, responseText)
                .withSuggestions(POST_ACTION);
    }

    private ChatResponseDto handleToolCall(String convId, String userId, String role,
                                            ConversationState state, ModelClientService.ModelResult mr) {
        ModelClientService.ToolCall tc = mr.toolCall();
        Map<String, Object> args = tc.arguments();

        switch (tc.name()) {
            case "create_booking" -> {
                String fromName = getStringArg(args, "fromName");
                String toName = getStringArg(args, "toName");
                String departAt = getStringArg(args, "departAt");
                Object passObj = args.get("passengers");
                Integer passengers = passObj instanceof Number ? ((Number) passObj).intValue() : null;

                if (fromName == null || toName == null || departAt == null || passengers == null) {
                    state.intent = "booking";
                    if (fromName != null) state.partialData.put("fromName", fromName);
                    if (toName != null) state.partialData.put("toName", toName);
                    if (departAt != null) state.partialData.put("departAt", departAt);
                    if (passengers != null) state.partialData.put("passengers", passengers);
                    return buildMissingFieldsResponse(convId, state);
                }

                ChatBookingService.BookingResult result = bookingService.createBooking(
                        userId, fromName, toName, departAt, passengers);
                return buildBookingCardResponse(convId, result, fromName, toName, departAt, passengers);
            }

            case "get_bus_arrivals" -> {
                String stopName = getStringArg(args, "stopName");
                String routeArg = getStringArg(args, "route");
                NusBusProvider.BusArrivalsResult busResult = busProvider.getArrivals(stopName, routeArg);

                // Safe access: check if arrivals list is non-empty
                if (busResult.arrivals() == null || busResult.arrivals().isEmpty()) {
                    String fallback = mr.text() != null && !mr.text().isBlank()
                            ? mr.text() : String.format("%s: No arrivals found.", busResult.stopName());
                    return new ChatResponseDto(convId, fallback)
                            .withSuggestions(List.of("🎫 Book a Trip", "📋 My Profile"));
                }

                StringBuilder busSb = new StringBuilder();
                busSb.append("🚌 ").append(busResult.stopName()).append(" — Arrivals:\n\n");
                for (Map<String, Object> arrival : busResult.arrivals()) {
                    String statusIcon;
                    String statusStr = String.valueOf(arrival.get("status"));
                    switch (statusStr) {
                        case "arriving" -> statusIcon = "🟢 Arriving";
                        case "delayed" -> statusIcon = "🟡 Delayed";
                        default -> statusIcon = "🔵 On time";
                    }
                    busSb.append(String.format("  Route %s — %s min  %s\n",
                            arrival.get("route"), arrival.get("etaMinutes"), statusIcon));
                }
                busSb.append(String.format("\n%d services total.", busResult.arrivals().size()));

                return new ChatResponseDto(convId, busSb.toString())
                        .withSuggestions(List.of("🎫 Book a Trip", "📋 My Profile"));
            }

            case "update_user" -> {
                String targetUserId = getStringArg(args, "userId");
                if (targetUserId == null) targetUserId = userId;
                @SuppressWarnings("unchecked")
                Map<String, Object> patchMap = (Map<String, Object>) args.getOrDefault("patch", Map.of());

                ProfilePatch patch = new ProfilePatch();
                if (patchMap.containsKey("nickname")) patch.nickname = String.valueOf(patchMap.get("nickname"));
                if (patchMap.containsKey("email")) patch.email = String.valueOf(patchMap.get("email"));
                if (patchMap.containsKey("phone")) patch.phone = String.valueOf(patchMap.get("phone"));
                if (patchMap.containsKey("faculty")) patch.faculty = String.valueOf(patchMap.get("faculty"));

                if (!targetUserId.equals(userId) && "user".equals(role)) {
                    return new ChatResponseDto(convId, "You don't have permission to modify another user's profile.")
                            .withSuggestions(List.of("📋 My Profile", "🎫 Book a Trip"));
                }

                if (!targetUserId.equals(userId)) {
                    state.intent = "user_update";
                    state.pendingUserUpdate = new PendingUserUpdate(targetUserId, patch);
                    return new ChatResponseDto(convId, "You're modifying another user's profile. Please confirm to proceed.")
                            .withShowConfirm("Confirm profile update",
                                    String.format("This will update user %s's profile. Reply \"confirm\" to continue.", targetUserId));
                }

                return executeUserUpdate(convId, userId, role, targetUserId, patch, state);
            }

            default -> {
                String fallbackText = mr.text() != null && !mr.text().isBlank() ? mr.text() : "This operation is not supported yet.";
                return new ChatResponseDto(convId, fallbackText)
                        .withSuggestions(POST_ACTION);
            }
        }
    }

    private ChatResponseDto handleRagQuery(String convId, String text) {
        List<Citation> citations;

        if (ragService.isAvailable()) {
            try {
                // 只取最相关的 1 条，避免信息轰炸
                citations = ragService.retrieve(text, 1);
            } catch (Exception e) {
                log.warn("[RAG] Retrieval failed: {}", e.getMessage());
                citations = Collections.emptyList();
            }
        } else {
            citations = Collections.emptyList();
        }

        String answer;
        if (!citations.isEmpty()) {
            Citation top = citations.get(0);
            // 截取前120字符作为精简回答
            String snippet = top.getSnippet();
            if (snippet.length() > 120) {
                snippet = snippet.substring(0, 120) + "...";
            }
            answer = snippet;
        } else {
            answer = "I'm not sure about that. Try rephrasing or explore the options below:";
            citations = Collections.emptyList();
        }

        ChatResponseDto response = new ChatResponseDto();
        response.setConversationId(convId);
        response.setAssistant(new AssistantMessage(answer, citations));
        response.setServerTimestamp(Instant.now());
        response.withSuggestions(MAIN_MENU);
        return response;
    }

    // ========== Building missing-fields form response ==========

    private ChatResponseDto buildMissingFieldsResponse(String convId, ConversationState state) {
        // Progressive information collection
        String fromName = (String) state.partialData.get("fromName");
        String toName = (String) state.partialData.get("toName");
        String departAt = (String) state.partialData.get("departAt");
        Object passObj = state.partialData.get("passengers");

        // Missing route → ask for route first
        if (fromName == null || toName == null) {
            return new ChatResponseDto(convId,
                    "Let me help you book a trip 🎫\n\nWhere would you like to go?")
                    .withSuggestions(List.of("NUS to Marina Bay", "PGP to Changi Airport", "Back to Menu"));
        }

        // Have route, missing time → ask for time
        if (departAt == null) {
            List<Map<String, Object>> fields = new ArrayList<>();
            fields.add(Map.of("key", "departAt", "label", "Departure Time", "type", "datetime", "required", true));
            if (passObj == null) {
                fields.add(Map.of("key", "passengers", "label", "Passengers (1-8)", "type", "int", "required", true, "min", 1, "max", 8));
            }
            return new ChatResponseDto(convId,
                    String.format("**%s** to **%s** — got it!\n\nPlease select departure time and passengers:", fromName, toName))
                    .withShowForm("booking_missing_fields", "Complete Booking", fields);
        }

        // Have route and time, missing passengers
        if (passObj == null) {
            return new ChatResponseDto(convId,
                    String.format("%s to %s, departing %s\n\nHow many passengers?", fromName, toName, departAt))
                    .withSuggestions(List.of("1 person", "2 people", "3 people", "4 people"));
        }

        // Fallback
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(Map.of("key", "route", "label", "Route (A to B)", "type", "text", "required", true));
        fields.add(Map.of("key", "departAt", "label", "Departure Time", "type", "datetime", "required", true));
        fields.add(Map.of("key", "passengers", "label", "Passengers (1-8)", "type", "int", "required", true, "min", 1, "max", 8));
        return new ChatResponseDto(convId, "Please provide the following to complete your booking:")
                .withShowForm("booking_missing_fields", "Complete Booking", fields);
    }

    // ========== Text Extraction Utilities ==========

    private String[] extractFromTo(String text) {
        // Chinese: 从A到B / 从A去B
        Pattern cnP = Pattern.compile("从(?<from>[^到去，,。]{1,20})(到|去)(?<to>[^，,。]{1,20})");
        Matcher cnM = cnP.matcher(text);
        if (cnM.find()) {
            return new String[]{cnM.group("from").trim(), cnM.group("to").trim()};
        }
        // English: "from A to B" (case-insensitive)
        Pattern enP = Pattern.compile("(?i)from\\s+(?<from>[^,.]{1,30})\\s+to\\s+(?<to>[^,.\n]{1,30})");
        Matcher enM = enP.matcher(text);
        if (enM.find()) {
            return new String[]{enM.group("from").trim(), enM.group("to").trim()};
        }
        return null;
    }

    /**
     * Looser route parsing for form input like "A到B" / "A->B" / "A to B" / "NUS to Marina Bay".
     * Supports multi-word place names by allowing spaces within capture groups.
     */
    private String[] extractFromToLooser(String text) {
        if (text == null) return null;
        String t = text.trim();

        // Pattern 1: English "to" separator (spaces required to avoid matching "to" inside words like "UTown")
        Matcher enM = Pattern.compile("(?<from>.+?)\\s+to\\s+(?<to>.+)", Pattern.CASE_INSENSITIVE).matcher(t);
        if (enM.find()) {
            String from = enM.group("from").trim();
            String to = enM.group("to").trim();
            if (!from.isEmpty() && !to.isEmpty()) return new String[]{from, to};
        }

        // Pattern 2: Chinese/symbol separators (到, ->, →, —)
        Matcher symM = Pattern.compile("(?<from>[^，,。]{1,30}?)\\s*(到|->|→|—)\\s*(?<to>[^，,。]{1,30})",
                Pattern.CASE_INSENSITIVE).matcher(t);
        if (symM.find()) {
            String from = symM.group("from").trim();
            String to = symM.group("to").trim();
            if (!from.isEmpty() && !to.isEmpty()) return new String[]{from, to};
        }

        return null;
    }

    private Integer extractPassengers(String text) {
        // Chinese: 2人 / 2位
        Pattern cnP = Pattern.compile("(?<n>[1-8])\\s*(人|位)");
        Matcher cnM = cnP.matcher(text);
        if (cnM.find()) return Integer.parseInt(cnM.group("n"));
        // English: 2 people / 1 person / 2 passengers
        Pattern enP = Pattern.compile("(?i)(?<n>[1-8])\\s*(people|person|passenger[s]?)");
        Matcher enM = enP.matcher(text);
        if (enM.find()) return Integer.parseInt(enM.group("n"));
        return null;
    }

    private String extractDepartAt(String text) {
        Pattern p = Pattern.compile("(\\d{4}[-/]\\d{2}[-/]\\d{2}[T\\s]\\d{2}:\\d{2}(?::\\d{2})?(?:Z|[+-]\\d{2}:\\d{2})?)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return normalizeDepartAt(m.group(1));
        }
        return null;
    }

    private Integer parsePassengers(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        // Accept "2", "2人", "2位"
        // Also accept full-width digits.
        s = s.replace('１','1').replace('２','2').replace('３','3').replace('４','4')
             .replace('５','5').replace('６','6').replace('７','7').replace('８','8').replace('９','9').replace('０','0');
        Matcher m = Pattern.compile("([1-8])").matcher(s);
        if (m.find()) {
            int n = Integer.parseInt(m.group(1));
            return (n >= 1 && n <= 8) ? n : null;
        }
        return null;
    }

    /**
     * Normalize various date-time inputs to a consistent ISO-ish format:
     * - Replace '/' with '-'
     * - Replace space with 'T'
     * - If seconds missing, add ":00"
     * Timezone is optional; we keep as provided.
     */
    private String normalizeDepartAt(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        s = s.replace('/', '-');
        if (s.contains(" ") && !s.contains("T")) {
            s = s.replace(' ', 'T');
        }
        // If only yyyy-MM-ddTHH:mm, append seconds
        if (s.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$")) {
            s = s + ":00";
        }
        return s;
    }

    /**
     * Parse "k=v, a=b" into a map. Keys are trimmed and case-sensitive.
     * Values are trimmed; surrounding quotes are removed.
     */
    private Map<String, String> parseKeyValuePairs(String text) {
        Map<String, String> out = new HashMap<>();
        if (text == null) return out;
        // Split by comma/Chinese comma/newlines to be safe with IME.
        String[] parts = text.split("[,，\\n\\r]\\s*");
        for (String part : parts) {
            int idx = part.indexOf('=');
            if (idx <= 0) continue;
            String key = part.substring(0, idx).trim();
            String val = part.substring(idx + 1).trim();
            if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                val = val.substring(1, val.length() - 1);
            }
            if (!key.isEmpty()) out.put(key, val);
        }
        return out;
    }

    private String extractUserId(String text) {
        Pattern p = Pattern.compile("\\b(u_\\d{3,})\\b");
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Extract profile field patches from free-form text.
     * Supports patterns like: nickname=xxx, email=xxx, phone=xxx, faculty=xxx
     */
    private ProfilePatch extractProfilePatch(String text) {
        ProfilePatch patch = new ProfilePatch();

        Pattern nicknameP = Pattern.compile("nickname\\s*=\\s*(?<val>[^，,。\\s]{1,30})");
        Matcher nicknameM = nicknameP.matcher(text);
        if (nicknameM.find()) patch.nickname = nicknameM.group("val");

        Pattern emailP = Pattern.compile("email\\s*=\\s*(?<val>[^\\s，,。]{1,50})");
        Matcher emailM = emailP.matcher(text);
        if (emailM.find()) patch.email = emailM.group("val");

        Pattern phoneP = Pattern.compile("phone\\s*=\\s*(?<val>[^\\s，,。]{1,20})");
        Matcher phoneM = phoneP.matcher(text);
        if (phoneM.find()) patch.phone = phoneM.group("val");

        Pattern facultyP = Pattern.compile("faculty\\s*=\\s*(?<val>[^，,。\\s]{1,30})");
        Matcher facultyM = facultyP.matcher(text);
        if (facultyM.find()) patch.faculty = facultyM.group("val");

        return patch;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private boolean isResetCommand(String text) {
        String lower = text.toLowerCase().trim();
        return Set.of("取消", "重新开始", "cancel", "reset", "重置", "back to menu").contains(lower);
    }

    private boolean isConfirmCommand(String text) {
        String lower = text.toLowerCase().trim();
        return Set.of("确认", "好的确认", "确认修改", "confirm", "yes", "ok").contains(lower);
    }

    private String getStringArg(Map<String, Object> args, String key) {
        Object val = args.get(key);
        if (val == null) return null;
        String s = String.valueOf(val);
        return s.isBlank() ? null : s;
    }

    // ========== Internal state classes ==========

    private static class ConversationState {
        String intent;
        Map<String, Object> partialData = new HashMap<>();
        PendingUserUpdate pendingUserUpdate;
        long lastAccessedAt = System.currentTimeMillis();

        void reset() {
            intent = null;
            partialData.clear();
            pendingUserUpdate = null;
        }
    }

    /**
     * Remove expired conversation states from the in-memory cache.
     * This prevents memory leaks from abandoned conversations.
     */
    private void cleanupExpiredConversations() {
        long now = System.currentTimeMillis();
        conversations.entrySet().removeIf(entry ->
                (now - entry.getValue().lastAccessedAt) > CONVERSATION_TTL_MS);
    }

    /**
     * Profile patch data: maps to real EcoGo User model fields.
     */
    private static class ProfilePatch {
        String nickname;
        String email;
        String phone;
        String faculty;

        boolean hasAnyField() {
            return nickname != null || email != null || phone != null || faculty != null;
        }
    }

    private record PendingUserUpdate(String targetUserId, ProfilePatch patch) {}
}
