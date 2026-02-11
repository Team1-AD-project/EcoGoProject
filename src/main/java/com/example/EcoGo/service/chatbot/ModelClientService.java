package com.example.EcoGo.service.chatbot;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls an OpenAI-compatible model server for tool-calling intent detection.
 * If the model server is unreachable, returns null (graceful degradation to keyword-based).
 */
@Service
public class ModelClientService {

    private static final Logger log = LoggerFactory.getLogger(ModelClientService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Value("${chatbot.model.base-url:http://localhost:9000}")
    private String modelBaseUrl;

    @Value("${chatbot.model.api-key:dev-model-key}")
    private String modelApiKey;

    @Value("${chatbot.model.name:greentravel-local}")
    private String modelName;

    @Value("${chatbot.model.enabled:false}")
    private boolean modelEnabled;

    private static final List<Map<String, Object>> TOOLS = List.of(
            Map.of("type", "function", "function", Map.of(
                    "name", "create_booking",
                    "description", "Create a travel booking and return a deeplink for client navigation.",
                    "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "fromName", Map.of("type", "string"),
                                    "toName", Map.of("type", "string"),
                                    "departAt", Map.of("type", "string"),
                                    "passengers", Map.of("type", "integer", "minimum", 1, "maximum", 8)
                            ),
                            "required", List.of("fromName", "toName", "departAt", "passengers")
                    )
            )),
            Map.of("type", "function", "function", Map.of(
                    "name", "get_bus_arrivals",
                    "description", "Query bus arrivals for a stop/route.",
                    "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "stopName", Map.of("type", "string"),
                                    "route", Map.of("type", "string")
                            )
                    )
            )),
            Map.of("type", "function", "function", Map.of(
                    "name", "update_user",
                    "description", "Update a user's profile fields; requires RBAC.",
                    "parameters", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "userId", Map.of("type", "string"),
                                    "patch", Map.of("type", "object")
                            ),
                            "required", List.of("userId", "patch")
                    )
            ))
    );

    /**
     * Call the model server for tool-calling intent detection.
     * Returns null if the model is disabled or unreachable.
     */
    public ModelResult callModelForTool(String userText) {
        if (!modelEnabled) {
            return null;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", modelName);
            payload.put("messages", List.of(
                    Map.of("role", "system", "content",
                            "你是绿色出行助手。优先使用工具完成预订/公交查询/用户修改。" +
                            "当且仅当需要调用工具时，输出工具调用；否则直接给出简洁回答。"),
                    Map.of("role", "user", "content", userText)
            ));
            payload.put("tools", TOOLS);
            payload.put("tool_choice", "auto");
            payload.put("temperature", 0.2);

            String jsonBody = objectMapper.writeValueAsString(payload);
            String url = modelBaseUrl.replaceAll("/+$", "") + "/v1/chat/completions";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + modelApiKey)
                    .timeout(Duration.ofSeconds(3))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.debug("Model server returned status {}", response.statusCode());
                return null;
            }

            return parseModelResponse(response.body());

        } catch (Exception e) {
            log.debug("Model server unreachable: {}", e.getMessage());
            return null;
        }
    }

    private ModelResult parseModelResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choice = root.path("choices").path(0).path("message");
            String text = choice.path("content").asText("");

            JsonNode toolCalls = choice.path("tool_calls");
            if (toolCalls.isArray() && !toolCalls.isEmpty()) {
                JsonNode tc0 = toolCalls.get(0);
                JsonNode fn = tc0.path("function");
                String name = fn.path("name").asText(null);
                String argsStr = fn.path("arguments").asText("{}");
                Map<String, Object> args = objectMapper.readValue(argsStr, Map.class);

                if (name != null) {
                    return new ModelResult(text, new ToolCall(name, args));
                }
            }

            return new ModelResult(text, null);
        } catch (Exception e) {
            log.debug("Failed to parse model response: {}", e.getMessage());
            return null;
        }
    }

    public boolean isEnabled() {
        return modelEnabled;
    }

    // --- Result types ---

    public record ModelResult(String text, ToolCall toolCall) {}

    public record ToolCall(String name, Map<String, Object> arguments) {}
}
