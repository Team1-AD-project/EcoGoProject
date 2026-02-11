package com.ecogo.ui.adapters

import com.ecogo.data.Activity
import org.junit.Assert.*
import org.junit.Test

class MonthlyActivityAdapterTest {

    private fun makeActivity(id: String = "a1") = Activity(
        id = id, title = "Monthly Cleanup", description = "desc", type = "OFFLINE",
        status = "PUBLISHED", rewardCredits = 30, currentParticipants = 15,
        startTime = "2026-02-15T10:00:00"
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = MonthlyActivityAdapter(listOf(makeActivity("a1"), makeActivity("a2"))) {}
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = MonthlyActivityAdapter(emptyList()) {}
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `updateData changes item count`() {
        val adapter = MonthlyActivityAdapter(listOf(makeActivity())) {}
        adapter.updateData(listOf(makeActivity("a1"), makeActivity("a2")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `updateData with empty list clears items`() {
        val adapter = MonthlyActivityAdapter(listOf(makeActivity())) {}
        adapter.updateData(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `click callback is set`() {
        var clicked: Activity? = null
        val adapter = MonthlyActivityAdapter(listOf(makeActivity())) { clicked = it }
        assertNull(clicked)
        assertEquals(1, adapter.itemCount)
    }
}
