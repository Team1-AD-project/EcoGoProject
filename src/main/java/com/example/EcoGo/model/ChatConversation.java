package com.example.EcoGo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "chat_conversations")
public class ChatConversation {

    @Id
    private String id;

    private String conversationId;
    private String userId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<Message> messages = new ArrayList<>();
    private ConversationState state = new ConversationState();

    // --- Nested classes ---

    public static class Message {
        private String role;   // "user" | "assistant"
        private String text;
        private Instant timestamp;
        private List<Map<String, Object>> uiActions = new ArrayList<>();

        public Message() {}

        public Message(String role, String text) {
            this.role = role;
            this.text = text;
            this.timestamp = Instant.now();
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
        public List<Map<String, Object>> getUiActions() { return uiActions; }
        public void setUiActions(List<Map<String, Object>> uiActions) { this.uiActions = uiActions; }
    }

    public static class ConversationState {
        private String intent; // booking | bus | user_update | null
        private Map<String, Object> partialData = new HashMap<>();

        public ConversationState() {
            // Empty constructor intentionally left blank.
            // Required by Spring Data MongoDB for deserialization of nested documents.
            // Fields are initialized with default values (intent=null, partialData=HashMap).
        }

        public String getIntent() { return intent; }
        public void setIntent(String intent) { this.intent = intent; }
        public Map<String, Object> getPartialData() { return partialData; }
        public void setPartialData(Map<String, Object> partialData) { this.partialData = partialData; }
    }

    // --- Getters/Setters ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }

    public ConversationState getState() { return state; }
    public void setState(ConversationState state) { this.state = state; }
}
