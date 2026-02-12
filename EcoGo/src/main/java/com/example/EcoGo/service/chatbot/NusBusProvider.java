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

    // ======= Common literals (Sonar S1192) =======
    private static final String DEFAULT_STOP = "UTOWN";

    private static final String STOP_PGP = "PGP";
    private static final String STOP_PGPR = "PGPR";
    private static final String STOP_COM3 = "COM3";
    private static final String STOP_COM2 = "COM2";
    private static final String STOP_UTOWN = "UTOWN";
    private static final String STOP_KR_MRT = "KR-MRT";
    private static final String STOP_BG_MRT = "BG-MRT";
    private static final String STOP_BIZ2 = "BIZ2";
    private static final String STOP_TCOMS = "TCOMS";
    private static final String STOP_CLB = "CLB";
    private static final String STOP_YIH = "YIH";
    private static final String STOP_IT = "IT";
    private static final String STOP_MUSEUM = "MUSEUM";
    private static final String STOP_RAFFLES = "RAFFLES";
    private static final String STOP_KV = "KV";
    private static final String STOP_LT13 = "LT13";
    private static final String STOP_AS5 = "AS5";
    private static final String STOP_LT27 = "LT27";
    private static final String STOP_S17 = "S17";
    private static final String STOP_UHC = "UHC";
    private static final String STOP_UHALL = "UHALL";
    private static final String STOP_KRB = "KRB";
    private static final String STOP_OTH = "OTH";
    private static final String STOP_CG = "CG";

    private static final String SUFFIX_OPP = "-OPP";

    // arrival map keys
    private static final String KEY_ROUTE = "route";
    private static final String KEY_DIRECTION = "direction";
    private static final String KEY_ETA_MINUTES = "etaMinutes";
    private static final String KEY_STATUS = "status";
    private static final String KEY_PLATE = "plate";
    private static final String KEY_SCHEDULED_TIME = "scheduledTime";

    // status values
    private static final String STATUS_ARRIVING = "arriving";
    private static final String STATUS_ON_TIME = "on_time";
    private static final String STATUS_SCHEDULED = "scheduled";

    // direction values
    private static final String DIR_LOOP = "loop";
    private static final String DIR_START = "start";
    private static final String DIR_END = "end";

    // ======= Config =======
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
        STOP_CODE_MAP.put(STOP_PGP, STOP_PGP);
        STOP_CODE_MAP.put(STOP_PGPR, STOP_PGPR);
        STOP_CODE_MAP.put(STOP_COM3, STOP_COM3);
        STOP_CODE_MAP.put(STOP_COM2, STOP_COM3); // closest match
        STOP_CODE_MAP.put(STOP_UTOWN, STOP_UTOWN);
        STOP_CODE_MAP.put("UTown", STOP_UTOWN);
        STOP_CODE_MAP.put(STOP_KR_MRT, STOP_KR_MRT);
        STOP_CODE_MAP.put(STOP_BIZ2, STOP_BIZ2);
        STOP_CODE_MAP.put(STOP_TCOMS, STOP_TCOMS);
        STOP_CODE_MAP.put(STOP_CLB, STOP_CLB);
        STOP_CODE_MAP.put(STOP_YIH, STOP_YIH);
        STOP_CODE_MAP.put(STOP_IT, STOP_IT);
        STOP_CODE_MAP.put(STOP_MUSEUM, STOP_MUSEUM);
        STOP_CODE_MAP.put(STOP_RAFFLES, STOP_RAFFLES);
        STOP_CODE_MAP.put(STOP_KV, STOP_KV);
        STOP_CODE_MAP.put(STOP_LT13, STOP_LT13);
        STOP_CODE_MAP.put(STOP_AS5, STOP_AS5);
        STOP_CODE_MAP.put(STOP_LT27, STOP_LT27);
        STOP_CODE_MAP.put(STOP_S17, STOP_S17);
        STOP_CODE_MAP.put(STOP_UHC, STOP_UHC);
        STOP_CODE_MAP.put(STOP_UHALL, STOP_UHALL);
        STOP_CODE_MAP.put(STOP_KRB, STOP_KRB);
        STOP_CODE_MAP.put(STOP_BG_MRT, STOP_BG_MRT);
        STOP_CODE_MAP.put(STOP_OTH, STOP_OTH);
        STOP_CODE_MAP.put(STOP_CG, STOP_CG);

        // Opposite-side stops
        STOP_CODE_MAP.put("HSSML" + SUFFIX_OPP, "HSSML" + SUFFIX_OPP);
        STOP_CODE_MAP.put("NUSS" + SUFFIX_OPP, "NUSS" + SUFFIX_OPP);
        STOP_CODE_MAP.put(STOP_LT13 + SUFFIX_OPP, STOP_LT13 + SUFFIX_OPP);
        STOP_CODE_MAP.put(STOP_YIH + SUFFIX_OPP, STOP_YIH + SUFFIX_OPP);
        STOP_CODE_MAP.put("SDE3" + SUFFIX_OPP, "SDE3" + SUFFIX_OPP);
        STOP_CODE_MAP.put(STOP_UHC + SUFFIX_OPP, STOP_UHC + SUFFIX_OPP);
        STOP_CODE_MAP.put(STOP_UHALL + SUFFIX_OPP, STOP_UHALL + SUFFIX_OPP);
        STOP_CODE_MAP.put(STOP_KR_MRT + SUFFIX_OPP, STOP_KR_MRT + SUFFIX_OPP);
        STOP_CODE_MAP.put(STOP_TCOMS + SUFFIX_OPP, STOP_TCOMS + SUFFIX_OPP);
        STOP_CODE_MAP.put("JP-SCH-16151", "JP-SCH-16151");

        // === Chinese aliases ===
        STOP_CODE_MAP.put("王子岭", STOP_PGP);
        STOP_CODE_MAP.put("大学城", STOP_UTOWN);
        STOP_CODE_MAP.put("肯特岗地铁站", STOP_KR_MRT);
        STOP_CODE_MAP.put("肯特岗地铁", STOP_KR_MRT);
        STOP_CODE_MAP.put("商学院", STOP_BIZ2);
        STOP_CODE_MAP.put("中央图书馆", STOP_CLB);
        STOP_CODE_MAP.put("图书馆", STOP_CLB);
        STOP_CODE_MAP.put("博物馆", STOP_MUSEUM);
        STOP_CODE_MAP.put("莱佛士", STOP_RAFFLES);
        STOP_CODE_MAP.put("植物园地铁站", STOP_BG_MRT);
        STOP_CODE_MAP.put("植物园地铁", STOP_BG_MRT);
        STOP_CODE_MAP.put("肯特岗巴士总站", STOP_KRB);
        STOP_CODE_MAP.put("大学堂", STOP_UHALL);
        STOP_CODE_MAP.put("校医院", STOP_UHC);
        STOP_CODE_MAP.put("绿色学院", STOP_CG);

        // === English aliases ===
        STOP_CODE_MAP.put("Kent Ridge MRT", STOP_KR_MRT);
        STOP_CODE_MAP.put("Botanic Gardens MRT", STOP_BG_MRT);
        STOP_CODE_MAP.put("University Town", STOP_UTOWN);
        STOP_CODE_MAP.put("Central Library", STOP_CLB);
        STOP_CODE_MAP.put("Kent Ridge Bus Terminal", STOP_KRB);
        STOP_CODE_MAP.put("Prince George Park", STOP_PGP);
        STOP_CODE_MAP.put("Raffles Hall", STOP_RAFFLES);
        STOP_CODE_MAP.put("Kent Vale", STOP_KV);
        STOP_CODE_MAP.put("University Hall", STOP_UHALL);
        STOP_CODE_MAP.put("Information Technology", STOP_IT);
        STOP_CODE_MAP.put("Yusof Ishak House", STOP_YIH);
        STOP_CODE_MAP.put("College Green", STOP_CG);
    }

    public NusBusProvider() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /** Check if a token looks like a known bus route name (e.g., "D1", "A2", "K"). */
    public static boolean isRouteName(String token) {
        if (token == null) return false;
        return KNOWN_ROUTES.contains(token.toUpperCase(Locale.ROOT).trim());
    }

    /**
     * Get bus arrivals for a stop. Resolves aliases to NUS NextBus stop codes,
     * calls the real API, and returns structured results.
     *
     * @param stopName User-provided stop name (Chinese or English), can be null
     * @param route    Optional route filter (e.g., "D1", "A1")
     */
    public BusArrivalsResult getArrivals(String stopName, String route) {
        StopRoute sr = normalizeStopAndRoute(stopName, route);
        String stopCode = resolveStopCode(sr.stop());

        try {
            List<Map<String, Object>> arrivals = fetchFromApi(stopCode, sr.stop());
            arrivals = filterByRouteIfNeeded(arrivals, sr.route());
            String displayStop = buildDisplayStop(sr.stop(), sr.route(), stopCode);
            return new BusArrivalsResult(displayStop, arrivals);
        } catch (Exception e) {
            log.warn("[NUS_BUS] API call failed for stop={}, code={}: {}", sr.stop(), stopCode, e.getMessage());
            return new BusArrivalsResult(sr.stop(), List.of());
        }
    }

    /** Normalize stop/route: if stopName is actually a route, convert it into route filter and use default stop. */
    private StopRoute normalizeStopAndRoute(String stopName, String route) {
        String effectiveStop = (stopName != null && !stopName.isBlank()) ? stopName.trim() : DEFAULT_STOP;
        String effectiveRoute = (route != null && !route.isBlank()) ? route.trim() : null;

        if (isRouteName(effectiveStop) && effectiveRoute == null) {
            effectiveRoute = effectiveStop.toUpperCase(Locale.ROOT);
            effectiveStop = DEFAULT_STOP;
        }
        return new StopRoute(effectiveStop, effectiveRoute);
    }

    private List<Map<String, Object>> filterByRouteIfNeeded(List<Map<String, Object>> arrivals, String route) {
        if (route == null || route.isBlank() || arrivals == null || arrivals.isEmpty()) return arrivals;

        String r = route.trim().toUpperCase(Locale.ROOT);
        List<Map<String, Object>> filtered = arrivals.stream()
                .filter(a -> r.equalsIgnoreCase(String.valueOf(a.get(KEY_ROUTE))))
                .toList();

        return filtered.isEmpty() ? arrivals : filtered;
    }

    private String buildDisplayStop(String effectiveStop, String route, String stopCode) {
        if (route == null || route.isBlank()) return effectiveStop;
        return route.toUpperCase(Locale.ROOT) + "@" + stopCode;
    }

    /** Resolve user input to NUS NextBus stop code. */
    private String resolveStopCode(String input) {
        if (input == null || input.isBlank()) return DEFAULT_STOP;

        // Exact match
        if (STOP_CODE_MAP.containsKey(input)) return STOP_CODE_MAP.get(input);

        // Case-insensitive match
        for (Map.Entry<String, String> entry : STOP_CODE_MAP.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(input)) return entry.getValue();
        }

        // Partial match
        String lower = input.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : STOP_CODE_MAP.entrySet()) {
            String k = entry.getKey().toLowerCase(Locale.ROOT);
            if (k.contains(lower) || lower.contains(k)) return entry.getValue();
        }

        // Fallback: use input directly as code
        return input.toUpperCase(Locale.ROOT).replace(" ", "-");
    }

    /** Call the real NUS NextBus ShuttleService API and return arrival list. */
    private List<Map<String, Object>> fetchFromApi(String stopCode, String displayName) throws Exception {
        URI uri = buildShuttleServiceUri(stopCode);
        HttpResponse<String> response = callApi(uri);

        if (response.statusCode() != 200) {
            log.warn("[NUS_BUS] API returned status {} for stop={}", response.statusCode(), stopCode);
            return List.of();
        }

        NusNextBusResponse apiResponse = parseApiResponse(response.body(), stopCode);
        if (apiResponse == null || apiResponse.shuttleServiceResult == null || apiResponse.shuttleServiceResult.shuttles == null) {
            log.warn("[NUS_BUS] Empty ShuttleServiceResult for stop={}", stopCode);
            return List.of();
        }

        List<Map<String, Object>> arrivals = buildArrivalsFromShuttles(apiResponse.shuttleServiceResult.shuttles);

        // Sort by ETA safely
        arrivals.sort(Comparator.comparingInt(a -> safeInt(a.get(KEY_ETA_MINUTES), Integer.MAX_VALUE)));

        log.info("[NUS_BUS] {} returned {} arrivals for {}", stopCode, arrivals.size(), displayName);
        return arrivals;
    }

    private URI buildShuttleServiceUri(String stopCode) {
        return URI.create(BASE_URL + "/ShuttleService?busstopname=" + stopCode);
    }

    private HttpResponse<String> callApi(URI uri) throws Exception {
        String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .header("Authorization", "Basic " + auth)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        log.info("[NUS_BUS] Calling: {}", uri);
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private NusNextBusResponse parseApiResponse(String body, String stopCode) {
        try {
            log.debug("[NUS_BUS] Raw response length: {}", body != null ? body.length() : 0);
            return objectMapper.readValue(body, NusNextBusResponse.class);
        } catch (Exception e) {
            log.warn("[NUS_BUS] Failed to parse response for stop={}: {}", stopCode, e.getMessage());
            return null;
        }
    }

    private List<Map<String, Object>> buildArrivalsFromShuttles(List<NusShuttle> shuttles) {
        List<Map<String, Object>> arrivals = new ArrayList<>();
        for (NusShuttle shuttle : shuttles) {
            if (shuttle == null) continue;
            String routeName = safeTrim(shuttle.name);
            if (routeName == null) continue;

            String direction = parseDirection(shuttle.busstopcode);

            if (hasEtas(shuttle)) {
                addArrivalsFromEtas(arrivals, routeName, direction, shuttle.etas);
            } else {
                addArrivalsFromLegacyFields(arrivals, routeName, direction, shuttle.arrivalTime, shuttle.arrivalTimeVehPlate);
                addArrivalsFromLegacyFields(arrivals, routeName, direction, shuttle.nextArrivalTime, shuttle.nextArrivalTimeVehPlate);
            }
        }
        return arrivals;
    }

    private boolean hasEtas(NusShuttle shuttle) {
        return shuttle.etas != null && !shuttle.etas.isEmpty();
    }

    private String parseDirection(String busstopcode) {
        if (busstopcode == null || !busstopcode.contains("-")) return DIR_LOOP;

        String[] parts = busstopcode.split("-");
        String last = parts[parts.length - 1];

        if ("S".equalsIgnoreCase(last)) return DIR_START;
        if ("E".equalsIgnoreCase(last)) return DIR_END;
        return DIR_LOOP;
    }

    private void addArrivalsFromEtas(List<Map<String, Object>> arrivals,
                                     String routeName, String direction,
                                     List<NusEta> etas) {
        for (NusEta eta : etas) {
            Integer etaMinObj = (eta != null) ? eta.eta : null;
            int etaMinutes = etaMinObj != null ? etaMinObj : -1;
            if (etaMinutes < 0) continue;

            Map<String, Object> arrival = buildArrival(routeName, direction, etaMinutes, statusFromEta(etaMinutes));
            String plate = safeTrim(eta.plate);
            String ts = safeTrim(eta.ts);

            if (plate != null) arrival.put(KEY_PLATE, plate);
            if (ts != null) arrival.put(KEY_SCHEDULED_TIME, ts);

            arrivals.add(arrival);
        }
    }

    /** Parse legacy arrivalTime/nextArrivalTime string fields (e.g., "5", "-1", ""). */
    private void addArrivalsFromLegacyFields(List<Map<String, Object>> arrivals,
                                             String routeName, String direction,
                                             String etaStr, String plate) {
        Integer etaMinutes = parseLegacyEtaMinutes(etaStr);
        if (etaMinutes == null || etaMinutes < 0) return;

        Map<String, Object> arrival = buildArrival(routeName, direction, etaMinutes, statusFromEta(etaMinutes));
        String safePlate = safeTrim(plate);
        if (safePlate != null) arrival.put(KEY_PLATE, safePlate);

        arrivals.add(arrival);
    }

    private Integer parseLegacyEtaMinutes(String etaStr) {
        if (etaStr == null) return null;
        String s = etaStr.trim();
        if (s.isEmpty() || "-".equals(s)) return null;

        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String statusFromEta(int etaMinutes) {
        if (etaMinutes <= 1) return STATUS_ARRIVING;
        if (etaMinutes <= 8) return STATUS_ON_TIME;
        return STATUS_SCHEDULED;
    }

    private Map<String, Object> buildArrival(String routeName, String direction, int etaMinutes, String status) {
        Map<String, Object> arrival = new LinkedHashMap<>();
        arrival.put(KEY_ROUTE, routeName.trim());
        arrival.put(KEY_DIRECTION, direction);
        arrival.put(KEY_ETA_MINUTES, etaMinutes);
        arrival.put(KEY_STATUS, status);
        return arrival;
    }

    private String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private int safeInt(Object val, int fallback) {
        if (val instanceof Integer i) return i;
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (Exception ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    // ========== NUS NextBus API Response Models ==========

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

    // small internal record to keep method flat (reduce cognitive complexity)
    private record StopRoute(String stop, String route) {}
}
