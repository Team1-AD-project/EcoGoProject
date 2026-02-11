package com.example.EcoGo.dto.chatbot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chat response sent back to Android client.
 */
public class ChatResponseDto {

    private String conversationId;
    private AssistantMessage assistant;
    private List<UiAction> uiActions = new ArrayList<>();
    private Instant serverTimestamp;

    public ChatResponseDto() {
        this.serverTimestamp = Instant.now();
    }

    public ChatResponseDto(String conversationId, String text) {
        this.conversationId = conversationId;
        this.assistant = new AssistantMessage(text);
        this.serverTimestamp = Instant.now();
    }

    // --- Nested classes ---

    public static class AssistantMessage {
        private String text;
        private List<Citation> citations = new ArrayList<>();

        public AssistantMessage() {}

        public AssistantMessage(String text) {
            this.text = text;
        }

        public AssistantMessage(String text, List<Citation> citations) {
            this.text = text;
            this.citations = citations != null ? citations : new ArrayList<>();
        }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public List<Citation> getCitations() { return citations; }
        public void setCitations(List<Citation> citations) { this.citations = citations; }
    }

    public static class Citation {
        private String title;
        private String source;
        private String snippet;

        public Citation() {}

        public Citation(String title, String source, String snippet) {
            this.title = title;
            this.source = source;
            this.snippet = snippet;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getSnippet() { return snippet; }
        public void setSnippet(String snippet) { this.snippet = snippet; }
    }

    public static class UiAction {
        private String type;  // SHOW_FORM | SHOW_CONFIRM | DEEPLINK | SUGGESTIONS
        private Map<String, Object> payload = new HashMap<>();

        public UiAction() {}

        public UiAction(String type, Map<String, Object> payload) {
            this.type = type;
            this.payload = payload != null ? payload : new HashMap<>();
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Map<String, Object> getPayload() { return payload; }
        public void setPayload(Map<String, Object> payload) { this.payload = payload; }
    }

    // --- Builder helpers ---

    public ChatResponseDto withUiAction(UiAction action) {
        this.uiActions.add(action);
        return this;
    }

    public ChatResponseDto withSuggestions(List<String> options) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("options", options);
        this.uiActions.add(new UiAction("SUGGESTIONS", payload));
        return this;
    }

    public ChatResponseDto withDeeplink(String url) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("url", url);
        this.uiActions.add(new UiAction("DEEPLINK", payload));
        return this;
    }

    public ChatResponseDto withShowForm(String formId, String title, List<Map<String, Object>> fields) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("formId", formId);
        payload.put("title", title);
        payload.put("fields", fields);
        this.uiActions.add(new UiAction("SHOW_FORM", payload));
        return this;
    }

    public ChatResponseDto withShowConfirm(String title, String body) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("body", body);
        payload.put("confirmAction", Map.of("type", "CHAT_MESSAGE", "text", "确认"));
        this.uiActions.add(new UiAction("SHOW_CONFIRM", payload));
        return this;
    }

    // --- Getters/Setters ---

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public AssistantMessage getAssistant() { return assistant; }
    public void setAssistant(AssistantMessage assistant) { this.assistant = assistant; }

    public List<UiAction> getUiActions() { return uiActions; }
    public void setUiActions(List<UiAction> uiActions) { this.uiActions = uiActions; }

    public Instant getServerTimestamp() { return serverTimestamp; }
    public void setServerTimestamp(Instant serverTimestamp) { this.serverTimestamp = serverTimestamp; }
}
