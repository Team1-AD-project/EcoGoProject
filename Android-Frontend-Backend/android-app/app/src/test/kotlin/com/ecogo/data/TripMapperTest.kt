package com.ecogo.data

import org.junit.Assert.*
import org.junit.Test

class TripMapperTest {

    companion object {
        private const val TEST_START_TIME = "2026-02-10T04:00:00.000"
        private const val TEST_END_TIME = "2026-02-10T04:30:00.000"
    }

    @Test
    fun `toSummary with complete trip builds correct summary`() {
        val trip = TripDto(
            id = "trip-001",
            startLocation = PlaceLocation(address = "NUS SOC", placeName = "School of Computing"),
            endLocation = PlaceLocation(address = "NUS BIZ", placeName = "Business School"),
            startTime = TEST_START_TIME,
            endTime = TEST_END_TIME,
            detectedMode = "WALKING",
            distance = 1.25,
            pointsGained = 50,
            carbonSaved = 0.15,
            carbonStatus = "GREEN"
        )

        val summary = TripMapper.toSummary(trip)

        assertEquals("trip-001", summary.id)
        assertTrue(summary.routeText.contains("School of Computing"))
        assertTrue(summary.routeText.contains("Business School"))
        assertTrue(summary.primaryText.contains("+50 pts"))
        assertTrue(summary.primaryText.contains("0.15"))
        assertTrue(summary.metaText.contains("WALKING"))
        assertTrue(summary.metaText.contains("1.25"))
        assertEquals("GREEN", summary.statusText)
    }

    @Test
    fun `toSummary with null locations uses fallback text`() {
        val trip = TripDto(
            id = "trip-002",
            startLocation = null,
            endLocation = null,
            startTime = TEST_START_TIME,
            endTime = null
        )

        val summary = TripMapper.toSummary(trip)

        assertTrue(summary.routeText.contains("Unknown start"))
        assertTrue(summary.routeText.contains("In progress"))
    }

    @Test
    fun `toSummary with null endTime shows Now and TRACKING status`() {
        val trip = TripDto(
            id = "trip-003",
            startTime = TEST_START_TIME,
            endTime = null,
            carbonStatus = null
        )

        val summary = TripMapper.toSummary(trip)

        assertTrue(summary.timeText.contains("Now"))
        assertEquals("TRACKING", summary.statusText)
    }

    @Test
    fun `toSummary with completed trip and no carbonStatus defaults to COMPLETED`() {
        val trip = TripDto(
            id = "trip-004",
            startTime = TEST_START_TIME,
            endTime = TEST_END_TIME,
            carbonStatus = null
        )

        val summary = TripMapper.toSummary(trip)

        assertEquals("COMPLETED", summary.statusText)
    }

    @Test
    fun `toSummary with null distance shows mode only in meta`() {
        val trip = TripDto(
            id = "trip-005",
            detectedMode = "BUS",
            distance = null
        )

        val summary = TripMapper.toSummary(trip)

        assertEquals("BUS", summary.metaText)
    }

    @Test
    fun `toSummary uses transportModes fallback when detectedMode is null`() {
        val trip = TripDto(
            id = "trip-006",
            detectedMode = null,
            transportModes = listOf(TransportSegmentDto(mode = "CYCLING", subDistance = 2.0)),
            distance = 2.0
        )

        val summary = TripMapper.toSummary(trip)

        assertTrue(summary.metaText.contains("CYCLING"))
    }

    @Test
    fun `toSummary with null points defaults to 0`() {
        val trip = TripDto(
            id = "trip-007",
            pointsGained = null,
            carbonSaved = null
        )

        val summary = TripMapper.toSummary(trip)

        assertTrue(summary.primaryText.contains("+0 pts"))
        assertTrue(summary.primaryText.contains("0.00"))
    }

    @Test
    fun `toSummary prefers placeName over address`() {
        val trip = TripDto(
            id = "trip-008",
            startLocation = PlaceLocation(address = "Address A", placeName = "Place A"),
            endLocation = PlaceLocation(address = "Address B", placeName = null)
        )

        val summary = TripMapper.toSummary(trip)

        // placeName is preferred; if null, falls back to address
        assertTrue(summary.routeText.contains("Place A"))
        assertTrue(summary.routeText.contains("Address B"))
    }
}
