package com.ecogo.ui.adapters

import com.ecogo.ui.fragments.MonthStat
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MonthStatAdapterTest {

    private fun makeStat(title: String = "Steps") = MonthStat(
        icon = "🚶", title = title, value = "12,345",
        subtitle = "+15% vs last month", color = "#4CAF50"
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = MonthStatAdapter(listOf(makeStat("Steps"), makeStat("Carbon")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = MonthStatAdapter(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `updateData changes item count`() {
        val adapter = MonthStatAdapter(emptyList())
        adapter.updateData(listOf(makeStat("Steps"), makeStat("Carbon"), makeStat("Trips")))
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `updateData with empty list clears items`() {
        val adapter = MonthStatAdapter(listOf(makeStat()))
        adapter.updateData(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `adapter handles various color formats`() {
        val stats = listOf(
            makeStat().copy(color = "#FF5722"),
            makeStat().copy(color = "#2196F3"),
            makeStat().copy(color = "#9C27B0")
        )
        val adapter = MonthStatAdapter(stats)
        assertEquals(3, adapter.itemCount)
    }
}
