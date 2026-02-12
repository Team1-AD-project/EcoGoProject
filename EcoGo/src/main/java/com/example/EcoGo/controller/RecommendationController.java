package com.example.EcoGo.controller;

import com.example.EcoGo.dto.RecommendationRequestDto;
import com.example.EcoGo.dto.RecommendationResponseDto;
import com.example.EcoGo.dto.ResponseMessage;
import com.example.EcoGo.service.chatbot.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * Recommendation controller for green travel suggestions.
 * Uses a database of real NUS campus locations with actual distances,
 * travel times, bus routes, stop names, and walking directions.
 */
@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    @Autowired
    private RagService ragService;

    private static final Random RANDOM = new Random();

    // ══════════════════════════════════════════════════════════════════
    //  NUS Campus Location Database with Route Details
    // ══════════════════════════════════════════════════════════════════
    private static final List<NusLocation> NUS_LOCATIONS = List.of(
        new NusLocation("Central Library (CLB)",
            List.of("central library", "clb", "central lib"),
            0.8, 10, 4, "library",
            List.of(
                new BusRoute("D1", "UTown", "Central Library", 2, "UTown → Raffles Hall → Central Library"),
                new BusRoute("A1", "Kent Ridge MRT", "Central Library", 3, "Kent Ridge MRT → LT13 → Central Library")
            ),
            "Walk via University Road past Yusof Ishak House (~10 min)"),

        new NusLocation("Science Library (S16)",
            List.of("science library", "sci lib", "s16 library", "s16"),
            1.2, 15, 5, "library",
            List.of(
                new BusRoute("D2", "COM3", "Science", 3, "COM3 → BIZ2 → Science"),
                new BusRoute("A2", "UTown", "Science", 4, "UTown → Museum → Ventus → Science")
            ),
            "Walk south along Science Drive 2, past S1-S8 blocks (~15 min)"),

        new NusLocation("Chinese Library",
            List.of("chinese library", "中文图书馆"),
            0.6, 8, 3, "library",
            List.of(new BusRoute("D1", "UTown", "Central Library", 2, "UTown → Central Library, then 2 min walk")),
            "Walk via Kent Ridge Crescent past AS5/AS6 (~8 min)"),

        new NusLocation("Hon Sui Sen Memorial Library (BIZ)",
            List.of("hssm library", "biz library", "business library"),
            1.5, 18, 6, "library",
            List.of(new BusRoute("D2", "COM3", "BIZ2", 2, "COM3 → BIZ2 (right at the library)")),
            "Walk along Business Link from COM area (~18 min)"),

        new NusLocation("UTown (University Town)",
            List.of("utown", "university town", "u-town", "utown residence"),
            1.0, 12, 4, "residential",
            List.of(
                new BusRoute("D1", "COM3", "UTown", 4, "COM3 → LT13 → Raffles Hall → UTown"),
                new BusRoute("D2", "Science", "UTown", 5, "Science → Ventus → Museum → UTown"),
                new BusRoute("E", "Kent Ridge MRT", "UTown", 3, "Kent Ridge MRT → UTown (express)")
            ),
            "Walk north via Kent Ridge Drive, turn into UTown Green (~12 min)"),

        new NusLocation("School of Computing (COM1/COM2/COM3)",
            List.of("soc", "computing", "com1", "com2", "com3", "school of computing", "i3"),
            1.3, 16, 5, "faculty",
            List.of(
                new BusRoute("D1", "UTown", "COM3", 3, "UTown → Raffles Hall → CLB → COM3"),
                new BusRoute("D2", "UTown", "COM3", 4, "UTown → Museum → Ventus → COM3")
            ),
            "Walk along Clementi Road, enter via Computing Drive (~16 min)"),

        new NusLocation("Faculty of Engineering (EA/E1/E2)",
            List.of("engineering", "foe", "e1", "e2", "ea", "faculty of engineering", "engine"),
            1.0, 12, 4, "faculty",
            List.of(
                new BusRoute("D2", "COM3", "EA", 2, "COM3 → EA (1 stop)"),
                new BusRoute("A2", "UTown", "EA", 3, "UTown → Museum → EA")
            ),
            "Walk along Engineering Drive 1 from Central Campus (~12 min)"),

        new NusLocation("Faculty of Science (S1-S17)",
            List.of("science", "fos", "faculty of science"),
            1.1, 14, 5, "faculty",
            List.of(
                new BusRoute("D2", "COM3", "Science", 3, "COM3 → BIZ2 → Science"),
                new BusRoute("A2", "Kent Ridge MRT", "Science", 4, "Kent Ridge MRT → EA → Science")
            ),
            "Walk south along Science Drive 4 (~14 min)"),

        new NusLocation("Business School (BIZ1/BIZ2)",
            List.of("business", "biz", "biz1", "biz2", "business school", "nus business"),
            1.5, 18, 6, "faculty",
            List.of(new BusRoute("D2", "COM3", "BIZ2", 2, "COM3 → BIZ2 (direct, 1 stop)")),
            "Walk via Business Link from Computing area (~18 min)"),

        new NusLocation("FASS (Faculty of Arts & Social Sciences)",
            List.of("fass", "arts", "as1", "as7", "as8", "faculty of arts", "arts and social sciences"),
            0.9, 11, 4, "faculty",
            List.of(
                new BusRoute("D1", "UTown", "AS5", 2, "UTown → Raffles Hall → AS5"),
                new BusRoute("A1", "Kent Ridge MRT", "AS5", 4, "Kent Ridge MRT → LT13 → AS5")
            ),
            "Walk via Kent Ridge Crescent, past YIH to AS buildings (~11 min)"),

        new NusLocation("YLL School of Medicine (MD1-MD11)",
            List.of("medicine", "yllsom", "md1", "md6", "md11", "medical", "yong loo lin"),
            1.8, 22, 7, "faculty",
            List.of(
                new BusRoute("A1", "Kent Ridge MRT", "NUHS", 5, "Kent Ridge MRT → LT13 → CLB → NUHS"),
                new BusRoute("D1", "UTown", "NUHS", 6, "UTown → Raffles Hall → CLB → NUHS")
            ),
            "Walk south via Lower Kent Ridge Road to MD buildings (~22 min)"),

        new NusLocation("School of Design & Environment (SDE)",
            List.of("sde", "design", "architecture", "school of design"),
            1.4, 17, 6, "faculty",
            List.of(
                new BusRoute("D2", "COM3", "SDE", 4, "COM3 → BIZ2 → Science → SDE"),
                new BusRoute("A2", "Kent Ridge MRT", "SDE", 3, "Kent Ridge MRT → EA → SDE")
            ),
            "Walk along SDE Drive off Engineering Drive (~17 min)"),

        new NusLocation("Faculty of Law (Bukit Timah Campus)",
            List.of("law", "faculty of law", "bukit timah"),
            5.0, -1, -1, "offcampus",
            List.of(),
            "Take Kent Ridge MRT (CC24) → Botanic Gardens (CC19), then walk 10 min"),

        new NusLocation("University Sports Centre (USC)",
            List.of("usc", "sports centre", "gym", "sport", "swimming pool", "sports", "运动", "健身"),
            1.6, 20, 6, "sports",
            List.of(new BusRoute("D2", "COM3", "USC/SRC", 5, "COM3 → BIZ2 → Science → SDE → USC")),
            "Walk south along Lower Kent Ridge Road, past SDE (~20 min)"),

        new NusLocation("MPSH (Multi-Purpose Sports Hall)",
            List.of("mpsh", "sports hall", "badminton"),
            1.3, 16, 5, "sports",
            List.of(new BusRoute("D2", "COM3", "MPSH", 4, "COM3 → EA → MPSH")),
            "Walk along Engineering Drive 3 to MPSH (~16 min)"),

        new NusLocation("Stephen Riady Centre (UTown)",
            List.of("src", "stephen riady", "utown src", "starbucks utown"),
            1.0, 12, 4, "amenity",
            List.of(
                new BusRoute("D1", "COM3", "UTown", 4, "COM3 → LT13 → Raffles Hall → UTown"),
                new BusRoute("E", "Kent Ridge MRT", "UTown", 3, "Kent Ridge MRT → UTown (express)")
            ),
            "Walk north via Kent Ridge Drive into UTown, SRC is at the main plaza (~12 min)"),

        new NusLocation("The Deck (Canteen)",
            List.of("the deck", "deck canteen", "deck"),
            0.7, 9, 3, "food",
            List.of(new BusRoute("D1", "UTown", "Central Library", 2, "UTown → CLB, then 1 min walk to The Deck")),
            "Walk via University Road, The Deck is behind FASS (~9 min)"),

        new NusLocation("Fine Food @ UTown",
            List.of("fine food", "utown canteen", "utown food"),
            1.0, 12, 4, "food",
            List.of(
                new BusRoute("D1", "COM3", "UTown", 4, "COM3 → Raffles Hall → UTown"),
                new BusRoute("E", "Kent Ridge MRT", "UTown", 3, "Kent Ridge MRT → UTown (express)")
            ),
            "Walk to UTown, Fine Food is at Education Resource Centre (~12 min)"),

        new NusLocation("Frontier Canteen",
            List.of("frontier", "frontier canteen", "science canteen"),
            1.2, 15, 5, "food",
            List.of(new BusRoute("D2", "COM3", "Science", 3, "COM3 → BIZ2 → Science, then 2 min walk")),
            "Walk along Science Drive 2, Frontier is next to S13 block (~15 min)"),

        new NusLocation("PGP Canteen",
            List.of("pgp", "pgp canteen", "prince george", "pgpr"),
            2.0, 25, 8, "food",
            List.of(
                new BusRoute("A1", "Kent Ridge MRT", "PGP", 6, "Kent Ridge MRT → LT13 → NUHS → PGP"),
                new BusRoute("A2", "UTown", "PGP", 7, "UTown → Museum → Science → PGP")
            ),
            "Walk along Lower Kent Ridge Road south to PGP (~25 min, bus recommended)"),

        new NusLocation("Kent Ridge MRT Station",
            List.of("kent ridge", "kent ridge mrt", "mrt", "地铁"),
            1.8, 22, 7, "transit",
            List.of(
                new BusRoute("D2", "COM3", "Kent Ridge MRT", 6, "COM3 → EA → MPSH → Kent Ridge MRT"),
                new BusRoute("A2", "UTown", "Kent Ridge MRT", 8, "UTown → Museum → Science → Kent Ridge MRT")
            ),
            "Walk via Kent Ridge Crescent then South Buona Vista Rd (~22 min)"),

        new NusLocation("NUH (National University Hospital)",
            List.of("nuh", "hospital", "national university hospital"),
            2.0, 25, 7, "medical",
            List.of(new BusRoute("D2", "COM3", "NUHS", 5, "COM3 → BIZ2 → Science → NUHS")),
            "Walk along Lower Kent Ridge Road to NUH entrance (~25 min)"),

        new NusLocation("Raffles Hall",
            List.of("raffles hall", "rh"),
            1.4, 17, 6, "residential",
            List.of(new BusRoute("D1", "COM3", "Raffles Hall", 2, "COM3 → LT13 → Raffles Hall")),
            "Walk along Kent Ridge Drive past LT13 (~17 min)"),

        new NusLocation("Eusoff Hall",
            List.of("eusoff", "eusoff hall", "eh"),
            1.2, 15, 5, "residential",
            List.of(new BusRoute("A1", "Kent Ridge MRT", "Eusoff Hall", 4, "Kent Ridge MRT → LT13 → Eusoff Hall")),
            "Walk along Kent Ridge Crescent (~15 min)"),

        new NusLocation("Temasek Hall",
            List.of("temasek", "temasek hall", "th"),
            1.3, 16, 5, "residential",
            List.of(new BusRoute("A1", "Kent Ridge MRT", "Temasek Hall", 4, "Kent Ridge MRT → Eusoff → Temasek Hall")),
            "Walk via Kent Ridge Crescent, past Eusoff Hall (~16 min)"),

        new NusLocation("Kent Ridge Hall",
            List.of("kent ridge hall", "krh"),
            1.5, 18, 6, "residential",
            List.of(new BusRoute("A2", "UTown", "Kent Ridge Hall", 5, "UTown → Museum → Science → Kent Ridge Hall")),
            "Walk south along Lower Kent Ridge Rd (~18 min)"),

        new NusLocation("NUS Museum",
            List.of("museum", "nus museum"),
            0.5, 6, 2, "amenity",
            List.of(new BusRoute("D1", "UTown", "Museum", 1, "UTown → Museum (1 stop)")),
            "Walk down University Road to Museum building (~6 min)"),

        new NusLocation("University Hall (UHall)",
            List.of("university hall", "uhall", "u hall", "admin"),
            0.6, 8, 3, "amenity",
            List.of(new BusRoute("D1", "UTown", "UHall", 2, "UTown → Museum → UHall")),
            "Walk along Tan Chin Tuan Wing road (~8 min)")
    );

    // ══════════════════════════════════════════════════════════════════
    //  Main Endpoint
    // ══════════════════════════════════════════════════════════════════
    @PostMapping
    public ResponseMessage<RecommendationResponseDto> recommend(@RequestBody RecommendationRequestDto request) {
        String dest = request.getDestination() == null ? "" : request.getDestination().trim();
        String destLower = dest.toLowerCase();

        if (dest.isEmpty()) {
            return ResponseMessage.success(getGeneralTip());
        }

        // 1. Match against NUS location database
        NusLocation matched = matchLocation(destLower);
        if (matched != null) {
            return ResponseMessage.success(buildRouteRecommendation(matched));
        }

        // 2. Try RAG service for unknown destinations
        RecommendationResponseDto ragRec = attemptRagRecommendation(dest);
        if (ragRec != null) {
            return ResponseMessage.success(ragRec);
        }

        // 3. Generic campus recommendation
        return ResponseMessage.success(buildGenericRecommendation(dest));
    }

    // ── Location matching ─────────────────────────────────────────────
    private NusLocation matchLocation(String destLower) {
        for (NusLocation loc : NUS_LOCATIONS) {
            for (String alias : loc.aliases) {
                if (destLower.equals(alias)) return loc;
            }
        }
        for (NusLocation loc : NUS_LOCATIONS) {
            for (String alias : loc.aliases) {
                if (destLower.contains(alias) || alias.contains(destLower)) return loc;
            }
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════
    //  Build Route-Specific Recommendation
    // ══════════════════════════════════════════════════════════════════
    private RecommendationResponseDto buildRouteRecommendation(NusLocation loc) {
        if ("offcampus".equals(loc.category)) {
            return new RecommendationResponseDto(
                String.format("🚇 %s is off the main campus.\n\n" +
                    "Route: %s\n\n" +
                    "Earn 80 green points for choosing public transit!", loc.name, loc.walkRoute),
                "Green-Transit"
            );
        }

        StringBuilder sb = new StringBuilder();
        String tag;

        // Header with destination name
        sb.append(String.format("📍 %s\n\n", loc.name));

        // Always show both options: bus route AND walking route
        if (!loc.busRoutes.isEmpty()) {
            // Pick the best (fastest) bus route
            BusRoute best = loc.busRoutes.get(0);
            for (BusRoute r : loc.busRoutes) {
                if (r.stops < best.stops) best = r;
            }

            sb.append(String.format("🚌 Bus Route: Take %s from %s → %s\n", best.line, best.boardAt, best.alightAt));
            sb.append(String.format("   Route: %s\n", best.routeDesc));
            sb.append(String.format("   ~%d min | %d stops | Earn %d green points\n\n", best.stops * 2, best.stops, greenPointsForBus(loc.busMin)));

            if (loc.busRoutes.size() > 1) {
                BusRoute alt = loc.busRoutes.get(1);
                sb.append(String.format("   Alt: %s (%s → %s, %d stops)\n\n", alt.line, alt.boardAt, alt.alightAt, alt.stops));
            }
            tag = "Campus-Bus";
        } else {
            tag = "Walk";
        }

        // Walking option
        sb.append(String.format("🚶 Walk Route: %s\n", loc.walkRoute));
        sb.append(String.format("   %.1f km | ~%d min | Earn %d green points\n\n", loc.distanceKm, loc.walkMin, greenPointsForWalk(loc.walkMin)));

        // CO2 impact
        sb.append(String.format("🌿 Saves ~%.0fg CO₂ vs private car", co2Saved(loc.distanceKm)));

        // Category tip
        String tip = getCategoryTip(loc.category);
        if (!tip.isEmpty()) {
            sb.append("\n").append(tip);
        }

        return new RecommendationResponseDto(sb.toString().trim(), tag);
    }

    // ── RAG fallback ──────────────────────────────────────────────────
    private RecommendationResponseDto attemptRagRecommendation(String dest) {
        if (!ragService.isAvailable()) return null;
        try {
            String query = "green travel recommendation to " + dest + " NUS campus";
            var citations = ragService.retrieve(query, 2);
            if (citations == null || citations.isEmpty()) return null;
            StringBuilder sb = new StringBuilder();
            sb.append("🌿 Green travel tips for ").append(dest).append(":\n\n");
            for (var c : citations) {
                sb.append("• ").append(c.getSnippet()).append("\n");
            }
            return new RecommendationResponseDto(sb.toString().trim(), "Eco-RAG");
        } catch (Exception ignored) {
            return null;
        }
    }

    // ── Generic recommendation ────────────────────────────────────────
    private RecommendationResponseDto buildGenericRecommendation(String dest) {
        String[] tips = {
            String.format("🚌 To reach %s, check the NUS shuttle routes on the Routes tab.\n\n" +
                "Available routes:\n" +
                "• D1: UTown ↔ COM3 ↔ CLB (loop)\n" +
                "• D2: UTown ↔ Science ↔ BIZ ↔ COM3 (loop)\n" +
                "• A1: Kent Ridge MRT ↔ CLB ↔ PGP\n" +
                "• A2: Kent Ridge MRT ↔ EA ↔ Science ↔ PGP\n" +
                "• E : Kent Ridge MRT ↔ UTown (express)\n\n" +
                "Buses run every 8-15 min during term. Earn green points for every ride!", dest),
            String.format("🚶 If %s is on campus, try walking!\n\n" +
                "Most NUS buildings are within 20 min walk from Central Library.\n" +
                "Use the Map tab for the best walking route.\n\n" +
                "Walking earns the most green points per trip!", dest),
            String.format("🌿 Heading to %s? Combine shuttle + walking for the greenest route.\n\n" +
                "Tip: Check real-time bus arrivals on the Routes tab before heading out.\n" +
                "Every green trip earns eco-points toward vouchers and rewards!", dest)
        };
        return new RecommendationResponseDto(tips[RANDOM.nextInt(tips.length)], "Eco-Tip");
    }

    // ── General tip (empty input) ─────────────────────────────────────
    private RecommendationResponseDto getGeneralTip() {
        String[] tips = {
            "🌿 Green commute tips:\n\n" +
                "• Under 1 km → Walk (~12 min/km, most points!)\n" +
                "• 1-3 km → NUS shuttle (D1, D2, A1, A2, E)\n" +
                "• Off-campus → Kent Ridge MRT (CC24)\n\n" +
                "Every green choice earns eco-points!",
            "🚌 NUS campus shuttles:\n\n" +
                "• D1: UTown ↔ CLB ↔ COM3 (inner loop)\n" +
                "• D2: UTown ↔ Science ↔ BIZ ↔ COM3\n" +
                "• A1/A2: Kent Ridge MRT ↔ Campus ↔ PGP\n" +
                "• E: Kent Ridge MRT ↔ UTown (express)\n\n" +
                "Check real-time arrivals on the Routes tab!",
            "🚶 Walking challenge: Most NUS buildings are within 15 min walk!\n\n" +
                "• CLB to UTown: 12 min\n" +
                "• CLB to SoC: 10 min\n" +
                "• CLB to Science: 14 min\n\n" +
                "Walk 10,000 steps/day = save ~2.5 kg CO₂. Start now!"
        };
        return new RecommendationResponseDto(tips[RANDOM.nextInt(tips.length)], "General");
    }

    // ══════════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════════
    private int greenPointsForWalk(int walkMin) {
        return Math.max(10, walkMin * 3);
    }

    private int greenPointsForBus(int busMin) {
        return Math.max(5, busMin * 2);
    }

    private double co2Saved(double distanceKm) {
        return distanceKm * 100; // grams
    }

    private String getCategoryTip(String category) {
        return switch (category) {
            case "library" -> "📚 Tip: Libraries can get cold — bring a light jacket!";
            case "food"    -> "🍽️ Tip: Off-peak hours (2-5 PM) = shorter queues!";
            case "sports"  -> "💪 Remember your matric card for facility access!";
            case "transit" -> "🎫 Use EZ-Link or SimplyGo for MRT.";
            case "medical" -> "🏥 NUH also accessible via Kent Ridge MRT (Exit A).";
            default -> "";
        };
    }

    // ══════════════════════════════════════════════════════════════════
    //  Inner Data Classes
    // ══════════════════════════════════════════════════════════════════
    private static class BusRoute {
        final String line;       // e.g. "D1", "A2"
        final String boardAt;    // boarding stop name
        final String alightAt;   // alighting stop name
        final int stops;         // number of stops
        final String routeDesc;  // full route description

        BusRoute(String line, String boardAt, String alightAt, int stops, String routeDesc) {
            this.line = line;
            this.boardAt = boardAt;
            this.alightAt = alightAt;
            this.stops = stops;
            this.routeDesc = routeDesc;
        }
    }

    private static class NusLocation {
        final String name;
        final List<String> aliases;
        final double distanceKm;
        final int walkMin;
        final int busMin;
        final String category;
        final List<BusRoute> busRoutes;  // specific route options
        final String walkRoute;          // walking directions

        NusLocation(String name, List<String> aliases, double distanceKm, int walkMin, int busMin,
                    String category, List<BusRoute> busRoutes, String walkRoute) {
            this.name = name;
            this.aliases = aliases;
            this.distanceKm = distanceKm;
            this.walkMin = walkMin;
            this.busMin = busMin;
            this.category = category;
            this.busRoutes = busRoutes;
            this.walkRoute = walkRoute;
        }
    }
}
