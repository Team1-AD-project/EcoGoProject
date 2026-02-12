package com.ecogo.utils

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TripTimeFormat {

    // Modify the display format here as needed
    // Example: 2026-02-10 12:26
    private val outFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())

    private val sgZone = ZoneId.of("Asia/Singapore")

    /**
     * Compatible with two backend time formats:
     * 1) 2026-02-06T14:26:36.736+00:00 (with offset)
     * 2) 2026-02-10T04:26:43.034       (without offset)
     */
    fun toSgTime(raw: String?): String {
        if (raw.isNullOrBlank()) return "--"

        return try {
            if (raw.contains("+") || raw.endsWith("Z")) {
                // With timezone offset: convert to Singapore time for display
                OffsetDateTime.parse(raw)
                    .atZoneSameInstant(sgZone)
                    .format(outFmt)
            } else {
                // Without offset: parse as LocalDateTime (treat as backend-provided local time)
                LocalDateTime.parse(raw)
                    .format(outFmt)
            }
        } catch (e: Exception) {
            raw // Fallback: display as-is if parsing fails
        }
    }

    fun rangeText(start: String?, end: String?): String {
        val s = toSgTime(start)
        val e = if (end.isNullOrBlank()) "Now" else toSgTime(end)
        return "$s ~ $e"
    }
}
