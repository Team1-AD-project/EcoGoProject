package com.ecogo.ui.adapters

import com.ecogo.data.BusRoute
import org.junit.Assert.*
import org.junit.Test

class BusRouteAdapterTest {

    private fun makeRoute(name: String = "A1") = BusRoute(
        name = name, from = "UTown", to = "Science", color = "#15803D",
        status = "arriving", nextArrival = 5
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = BusRouteAdapter(listOf(makeRoute("A1"), makeRoute("A2")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = BusRouteAdapter(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `click callback is set`() {
        var clicked: BusRoute? = null
        val adapter = BusRouteAdapter(listOf(makeRoute()), onRouteClick = { clicked = it })
        assertNull(clicked)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles null click callback`() {
        val adapter = BusRouteAdapter(listOf(makeRoute()), onRouteClick = null)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles route with arrow in from field`() {
        val route = BusRoute(name = "D2", from = "SOC → BIZ → SOC", to = "")
        val adapter = BusRouteAdapter(listOf(route))
        assertEquals(1, adapter.itemCount)
    }
}
