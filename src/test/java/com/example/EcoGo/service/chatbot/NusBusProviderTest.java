package com.example.EcoGo.service.chatbot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NusBusProviderTest {

    // ---------- isRouteName (static method) ----------
    @Test
    void isRouteName_knownRoutes_shouldReturnTrue() {
        assertTrue(NusBusProvider.isRouteName("A1"));
        assertTrue(NusBusProvider.isRouteName("A2"));
        assertTrue(NusBusProvider.isRouteName("D1"));
        assertTrue(NusBusProvider.isRouteName("D2"));
        assertTrue(NusBusProvider.isRouteName("K"));
        assertTrue(NusBusProvider.isRouteName("E"));
        assertTrue(NusBusProvider.isRouteName("BTC"));
        assertTrue(NusBusProvider.isRouteName("L"));
    }

    @Test
    void isRouteName_caseInsensitive_shouldReturnTrue() {
        assertTrue(NusBusProvider.isRouteName("a1"));
        assertTrue(NusBusProvider.isRouteName("d2"));
        assertTrue(NusBusProvider.isRouteName("btc"));
    }

    @Test
    void isRouteName_unknownRoute_shouldReturnFalse() {
        assertFalse(NusBusProvider.isRouteName("X1"));
        assertFalse(NusBusProvider.isRouteName("Z99"));
        assertFalse(NusBusProvider.isRouteName("UNKNOWN"));
    }

    @Test
    void isRouteName_null_shouldReturnFalse() {
        assertFalse(NusBusProvider.isRouteName(null));
    }

    @Test
    void isRouteName_empty_shouldReturnFalse() {
        assertFalse(NusBusProvider.isRouteName(""));
    }

    @Test
    void isRouteName_withWhitespace_shouldReturnTrue() {
        assertTrue(NusBusProvider.isRouteName(" A1 "));
        assertTrue(NusBusProvider.isRouteName("D2 "));
    }

    @Test
    void isRouteName_expressRoutes_shouldReturnTrue() {
        assertTrue(NusBusProvider.isRouteName("A1E"));
        assertTrue(NusBusProvider.isRouteName("A2E"));
        assertTrue(NusBusProvider.isRouteName("D1E"));
        assertTrue(NusBusProvider.isRouteName("D2E"));
    }

    // ---------- BusArrivalsResult record ----------
    @Test
    void busArrivalsResult_shouldStoreValues() {
        NusBusProvider.BusArrivalsResult result = new NusBusProvider.BusArrivalsResult(
                "COM3", java.util.List.of(
                        java.util.Map.of("route", "D2", "etaMinutes", 3, "status", "on_time")
                ));

        assertEquals("COM3", result.stopName());
        assertEquals(1, result.arrivals().size());
        assertEquals("D2", result.arrivals().get(0).get("route"));
        assertEquals(3, result.arrivals().get(0).get("etaMinutes"));
    }

    @Test
    void busArrivalsResult_emptyArrivals() {
        NusBusProvider.BusArrivalsResult result = new NusBusProvider.BusArrivalsResult(
                "PGP", java.util.List.of());

        assertEquals("PGP", result.stopName());
        assertTrue(result.arrivals().isEmpty());
    }
}
