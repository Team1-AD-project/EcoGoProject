package com.example.EcoGo.service.chatbot;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Mock bus arrival data provider for Singapore bus stops.
 * In production, this would call a real transit API (e.g., LTA DataMall).
 */
@Service
public class MockBusProvider {

    /** Known NUS / Singapore bus stops with realistic multi-route data. */
    private static final Map<String, List<StopRoute>> STOP_DATA = new LinkedHashMap<>();

    static {
        // NUS campus stops
        STOP_DATA.put("COM2", List.of(
                new StopRoute("A1", "up", 2, "on_time"),
                new StopRoute("A1", "up", 10, "on_time"),
                new StopRoute("D2", "down", 5, "on_time")
        ));
        STOP_DATA.put("PGP", List.of(
                new StopRoute("A1", "down", 4, "on_time"),
                new StopRoute("A2", "up", 7, "on_time"),
                new StopRoute("D2", "up", 3, "on_time")
        ));
        STOP_DATA.put("UTown", List.of(
                new StopRoute("D1", "up", 3, "on_time"),
                new StopRoute("D1", "down", 15, "delayed"),
                new StopRoute("D2", "up", 6, "on_time")
        ));
        STOP_DATA.put("KR MRT", List.of(
                new StopRoute("A1", "up", 1, "arriving"),
                new StopRoute("A2", "down", 8, "on_time"),
                new StopRoute("D1", "up", 12, "on_time")
        ));
        STOP_DATA.put("BIZ2", List.of(
                new StopRoute("A1", "up", 6, "on_time"),
                new StopRoute("A2", "up", 9, "on_time")
        ));
        STOP_DATA.put("TCOMS", List.of(
                new StopRoute("A2", "down", 4, "on_time"),
                new StopRoute("D2", "down", 11, "delayed")
        ));

        // Singapore city stops
        STOP_DATA.put("乌节路", List.of(
                new StopRoute("7", "up", 3, "on_time"),
                new StopRoute("14", "up", 5, "on_time"),
                new StopRoute("36", "down", 8, "on_time"),
                new StopRoute("77", "up", 12, "delayed")
        ));
        STOP_DATA.put("滨海湾", List.of(
                new StopRoute("36", "up", 2, "arriving"),
                new StopRoute("97", "down", 6, "on_time"),
                new StopRoute("106", "up", 10, "on_time")
        ));
        STOP_DATA.put("莱佛士坊", List.of(
                new StopRoute("10", "up", 4, "on_time"),
                new StopRoute("75", "down", 7, "on_time"),
                new StopRoute("100", "up", 15, "delayed")
        ));
        STOP_DATA.put("牛车水", List.of(
                new StopRoute("2", "up", 5, "on_time"),
                new StopRoute("12", "down", 8, "on_time"),
                new StopRoute("33", "up", 3, "arriving")
        ));
        STOP_DATA.put("人民广场", List.of(
                new StopRoute("20", "up", 3, "on_time"),
                new StopRoute("20", "up", 12, "on_time"),
                new StopRoute("51", "down", 6, "on_time")
        ));
        STOP_DATA.put("Orchard", List.of(
                new StopRoute("7", "up", 3, "on_time"),
                new StopRoute("14", "up", 5, "on_time"),
                new StopRoute("36", "down", 8, "on_time")
        ));
        STOP_DATA.put("Marina Bay", List.of(
                new StopRoute("36", "up", 2, "arriving"),
                new StopRoute("97", "down", 6, "on_time"),
                new StopRoute("106", "up", 10, "on_time")
        ));
        STOP_DATA.put("Clementi", List.of(
                new StopRoute("96", "up", 4, "on_time"),
                new StopRoute("165", "down", 9, "on_time"),
                new StopRoute("7", "up", 13, "on_time")
        ));
    }

    public BusArrivalsResult getArrivals(String stopName, String route) {
        String effectiveStop = (stopName != null && !stopName.isBlank()) ? stopName.trim() : "人民广场";

        // Try exact match first, then case-insensitive, then partial
        List<StopRoute> routes = findRoutes(effectiveStop);

        // Filter by route if specified
        if (route != null && !route.isBlank()) {
            String r = route.trim();
            List<StopRoute> filtered = routes.stream()
                    .filter(sr -> sr.route.equalsIgnoreCase(r))
                    .toList();
            if (!filtered.isEmpty()) {
                routes = filtered;
            }
        }

        List<Map<String, Object>> arrivals = new ArrayList<>();
        for (StopRoute sr : routes) {
            arrivals.add(Map.of(
                    "route", sr.route,
                    "direction", sr.direction,
                    "etaMinutes", sr.etaMinutes,
                    "status", sr.status
            ));
        }

        return new BusArrivalsResult(effectiveStop, arrivals);
    }

    private List<StopRoute> findRoutes(String stopName) {
        // Exact match
        if (STOP_DATA.containsKey(stopName)) {
            return STOP_DATA.get(stopName);
        }
        // Case-insensitive match
        for (Map.Entry<String, List<StopRoute>> entry : STOP_DATA.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(stopName)) {
                return entry.getValue();
            }
        }
        // Partial match
        for (Map.Entry<String, List<StopRoute>> entry : STOP_DATA.entrySet()) {
            if (entry.getKey().contains(stopName) || stopName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        // Default fallback: generate generic arrivals
        return List.of(
                new StopRoute("A1", "up", 5, "on_time"),
                new StopRoute("A2", "down", 10, "on_time")
        );
    }

    public record BusArrivalsResult(String stopName, List<Map<String, Object>> arrivals) {}

    private record StopRoute(String route, String direction, int etaMinutes, String status) {}
}
