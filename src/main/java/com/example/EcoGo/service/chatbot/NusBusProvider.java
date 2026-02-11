package com.example.EcoGo.service.chatbot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.*;

/**
 * Real NUS NextBus API provider for campus bus arrival data.
 * API doc: https://suibianp.github.io/nus-nextbus-new-api/
 *
 * Calls https://nnextbus.nus.edu.sg/ShuttleService?busstopname={code}
 * with Basic Auth credentials (configured via application.yaml).
 *
 * Falls back to empty results on API failure (never throws).
 */
@Service
public class NusBusProvider {

    private static final Logger log = LoggerFactory.getLogger(NusBusProvider.class);

    private static final String BASE_URL = "https://nnextbus.nus.edu.sg";

    @Value("${chatbot.nus-bus.username}")
    private String username;

    @Value("${chatbot.nus-bus.password}")
    private String password;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /** Known NUS shuttle route names — used to distinguish routes from stops. */
    private static final Set<String> KNOWN_ROUTES = Set.of(
            "A1", "A2", "D1", "D2", "K", "E", "BTC", "L",
            "A1E", "A2E", "D1E", "D2E"
    );

    /** Map of Chinese/English aliases to NUS NextBus stop codes (based on real /BusStops API). */
    private static final Map<String, String> STOP_CODE_MAP = new LinkedHashMap<>();

    static {
        // === Actual stop codes from /BusStops API ===
        STOP_CODE_MAP.put("PGP", "PGP");
        STOP_CODE_MAP.put("PGPR", "PGPR");
        STOP_CODE_MAP.put("COM3", "COM3");
        STOP_CODE_MAP.put("COM2", "COM3");          // closest match
        STOP_CODE_MAP.put("UTOWN", "UTOWN");
        STOP_CODE_MAP.put("UTown", "UTOWN");
        STOP_CODE_MAP.put("KR-MRT", "KR-MRT");
        STOP_CODE_MAP.put("BIZ2", "BIZ2");
        STOP_CODE_MAP.put("TCOMS", "TCOMS");
        STOP_CODE_MAP.put("CLB", "CLB");
        STOP_CODE_MAP.put("YIH", "YIH");
        STOP_CODE_MAP.put("IT", "IT");
        STOP_CODE_MAP.put("MUSEUM", "MUSEUM");
        STOP_CODE_MAP.put("RAFFLES", "RAFFLES");
        STOP_CODE_MAP.put("KV", "KV");
        STOP_CODE_MAP.put("LT13", "LT13");
        STOP_CODE_MAP.put("AS5", "AS5");
        STOP_CODE_MAP.put("LT27", "LT27");
        STOP_CODE_MAP.put("S17", "S17");
        STOP_CODE_MAP.put("UHC", "UHC");
        STOP_CODE_MAP.put("UHALL", "UHALL");
        STOP_CODE_MAP.put("KRB", "KRB");
        STOP_CODE_MAP.put("BG-MRT", "BG-MRT");
        STOP_CODE_MAP.put("OTH", "OTH");
        STOP_CODE_MAP.put("CG", "CG");
        // Opposite-side stops
        STOP_CODE_MAP.put("HSSML-OPP", "HSSML-OPP");
        STOP_CODE_MAP.put("NUSS-OPP", "NUSS-OPP");
        STOP_CODE_MAP.put("LT13-OPP", "LT13-OPP");
        STOP_CODE_MAP.put("YIH-OPP", "YIH-OPP");
        STOP_CODE_MAP.put("SDE3-OPP", "SDE3-OPP");
        STOP_CODE_MAP.put("UHC-OPP", "UHC-OPP");
        STOP_CODE_MAP.put("UHALL-OPP", "UHALL-OPP");
        STOP_CODE_MAP.put("KR-MRT-OPP", "KR-MRT-OPP");
        STOP_CODE_MAP.put("TCOMS-OPP", "TCOMS-OPP");
        STOP_CODE_MAP.put("JP-SCH-16151", "JP-SCH-16151");

        // === Chinese aliases ===
        STOP_CODE_MAP.put("王子岭", "PGP");
        STOP_CODE_MAP.put("大学城", "UTOWN");
        STOP_CODE_MAP.put("肯特岗地铁站", "KR-MRT");
        STOP_CODE_MAP.put("肯特岗地铁", "KR-MRT");
        STOP_CODE_MAP.put("商学院", "BIZ2");
        STOP_CODE_MAP.put("中央图书馆", "CLB");
        STOP_CODE_MAP.put("图书馆", "CLB");
        STOP_CODE_MAP.put("博物馆", "MUSEUM");
        STOP_CODE_MAP.put("莱佛士", "RAFFLES");
        STOP_CODE_MAP.put("植物园地铁站", "BG-MRT");
        STOP_CODE_MAP.put("植物园地铁", "BG-MRT");
        STOP_CODE_MAP.put("肯特岗巴士总站", "KRB");
        STOP_CODE_MAP.put("大学堂", "UHALL");
        STOP_CODE_MAP.put("校医院", "UHC");
        STOP_CODE_MAP.put("绿色学院", "CG");

        // === English aliases ===
        STOP_CODE_MAP.put("Kent Ridge MRT", "KR-MRT");
        STOP_CODE_MAP.put("Botanic Gardens MRT", "BG-MRT");
        STOP_CODE_MAP.put("University Town", "UTOWN");
        STOP_CODE_MAP.put("Central Library", "CLB");
        STOP_CODE_MAP.put("Kent Ridge Bus Terminal", "KRB");
        STOP_CODE_MAP.put("Prince George Park", "PGP");
        STOP_CODE_MAP.put("Raffles Hall", "RAFFLES");
        STOP_CODE_MAP.put("Kent Vale", "KV");
        STOP_CODE_MAP.put("University Hall", "UHALL");
        STOP_CODE_MAP.put("Information Technology", "IT");
        STOP_CODE_MAP.put("Yusof Ishak House", "YIH");
        STOP_CODE_MAP.put("College Green", "CG");
    }

    public NusBusProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Check if a token looks like a known bus route name (e.g., "D1", "A2", "K").
     */
    public static boolean isRouteName(String token) {
        if (token == null) return false;
        return KNOWN_ROUTES.contains(token.toUpperCase().trim());
    }

    /**
     * Get bus arrivals for a stop. Resolves aliases to NUS NextBus stop codes,
     * calls the real API, and returns structured results.
     *
     * @param stopName User-provided stop name (Chinese or English), can be null
     * @param route    Optional route filter (e.g., "D1", "A1")
     * @return BusArrivalsResult with arrivals list
     */
    public BusArrivalsResult getArrivals(String stopName, String route) {
        String effectiveStop = (stopName != null && !stopName.isBlank()) ? stopName.trim() : "UTOWN";

        // If the "stop" is actually a route name, use default stop and set it as route filter
        if (isRouteName(effectiveStop) && (route == null || route.isBlank())) {
            route = effectiveStop.toUpperCase();
            effectiveStop = "UTOWN";  // default stop when only route is specified
        }

        // Resolve to NUS NextBus stop code
        String stopCode = resolveStopCode(effectiveStop);

        try {
            List<Map<String, Object>> arrivals = fetchFromApi(stopCode, effectiveStop);

            // Filter by route if specified
            if (route != null && !route.isBlank()) {
                String r = route.trim().toUpperCase();
                List<Map<String, Object>> filtered = arrivals.stream()
                        .filter(a -> r.equalsIgnoreCase(String.valueOf(a.get("route"))))
                        .toList();
                if (!filtered.isEmpty()) {
                    arrivals = filtered;
                }
            }

            // Build display stop name
            String displayStop = effectiveStop;
            if (route != null && !route.isBlank()) {
                displayStop = route.toUpperCase() + "@" + stopCode;
            }

            return new BusArrivalsResult(displayStop, arrivals);
        } catch (Exception e) {
            log.warn("[NUS_BUS] API call failed for stop={}, code={}: {}", effectiveStop, stopCode, e.getMessage());
            return new BusArrivalsResult(effectiveStop, List.of());
        }
    }

    /**
     * Resolve user input to NUS NextBus stop code.
     */
    private String resolveStopCode(String input) {
        // Exact match
        if (STOP_CODE_MAP.containsKey(input)) return STOP_CODE_MAP.get(input);

        // Case-insensitive match
        for (Map.Entry<String, String> entry : STOP_CODE_MAP.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(input)) return entry.getValue();
        }

        // Partial match
        for (Map.Entry<String, String> entry : STOP_CODE_MAP.entrySet()) {
            if (entry.getKey().toLowerCase().contains(input.toLowerCase())
                    || input.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }

        // Fallback: use input directly as code
        return input.toUpperCase().replace(" ", "-");
    }

    /**
     * Call the real NUS NextBus ShuttleService API.
     */
    private List<Map<String, Object>> fetchFromApi(String stopCode, String displayName) throws Exception {
        String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        URI uri = URI.create(BASE_URL + "/ShuttleService?busstopname=" + stopCode);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .header("Authorization", "Basic " + auth)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        log.info("[NUS_BUS] Calling: {}", uri);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("[NUS_BUS] API returned status {} for stop={}", response.statusCode(), stopCode);
            return List.of();
        }

        log.debug("[NUS_BUS] Raw response length: {}", response.body().length());

        // Parse response
        NusNextBusResponse apiResponse = objectMapper.readValue(response.body(), NusNextBusResponse.class);
        if (apiResponse.shuttleServiceResult == null || apiResponse.shuttleServiceResult.shuttles == null) {
            log.warn("[NUS_BUS] Empty ShuttleServiceResult for stop={}", stopCode);
            return List.of();
        }

        List<Map<String, Object>> arrivals = new ArrayList<>();
        for (NusShuttle shuttle : apiResponse.shuttleServiceResult.shuttles) {
            String routeName = shuttle.name;
            if (routeName == null || routeName.isBlank()) continue;

            // Parse direction from busstopcode (e.g., "COM3-D1-S" -> "S", "COM3-D1-E" -> "E")
            String direction = "loop";
            if (shuttle.busstopcode != null && shuttle.busstopcode.contains("-")) {
                String[] parts = shuttle.busstopcode.split("-");
                String lastPart = parts[parts.length - 1];
                if ("S".equals(lastPart)) direction = "start";
                else if ("E".equals(lastPart)) direction = "end";
            }

            // Method 1: Use _etas array (more detailed, includes multiple upcoming buses)
            if (shuttle.etas != null && !shuttle.etas.isEmpty()) {
                for (NusEta eta : shuttle.etas) {
                    int etaMinutes = eta.eta != null ? eta.eta : -1;
                    if (etaMinutes < 0) continue;

                    String status;
                    if (etaMinutes <= 1) status = "arriving";
                    else if (etaMinutes <= 8) status = "on_time";
                    else status = "scheduled";

                    Map<String, Object> arrival = new LinkedHashMap<>();
                    arrival.put("route", routeName.trim());
                    arrival.put("direction", direction);
                    arrival.put("etaMinutes", etaMinutes);
                    arrival.put("status", status);
                    if (eta.plate != null && !eta.plate.isBlank()) {
                        arrival.put("plate", eta.plate);
                    }
                    if (eta.ts != null && !eta.ts.isBlank()) {
                        arrival.put("scheduledTime", eta.ts);
                    }
                    arrivals.add(arrival);
                }
            }
            // Method 2: Fallback to arrivalTime/nextArrivalTime fields
            else {
                addArrivalFromLegacyFields(arrivals, routeName, direction,
                        shuttle.arrivalTime, shuttle.arrivalTimeVehPlate);
                addArrivalFromLegacyFields(arrivals, routeName, direction,
                        shuttle.nextArrivalTime, shuttle.nextArrivalTimeVehPlate);
            }
        }

        // Sort by ETA
        arrivals.sort(Comparator.comparingInt(a -> (int) a.get("etaMinutes")));

        log.info("[NUS_BUS] {} returned {} arrivals for {}", stopCode, arrivals.size(), displayName);
        return arrivals;
    }

    /**
     * Parse legacy arrivalTime/nextArrivalTime string fields (e.g., "5", "-1", "").
     */
    private void addArrivalFromLegacyFields(List<Map<String, Object>> arrivals,
                                             String routeName, String direction,
                                             String etaStr, String plate) {
        if (etaStr == null || etaStr.isBlank() || etaStr.equals("-")) return;
        try {
            int etaMinutes = Integer.parseInt(etaStr.trim());
            if (etaMinutes < 0) return;

            String status;
            if (etaMinutes <= 1) status = "arriving";
            else if (etaMinutes <= 8) status = "on_time";
            else status = "scheduled";

            Map<String, Object> arrival = new LinkedHashMap<>();
            arrival.put("route", routeName.trim());
            arrival.put("direction", direction);
            arrival.put("etaMinutes", etaMinutes);
            arrival.put("status", status);
            if (plate != null && !plate.isBlank()) {
                arrival.put("plate", plate);
            }
            arrivals.add(arrival);
        } catch (NumberFormatException ignored) {
            // Not a valid ETA number
        }
    }

    // ========== NUS NextBus API Response Models ==========
    // Based on real API response from https://nnextbus.nus.edu.sg/ShuttleService

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NusNextBusResponse {
        @JsonProperty("ShuttleServiceResult")
        public NusShuttleServiceResult shuttleServiceResult;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NusShuttleServiceResult {
        @JsonProperty("TimeStamp")
        public String timestamp;
        @JsonProperty("name")
        public String name;
        @JsonProperty("caption")
        public String caption;
        @JsonProperty("shuttles")
        public List<NusShuttle> shuttles;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NusShuttle {
        @JsonProperty("name")
        public String name;
        @JsonProperty("busstopcode")
        public String busstopcode;
        @JsonProperty("routeid")
        public Integer routeid;

        // New API: detailed ETA array
        @JsonProperty("_etas")
        public List<NusEta> etas;

        // Legacy flat fields (used as fallback)
        @JsonProperty("arrivalTime")
        public String arrivalTime;
        @JsonProperty("arrivalTime_veh_plate")
        public String arrivalTimeVehPlate;
        @JsonProperty("nextArrivalTime")
        public String nextArrivalTime;
        @JsonProperty("nextArrivalTime_veh_plate")
        public String nextArrivalTimeVehPlate;
        @JsonProperty("passengers")
        public String passengers;
        @JsonProperty("nextPassengers")
        public String nextPassengers;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NusEta {
        @JsonProperty("eta")
        public Integer eta;          // minutes until arrival
        @JsonProperty("eta_s")
        public Integer etaSeconds;   // seconds until arrival
        @JsonProperty("plate")
        public String plate;         // vehicle plate number
        @JsonProperty("ts")
        public String ts;            // scheduled timestamp
        @JsonProperty("jobid")
        public Integer jobid;
    }

    // ========== Result record ==========

    public record BusArrivalsResult(String stopName, List<Map<String, Object>> arrivals) {}
}
