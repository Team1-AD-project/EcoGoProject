package com.ecogo.utils

import com.ecogo.data.TransportMode
import org.junit.Assert.*
import org.junit.Test

class CarbonCalculatorTest {

    // ==================== calculateEmission ====================

    @Test
    fun `calculateEmission WALK returns 0`() {
        val result = CarbonCalculator.calculateEmission(TransportMode.WALK, 5.0)
        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun `calculateEmission CYCLE returns 0`() {
        val result = CarbonCalculator.calculateEmission(TransportMode.CYCLE, 10.0)
        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun `calculateEmission BUS calculates correctly`() {
        // BUS rate = 50 g/km, distance = 4 km => 200g
        val result = CarbonCalculator.calculateEmission(TransportMode.BUS, 4.0)
        assertEquals(200.0, result, 0.001)
    }

    @Test
    fun `calculateEmission MIXED calculates correctly`() {
        // MIXED rate = 30 g/km, distance = 10 km => 300g
        val result = CarbonCalculator.calculateEmission(TransportMode.MIXED, 10.0)
        assertEquals(300.0, result, 0.001)
    }

    @Test
    fun `calculateEmission with zero distance returns 0`() {
        val result = CarbonCalculator.calculateEmission(TransportMode.BUS, 0.0)
        assertEquals(0.0, result, 0.001)
    }

    // ==================== calculateSavings ====================

    @Test
    fun `calculateSavings WALK saves full car emission`() {
        // Car: 120 g/km * 5 km = 600g, Walk: 0g => savings = 600g
        val result = CarbonCalculator.calculateSavings(TransportMode.WALK, 5.0)
        assertEquals(600.0, result, 0.001)
    }

    @Test
    fun `calculateSavings BUS saves partial emission`() {
        // Car: 120 * 4 = 480g, Bus: 50 * 4 = 200g => savings = 280g
        val result = CarbonCalculator.calculateSavings(TransportMode.BUS, 4.0)
        assertEquals(280.0, result, 0.001)
    }

    @Test
    fun `calculateSavings never returns negative`() {
        // Even with zero distance, result should be >= 0
        val result = CarbonCalculator.calculateSavings(TransportMode.BUS, 0.0)
        assertTrue(result >= 0.0)
    }

    // ==================== calculatePoints ====================

    @Test
    fun `calculatePoints converts grams to points at 0_5 rate`() {
        // 200g * 0.5 = 100 points
        val result = CarbonCalculator.calculatePoints(200.0)
        assertEquals(100, result)
    }

    @Test
    fun `calculatePoints rounds correctly`() {
        // 101g * 0.5 = 50.5 => rounds to 51
        val result = CarbonCalculator.calculatePoints(101.0)
        assertEquals(51, result)
    }

    @Test
    fun `calculatePoints with zero returns 0`() {
        assertEquals(0, CarbonCalculator.calculatePoints(0.0))
    }

    // ==================== calculateMoneySaved ====================

    @Test
    fun `calculateMoneySaved returns positive for non-zero distance`() {
        val result = CarbonCalculator.calculateMoneySaved(5.0)
        assertTrue(result > 0.0)
    }

    @Test
    fun `calculateMoneySaved never returns negative`() {
        val result = CarbonCalculator.calculateMoneySaved(0.0)
        assertTrue(result >= 0.0)
    }

    // ==================== formatCarbon ====================

    @Test
    fun `formatCarbon with zero returns 0g CO2`() {
        assertEquals("0g CO\u2082", CarbonCalculator.formatCarbon(0.0))
    }

    @Test
    fun `formatCarbon with grams shows grams`() {
        assertEquals("500g CO\u2082", CarbonCalculator.formatCarbon(500.0))
    }

    @Test
    fun `formatCarbon with kilograms shows kg`() {
        assertEquals("1.5kg CO\u2082", CarbonCalculator.formatCarbon(1500.0))
    }

    // ==================== getEcoRating ====================

    @Test
    fun `getEcoRating thresholds are correct`() {
        assertTrue(CarbonCalculator.getEcoRating(200.0).contains("超级环保"))
        assertTrue(CarbonCalculator.getEcoRating(100.0).contains("非常环保"))
        assertTrue(CarbonCalculator.getEcoRating(50.0).contains("环保"))
        assertTrue(CarbonCalculator.getEcoRating(10.0).contains("低碳"))
        assertTrue(CarbonCalculator.getEcoRating(0.0).contains("普通"))
    }
}
