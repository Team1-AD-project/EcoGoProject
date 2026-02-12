package com.example.EcoGo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WeatherService weatherService;

    @BeforeEach
    void setUp() throws Exception {
        // Inject @Value fields via reflection
        Field apiKeyField = WeatherService.class.getDeclaredField("apiKey");
        apiKeyField.setAccessible(true);
        apiKeyField.set(weatherService, "test-api-key");

        Field weatherUrlField = WeatherService.class.getDeclaredField("weatherUrl");
        weatherUrlField.setAccessible(true);
        weatherUrlField.set(weatherService, "https://api.openweathermap.org/data/2.5/weather");
    }

    // ---------- helpers ----------

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildWeatherApiResponse() {
        Map<String, Object> main = new HashMap<>();
        main.put("temp", 30.5);
        main.put("humidity", 80);

        Map<String, Object> weatherItem = new HashMap<>();
        weatherItem.put("main", "Clear");
        weatherItem.put("description", "clear sky");
        weatherItem.put("icon", "01d");

        Map<String, Object> response = new HashMap<>();
        response.put("main", main);
        response.put("weather", List.of(weatherItem));
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildPollutionApiResponse() {
        Map<String, Object> pMain = new HashMap<>();
        pMain.put("aqi", 2);

        Map<String, Object> listItem = new HashMap<>();
        listItem.put("main", pMain);

        Map<String, Object> response = new HashMap<>();
        response.put("list", List.of(listItem));
        return response;
    }

    // ========== getWeather ==========

    @Test
    void getWeather_success_fullData() {
        // First call = weather API, Second call = pollution API
        when(restTemplate.getForObject(contains("/weather?"), eq(Map.class)))
                .thenReturn(buildWeatherApiResponse());
        when(restTemplate.getForObject(contains("/air_pollution?"), eq(Map.class)))
                .thenReturn(buildPollutionApiResponse());

        Map<String, Object> result = weatherService.getWeather(1.3521, 103.8198);

        assertNotNull(result);
        assertEquals(31L, result.get("temperature")); // Math.round(30.5) = 31
        assertEquals("80%", result.get("humidity"));
        assertEquals("Clear", result.get("condition"));
        assertEquals("clear sky", result.get("description"));
        assertEquals("01d", result.get("icon"));
        assertEquals(2, result.get("aqiLevel"));
        assertEquals("AQI Level 2", result.get("aqiText"));
    }

    @Test
    void getWeather_success_noAqi() {
        when(restTemplate.getForObject(contains("/weather?"), eq(Map.class)))
                .thenReturn(buildWeatherApiResponse());
        when(restTemplate.getForObject(contains("/air_pollution?"), eq(Map.class)))
                .thenReturn(null);

        Map<String, Object> result = weatherService.getWeather(1.3521, 103.8198);

        assertNotNull(result);
        assertEquals(31L, result.get("temperature"));
        assertEquals("Clear", result.get("condition"));
        assertNull(result.get("aqiLevel"));
    }

    @Test
    void getWeather_success_noWeatherData() {
        when(restTemplate.getForObject(contains("/weather?"), eq(Map.class)))
                .thenReturn(null);
        when(restTemplate.getForObject(contains("/air_pollution?"), eq(Map.class)))
                .thenReturn(buildPollutionApiResponse());

        Map<String, Object> result = weatherService.getWeather(1.3521, 103.8198);

        assertNotNull(result);
        assertNull(result.get("temperature"));
        assertNull(result.get("condition"));
        assertEquals(2, result.get("aqiLevel"));
    }

    @Test
    void getWeather_success_emptyWeatherList() {
        Map<String, Object> weatherResp = new HashMap<>();
        weatherResp.put("main", Map.of("temp", 28.0, "humidity", 75));
        weatherResp.put("weather", List.of()); // empty weather list

        when(restTemplate.getForObject(contains("/weather?"), eq(Map.class)))
                .thenReturn(weatherResp);
        when(restTemplate.getForObject(contains("/air_pollution?"), eq(Map.class)))
                .thenReturn(null);

        Map<String, Object> result = weatherService.getWeather(1.3521, 103.8198);

        assertEquals(28L, result.get("temperature"));
        assertNull(result.get("condition")); // no weather item
    }

    @Test
    void getWeather_success_emptyPollutionList() {
        when(restTemplate.getForObject(contains("/weather?"), eq(Map.class)))
                .thenReturn(buildWeatherApiResponse());

        Map<String, Object> pollutionResp = new HashMap<>();
        pollutionResp.put("list", List.of()); // empty list

        when(restTemplate.getForObject(contains("/air_pollution?"), eq(Map.class)))
                .thenReturn(pollutionResp);

        Map<String, Object> result = weatherService.getWeather(1.3521, 103.8198);

        assertNotNull(result);
        assertEquals("Clear", result.get("condition"));
        assertNull(result.get("aqiLevel")); // no pollution data
    }

    @Test
    void getWeather_apiException() {
        when(restTemplate.getForObject(contains("/weather?"), eq(Map.class)))
                .thenThrow(new RuntimeException("API connection failed"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> weatherService.getWeather(1.3521, 103.8198));
        assertEquals("获取天气服务异常", ex.getMessage());
    }

    @Test
    void getWeather_temperatureRounding() {
        Map<String, Object> main = new HashMap<>();
        main.put("temp", 25.4); // should round to 25
        main.put("humidity", 60);

        Map<String, Object> weatherResp = new HashMap<>();
        weatherResp.put("main", main);
        weatherResp.put("weather", List.of(Map.of("main", "Clouds", "description", "few clouds", "icon", "02d")));

        when(restTemplate.getForObject(contains("/weather?"), eq(Map.class)))
                .thenReturn(weatherResp);
        when(restTemplate.getForObject(contains("/air_pollution?"), eq(Map.class)))
                .thenReturn(null);

        Map<String, Object> result = weatherService.getWeather(1.3521, 103.8198);

        assertEquals(25L, result.get("temperature")); // Math.round(25.4) = 25
    }
}
