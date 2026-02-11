package com.example.EcoGo.service.chatbot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class ModelClientServiceTest {

    private ModelClientService modelClientService;

    @BeforeEach
    void setUp() throws Exception {
        modelClientService = new ModelClientService();
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field f = ModelClientService.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(modelClientService, value);
    }

    // ---------- isEnabled ----------
    @Test
    void isEnabled_whenDisabled_shouldReturnFalse() throws Exception {
        setField("modelEnabled", false);

        assertFalse(modelClientService.isEnabled());
    }

    @Test
    void isEnabled_whenEnabled_shouldReturnTrue() throws Exception {
        setField("modelEnabled", true);

        assertTrue(modelClientService.isEnabled());
    }

    // ---------- callModelForTool ----------
    @Test
    void callModelForTool_whenDisabled_shouldReturnNull() throws Exception {
        setField("modelEnabled", false);

        ModelClientService.ModelResult result = modelClientService.callModelForTool("hello");

        assertNull(result);
    }

    @Test
    void callModelForTool_whenEnabled_unreachableServer_shouldReturnNull() throws Exception {
        setField("modelEnabled", true);
        setField("modelBaseUrl", "http://localhost:1"); // unreachable port
        setField("modelApiKey", "test-key");
        setField("modelName", "test-model");

        ModelClientService.ModelResult result = modelClientService.callModelForTool("book a trip");

        // Should return null due to unreachable server (graceful degradation)
        assertNull(result);
    }

    // ---------- ModelResult record ----------
    @Test
    void modelResult_withTextOnly_shouldHaveNullToolCall() {
        ModelClientService.ModelResult result = new ModelClientService.ModelResult("some text", null);

        assertEquals("some text", result.text());
        assertNull(result.toolCall());
    }

    @Test
    void modelResult_withToolCall_shouldContainToolCallData() {
        ModelClientService.ToolCall tc = new ModelClientService.ToolCall("create_booking",
                java.util.Map.of("fromName", "PGP", "toName", "CLB"));
        ModelClientService.ModelResult result = new ModelClientService.ModelResult("", tc);

        assertNotNull(result.toolCall());
        assertEquals("create_booking", result.toolCall().name());
        assertEquals("PGP", result.toolCall().arguments().get("fromName"));
    }

    // ---------- ToolCall record ----------
    @Test
    void toolCall_shouldStoreNameAndArguments() {
        ModelClientService.ToolCall tc = new ModelClientService.ToolCall("get_bus_arrivals",
                java.util.Map.of("stopName", "COM3", "route", "D2"));

        assertEquals("get_bus_arrivals", tc.name());
        assertEquals("COM3", tc.arguments().get("stopName"));
        assertEquals("D2", tc.arguments().get("route"));
    }
}
