package com.example.EcoGo.controller;

import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WeatherControllerTest {

    private WeatherService weatherService;
    private WeatherController controller;

    private static final double SG_LAT = 1.3521;
    private static final double SG_LON = 103.8198;

    @BeforeEach
    void setUp() throws Exception {
        weatherService = mock(WeatherService.class);
        controller = new WeatherController();

        Field f = WeatherController.class.getDeclaredField("weatherService");
        f.setAccessible(true);
        f.set(controller, weatherService);
    }

    // ========== getCurrentWeather ==========

    @Test
    void getCurrentWeather_success() {
        Map<String, Object> mockData = Map.of(
                "temperature", 30L,
                "humidity", "80%",
                "condition", "Clear",
                "description", "clear sky",
                "icon", "01d",
                "aqiLevel", 2,
                "aqiText", "AQI Level 2"
        );
        when(weatherService.getWeather(SG_LAT, SG_LON)).thenReturn(mockData);

        ResponseMessage<Object> resp = controller.getCurrentWeather();

        assertEquals(200, resp.getCode());
        assertNotNull(resp.getData());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.getData();
        assertEquals(30L, data.get("temperature"));
        assertEquals("80%", data.get("humidity"));
        assertEquals("Clear", data.get("condition"));
        assertEquals(2, data.get("aqiLevel"));
        verify(weatherService).getWeather(SG_LAT, SG_LON);
    }

    @Test
    void getCurrentWeather_emptyResult() {
        when(weatherService.getWeather(SG_LAT, SG_LON)).thenReturn(Map.of());

        ResponseMessage<Object> resp = controller.getCurrentWeather();

        assertEquals(200, resp.getCode());
        assertNotNull(resp.getData());
    }

    @Test
    void getCurrentWeather_serviceException() {
        when(weatherService.getWeather(SG_LAT, SG_LON))
                .thenThrow(new RuntimeException("获取天气服务异常"));

        ResponseMessage<Object> resp = controller.getCurrentWeather();

        assertEquals(500, resp.getCode());
        assertTrue(resp.getMessage().contains("获取天气信息失败"));
        assertNull(resp.getData());
    }

    @Test
    void getCurrentWeather_networkException() {
        when(weatherService.getWeather(SG_LAT, SG_LON))
                .thenThrow(new RuntimeException("Connection timeout"));

        ResponseMessage<Object> resp = controller.getCurrentWeather();

        assertEquals(500, resp.getCode());
        assertTrue(resp.getMessage().contains("Connection timeout"));
        assertNull(resp.getData());
    }
}
