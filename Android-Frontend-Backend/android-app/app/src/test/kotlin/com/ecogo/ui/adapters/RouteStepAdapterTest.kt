package com.ecogo.ui.adapters

import com.ecogo.data.RouteStep
import com.ecogo.data.TransportMode
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RouteStepAdapterTest {

    private fun makeStep(instruction: String = "Walk north") = RouteStep(
        instruction = instruction, distance = 200.0, duration = 180,
        mode = TransportMode.WALK, startLat = 1.2945, startLng = 103.7735,
        endLat = 1.2955, endLng = 103.7740
    )

    @Test
    fun `getItemCount returns 0 initially`() {
        val adapter = RouteStepAdapter()
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `getItemCount with initial data`() {
        val adapter = RouteStepAdapter(listOf(makeStep("Step 1"), makeStep("Step 2")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `updateSteps changes item count`() {
        val adapter = RouteStepAdapter()
        adapter.updateSteps(listOf(makeStep("A"), makeStep("B"), makeStep("C")))
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `updateSteps with empty list clears items`() {
        val adapter = RouteStepAdapter(listOf(makeStep()))
        adapter.updateSteps(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `adapter handles bus mode step`() {
        val step = makeStep().copy(mode = TransportMode.BUS, distance = 2500.0, duration = 600)
        val adapter = RouteStepAdapter(listOf(step))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles step with polyline`() {
        val step = makeStep().copy(polyline = "a~l~Fjk~uOwHJy@P")
        val adapter = RouteStepAdapter(listOf(step))
        assertEquals(1, adapter.itemCount)
    }
}
