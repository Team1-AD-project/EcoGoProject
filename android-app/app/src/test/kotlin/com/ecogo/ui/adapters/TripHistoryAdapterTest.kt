package com.ecogo.ui.adapters

import com.ecogo.data.TripSummaryUi
import org.junit.Assert.*
import org.junit.Test

class TripHistoryAdapterTest {

    private fun makeTrip(id: String = "t1") = TripSummaryUi(
        id = id, routeText = "COM1 → UTown",
        timeText = "10:30 AM", primaryText = "1.2 km walk",
        metaText = "Saved 0.3 kg CO₂", statusText = "Completed"
    )

    @Test
    fun `getItemCount returns 0 initially`() {
        val adapter = TripHistoryAdapter()
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `getItemCount with initial data`() {
        val adapter = TripHistoryAdapter(listOf(makeTrip("t1"), makeTrip("t2")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `update changes item count`() {
        val adapter = TripHistoryAdapter()
        adapter.update(listOf(makeTrip("t1"), makeTrip("t2"), makeTrip("t3")))
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `update with empty list clears items`() {
        val adapter = TripHistoryAdapter(listOf(makeTrip()))
        adapter.update(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `update replaces previous data`() {
        val adapter = TripHistoryAdapter(listOf(makeTrip("t1")))
        assertEquals(1, adapter.itemCount)
        adapter.update(listOf(makeTrip("t2"), makeTrip("t3")))
        assertEquals(2, adapter.itemCount)
    }
}
