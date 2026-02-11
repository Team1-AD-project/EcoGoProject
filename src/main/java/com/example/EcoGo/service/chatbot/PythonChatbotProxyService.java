package com.example.EcoGo.service.chatbot;

import com.example.EcoGo.dto.chatbot.ChatResponseDto;
import com.example.EcoGo.dto.chatbot.ChatResponseDto.AssistantMessage;
import com.example.EcoGo.dto.chatbot.ChatResponseDto.Citation;
import com.example.EcoGo.dto.chatbot.ChatResponseDto.UiAction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Proxy service that forwards chat requests to the Python FastAPI chatbot backend.
 * This enables the Spring Boot backend to leverage the Python chatbot's RAG and
 * model-based capabilities when available.
 *
 * Falls back gracefully (returns null) when the Python backend is unreachable.
 */
@Service
public class PythonChatbotProxyService {

    private static final Logger log = LoggerFactory.getLogger(PythonChatbotProxyService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    @Value("${chatbot.python-backend.base-url:http://localhost:8000}")
    private String pythonBaseUrl;

    @Value("${chatbot.python-backend.enabled:false}")
    private boolean pythonEnabled;

    @Value("${chatbot.python-backend.timeout-seconds:5}")
    private int timeoutSeconds;

    public PythonChatbotProxyService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    /**
     * Check if the Python chatbot backend proxy is enabled.
     */
    public boolean isEnabled() {
        return pythonEnabled;
    }

    /**
     * Forward a chat request to the Python FastAPI chatbot backend.
     *
     * @param userId         User ID
     * @param role           User role ("user" or "admin")
     * @param conversationId Conversation ID
     * @param messageText    User message text
     * @return ChatResponseDto from the Python backend, or null if unavailable
     */
    public ChatResponseDto forwardChat(String userId, String role, String conversationId, String messageText) {
        if (!pythonEnabled) {
            return null;
        }

        try {
            // Build request payload matching Python ChatRequest schema
            Map<String, Object> payload = new HashMap<>();
            payload.put("conversationId", conversationId);
            payload.put("user", Map.of(
                    "userId", userId,
                    "role", role
            ));
            payload.put("message", Map.of(
                    "text", messageText
            ));
            payload.put("client", Map.of(
                    "platform", "android",
                    "appVersion", "1.0.0",
                    "timezone", "Asia/Singapore"
            ));

            String jsonBody = objectMapper.writeValueAsString(payload);
            String url = pythonBaseUrl.replaceAll("/+$", "") + "/api/chat";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            log.debug("[PYTHON_PROXY] Forwarding to {}: {}", url, messageText);

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.debug("[PYTHON_PROXY] Python backend returned status {}", response.statusCode());
                return null;
            }

            return parsePythonResponse(response.body(), conversationId);

        } catch (Exception e) {
            log.debug("[PYTHON_PROXY] Python backend unreachable: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse the Python FastAPI chatbot response into a ChatResponseDto.
     */
    private ChatResponseDto parsePythonResponse(String responseBody, String fallbackConvId) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            String convId = root.has("conversationId")
                    ? root.get("conversationId").asText(fallbackConvId)
                    : fallbackConvId;

            // Parse assistant message
            JsonNode assistantNode = root.path("assistant");
            String text = assistantNode.path("text").asText("");

            List<Citation> citations = new ArrayList<>();
            JsonNode citationsNode = assistantNode.path("citations");
            if (citationsNode.isArray()) {
                for (JsonNode c : citationsNode) {
                    citations.add(new Citation(
                            c.path("title").asText(""),
                            c.path("source").asText(""),
                            c.path("snippet").asText("")
                    ));
                }
            }

            // Parse UI actions
            List<UiAction> uiActions = new ArrayList<>();
            JsonNode actionsNode = root.path("uiActions");
            if (actionsNode.isArray()) {
                for (JsonNode a : actionsNode) {
                    String type = a.path("type").asText("");
                    Map<String, Object> payload = new HashMap<>();
                    JsonNode payloadNode = a.path("payload");
                    if (payloadNode.isObject()) {
                        payload = objectMapper.convertValue(payloadNode, Map.class);
                    }
                    uiActions.add(new UiAction(type, payload));
                }
            }

            ChatResponseDto dto = new ChatResponseDto();
            dto.setConversationId(convId);
            dto.setAssistant(new AssistantMessage(text, citations));
            dto.setUiActions(uiActions);
            dto.setServerTimestamp(Instant.now());

            log.debug("[PYTHON_PROXY] Received response: text length={}, citations={}, uiActions={}",
                    text.length(), citations.size(), uiActions.size());

            return dto;

        } catch (Exception e) {
            log.warn("[PYTHON_PROXY] Failed to parse Python response: {}", e.getMessage());
            return null;
        }
    }
}
