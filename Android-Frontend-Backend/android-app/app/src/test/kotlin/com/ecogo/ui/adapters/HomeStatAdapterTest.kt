package com.ecogo.ui.adapters

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HomeStatAdapterTest {

    private fun makeStat(title: String = "Carbon Saved") = HomeStat(
        icon = "🌱", title = title, value = "120kg", subtitle = "This month", color = "#10B981"
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = HomeStatAdapter(listOf(makeStat(), makeStat("Trips")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = HomeStatAdapter(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `updateData changes item count`() {
        val adapter = HomeStatAdapter(listOf(makeStat()))
        adapter.updateData(listOf(makeStat("A"), makeStat("B"), makeStat("C")))
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `updateData with empty list clears items`() {
        val adapter = HomeStatAdapter(listOf(makeStat()))
        adapter.updateData(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `click callback is set`() {
        var clicked: HomeStat? = null
        val adapter = HomeStatAdapter(listOf(makeStat())) { clicked = it }
        assertNull(clicked)
        assertEquals(1, adapter.itemCount)
    }
}
