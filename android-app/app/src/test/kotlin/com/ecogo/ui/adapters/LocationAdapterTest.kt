package com.ecogo.ui.adapters

import com.ecogo.data.LocationType
import com.ecogo.data.NavLocation
import org.junit.Assert.*
import org.junit.Test

class LocationAdapterTest {

    private fun makeLocation(id: String = "loc1") = NavLocation(
        id = id, name = "COM1", address = "13 Computing Drive",
        latitude = 1.2945, longitude = 103.7735, type = LocationType.FACULTY, icon = "🏫"
    )

    @Test
    fun `getItemCount returns 0 initially`() {
        val adapter = LocationAdapter(onLocationClick = {})
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `getItemCount with initial data`() {
        val adapter = LocationAdapter(listOf(makeLocation("l1"), makeLocation("l2"))) {}
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `updateLocations changes item count`() {
        val adapter = LocationAdapter(onLocationClick = {})
        adapter.updateLocations(listOf(makeLocation("l1"), makeLocation("l2")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `updateLocations with empty list clears items`() {
        val adapter = LocationAdapter(listOf(makeLocation()), onLocationClick = {})
        adapter.updateLocations(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `click callback is set`() {
        var clicked: NavLocation? = null
        val adapter = LocationAdapter(listOf(makeLocation())) { clicked = it }
        assertNull(clicked)
        assertEquals(1, adapter.itemCount)
    }
}
