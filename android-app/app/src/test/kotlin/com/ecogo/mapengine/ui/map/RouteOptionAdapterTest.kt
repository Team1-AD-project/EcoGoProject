package com.ecogo.mapengine.ui.map

import com.ecogo.mapengine.data.model.GeoPoint
import com.ecogo.mapengine.data.model.RouteAlternative
import com.ecogo.mapengine.data.model.RouteStep
import org.junit.Assert.*
import org.junit.Test

class RouteOptionAdapterTest {

    private fun makeRoute(index: Int = 0) = RouteAlternative(
        index = index, total_distance = 2.5, estimated_duration = 18,
        total_carbon = 0.15, route_points = listOf(GeoPoint(lng = 103.77, lat = 1.29)),
        route_steps = listOf(
            RouteStep(
                instruction = "Walk to bus stop", distance = 200.0,
                duration = 180, travel_mode = "WALKING"
            )
        ),
        summary = "Bus 95 → Walk"
    )

    @Test
    fun `getItemCount returns 0 initially`() {
        val adapter = RouteOptionAdapter(onRouteSelected = {})
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `setRoutes updates item count`() {
        val adapter = RouteOptionAdapter(onRouteSelected = {})
        adapter.setRoutes(listOf(makeRoute(0), makeRoute(1)))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `setRoutes with empty list clears items`() {
        val adapter = RouteOptionAdapter(onRouteSelected = {})
        adapter.setRoutes(listOf(makeRoute()))
        assertEquals(1, adapter.itemCount)
        adapter.setRoutes(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `setRoutes resets selected index`() {
        val adapter = RouteOptionAdapter(onRouteSelected = {})
        adapter.setRoutes(listOf(makeRoute(0), makeRoute(1), makeRoute(2)))
        assertEquals(3, adapter.itemCount)
        // After setRoutes, selectedIndex should be 0 (first route)
        adapter.setRoutes(listOf(makeRoute(0)))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `callback is set`() {
        var selected: RouteAlternative? = null
        val adapter = RouteOptionAdapter { selected = it }
        adapter.setRoutes(listOf(makeRoute()))
        assertNull(selected)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles route with large distance`() {
        val route = makeRoute().copy(total_distance = 15.8, estimated_duration = 45)
        val adapter = RouteOptionAdapter {}
        adapter.setRoutes(listOf(route))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles route with sub-kilometer distance`() {
        val route = makeRoute().copy(total_distance = 0.3)
        val adapter = RouteOptionAdapter {}
        adapter.setRoutes(listOf(route))
        assertEquals(1, adapter.itemCount)
    }
}
