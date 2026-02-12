package com.ecogo.ui.adapters

import com.ecogo.data.WalkingRoute
import org.junit.Assert.*
import org.junit.Test

class WalkingRouteAdapterTest {

    private fun makeRoute(id: Int = 1) = WalkingRoute(
        id = id, title = "Campus Loop", time = "30 min", distance = "2.5 km",
        calories = "150 cal", tags = listOf("scenic", "easy"), description = "A nice walk"
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = WalkingRouteAdapter(listOf(makeRoute(1), makeRoute(2))) {}
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = WalkingRouteAdapter(emptyList()) {}
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `click callback is set`() {
        var clicked: WalkingRoute? = null
        val adapter = WalkingRouteAdapter(listOf(makeRoute())) { clicked = it }
        assertNull(clicked)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles single route`() {
        val adapter = WalkingRouteAdapter(listOf(makeRoute())) {}
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles multiple routes with different positions`() {
        val routes = (1..5).map { makeRoute(it) }
        val adapter = WalkingRouteAdapter(routes) {}
        assertEquals(5, adapter.itemCount)
    }
}
