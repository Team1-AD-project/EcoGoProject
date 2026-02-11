package com.example.EcoGo.service.chatbot;

import com.example.EcoGo.dto.chatbot.ChatResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class PythonChatbotProxyServiceTest {

    private PythonChatbotProxyService proxyService;

    @BeforeEach
    void setUp() throws Exception {
        proxyService = new PythonChatbotProxyService();
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field f = PythonChatbotProxyService.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(proxyService, value);
    }

    // ---------- isEnabled ----------
    @Test
    void isEnabled_whenDisabled_shouldReturnFalse() throws Exception {
        setField("pythonEnabled", false);

        assertFalse(proxyService.isEnabled());
    }

    @Test
    void isEnabled_whenEnabled_shouldReturnTrue() throws Exception {
        setField("pythonEnabled", true);

        assertTrue(proxyService.isEnabled());
    }

    // ---------- forwardChat ----------
    @Test
    void forwardChat_whenDisabled_shouldReturnNull() throws Exception {
        setField("pythonEnabled", false);

        ChatResponseDto result = proxyService.forwardChat("u_001", "user", "c1", "hello");

        assertNull(result);
    }

    @Test
    void forwardChat_whenEnabled_unreachableServer_shouldReturnNull() throws Exception {
        setField("pythonEnabled", true);
        setField("pythonBaseUrl", "http://localhost:1"); // unreachable port
        setField("timeoutSeconds", 1);

        ChatResponseDto result = proxyService.forwardChat("u_001", "user", "c1", "hello");

        // Should return null due to unreachable server (graceful degradation)
        assertNull(result);
    }

    @Test
    void forwardChat_whenDisabled_shouldNotCallHttpClient() throws Exception {
        setField("pythonEnabled", false);

        // When disabled, should immediately return null without making HTTP calls
        ChatResponseDto result = proxyService.forwardChat("u_001", "admin", "c1", "test message");

        assertNull(result);
    }
}
