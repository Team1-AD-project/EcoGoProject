package com.ecogo.ui.adapters

import com.ecogo.data.Activity
import org.junit.Assert.*
import org.junit.Test

class ActivityAdapterTest {

    private fun makeActivity(id: String = "act1", title: String = "Beach Clean") = Activity(
        id = id, title = title, description = "Help clean the beach", type = "OFFLINE",
        status = "PUBLISHED", rewardCredits = 50, startTime = "2026-03-01T10:00:00"
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = ActivityAdapter(listOf(makeActivity(), makeActivity("act2")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = ActivityAdapter(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `updateActivities changes item count`() {
        val adapter = ActivityAdapter(listOf(makeActivity()))
        assertEquals(1, adapter.itemCount)
        adapter.updateActivities(listOf(makeActivity("a1"), makeActivity("a2"), makeActivity("a3")))
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `updateActivities with empty list clears items`() {
        val adapter = ActivityAdapter(listOf(makeActivity()))
        adapter.updateActivities(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `click callback is set`() {
        var clicked: Activity? = null
        val adapter = ActivityAdapter(listOf(makeActivity())) { clicked = it }
        assertNull(clicked)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles activity with null startTime`() {
        val act = Activity(title = "Online Event", type = "ONLINE", startTime = null)
        val adapter = ActivityAdapter(listOf(act))
        assertEquals(1, adapter.itemCount)
    }
}
