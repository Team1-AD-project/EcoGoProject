package com.ecogo.ui.adapters

import com.ecogo.data.HistoryItem
import org.junit.Assert.*
import org.junit.Test

class HistoryAdapterTest {

    private fun makeHistory(id: Int = 1, type: String = "earn") = HistoryItem(
        id = id, action = "Completed trip", time = "2026-02-11", points = "+50", type = type
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = HistoryAdapter(listOf(makeHistory(1), makeHistory(2)))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = HistoryAdapter(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `adapter handles earn type`() {
        val adapter = HistoryAdapter(listOf(makeHistory(type = "earn")))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles spend type`() {
        val adapter = HistoryAdapter(listOf(makeHistory(type = "spend")))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles mixed types`() {
        val items = listOf(makeHistory(1, "earn"), makeHistory(2, "spend"), makeHistory(3, "earn"))
        val adapter = HistoryAdapter(items)
        assertEquals(3, adapter.itemCount)
    }
}
