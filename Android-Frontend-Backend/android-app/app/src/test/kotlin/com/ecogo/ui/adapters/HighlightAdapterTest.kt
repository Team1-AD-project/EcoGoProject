package com.ecogo.ui.adapters

import com.ecogo.data.Activity
import org.junit.Assert.*
import org.junit.Test

class HighlightAdapterTest {

    private fun makeActivity(title: String = "Beach Cleanup") = Activity(
        id = "a1", title = title, description = "Environmental event",
        type = "OFFLINE", startTime = "2026-03-01T10:00:00"
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = HighlightAdapter(listOf(makeActivity(), makeActivity("Workshop"))) {}
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = HighlightAdapter(emptyList()) {}
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `click callback is set`() {
        var clicked: Activity? = null
        val adapter = HighlightAdapter(listOf(makeActivity())) { clicked = it }
        assertNull(clicked)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles activity with null startTime`() {
        val act = Activity(title = "Online Event", startTime = null)
        val adapter = HighlightAdapter(listOf(act)) {}
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles various title keywords`() {
        val activities = listOf(
            makeActivity("Clean the Park"),
            makeActivity("Workshop on Recycling"),
            makeActivity("Run for Green"),
            makeActivity("Recycle Drive")
        )
        val adapter = HighlightAdapter(activities) {}
        assertEquals(4, adapter.itemCount)
    }
}
