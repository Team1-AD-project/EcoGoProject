package com.ecogo.ui.adapters

import org.junit.Assert.*
import org.junit.Test

class TransportModeAdapterTest {

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = TransportModeAdapter(listOf("walk", "bus", "bike"), mutableSetOf()) {}
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = TransportModeAdapter(emptyList(), mutableSetOf()) {}
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `adapter handles all transport modes`() {
        val modes = listOf("walk", "bike", "bus", "subway", "car", "electric_bike")
        val adapter = TransportModeAdapter(modes, mutableSetOf()) {}
        assertEquals(6, adapter.itemCount)
    }

    @Test
    fun `adapter accepts pre-selected modes`() {
        val selected = mutableSetOf("walk", "bus")
        val adapter = TransportModeAdapter(listOf("walk", "bus", "bike"), selected) {}
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `selection callback is set`() {
        var changedSelection: Set<String>? = null
        val adapter = TransportModeAdapter(
            listOf("walk"), mutableSetOf()
        ) { changedSelection = it }
        assertNull(changedSelection)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles unknown mode`() {
        val adapter = TransportModeAdapter(listOf("unknown_mode"), mutableSetOf()) {}
        assertEquals(1, adapter.itemCount)
    }
}
