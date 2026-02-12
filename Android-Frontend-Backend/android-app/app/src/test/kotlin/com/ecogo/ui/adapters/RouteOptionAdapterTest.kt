package com.ecogo.ui.adapters

import com.ecogo.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RouteOptionAdapterTest {

    private fun makeLocation(id: String = "loc1") = NavLocation(
        id = id, name = "COM1", address = "13 Computing Drive",
        latitude = 1.2945, longitude = 103.7735,
        type = LocationType.FACULTY, icon = "🏫"
    )

    private fun makeRoute(id: String = "r1") = NavRoute(
        id = id, origin = makeLocation("o1"), destination = makeLocation("d1"),
        mode = TransportMode.WALK, distance = 1.2, duration = 15,
        carbonEmission = 0.0, carbonSaved = 150.0, points = 20,
        steps = emptyList(), polyline = "", isRecommended = false
    )

    @Test
    fun `getItemCount returns 0 initially`() {
        val adapter = RouteOptionAdapter(onRouteClick = {})
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `getItemCount with initial data`() {
        val adapter = RouteOptionAdapter(listOf(makeRoute("r1"), makeRoute("r2"))) {}
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `updateRoutes changes item count`() {
        val adapter = RouteOptionAdapter(onRouteClick = {})
        adapter.updateRoutes(listOf(makeRoute("r1"), makeRoute("r2")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `updateRoutes with empty list clears items`() {
        val adapter = RouteOptionAdapter(listOf(makeRoute())) {}
        adapter.updateRoutes(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `click callback is set`() {
        var clicked: NavRoute? = null
        val adapter = RouteOptionAdapter(listOf(makeRoute())) { clicked = it }
        assertNull(clicked)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles recommended route`() {
        val route = makeRoute().copy(isRecommended = true, badge = "Greenest")
        val adapter = RouteOptionAdapter(listOf(route)) {}
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles all transport modes`() {
        val routes = TransportMode.values().map { mode ->
            makeRoute().copy(id = mode.name, mode = mode)
        }
        val adapter = RouteOptionAdapter(routes) {}
        assertEquals(TransportMode.values().size, adapter.itemCount)
    }
}
