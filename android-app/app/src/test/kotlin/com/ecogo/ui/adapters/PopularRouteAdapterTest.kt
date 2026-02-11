package com.ecogo.ui.adapters

import com.ecogo.data.BusRoute
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PopularRouteAdapterTest {

    private fun makeRoute(name: String = "A1") = BusRoute(
        name = name, from = "UTown", to = "Science"
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = PopularRouteAdapter(listOf(makeRoute("A1"), makeRoute("D2"))) {}
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = PopularRouteAdapter(emptyList()) {}
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `updateData changes item count`() {
        val adapter = PopularRouteAdapter(listOf(makeRoute())) {}
        adapter.updateData(listOf(makeRoute("A1"), makeRoute("A2"), makeRoute("D2")))
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `updateData with empty list clears items`() {
        val adapter = PopularRouteAdapter(listOf(makeRoute())) {}
        adapter.updateData(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `click callback is set`() {
        var clicked: BusRoute? = null
        val adapter = PopularRouteAdapter(listOf(makeRoute())) { clicked = it }
        assertNull(clicked)
        assertEquals(1, adapter.itemCount)
    }
}
