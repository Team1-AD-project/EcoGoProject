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

    // =========================
    // Sonar: duplicated literals -> constants (S1192)
    // =========================
    private static final long CONVERSATION_TTL_MS = 30 * 60 * 1000L; // 30 minutes

    // intents
    private static final String INTENT_BOOKING = "booking";
    private static final String INTENT_USER_UPDATE = "user_update";
    private static final String INTENT_AWAITING_BUS_STOP = "awaiting_bus_stop";
    private static final String INTENT_AWAITING_DESTINATION = "awaiting_destination";

    // common button labels
    private static final String BTN_BUS_ARRIVALS = "🚌 Bus Arrivals";
    private static final String BTN_TRAVEL_ADVICE = "📍 Travel Advice";
    private static final String BTN_BOOK_A_TRIP = "🎫 Book a Trip";
    private static final String BTN_MY_PROFILE = "📋 My Profile";

    private static final String BTN_BACK_TO_MENU = "Back to Menu";
    private static final String BTN_SHOW_MORE = "Show more";
    private static final String BTN_TRY_ANOTHER_STOP = "Try another stop";
    private static final String BTN_CHANGE_STOP = "🚌 Change stop";

    private static final String TXT_NOT_SET = "Not set";

    // role
    private static final String ROLE_USER = "user";
    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_ASSISTANT = "assistant";

    // booking form constants
    private static final String FORM_BOOKING_MISSING_FIELDS = "booking_missing_fields";
    private static final String FORM_TITLE_COMPLETE_BOOKING = "Complete Booking";

    // keys used in partialData / forms / tool args
    private static final String KEY_ROUTE = "route";
    private static final String KEY_DEPART_AT = "departAt";
    private static final String KEY_PASSENGERS = "passengers";
    private static final String KEY_FROM_NAME = "fromName";
    private static final String KEY_TO_NAME = "toName";
    private static final String KEY_STATUS = "status";
    // profile field keys (avoid duplicated literals)
    private static final String PROFILE_KEY_NICKNAME = "nickname";
    private static final String PROFILE_KEY_EMAIL = "email";
    private static final String PROFILE_KEY_PHONE = "phone";
    private static final String PROFILE_KEY_FACULTY = "faculty";

    // bus cache keys
    private static final String KEY_LAST_BUS_STOP = "_lastBusStop";
    private static final String KEY_LAST_BUS_ROUTE = "_lastBusRoute";

    // switches / tool names
    private static final String TOOL_CREATE_BOOKING = "create_booking";
    private static final String TOOL_GET_BUS_ARRIVALS = "get_bus_arrivals";
    private static final String TOOL_UPDATE_USER = "update_user";

    // status literals
    private static final String BUS_STATUS_ARRIVING = "arriving";
    private static final String BUS_STATUS_ON_TIME = "on_time";
    private static final String BUS_STATUS_DELAYED = "delayed";

    // main menus
    private static final List<String> MAIN_MENU = List.of(BTN_BUS_ARRIVALS, BTN_TRAVEL_ADVICE, BTN_BOOK_A_TRIP, BTN_MY_PROFILE);
    private static final List<String> POST_ACTION = List.of(BTN_BUS_ARRIVALS, BTN_TRAVEL_ADVICE, BTN_BOOK_A_TRIP);
    private static final String STOP_COM3 = "COM3";
    private static final List<String> BUS_STOP_OPTIONS = List.of(STOP_COM3, "UTOWN", "KR-MRT", "PGP", "CLB", "BIZ2");

    // common suggestion literals
    private static final String SUGGEST_COM3_TO_UTOWN = "COM3 to UTown";
    private static final String SUGGEST_PGP_TO_CLB = "PGP to CLB";

    // =========================
    // Dependencies
    // =========================
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

    // =========================
    // Entry
    // =========================
    public ChatResponseDto handleChat(String userId, boolean isAdmin, String conversationId, String messageText) {
        String convId = ensureConversationId(conversationId);
        String text = safeTrim(messageText);
        String role = isAdmin ? ROLE_ADMIN : ROLE_USER;

        log.info("[ORCHESTRATOR] User: {}, isAdmin: {}, Message: {}", userId, isAdmin, text);

        ConversationState state = getOrCreateState(convId, userId);
        log.info("[ORCHESTRATOR] ConversationId: {}, State.intent: {}", convId, state.intent);

        // Persist user message
        persistMessage(convId, userId, ROLE_USER, text);

        // Route message
        ChatResponseDto response = routeMessage(convId, userId, role, state, text);

        // Persist assistant + state
        persistAssistantAndState(convId, userId, response, state);

        return response;
    }

    private String ensureConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return "c_" + System.currentTimeMillis();
        }
        return conversationId;
    }

    private String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private void persistAssistantAndState(String convId, String userId, ChatResponseDto response, ConversationState state) {
        String assistantText = response.getAssistant() != null ? response.getAssistant().getText() : "";
        persistMessage(convId, userId, ROLE_ASSISTANT, assistantText);
        persistState(convId, userId, state);
    }

    // =========================
    // Router (split to reduce Cognitive Complexity - S3776)
    // =========================
    private ChatResponseDto routeMessage(String convId, String userId, String role, ConversationState state, String text) {

        if (isResetCommand(text)) {
            state.reset();
            return buildMainMenu(convId, "Session reset. How can I help you?");
        }

        if (isPendingUserUpdateConfirm(text, state)) {
            return handleUserUpdateConfirmation(convId, userId, role, state);
        }

        if (INTENT_BOOKING.equals(state.intent)) {
            return handleBookingFlow(convId, userId, state, text);
        }

        if (INTENT_AWAITING_BUS_STOP.equals(state.intent)) {
            return handleBusQueryWithStop(convId, state, text);
        }

        if (INTENT_AWAITING_DESTINATION.equals(state.intent)) {
            return handleRecommendWithDestination(convId, state, text);
        }

        if (isQuickBusAction(text)) {
            return handleQuickBusAction(convId, state, text);
        }

        if (modelClientService.isEnabled()) {
            return handleModelOrFallback(convId, userId, role, state, text);
        }

        return tryPythonProxyOrKeyword(convId, userId, role, state, text);
    }

    private boolean isPendingUserUpdateConfirm(String text, ConversationState state) {
        return isConfirmCommand(text)
                && INTENT_USER_UPDATE.equals(state.intent)
                && state.pendingUserUpdate != null;
    }

    private boolean isQuickBusAction(String text) {
        return isShowMoreCommand(text) || isChangeStopCommand(text);
    }

    private boolean isShowMoreCommand(String text) {
        return text.equalsIgnoreCase(BTN_SHOW_MORE) || text.equalsIgnoreCase("查看更多班次");
    }

    private boolean isChangeStopCommand(String text) {
        return text.contains("Change stop")
                || text.contains("换个站点")
                || text.equalsIgnoreCase(BTN_TRY_ANOTHER_STOP)
                || text.equalsIgnoreCase("换个站点查询");
    }

    private ChatResponseDto handleQuickBusAction(String convId, ConversationState state, String text) {
        if (isShowMoreCommand(text)) {
            return handleBusQueryExpanded(convId, text);
        }
        // change stop / try another stop
        return buildBusStopPrompt(convId, state);
    }

    private ChatResponseDto handleModelOrFallback(String convId, String userId, String role, ConversationState state, String text) {
        ModelClientService.ModelResult mr = modelClientService.callModelForTool(text);

        if (mr != null && mr.toolCall() != null) {
            return handleToolCall(convId, userId, role, state, mr);
        }

        if (mr != null && mr.text() != null && !mr.text().isBlank()) {
            return new ChatResponseDto(convId, mr.text()).withSuggestions(POST_ACTION);
        }

        return tryPythonProxyOrKeyword(convId, userId, role, state, text);
    }

    // =========================
    // Python proxy fallback
    // =========================
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

    // =========================
    // Keyword / RAG router (split to reduce complexity)
    // =========================
    private ChatResponseDto handleKeywordOrRag(String convId, String userId, String role,
                                              ConversationState state, String text) {

        if (isBlankOrEmoji(text)) {
            return buildMainMenu(convId, "How can I help you?");
        }

        if (isGreeting(text)) {
            return buildMainMenu(convId, "Hi there! I'm EcoGo Assistant 😊\nHow can I help you today?");
        }

        ChatResponseDto menuHit = tryHandleMenuButtons(convId, userId, state, text);
        if (menuHit != null) return menuHit;

        ChatResponseDto followUpHit = tryHandleFollowUpButtons(convId, state, text);
        if (followUpHit != null) return followUpHit;

        ChatResponseDto bookingHit = tryHandleBookingIntent(convId, state, text);
        if (bookingHit != null) return bookingHit;

        if (isBusQueryIntent(text)) {
            return handleBusQuery(convId, text);
        }

        if (isProfileQuery(text)) {
            return handleProfileQuery(convId, userId);
        }

        if (isUserUpdateIntent(text)) {
            return handleUserUpdateRequest(convId, userId, role, state, text);
        }

        if (isRecommendationIntent(text)) {
            return handleRecommendation(convId, text);
        }

        return handleRagQuery(convId, text);
    }

    private boolean isBlankOrEmoji(String text) {
        return text.isBlank() || text.matches("[\\s\\p{So}\\p{Cn}]+");
    }

    private ChatResponseDto tryHandleMenuButtons(String convId, String userId, ConversationState state, String text) {
        if (text.contains("Bus Arrivals") || text.contains("查公交") || text.equals(BTN_BUS_ARRIVALS)) {
            return buildBusStopPrompt(convId, state);
        }
        if (text.contains("Travel Advice") || text.contains("出行推荐") || text.equals(BTN_TRAVEL_ADVICE)) {
            return buildRecommendPrompt(convId, state);
        }
        if (text.contains("Book a Trip") || text.contains("预订行程") || text.equals(BTN_BOOK_A_TRIP)) {
            state.intent = INTENT_BOOKING;
            return new ChatResponseDto(convId,
                    "Sure, let me help you book a trip 🎫\n\nWhere would you like to go?\ne.g. from NUS to Changi Airport")
                    .withSuggestions(List.of("NUS to Marina Bay", "PGP to UTown", BTN_BACK_TO_MENU));
        }
        if (text.contains("My Profile") || text.contains("我的资料") || text.equals(BTN_MY_PROFILE)) {
            return handleProfileQuery(convId, userId);
        }
        if (isBackToMenu(text)) {
            state.reset();
            return buildMainMenu(convId, "Sure! What else can I help you with?");
        }
        return null;
    }

    private boolean isBackToMenu(String text) {
        return text.equalsIgnoreCase(BTN_BACK_TO_MENU) || text.equals("返回主菜单") || text.equals("主菜单")
                || text.equalsIgnoreCase("menu") || text.equalsIgnoreCase("help")
                || text.equalsIgnoreCase("start") || text.equals("帮助") || text.equals("菜单");
    }

    private ChatResponseDto tryHandleFollowUpButtons(String convId, ConversationState state, String text) {
        if (isShowMoreCommand(text)) {
            return handleBusQueryExpanded(convId, text);
        }
        if (isChangeStopCommand(text)) {
            return buildBusStopPrompt(convId, state);
        }

        // "Book X to Y" buttons from recommendation follow-ups
        if (isBookRouteButton(text)) {
            return handleBookRouteButton(convId, state, text);
        }

        return null;
    }

    private boolean isBookRouteButton(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("book ") && !lower.contains("book a trip");
    }

    private ChatResponseDto handleBookRouteButton(String convId, ConversationState state, String text) {
        String routePart = text.replaceAll("(?i).*\\bbook\\s+", "").trim();
        routePart = routePart.replaceAll("^[\\p{So}\\p{Cn}\\s]+", "").trim();
        String[] ft = extractFromToLooser(routePart);
        if (ft == null) return null;

        state.intent = INTENT_BOOKING;
        state.partialData.put(KEY_FROM_NAME, ft[0]);
        state.partialData.put(KEY_TO_NAME, ft[1]);
        return buildMissingFieldsResponse(convId, state);
    }

    private ChatResponseDto tryHandleBookingIntent(String convId, ConversationState state, String text) {
        if (!isBookingIntent(text)) return null;

        state.intent = INTENT_BOOKING;

        String[] fromTo = extractFromTo(text);
        if (fromTo != null) {
            state.partialData.put(KEY_FROM_NAME, fromTo[0]);
            state.partialData.put(KEY_TO_NAME, fromTo[1]);
        }

        Integer passengers = extractPassengers(text);
        if (passengers != null) {
            state.partialData.put(KEY_PASSENGERS, passengers);
        }

        return buildMissingFieldsResponse(convId, state);
    }

    private boolean isProfileQuery(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        boolean en = containsAny(lower, "my profile", "my info", "my account", "view profile");
        boolean zh = (containsAny(text, "查询", "查看", "看看", "我的资料", "个人信息", "我的信息")
                && !containsAny(text, "修改", "更新", "改"));
        return en || zh;
    }

    private boolean isUserUpdateIntent(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        boolean zh = (containsAny(text, "修改", "更新", "资料", "用户") && (text.contains("u_") || text.contains("我的")));
        boolean en = (containsAny(lower, "update my", "change my", "edit my")
                && containsAny(lower, "profile", PROFILE_KEY_NICKNAME, PROFILE_KEY_EMAIL, PROFILE_KEY_PHONE, PROFILE_KEY_FACULTY));
        return zh || en;
    }

    // =========================
    // Greeting + menus
    // =========================
    private boolean isGreeting(String text) {
        String lower = text.toLowerCase().trim();
        return Set.of("你好", "hi", "hello", "嗨", "hey", "早上好", "下午好", "晚上好",
                "在吗", "在不在", "有人吗", "good morning", "good afternoon", "good evening",
                "hi there", "hey there", "howdy", "greetings").contains(lower)
                || lower.matches("^(你好|hi|hello|嗨|hey|howdy).*");
    }

    private ChatResponseDto buildMainMenu(String convId, String greeting) {
        return new ChatResponseDto(convId, greeting).withSuggestions(MAIN_MENU);
    }

    private ChatResponseDto buildBusStopPrompt(String convId, ConversationState state) {
        state.intent = INTENT_AWAITING_BUS_STOP;
        return new ChatResponseDto(convId,
                "Sure, let me check bus arrivals 🚌\n\nWhich stop would you like to check? Type a stop name or pick one below:")
                .withSuggestions(BUS_STOP_OPTIONS);
    }

    private ChatResponseDto handleBusQueryWithStop(String convId, ConversationState state, String text) {
        state.reset();
        return handleBusQuery(convId, text);
    }

    private ChatResponseDto buildRecommendPrompt(String convId, ConversationState state) {
        state.intent = INTENT_AWAITING_DESTINATION;
        return new ChatResponseDto(convId,
                "Sure, let me recommend a travel option 📍\n\nWhere would you like to go?\ne.g. COM3 to UTown")
                .withSuggestions(List.of(SUGGEST_COM3_TO_UTOWN, SUGGEST_PGP_TO_CLB, "KR MRT to BIZ2", BTN_BACK_TO_MENU));
    }

    private ChatResponseDto handleRecommendWithDestination(String convId, ConversationState state, String text) {
        state.reset();

        String[] fromTo = extractFromTo(text);
        if (fromTo == null) fromTo = extractFromToLooser(text);

        if (fromTo != null) {
            return buildSmartRecommendation(convId, fromTo[0], fromTo[1]);
        }

        String dest = text.trim();
        if (!dest.isEmpty() && dest.length() < 30) {
            return buildSmartRecommendation(convId, null, dest);
        }

        return new ChatResponseDto(convId,
                "Sorry, I couldn't identify the origin and destination.\nPlease use the format: A to B, e.g. COM3 to UTown")
                .withSuggestions(List.of(SUGGEST_COM3_TO_UTOWN, SUGGEST_PGP_TO_CLB, BTN_BACK_TO_MENU));
    }

    private ChatResponseDto buildSmartRecommendation(String convId, String from, String to) {
        String queryText = from != null ? from + " to " + to : "to " + to;

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
            } catch (Exception ignored) {
                // ignore
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📍 Travel advice for ").append(queryText).append(":\n\n");

        boolean isNusCampus = isNusCampusLocation(from) || isNusCampusLocation(to);
        appendTravelAdvice(sb, isNusCampus);

        sb.append(ragTip);

        ChatResponseDto response = new ChatResponseDto();
        response.setConversationId(convId);
        response.setAssistant(new AssistantMessage(sb.toString().trim(), citations));
        response.setServerTimestamp(Instant.now());

        response.withSuggestions(buildRecommendFollowUps(isNusCampus, from, queryText));
        return response;
    }

    private void appendTravelAdvice(StringBuilder sb, boolean isNusCampus) {
        if (isNusCampus) {
            sb.append("🚌 **Campus Shuttle**: Take the free NUS shuttle bus — check real-time arrivals\n");
            sb.append("🚶 **Walk**: Most campus routes are 10-15 min on foot — zero emissions!\n");
            sb.append("🚇 **MRT Link**: For off-campus destinations, connect at KR MRT station\n");
            return;
        }
        sb.append("🚇 **MRT**: Fastest and low-carbon option\n");
        sb.append("🚌 **Public Bus**: Wide coverage, affordable fares\n");
        sb.append("🚶 **Walk / Cycle**: Best for short distances — zero emissions\n");
    }

    private List<String> buildRecommendFollowUps(boolean isNusCampus, String from, String queryText) {
        List<String> followUp = new ArrayList<>();
        if (isNusCampus) followUp.add(BTN_BUS_ARRIVALS);
        if (from != null) followUp.add("🎫 Book " + queryText);
        followUp.add(BTN_BACK_TO_MENU);
        return followUp;
    }

    private boolean isNusCampusLocation(String location) {
        if (location == null) return false;
        String lower = location.toLowerCase(Locale.ROOT);
        return Set.of("com3", "com2", "pgp", "pgpr", "utown", "clb", "yih", "biz2",
                "kr-mrt", "kr mrt", "it", "museum", "raffles", "kv", "lt13", "lt27",
                "as5", "s17", "uhc", "uhall", "krb", "tcoms", "nuss", "nus").contains(lower)
                || lower.contains("nus") || lower.contains("utown");
    }

    // =========================
    // Intent detection
    // =========================
    private boolean isBookingIntent(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (containsBookingKeywords(text)) return true;
        if (containsBookPhrases(lower)) return true;
        if (containsExplicitFromTo(text)) return true;
        return containsBookAndContext(lower);
    }

    private boolean containsBookingKeywords(String text) {
        return containsAny(text, "预订", "预定", "订票", "帮我订", "我要订", "我想订");
    }

    private boolean containsBookPhrases(String lower) {
        return lower.contains("book a trip") || lower.contains("book trip") || lower.contains("book a ride")
                || lower.contains("i want to book") || lower.contains("i'd like to book");
    }

    private boolean containsExplicitFromTo(String text) {
        if (text.contains("从") && (text.contains("到") || text.contains("去"))) {
            return extractFromTo(text) != null;
        }
        return false;
    }

    private boolean containsBookAndContext(String lower) {
        return lower.contains("book") && containsAny(lower, "trip", "ride", "travel", "行程", "路线");
    }

    private boolean isBusQueryIntent(String text) {
        String lower = text.toLowerCase(Locale.ROOT);

        // Exclude informational queries about routes/stops (should go to RAG)
        if (containsAny(lower, "what stop", "which stop", "what route", "which route",
                "tell me about", "what does", "where does", "go to", "goes to",
                "stops at", "route info", "route detail", "about the")) {
            return false;
        }

        if (containsArrivalKeywords(lower)) return true;
        if (containsBusPhrases(lower)) return true;
        if (mentionsShuttleWithTime(text)) return true;
        if (matchesRouteWithContext(text)) return true;
        if (shortChineseBusQuery(text)) return true;

        return false;
    }

    private boolean containsArrivalKeywords(String lower) {
        return lower.contains("到站时间") || lower.contains("到站信息") || lower.contains("公交到站") || lower.contains("公交查询") || lower.contains("下一班") || lower.contains("几分钟到");
    }

    private boolean containsBusPhrases(String lower) {
        return lower.contains("bus arrival") || lower.contains("next bus") || lower.contains("bus schedule")
                || (lower.contains("when is the") && lower.contains("bus"))
                || lower.contains("check bus") || lower.contains("bus eta") || lower.contains("bus time");
    }

    private boolean mentionsShuttleWithTime(String text) {
        return text.toLowerCase(Locale.ROOT).contains("shuttle") && containsAny(text, "when", "time", "到站", "arrival", "next");
    }

    private boolean matchesRouteWithContext(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.matches(".*[a-z]\\d.*") && containsAny(text, "线", "路", "到站", "公交", "bus")) {
            Matcher rm = Pattern.compile("(?i)([A-Z]\\d[A-Z]?)").matcher(text);
            if (rm.find() && NusBusProvider.isRouteName(rm.group(1))) return true;
        }
        return false;
    }

    private boolean shortChineseBusQuery(String text) {
        return containsAny(text, "查询", "查看", "查") && containsAny(text, "公交", "巴士", "shuttle") && text.length() < 30;
    }

    private boolean isRecommendationIntent(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (containsAny(text, "推荐", "建议一下", "suggest", "recommend")) return true;
        if (containsAny(text, "怎么去", "怎样去", "如何去")) return true;
        if (lower.contains("how to get") || lower.contains("how do i get") || lower.contains("best way to")) return true;
        if (lower.contains("best route") || containsAny(text, "最佳路线", "最好的路线")) return true;
        if (lower.contains("travel advice") || lower.contains("travel tip")) return true;

        if (containsAny(text, "出行方式", "交通方式")) {
            return text.length() < 15 && !containsAny(text, "哪些", "什么", "有哪", "有什么");
        }
        return false;
    }

    private ChatResponseDto handleRecommendation(String convId, String text) {
        String[] fromTo = extractFromTo(text);
        if (fromTo == null) fromTo = extractFromToLooser(text);

        if (fromTo != null) return buildSmartRecommendation(convId, fromTo[0], fromTo[1]);

        return new ChatResponseDto(convId,
            "Want travel advice? Tell me your **origin and destination** 😊\ne.g. COM3 to UTown")
            .withSuggestions(List.of(SUGGEST_COM3_TO_UTOWN, SUGGEST_PGP_TO_CLB, BTN_BACK_TO_MENU));
    }

    // =========================
    // Persistence
    // =========================
    private ConversationState getOrCreateState(String conversationId, String userId) {
        cleanupExpiredConversations();

        ConversationState cached = conversations.get(conversationId);
        if (cached != null) {
            cached.lastAccessedAt = System.currentTimeMillis();
            return cached;
        }

        Optional<ChatConversation> existing = conversationRepository.findByConversationId(conversationId);
        ConversationState state;

        if (existing.isPresent()) {
            state = restoreStateFromDb(existing.get());
        } else {
            state = new ConversationState();
            createConversationInDb(conversationId, userId);
        }

        conversations.put(conversationId, state);
        return state;
    }

    private ConversationState restoreStateFromDb(ChatConversation conv) {
        ConversationState state = new ConversationState();
        ChatConversation.ConversationState dbState = conv.getState();
        if (dbState != null) {
            state.intent = dbState.getIntent();
            if (dbState.getPartialData() != null) {
                state.partialData.putAll(dbState.getPartialData());
            }
        }
        return state;
    }

    private void createConversationInDb(String conversationId, String userId) {
        ChatConversation conv = new ChatConversation();
        conv.setConversationId(conversationId);
        conv.setUserId(userId);
        conv.setCreatedAt(Instant.now());
        conv.setUpdatedAt(Instant.now());
        conversationRepository.save(conv);
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

    private void cleanupExpiredConversations() {
        long now = System.currentTimeMillis();
        conversations.entrySet().removeIf(entry ->
                (now - entry.getValue().lastAccessedAt) > CONVERSATION_TTL_MS);
    }

    // =========================
    // Booking flow (split to reduce complexity)
    // =========================
    private ChatResponseDto handleBookingFlow(String convId, String userId, ConversationState state, String text) {
        BookingContext ctx = new BookingContext(state);

        // Parse form submissions: k=v
        if (text.contains("=")) {
            applyKvBookingFields(text, state, ctx);
        }

        // Extract missing fields from free text
        fillRouteIfMissing(text, state, ctx);
        fillPassengersIfMissing(text, state, ctx);
        fillDepartAtIfMissing(text, state, ctx);

        // If complete -> create booking
        if (ctx.isComplete()) {
            ChatBookingService.BookingResult result = bookingService.createBooking(
                    userId, ctx.fromName, ctx.toName, ctx.departAt, ctx.passengers
            );
            state.reset();
            return buildBookingCardResponse(convId, result, ctx.fromName, ctx.toName, ctx.departAt, ctx.passengers);
        }

        return buildMissingFieldsResponse(convId, state);
    }

    private void applyKvBookingFields(String text, ConversationState state, BookingContext ctx) {
        Map<String, String> kv = parseKeyValuePairs(text);
        handleKvRoute(kv, state, ctx);
        handleKvDepartAt(kv, state, ctx);
        handleKvPassengers(kv, state, ctx);
        handlePassengersFromTextIfNeeded(text, state, ctx);
    }

    private void handleKvRoute(Map<String, String> kv, ConversationState state, BookingContext ctx) {
        if ((ctx.fromName == null || ctx.toName == null) && kv.containsKey(KEY_ROUTE)) {
            String route = kv.get(KEY_ROUTE);
            String[] ft = extractFromTo(route);
            if (ft == null) ft = extractFromToLooser(route);
            if (ft != null) {
                state.partialData.put(KEY_FROM_NAME, ft[0]);
                state.partialData.put(KEY_TO_NAME, ft[1]);
                ctx.fromName = ft[0];
                ctx.toName = ft[1];
            }
        }
    }

    private void handleKvDepartAt(Map<String, String> kv, ConversationState state, BookingContext ctx) {
        if (ctx.departAt == null && kv.containsKey(KEY_DEPART_AT)) {
            String norm = normalizeDepartAt(kv.get(KEY_DEPART_AT));
            if (norm != null) {
                state.partialData.put(KEY_DEPART_AT, norm);
                ctx.departAt = norm;
            }
        }
    }

    private void handleKvPassengers(Map<String, String> kv, ConversationState state, BookingContext ctx) {
        if (ctx.passengers == null && kv.containsKey(KEY_PASSENGERS)) {
            Integer p = parsePassengers(kv.get(KEY_PASSENGERS));
            if (p != null) {
                state.partialData.put(KEY_PASSENGERS, p);
                ctx.passengers = p;
            }
        }
    }

    private void handlePassengersFromTextIfNeeded(String text, ConversationState state, BookingContext ctx) {
        if (ctx.passengers == null && text.toLowerCase(Locale.ROOT).contains(KEY_PASSENGERS)) {
            Integer p2 = parsePassengers(text);
            if (p2 != null) {
                state.partialData.put(KEY_PASSENGERS, p2);
                ctx.passengers = p2;
            }
        }
    }

    private void fillRouteIfMissing(String text, ConversationState state, BookingContext ctx) {
        if (ctx.fromName != null && ctx.toName != null) return;

        String[] ft = extractFromTo(text);
        if (ft == null) ft = extractFromToLooser(text);
        if (ft == null) return;

        state.partialData.put(KEY_FROM_NAME, ft[0]);
        state.partialData.put(KEY_TO_NAME, ft[1]);
        ctx.fromName = ft[0];
        ctx.toName = ft[1];
    }

    private void fillPassengersIfMissing(String text, ConversationState state, BookingContext ctx) {
        if (ctx.passengers != null) return;

        Integer p = extractPassengers(text);
        if (p == null) return;

        state.partialData.put(KEY_PASSENGERS, p);
        ctx.passengers = p;
    }

    private void fillDepartAtIfMissing(String text, ConversationState state, BookingContext ctx) {
        if (ctx.departAt != null) return;

        String dt = extractDepartAt(text);
        if (dt == null) return;

        state.partialData.put(KEY_DEPART_AT, dt);
        ctx.departAt = dt;
    }

    private static class BookingContext {
        String fromName;
        String toName;
        String departAt;
        Integer passengers;

        BookingContext(ConversationState state) {
            this.fromName = (String) state.partialData.get(KEY_FROM_NAME);
            this.toName = (String) state.partialData.get(KEY_TO_NAME);
            this.departAt = (String) state.partialData.get(KEY_DEPART_AT);
            Object passObj = state.partialData.get(KEY_PASSENGERS);
            this.passengers = passObj instanceof Number ? ((Number) passObj).intValue() : null;
        }

        boolean isComplete() {
            return fromName != null && toName != null && departAt != null && passengers != null;
        }
    }

    private ChatResponseDto buildBookingCardResponse(String convId,
                                                    ChatBookingService.BookingResult result,
                                                    String fromName, String toName,
                                                    String departAt, int passengers) {
        Map<String, Object> cardPayload = new HashMap<>();
        cardPayload.put("bookingId", result.bookingId());
        cardPayload.put(KEY_FROM_NAME, fromName);
        cardPayload.put(KEY_TO_NAME, toName);
        cardPayload.put(KEY_DEPART_AT, departAt);
        cardPayload.put(KEY_PASSENGERS, passengers);
        cardPayload.put(KEY_STATUS, "confirmed");
        if (result.tripId() != null) {
            cardPayload.put("tripId", result.tripId());
        }

        return new ChatResponseDto(convId, "Booking confirmed! 🎉")
                .withUiAction(new UiAction("BOOKING_CARD", cardPayload))
                .withSuggestions(List.of(BTN_MY_PROFILE, BTN_BUS_ARRIVALS, BTN_BACK_TO_MENU));
    }

    // =========================
    // Bus query (split to reduce complexity)
    // =========================
    private ChatResponseDto handleBusQuery(String convId, String text) {
        return handleBusQueryInternal(convId, text, 3);
    }

    private ChatResponseDto handleBusQueryExpanded(String convId, String text) {
        ConversationState state = conversations.get(convId);
        String lastStop = state != null ? (String) state.partialData.get(KEY_LAST_BUS_STOP) : null;
        String lastRoute = state != null ? (String) state.partialData.get(KEY_LAST_BUS_ROUTE) : null;

        if (lastStop != null) {
            NusBusProvider.BusArrivalsResult result = busProvider.getArrivals(lastStop, lastRoute);
            if (result.arrivals() != null && !result.arrivals().isEmpty()) {
                return buildBusResultResponse(convId, result, result.arrivals().size());
            }
        }

        if (state != null) state.intent = INTENT_AWAITING_BUS_STOP;
        return new ChatResponseDto(convId, "Which stop would you like to check? Pick one below:")
                .withSuggestions(BUS_STOP_OPTIONS);
    }

    private ChatResponseDto handleBusQueryInternal(String convId, String text, int maxShow) {
        String cleaned = cleanBusQueryText(text);

        String route = extractBusRoute(text);
        String stop = extractBusStop(text, cleaned);

        log.info("[BUS_QUERY] Extracted: stop={}, route={} from text: {}", stop, route, text);

        NusBusProvider.BusArrivalsResult result = busProvider.getArrivals(stop, route);
        cacheLastBusQuery(convId, stop, route, result);

        if (result.arrivals() == null || result.arrivals().isEmpty()) {
            return new ChatResponseDto(convId,
                    String.format("🚌 %s: No arrivals at the moment.\n\nThis may be outside operating hours. Try again later!", result.stopName()))
                    .withSuggestions(List.of(BTN_TRY_ANOTHER_STOP, BTN_BUS_ARRIVALS, BTN_BACK_TO_MENU));
        }

        return buildBusResultResponse(convId, result, maxShow);
    }

    private String cleanBusQueryText(String text) {
        return text.replaceAll("(?i)(查询|查看|查|公交|到站|信息|时间|bus|arrival[s]?|stop|shuttle)", "").trim();
    }

    private String extractBusRoute(String text) {
        Matcher routeMatcher = Pattern.compile("(?i)([A-Z]\\d[A-Z]?|[A-Z]{1,3})").matcher(text);
        while (routeMatcher.find()) {
            String candidate = routeMatcher.group(1).toUpperCase(Locale.ROOT);
            if (NusBusProvider.isRouteName(candidate)) return candidate;
        }
        return null;
    }

    private String extractBusStop(String text, String cleaned) {
        String stop = extractBusStopByCode(text);
        if (stop != null) return stop;
        return extractBusStopByChineseName(cleaned);
    }

    private String extractBusStopByCode(String text) {
        Matcher codeMatcher = Pattern.compile("(?i)([A-Z][A-Z0-9\\-]{1,10})").matcher(text);
        while (codeMatcher.find()) {
            String candidate = codeMatcher.group(1).trim();
            if (NusBusProvider.isRouteName(candidate)) continue;
            if (candidate.matches("(?i)(bus|stop|arrival|shuttle|line|ETA)")) continue;
            return candidate;
        }
        return null;
    }

    private String extractBusStopByChineseName(String cleaned) {
        Matcher cnMatcher = Pattern.compile("([\\u4e00-\\u9fa5]{2,10})(站)?").matcher(cleaned);
        while (cnMatcher.find()) {
            String candidate = cnMatcher.group(1).trim();
            boolean isKeyword = candidate.matches(".*(几分钟|时间|线路|下一班|到站|查询|公交|巴士|信息|分钟到).*");
            if (!isKeyword && candidate.length() >= 2) return candidate;
        }
        return null;
    }

    private void cacheLastBusQuery(String convId, String stop, String route, NusBusProvider.BusArrivalsResult result) {
        ConversationState state = conversations.get(convId);
        if (state == null) return;

        state.partialData.put(KEY_LAST_BUS_STOP, stop != null ? stop : result.stopName());
        state.partialData.put(KEY_LAST_BUS_ROUTE, route);
    }

    private ChatResponseDto buildBusResultResponse(String convId, NusBusProvider.BusArrivalsResult result, int maxShow) {
        StringBuilder sb = new StringBuilder();
        sb.append("🚌 ").append(result.stopName()).append(" — Next arrivals:\n\n");

        int shown = 0;
        for (Map<String, Object> arrival : result.arrivals()) {
            if (shown >= maxShow) break;

            String statusStr = String.valueOf(arrival.get(KEY_STATUS));
            String statusIcon = busStatusIcon(statusStr);

            sb.append(String.format("%s Route %s — %d min\n",
                    statusIcon, arrival.get(KEY_ROUTE), ((Number) arrival.get("etaMinutes")).intValue()));
            shown++;
        }

        int total = result.arrivals().size();
        List<String> buttons = buildBusButtons(total, shown, sb);

        return new ChatResponseDto(convId, sb.toString().trim())
                .withSuggestions(buttons);
    }

    private String busStatusIcon(String statusStr) {
        return switch (statusStr) {
            case BUS_STATUS_ARRIVING -> "🟢";
            case BUS_STATUS_ON_TIME -> "🔵";
            default -> "⏳";
        };
    }

    private List<String> buildBusButtons(int total, int shown, StringBuilder sb) {
        List<String> buttons = new ArrayList<>();
        if (total > shown) {
            sb.append(String.format("\n%d more services available", total - shown));
            buttons.add(BTN_SHOW_MORE);
        }
        buttons.add(BTN_CHANGE_STOP);
        buttons.add(BTN_BACK_TO_MENU);
        return buttons;
    }

    // =========================
    // Profile
    // =========================
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

        sb.append("👤 Nickname: ").append(nvl(user.getNickname(), TXT_NOT_SET)).append("\n");
        sb.append("📧 Email: ").append(nvl(user.getEmail(), TXT_NOT_SET)).append("\n");
        sb.append("📱 Phone: ").append(nvl(user.getPhone(), TXT_NOT_SET)).append("\n");
        sb.append("🏫 Faculty: ").append(nvl(user.getFaculty(), TXT_NOT_SET)).append("\n");
        sb.append("\n");

        User.Stats stats = user.getStats();
        if (stats != null) {
            sb.append("📊 Statistics:\n");
            sb.append("  • Total Trips: ").append(stats.getTotalTrips()).append("\n");
            sb.append("  • Total Distance: ").append(String.format("%.1f km", stats.getTotalDistance())).append("\n");
            sb.append("  • Green Travel Days: ").append(stats.getGreenDays()).append("\n");
            if (stats.getWeeklyRank() > 0) {
                sb.append("  • Weekly Rank: #").append(stats.getWeeklyRank()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("🌿 Eco Impact:\n");
        sb.append("  • Carbon Saved: ").append(String.format("%.1f g", user.getTotalCarbon())).append("\n");
        sb.append("  • Current Points: ").append(user.getCurrentPoints()).append("\n");
        sb.append("  • Total Points: ").append(user.getTotalPoints()).append("\n");

        User.Vip vip = user.getVip();
        if (vip != null && vip.isActive()) {
            sb.append("\n⭐ VIP: ").append(nvl(vip.getPlan(), "active"));
            if (vip.getExpiryDate() != null) {
                sb.append(" (expires: ").append(vip.getExpiryDate().toLocalDate()).append(")");
            }
            sb.append("\n");
        }

        return new ChatResponseDto(convId, sb.toString())
                .withSuggestions(List.of("Update my nickname", BTN_BOOK_A_TRIP, BTN_BACK_TO_MENU));
    }

    private static String nvl(String val, String fallback) {
        return (val != null && !val.isBlank()) ? val : fallback;
    }

    // =========================
    // User update
    // =========================
    private ChatResponseDto handleUserUpdateRequest(String convId, String userId, String role,
                                                   ConversationState state, String text) {
        String targetUserId = extractUserId(text);
        if (targetUserId == null) targetUserId = userId;

        ProfilePatch patch = extractProfilePatch(text);

        if (!targetUserId.equals(userId) && ROLE_USER.equals(role)) {
            return new ChatResponseDto(convId, "You don't have permission to modify another user's profile.")
                    .withSuggestions(List.of(BTN_MY_PROFILE, BTN_BOOK_A_TRIP));
        }

        if (!targetUserId.equals(userId)) {
            state.intent = INTENT_USER_UPDATE;
            state.pendingUserUpdate = new PendingUserUpdate(targetUserId, patch);
            return new ChatResponseDto(convId, "You're modifying another user's profile. Please confirm to proceed.")
                    .withShowConfirm("Confirm profile update",
                            String.format("This will update user %s's profile. Reply \"confirm\" to continue.", targetUserId));
        }

        return executeUserUpdate(convId, userId, role, targetUserId, patch, state);
    }

    private ChatResponseDto handleUserUpdateConfirmation(String convId, String userId, String role,
                                                        ConversationState state) {
        PendingUserUpdate pending = state.pendingUserUpdate;
        ChatResponseDto response = executeUserUpdate(convId, userId, role, pending.targetUserId, pending.patch, state);
        state.reset();
        return response;
    }

    private ChatResponseDto executeUserUpdate(String convId, String actorUserId, String actorRole,
                                              String targetUserId, ProfilePatch patch, ConversationState state) {
        Optional<User> userOpt = userRepository.findByUserid(targetUserId);
        if (userOpt.isEmpty()) {
            return new ChatResponseDto(convId, "User not found.");
        }

        User user = userOpt.get();

        PatchApplyResult apply = applyProfilePatch(user, patch);
        if (!apply.updated) {
            return new ChatResponseDto(convId,
                    "No fields to update detected. Supported fields: nickname, email, phone, faculty.\n" +
                            "Example: update my nickname=NewName")
                    .withSuggestions(POST_ACTION);
        }

        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("[ORCHESTRATOR] Updated user {} fields: {}", targetUserId, apply.patchDetails.keySet());

        AuditAndNotifyResult an = maybeAuditAndNotify(actorUserId, actorRole, targetUserId, apply.patchDetails);

        String responseText = buildUserUpdateResponseText(targetUserId, apply.patchDetails.keySet(), an);
        state.reset();

        return new ChatResponseDto(convId, responseText).withSuggestions(POST_ACTION);
    }

    private static class PatchApplyResult {
        boolean updated;
        Map<String, Object> patchDetails = new HashMap<>();
    }

    private PatchApplyResult applyProfilePatch(User user, ProfilePatch patch) {
        PatchApplyResult r = new PatchApplyResult();

        if (patch.nickname != null) {
            user.setNickname(patch.nickname);
            r.patchDetails.put(PROFILE_KEY_NICKNAME, patch.nickname);
            r.updated = true;
        }
        if (patch.email != null) {
            user.setEmail(patch.email);
            r.patchDetails.put(PROFILE_KEY_EMAIL, patch.email);
            r.updated = true;
        }
        if (patch.phone != null) {
            user.setPhone(patch.phone);
            r.patchDetails.put(PROFILE_KEY_PHONE, patch.phone);
            r.updated = true;
        }
        if (patch.faculty != null) {
            user.setFaculty(patch.faculty);
            r.patchDetails.put(PROFILE_KEY_FACULTY, patch.faculty);
            r.updated = true;
        }

        return r;
    }

    private static class AuditAndNotifyResult {
        String auditId;
        String notifId;
        boolean didAudit;
    }

    private AuditAndNotifyResult maybeAuditAndNotify(String actorUserId, String actorRole,
                                                     String targetUserId, Map<String, Object> patchDetails) {
        AuditAndNotifyResult r = new AuditAndNotifyResult();
        if (actorUserId.equals(targetUserId)) return r;

        r.auditId = auditLogService.createAuditLog(actorUserId, TOOL_UPDATE_USER, targetUserId, patchDetails);
        r.notifId = notificationService.createNotification(
                targetUserId, "profile_updated", "Your profile has been updated",
                String.format("%s(%s) modified your profile.", actorRole, actorUserId)
        );
        r.didAudit = true;
        return r;
    }

    private String buildUserUpdateResponseText(String targetUserId, Set<String> fields, AuditAndNotifyResult an) {
        if (!an.didAudit) {
            return String.format("Your profile has been updated (%s).", fields);
        }
        return String.format("Profile updated (%s). Audit ID: %s. User %s notified (notification=%s).",
                fields, an.auditId, targetUserId, an.notifId);
    }

    // =========================
    // Tool call (split to reduce complexity)
    // =========================
    private ChatResponseDto handleToolCall(String convId, String userId, String role,
                                          ConversationState state, ModelClientService.ModelResult mr) {
        ModelClientService.ToolCall tc = mr.toolCall();
        Map<String, Object> args = tc.arguments();

        return switch (tc.name()) {
            case TOOL_CREATE_BOOKING -> handleToolCreateBooking(convId, userId, state, args);
            case TOOL_GET_BUS_ARRIVALS -> handleToolGetBusArrivals(convId, mr, args);
            case TOOL_UPDATE_USER -> handleToolUpdateUser(convId, userId, role, state, args);
            default -> toolFallback(convId, mr);
        };
    }

    private ChatResponseDto handleToolCreateBooking(String convId, String userId, ConversationState state, Map<String, Object> args) {
        String fromName = getStringArg(args, KEY_FROM_NAME);
        String toName = getStringArg(args, KEY_TO_NAME);
        String departAt = getStringArg(args, KEY_DEPART_AT);
        Integer passengers = toInt(args.get(KEY_PASSENGERS));

        if (fromName == null || toName == null || departAt == null || passengers == null) {
            state.intent = INTENT_BOOKING;
            putIfNotNull(state.partialData, KEY_FROM_NAME, fromName);
            putIfNotNull(state.partialData, KEY_TO_NAME, toName);
            putIfNotNull(state.partialData, KEY_DEPART_AT, departAt);
            putIfNotNull(state.partialData, KEY_PASSENGERS, passengers);
            return buildMissingFieldsResponse(convId, state);
        }

        ChatBookingService.BookingResult result = bookingService.createBooking(userId, fromName, toName, departAt, passengers);
        return buildBookingCardResponse(convId, result, fromName, toName, departAt, passengers);
    }

    private ChatResponseDto handleToolGetBusArrivals(String convId, ModelClientService.ModelResult mr, Map<String, Object> args) {
        String stopName = getStringArg(args, "stopName");
        String routeArg = getStringArg(args, KEY_ROUTE);

        NusBusProvider.BusArrivalsResult busResult = busProvider.getArrivals(stopName, routeArg);
        if (busResult.arrivals() == null || busResult.arrivals().isEmpty()) {
            String fallback = (mr.text() != null && !mr.text().isBlank())
                    ? mr.text()
                    : String.format("%s: No arrivals found.", busResult.stopName());
            return new ChatResponseDto(convId, fallback)
                    .withSuggestions(List.of(BTN_BOOK_A_TRIP, BTN_MY_PROFILE));
        }

        return new ChatResponseDto(convId, formatBusArrivals(busResult))
                .withSuggestions(List.of(BTN_BOOK_A_TRIP, BTN_MY_PROFILE));
    }

    private String formatBusArrivals(NusBusProvider.BusArrivalsResult busResult) {
        StringBuilder busSb = new StringBuilder();
        busSb.append("🚌 ").append(busResult.stopName()).append(" — Arrivals:\n\n");

        for (Map<String, Object> arrival : busResult.arrivals()) {
            String statusStr = String.valueOf(arrival.get(KEY_STATUS));
            String statusIcon = toolBusStatusLabel(statusStr);
            busSb.append(String.format("  Route %s — %s min  %s\n",
                    arrival.get(KEY_ROUTE), arrival.get("etaMinutes"), statusIcon));
        }
        busSb.append(String.format("\n%d services total.", busResult.arrivals().size()));
        return busSb.toString();
    }

    private String toolBusStatusLabel(String statusStr) {
        return switch (statusStr) {
            case BUS_STATUS_ARRIVING -> "🟢 Arriving";
            case BUS_STATUS_DELAYED -> "🟡 Delayed";
            default -> "🔵 On time";
        };
    }

    private ChatResponseDto handleToolUpdateUser(String convId, String userId, String role,
                                                ConversationState state, Map<String, Object> args) {
        String targetUserId = getStringArg(args, "userId");
        if (targetUserId == null) targetUserId = userId;

        @SuppressWarnings("unchecked")
        Map<String, Object> patchMap = (Map<String, Object>) args.getOrDefault("patch", Map.of());
        ProfilePatch patch = patchFromMap(patchMap);

        if (!targetUserId.equals(userId) && ROLE_USER.equals(role)) {
            return new ChatResponseDto(convId, "You don't have permission to modify another user's profile.")
                    .withSuggestions(List.of(BTN_MY_PROFILE, BTN_BOOK_A_TRIP));
        }

        if (!targetUserId.equals(userId)) {
            state.intent = INTENT_USER_UPDATE;
            state.pendingUserUpdate = new PendingUserUpdate(targetUserId, patch);
            return new ChatResponseDto(convId, "You're modifying another user's profile. Please confirm to proceed.")
                    .withShowConfirm("Confirm profile update",
                            String.format("This will update user %s's profile. Reply \"confirm\" to continue.", targetUserId));
        }

        return executeUserUpdate(convId, userId, role, targetUserId, patch, state);
    }

    private ProfilePatch patchFromMap(Map<String, Object> patchMap) {
        ProfilePatch patch = new ProfilePatch();
        if (patchMap.containsKey(PROFILE_KEY_NICKNAME)) patch.nickname = String.valueOf(patchMap.get(PROFILE_KEY_NICKNAME));
        if (patchMap.containsKey(PROFILE_KEY_EMAIL)) patch.email = String.valueOf(patchMap.get(PROFILE_KEY_EMAIL));
        if (patchMap.containsKey(PROFILE_KEY_PHONE)) patch.phone = String.valueOf(patchMap.get(PROFILE_KEY_PHONE));
        if (patchMap.containsKey(PROFILE_KEY_FACULTY)) patch.faculty = String.valueOf(patchMap.get(PROFILE_KEY_FACULTY));
        return patch;
    }

    private ChatResponseDto toolFallback(String convId, ModelClientService.ModelResult mr) {
        String fallbackText = (mr.text() != null && !mr.text().isBlank())
                ? mr.text()
                : "This operation is not supported yet.";
        return new ChatResponseDto(convId, fallbackText).withSuggestions(POST_ACTION);
    }

    private Integer toInt(Object obj) {
        return obj instanceof Number ? ((Number) obj).intValue() : null;
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object val) {
        if (val != null) map.put(key, val);
    }

    // =========================
    // RAG
    // =========================
    private ChatResponseDto handleRagQuery(String convId, String text) {
        List<Citation> citations;

        if (ragService.isAvailable()) {
            try {
                // Retrieve top-3 for better matching quality
                citations = ragService.retrieve(text, 3);
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
            // Return the full snippet from the best match (up to 600 chars from RagService)
            answer = top.getSnippet() != null ? top.getSnippet() : "I found something but couldn't render it.";
        } else {
            answer = "I'm not sure about that. Try rephrasing or explore the options below:";
        }

        ChatResponseDto response = new ChatResponseDto();
        response.setConversationId(convId);
        response.setAssistant(new AssistantMessage(answer, citations));
        response.setServerTimestamp(Instant.now());
        response.withSuggestions(MAIN_MENU);
        return response;
    }

    // =========================
    // Missing-fields form (split to reduce complexity)
    // =========================
    private ChatResponseDto buildMissingFieldsResponse(String convId, ConversationState state) {
        String fromName = (String) state.partialData.get(KEY_FROM_NAME);
        String toName = (String) state.partialData.get(KEY_TO_NAME);
        String departAt = (String) state.partialData.get(KEY_DEPART_AT);
        Object passObj = state.partialData.get(KEY_PASSENGERS);

        if (fromName == null || toName == null) {
            return bookingAskRoute(convId);
        }

        if (departAt == null) {
            return bookingAskDepartAtAndMaybePassengers(convId, fromName, toName, passObj);
        }

        if (passObj == null) {
            return bookingAskPassengers(convId, fromName, toName, departAt);
        }

        return bookingAskAllFallback(convId);
    }

    private ChatResponseDto bookingAskRoute(String convId) {
        return new ChatResponseDto(convId,
                "Let me help you book a trip 🎫\n\nWhere would you like to go?")
                .withSuggestions(List.of("NUS to Marina Bay", "PGP to Changi Airport", BTN_BACK_TO_MENU));
    }

    private ChatResponseDto bookingAskDepartAtAndMaybePassengers(String convId, String fromName, String toName, Object passObj) {
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(formField(KEY_DEPART_AT, "Departure Time", "datetime", true));

        if (passObj == null) {
            fields.add(formFieldPassengers());
        }

        return new ChatResponseDto(convId,
                String.format("**%s** to **%s** — got it!\n\nPlease select departure time and passengers:", fromName, toName))
                .withShowForm(FORM_BOOKING_MISSING_FIELDS, FORM_TITLE_COMPLETE_BOOKING, fields);
    }

    private ChatResponseDto bookingAskPassengers(String convId, String fromName, String toName, String departAt) {
        return new ChatResponseDto(convId,
                String.format("%s to %s, departing %s\n\nHow many passengers?", fromName, toName, departAt))
                .withSuggestions(List.of("1 person", "2 people", "3 people", "4 people"));
    }

    private ChatResponseDto bookingAskAllFallback(String convId) {
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(formField(KEY_ROUTE, "Route (A to B)", "text", true));
        fields.add(formField(KEY_DEPART_AT, "Departure Time", "datetime", true));
        fields.add(formFieldPassengers());

        return new ChatResponseDto(convId, "Please provide the following to complete your booking:")
                .withShowForm(FORM_BOOKING_MISSING_FIELDS, FORM_TITLE_COMPLETE_BOOKING, fields);
    }

    private Map<String, Object> formField(String key, String label, String type, boolean required) {
        Map<String, Object> m = new HashMap<>();
        m.put("key", key);
        m.put("label", label);
        m.put("type", type);
        m.put("required", required);
        return m;
    }

    private Map<String, Object> formFieldPassengers() {
        Map<String, Object> m = formField(KEY_PASSENGERS, "Passengers (1-8)", "int", true);
        m.put("min", 1);
        m.put("max", 8);
        return m;
    }

    // =========================
    // Text extraction utilities
    // =========================
    private String[] extractFromTo(String text) {
        Pattern cnP = Pattern.compile("从(?<from>[^到去，,。]{1,20})(到|去)(?<to>[^，,。]{1,20})");
        Matcher cnM = cnP.matcher(text);
        if (cnM.find()) return new String[]{cnM.group("from").trim(), cnM.group("to").trim()};

        Pattern enP = Pattern.compile("(?i)from\\s+(?<from>[^,.]{1,30})\\s+to\\s+(?<to>[^,.\n]{1,30})");
        Matcher enM = enP.matcher(text);
        if (enM.find()) return new String[]{enM.group("from").trim(), enM.group("to").trim()};

        return null;
    }

    private String[] extractFromToLooser(String text) {
        if (text == null) return null;
        String t = text.trim();

        Matcher enM = Pattern.compile("(?<from>.+?)\\s+to\\s+(?<to>.+)", Pattern.CASE_INSENSITIVE).matcher(t);
        if (enM.find()) {
            String from = enM.group("from").trim();
            String to = enM.group("to").trim();
            if (!from.isEmpty() && !to.isEmpty()) return new String[]{from, to};
        }

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
        Matcher cnM = Pattern.compile("(?<n>[1-8])\\s*(人|位)").matcher(text);
        if (cnM.find()) return Integer.parseInt(cnM.group("n"));

        Matcher enM = Pattern.compile("(?i)(?<n>[1-8])\\s*(people|person|passenger[s]?)").matcher(text);
        if (enM.find()) return Integer.parseInt(enM.group("n"));

        return null;
    }

    private String extractDepartAt(String text) {
        Pattern p = Pattern.compile("(\\d{4}[-/]\\d{2}[-/]\\d{2}[T\\s]\\d{2}:\\d{2}(?::\\d{2})?(?:Z|[+-]\\d{2}:\\d{2})?)");
        Matcher m = p.matcher(text);
        if (m.find()) return normalizeDepartAt(m.group(1));
        return null;
    }

    private Integer parsePassengers(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        s = s.replace('１','1').replace('２','2').replace('３','3').replace('４','4')
             .replace('５','5').replace('６','6').replace('７','7').replace('８','8').replace('９','9').replace('０','0');
        Matcher m = Pattern.compile("([1-8])").matcher(s);
        if (!m.find()) return null;

        int n = Integer.parseInt(m.group(1));
        return (n >= 1 && n <= 8) ? n : null;
    }

    private String normalizeDepartAt(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        s = s.replace('/', '-');
        if (s.contains(" ") && !s.contains("T")) {
            s = s.replace(' ', 'T');
        }
        if (s.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$")) {
            s = s + ":00";
        }
        return s;
    }

    private Map<String, String> parseKeyValuePairs(String text) {
        Map<String, String> out = new HashMap<>();
        if (text == null) return out;

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
        Matcher m = Pattern.compile("\\b(u_\\d{3,})\\b").matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private ProfilePatch extractProfilePatch(String text) {
        ProfilePatch patch = new ProfilePatch();

        Matcher nicknameM = Pattern.compile("nickname\\s*=\\s*(?<val>[^，,。\\s]{1,30})").matcher(text);
        if (nicknameM.find()) patch.nickname = nicknameM.group("val");

        Matcher emailM = Pattern.compile("email\\s*=\\s*(?<val>[^\\s，,。]{1,50})").matcher(text);
        if (emailM.find()) patch.email = emailM.group("val");

        Matcher phoneM = Pattern.compile("phone\\s*=\\s*(?<val>[^\\s，,。]{1,20})").matcher(text);
        if (phoneM.find()) patch.phone = phoneM.group("val");

        Matcher facultyM = Pattern.compile("faculty\\s*=\\s*(?<val>[^，,。\\s]{1,30})").matcher(text);
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
        String lower = text.toLowerCase(Locale.ROOT).trim();
        return Set.of("取消", "重新开始", "cancel", "reset", "重置", "back to menu").contains(lower);
    }

    private boolean isConfirmCommand(String text) {
        String lower = text.toLowerCase(Locale.ROOT).trim();
        return Set.of("确认", "好的确认", "确认修改", "confirm", "yes", "ok").contains(lower);
    }

    private String getStringArg(Map<String, Object> args, String key) {
        Object val = args.get(key);
        if (val == null) return null;
        String s = String.valueOf(val);
        return s.isBlank() ? null : s;
    }

    // =========================
    // Internal state classes
    // =========================
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
