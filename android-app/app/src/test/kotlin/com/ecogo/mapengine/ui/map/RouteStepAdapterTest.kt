package com.ecogo.mapengine.ui.map

import com.ecogo.mapengine.data.model.GeoPoint
import com.ecogo.mapengine.data.model.RouteStep
import com.ecogo.mapengine.data.model.TransitDetails
import org.junit.Assert.*
import org.junit.Test

class RouteStepAdapterTest {

    private fun makeStep(instruction: String = "Walk north") = RouteStep(
        instruction = instruction, distance = 200.0, duration = 180,
        travel_mode = "WALKING"
    )

    private fun makeTransitStep() = RouteStep(
        instruction = "Take Bus 95", distance = 3500.0, duration = 720,
        travel_mode = "TRANSIT",
        transit_details = TransitDetails(
            line_name = "Bus 95", line_short_name = "95",
            departure_stop = "COM2", arrival_stop = "UTown",
            num_stops = 4, vehicle_type = "BUS", headsign = "Clementi"
        )
    )

    @Test
    fun `getItemCount returns 0 initially`() {
        val adapter = RouteStepAdapter()
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `setSteps updates item count`() {
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(makeStep("Step 1"), makeStep("Step 2")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `setSteps with empty list clears items`() {
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(makeStep()))
        assertEquals(1, adapter.itemCount)
        adapter.setSteps(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `adapter handles transit step with details`() {
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(makeTransitStep()))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles mixed walking and transit steps`() {
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(makeStep("Walk to stop"), makeTransitStep(), makeStep("Walk to dest")))
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `adapter handles step with polyline points`() {
        val step = makeStep().copy(
            polyline_points = listOf(GeoPoint(lng = 103.77, lat = 1.29), GeoPoint(lng = 103.78, lat = 1.30))
        )
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(step))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles step with null transit details`() {
        val step = makeStep().copy(transit_details = null)
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(step))
        assertEquals(1, adapter.itemCount)
    }
}
