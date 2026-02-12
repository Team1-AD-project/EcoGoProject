package com.ecogo.utils

import org.junit.Assert.*
import org.junit.Test

class TripTimeFormatTest {

    companion object {
        private const val FALLBACK_TEXT = "--"
        private const val TEST_LOCAL_TIME = "2026-02-10T04:26:43.034"
        private const val TEST_END_TIME = "2026-02-10T05:00:00.000"
    }

    @Test
    fun `toSgTime with null returns --`() {
        assertEquals(FALLBACK_TEXT, TripTimeFormat.toSgTime(null))
    }

    @Test
    fun `toSgTime with blank returns --`() {
        assertEquals(FALLBACK_TEXT, TripTimeFormat.toSgTime(""))
        assertEquals(FALLBACK_TEXT, TripTimeFormat.toSgTime("   "))
    }

    @Test
    fun `toSgTime with offset converts to Singapore time`() {
        // 2026-02-06T14:26:36.736+00:00 => UTC+8 = 2026-02-06 22:26
        val result = TripTimeFormat.toSgTime("2026-02-06T14:26:36.736+00:00")
        assertEquals("2026-02-06 22:26", result)
    }

    @Test
    fun `toSgTime with Z suffix converts to Singapore time`() {
        // 2026-01-15T06:00:00Z => UTC+8 = 2026-01-15 14:00
        val result = TripTimeFormat.toSgTime("2026-01-15T06:00:00Z")
        assertEquals("2026-01-15 14:00", result)
    }

    @Test
    fun `toSgTime without offset parses as local time`() {
        val result = TripTimeFormat.toSgTime(TEST_LOCAL_TIME)
        assertEquals("2026-02-10 04:26", result)
    }

    @Test
    fun `toSgTime with malformed input returns raw string`() {
        val raw = "not-a-date"
        assertEquals(raw, TripTimeFormat.toSgTime(raw))
    }

    @Test
    fun `rangeText with start and end`() {
        val result = TripTimeFormat.rangeText(
            TEST_LOCAL_TIME,
            TEST_END_TIME
        )
        assertEquals("2026-02-10 04:26 ~ 2026-02-10 05:00", result)
    }

    @Test
    fun `rangeText with null end shows Now`() {
        val result = TripTimeFormat.rangeText(TEST_LOCAL_TIME, null)
        assertTrue(result.endsWith("Now"))
    }

    @Test
    fun `rangeText with blank end shows Now`() {
        val result = TripTimeFormat.rangeText(TEST_LOCAL_TIME, "")
        assertTrue(result.endsWith("Now"))
    }
}
