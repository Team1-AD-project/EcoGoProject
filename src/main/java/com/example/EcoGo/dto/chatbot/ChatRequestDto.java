package com.example.EcoGo.dto.chatbot;

/**
 * Incoming chat request from Android client.
 */
public class ChatRequestDto {

    private String conversationId;  // null for new conversation
    private String message;         // user's text message

    public ChatRequestDto() {}

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
