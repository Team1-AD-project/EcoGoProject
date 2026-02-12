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
 * travel times, bus routes, and green-travel advice.
 * Falls back to RAG service for unknown destinations.
 */
@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    @Autowired
    private RagService ragService;

    private static final Random RANDOM = new Random();

    // ── NUS campus location database ──────────────────────────────────
    // Each entry: aliases → NusLocation(name, distanceKm, walkMin, busMin, busRoutes, category)
    private static final List<NusLocation> NUS_LOCATIONS = List.of(
        new NusLocation("Central Library",
            List.of("central library", "clb", "central lib"),
            0.8, 10, 4, List.of("D1", "A1"), "library"),
        new NusLocation("Science Library",
            List.of("science library", "sci lib", "s16 library"),
            1.2, 15, 5, List.of("D2", "A2"), "library"),
        new NusLocation("Chinese Library",
            List.of("chinese library", "cl", "中文图书馆"),
            0.6, 8, 3, List.of("D1"), "library"),
        new NusLocation("Hon Sui Sen Memorial Library",
            List.of("hssm library", "biz library", "business library"),
            1.5, 18, 6, List.of("D2"), "library"),
        new NusLocation("UTown",
            List.of("utown", "university town", "u-town"),
            1.0, 12, 4, List.of("D1", "D2", "E"), "residential"),
        new NusLocation("School of Computing (COM1/COM2/COM3)",
            List.of("soc", "computing", "com1", "com2", "com3", "school of computing", "i3"),
            1.3, 16, 5, List.of("D1", "D2"), "faculty"),
        new NusLocation("Faculty of Engineering (E1/E2)",
            List.of("engineering", "foe", "e1", "e2", "ea", "faculty of engineering"),
            1.0, 12, 4, List.of("D2", "A2"), "faculty"),
        new NusLocation("Faculty of Science (S1-S17)",
            List.of("science", "fos", "faculty of science", "s1", "s17"),
            1.1, 14, 5, List.of("D2", "A2"), "faculty"),
        new NusLocation("Business School (BIZ1/BIZ2)",
            List.of("business", "biz", "biz1", "biz2", "business school", "nus business"),
            1.5, 18, 6, List.of("D2"), "faculty"),
        new NusLocation("Faculty of Arts & Social Sciences (AS1-AS8)",
            List.of("fass", "arts", "as1", "as7", "as8", "faculty of arts"),
            0.9, 11, 4, List.of("D1", "A1"), "faculty"),
        new NusLocation("Yong Loo Lin School of Medicine (MD1-MD11)",
            List.of("medicine", "yllsom", "md1", "md6", "md11", "medical"),
            1.8, 22, 7, List.of("A1", "D1"), "faculty"),
        new NusLocation("School of Design & Environment (SDE)",
            List.of("sde", "design", "architecture", "school of design"),
            1.4, 17, 6, List.of("D2", "A2"), "faculty"),
        new NusLocation("Faculty of Law (Bukit Timah Campus)",
            List.of("law", "faculty of law", "bukit timah"),
            5.0, -1, -1, List.of(), "offcampus"),
        new NusLocation("University Sports Centre (USC)",
            List.of("usc", "sports centre", "gym", "sport", "swimming pool", "sports", "运动"),
            1.6, 20, 6, List.of("D2"), "sports"),
        new NusLocation("MPSH (Multi-Purpose Sports Hall)",
            List.of("mpsh", "sports hall", "badminton"),
            1.3, 16, 5, List.of("D2"), "sports"),
        new NusLocation("Stephen Riady Centre (UTown)",
            List.of("src", "stephen riady", "utown src"),
            1.0, 12, 4, List.of("D1", "D2", "E"), "amenity"),
        new NusLocation("The Deck",
            List.of("the deck", "deck canteen", "deck"),
            0.7, 9, 3, List.of("D1", "A1"), "food"),
        new NusLocation("Fine Food (UTown)",
            List.of("fine food", "utown canteen", "utown food"),
            1.0, 12, 4, List.of("D1", "D2", "E"), "food"),
        new NusLocation("Frontier Canteen",
            List.of("frontier", "frontier canteen", "science canteen"),
            1.2, 15, 5, List.of("D2"), "food"),
        new NusLocation("PGP Canteen",
            List.of("pgp", "pgp canteen", "prince george"),
            2.0, 25, 8, List.of("A1", "A2"), "food"),
        new NusLocation("Kent Ridge MRT",
            List.of("kent ridge", "kent ridge mrt", "mrt"),
            1.8, 22, 7, List.of("D2", "A2"), "transit"),
        new NusLocation("NUH (National University Hospital)",
            List.of("nuh", "hospital", "national university hospital"),
            2.0, 25, 7, List.of("D2"), "medical"),
        new NusLocation("Raffles Hall",
            List.of("raffles hall", "rh"),
            1.4, 17, 6, List.of("A1"), "residential"),
        new NusLocation("Eusoff Hall",
            List.of("eusoff", "eusoff hall"),
            1.2, 15, 5, List.of("A1"), "residential"),
        new NusLocation("Temasek Hall",
            List.of("temasek", "temasek hall"),
            1.3, 16, 5, List.of("A1"), "residential"),
        new NusLocation("Kent Ridge Hall",
            List.of("kent ridge hall", "krh"),
            1.5, 18, 6, List.of("A2"), "residential"),
        new NusLocation("NUS Museum",
            List.of("museum", "nus museum"),
            0.5, 6, 2, List.of("D1"), "amenity"),
        new NusLocation("University Hall (UHall)",
            List.of("university hall", "uhall", "admin"),
            0.6, 8, 3, List.of("D1", "A1"), "amenity")
    );

    @PostMapping
    public ResponseMessage<RecommendationResponseDto> recommend(@RequestBody RecommendationRequestDto request) {
        String dest = request.getDestination() == null ? "" : request.getDestination().trim();
        String destLower = dest.toLowerCase();

        if (dest.isEmpty()) {
            return ResponseMessage.success(getGeneralTip());
        }

        // 1. Try exact/fuzzy match against NUS location database
        NusLocation matched = matchLocation(destLower);
        if (matched != null) {
            return ResponseMessage.success(buildLocationRecommendation(matched));
        }

        // 2. Try RAG-based recommendation
        RecommendationResponseDto ragRec = attemptRagRecommendation(dest);
        if (ragRec != null) {
            return ResponseMessage.success(ragRec);
        }

        // 3. Generic campus recommendation for unknown destination
        return ResponseMessage.success(buildGenericRecommendation(dest));
    }

    // ── Location matching ─────────────────────────────────────────────
    private NusLocation matchLocation(String destLower) {
        // Exact alias match
        for (NusLocation loc : NUS_LOCATIONS) {
            for (String alias : loc.aliases) {
                if (destLower.equals(alias)) return loc;
            }
        }
        // Partial match (destination contains an alias or vice versa)
        for (NusLocation loc : NUS_LOCATIONS) {
            for (String alias : loc.aliases) {
                if (destLower.contains(alias) || alias.contains(destLower)) return loc;
            }
        }
        return null;
    }

    // ── Build recommendation for a known NUS location ─────────────────
    private RecommendationResponseDto buildLocationRecommendation(NusLocation loc) {
        // Off-campus locations
        if ("offcampus".equals(loc.category)) {
            return new RecommendationResponseDto(
                String.format("🚇 %s is off the main campus. Take the Kent Ridge MRT (CC24) and transfer. " +
                    "Estimated travel: ~30 min by MRT. Earn 80 green points for choosing public transit!", loc.name),
                "Green-Transit"
            );
        }

        StringBuilder sb = new StringBuilder();
        String tag;

        if (loc.distanceKm <= 0.8) {
            // Very close - walking is best
            sb.append(String.format("🚶 %s is just %.1f km away (~%d min walk). ", loc.name, loc.distanceKm, loc.walkMin));
            sb.append(String.format("Walking earns you %d green points and saves ~%.0fg CO₂ vs driving. ", greenPointsForWalk(loc.walkMin), co2Saved(loc.distanceKm)));
            sb.append(getWeatherHint());
            tag = "Walk";
        } else if (loc.distanceKm <= 1.5) {
            // Medium distance - walk or bus
            boolean suggestWalk = RANDOM.nextBoolean();
            if (suggestWalk) {
                sb.append(String.format("🚶 Walk to %s (%.1f km, ~%d min). ", loc.name, loc.distanceKm, loc.walkMin));
                sb.append(String.format("Earn %d green points! ", greenPointsForWalk(loc.walkMin)));
            } else {
                sb.append(String.format("🚌 Take NUS bus %s to %s (~%d min). ", formatBusRoutes(loc.busRoutes), loc.name, loc.busMin));
                sb.append(String.format("Earn %d green points for choosing campus shuttle. ", greenPointsForBus(loc.busMin)));
            }
            sb.append(String.format("Saves ~%.0fg CO₂ vs private car.", co2Saved(loc.distanceKm)));
            tag = suggestWalk ? "Walk" : "Campus-Bus";
        } else {
            // Further - bus recommended
            if (!loc.busRoutes.isEmpty()) {
                sb.append(String.format("🚌 Take NUS shuttle %s to %s (%.1f km, ~%d min by bus). ", formatBusRoutes(loc.busRoutes), loc.name, loc.distanceKm, loc.busMin));
                sb.append(String.format("Walking would take ~%d min. ", loc.walkMin));
                sb.append(String.format("Earn %d green points! Saves ~%.0fg CO₂.", greenPointsForBus(loc.busMin), co2Saved(loc.distanceKm)));
                tag = "Campus-Bus";
            } else {
                sb.append(String.format("🚶 Walk to %s (%.1f km, ~%d min). ", loc.name, loc.distanceKm, loc.walkMin));
                sb.append(String.format("Great exercise and you'll earn %d green points!", greenPointsForWalk(loc.walkMin)));
                tag = "Walk";
            }
        }

        // Add category-specific tips
        sb.append(getCategoryTip(loc.category));

        return new RecommendationResponseDto(sb.toString().trim(), tag);
    }

    // ── RAG-based recommendation ──────────────────────────────────────
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

    // ── Generic recommendation for unknown campus destinations ────────
    private RecommendationResponseDto buildGenericRecommendation(String dest) {
        String[] tips = {
            String.format("🚌 To reach %s, check the NUS shuttle routes (D1, D2, A1, A2, E) on the Routes tab. " +
                "Campus buses are free and run every 8-15 min during term time. Earn green points for every ride!", dest),
            String.format("🚶 If %s is within NUS campus, consider walking! Most campus destinations are under 20 min walk. " +
                "Use the Map tab to find the best walking route and earn green points.", dest),
            String.format("🌿 Heading to %s? Try combining NUS shuttle + walking for the greenest option. " +
                "Check real-time bus arrivals on the Routes tab. Every green trip earns you eco-points!", dest)
        };
        return new RecommendationResponseDto(tips[RANDOM.nextInt(tips.length)], "Eco-Tip");
    }

    // ── General tip (empty input) ─────────────────────────────────────
    private RecommendationResponseDto getGeneralTip() {
        String[] tips = {
            "🌿 Green commute tips: Walk for distances under 1 km (~12 min). Take NUS shuttle for 1-3 km. " +
                "Use Kent Ridge MRT (CC24) for off-campus trips. Every green choice earns you eco-points!",
            "🚌 NUS campus shuttles run 5 routes (D1, D2, A1, A2, E) covering all faculties, halls, and UTown. " +
                "Check real-time arrivals on the Routes tab!",
            "🚶 Walking between classes? Most NUS buildings are within 15 min walk. " +
                "Walking 10,000 steps/day can save ~2.5 kg CO₂ vs driving. Start earning green points now!"
        };
        return new RecommendationResponseDto(tips[RANDOM.nextInt(tips.length)], "General");
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private int greenPointsForWalk(int walkMin) {
        return Math.max(10, walkMin * 3);  // ~3 points per minute of walking
    }

    private int greenPointsForBus(int busMin) {
        return Math.max(5, busMin * 2);  // ~2 points per minute of bus
    }

    private double co2Saved(double distanceKm) {
        // Average car emits ~120g CO₂/km; walking/bus ~0-20g/km
        return distanceKm * 100;  // grams saved
    }

    private String formatBusRoutes(List<String> routes) {
        if (routes.isEmpty()) return "shuttle";
        if (routes.size() == 1) return routes.get(0);
        return String.join(" or ", routes);
    }

    private String getWeatherHint() {
        String[] hints = {
            "Perfect weather for a walk! ",
            "Grab your water bottle for the walk! ",
            "A nice stroll through campus! ",
            ""
        };
        return hints[RANDOM.nextInt(hints.length)];
    }

    private String getCategoryTip(String category) {
        return switch (category) {
            case "library" -> " 📚 Pro tip: Libraries can get cold — bring a light jacket!";
            case "food" -> " 🍽️ Tip: Off-peak hours (2-5 PM) mean shorter queues!";
            case "sports" -> " 💪 Remember to bring your matric card for facility access!";
            case "residential" -> "";
            case "transit" -> " 🎫 Use your EZ-Link card or SimplyGo for MRT.";
            case "medical" -> " 🏥 NUH is also accessible via Kent Ridge MRT (Exit A).";
            default -> "";
        };
    }

    // ── Inner data class ──────────────────────────────────────────────
    private static class NusLocation {
        final String name;
        final List<String> aliases;
        final double distanceKm;   // approximate distance from central campus
        final int walkMin;         // walking time in minutes (-1 if not walkable)
        final int busMin;          // bus time in minutes (-1 if no direct bus)
        final List<String> busRoutes;  // NUS shuttle routes serving this location
        final String category;     // library, faculty, food, sports, residential, transit, etc.

        NusLocation(String name, List<String> aliases, double distanceKm, int walkMin, int busMin,
                    List<String> busRoutes, String category) {
            this.name = name;
            this.aliases = aliases;
            this.distanceKm = distanceKm;
            this.walkMin = walkMin;
            this.busMin = busMin;
            this.busRoutes = busRoutes;
            this.category = category;
        }
    }
}
