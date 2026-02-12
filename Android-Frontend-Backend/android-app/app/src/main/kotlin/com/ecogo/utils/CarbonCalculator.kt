package com.ecogo.utils

import com.ecogo.data.TransportMode
import kotlin.math.roundToInt

/**
 * Carbon emission calculator
 * Calculates carbon emissions and savings based on transport mode and distance
 */
object CarbonCalculator {

    // Carbon emission rates (g CO2/km)
    private val CARBON_RATES = mapOf(
        TransportMode.WALK to 0.0,
        TransportMode.CYCLE to 0.0,
        TransportMode.BUS to 50.0,
        TransportMode.MIXED to 30.0  // Average
    )

    // Baseline: driving carbon emission (g CO2/km)
    private const val CAR_CARBON_RATE = 120.0

    // Points conversion rate: how many points per 1g CO2 saved
    private const val POINTS_PER_GRAM = 0.5

    /**
     * Calculate carbon emissions
     * @param mode Transport mode
     * @param distanceKm Distance (kilometers)
     * @return Carbon emissions (grams)
     */
    fun calculateEmission(mode: TransportMode, distanceKm: Double): Double {
        val rate = CARBON_RATES[mode] ?: 0.0
        return (rate * distanceKm).roundToInt().toDouble()
    }

    /**
     * Calculate carbon emissions saved (compared to driving)
     * @param mode Transport mode
     * @param distanceKm Distance (kilometers)
     * @return Carbon emissions saved (grams)
     */
    fun calculateSavings(mode: TransportMode, distanceKm: Double): Double {
        val carEmission = CAR_CARBON_RATE * distanceKm
        val currentEmission = calculateEmission(mode, distanceKm)
        return (carEmission - currentEmission).coerceAtLeast(0.0).roundToInt().toDouble()
    }

    /**
     * Calculate green points based on carbon emissions saved
     * @param carbonSavedGrams Carbon emissions saved (grams)
     * @return Green points
     */
    fun calculatePoints(carbonSavedGrams: Double): Int {
        return (carbonSavedGrams * POINTS_PER_GRAM).roundToInt()
    }

    /**
     * Calculate money saved (assuming taxi as alternative)
     * @param distanceKm Distance (kilometers)
     * @return Money saved (SGD)
     */
    fun calculateMoneySaved(distanceKm: Double): Double {
        // Singapore taxi fare: base fare SGD 3.9 + SGD 0.22 per 400m
        val taxiFare = 3.9 + (distanceKm * 1000 / 400) * 0.22
        // Walking/cycling/bus cost (bus approx. SGD 1)
        val costBus = 1.0
        return (taxiFare - costBus).coerceAtLeast(0.0)
    }

    /**
     * Format carbon emissions for display
     * @param grams Carbon emissions (grams)
     * @return Formatted string
     */
    fun formatCarbon(grams: Double): String {
        return when {
            grams >= 1000 -> String.format("%.1fkg CO₂", grams / 1000)
            grams > 0 -> String.format("%.0fg CO₂", grams)
            else -> "0g CO₂"
        }
    }

    /**
     * Get eco-friendly rating
     * @param carbonSaved Carbon emissions saved (grams)
     * @return Eco-friendly rating description
     */
    fun getEcoRating(carbonSaved: Double): String {
        return when {
            carbonSaved >= 200 -> "🌟 Super Eco-friendly"
            carbonSaved >= 100 -> "🌿 Very Eco-friendly"
            carbonSaved >= 50 -> "🍃 Eco-friendly"
            carbonSaved > 0 -> "♻️ Low-carbon"
            else -> "🚗 Standard"
        }
    }
}
