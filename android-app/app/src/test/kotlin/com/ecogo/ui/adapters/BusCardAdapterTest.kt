package com.ecogo.ui.adapters

import com.ecogo.data.BusInfo
import org.junit.Assert.*
import org.junit.Test

class BusCardAdapterTest {

    private fun makeBusInfo(id: String = "bus1") = BusInfo(
        busId = id, routeName = "A1", destination = "NUS", currentLat = 1.296,
        currentLng = 103.776, etaMinutes = 5, stopsAway = 2, crowdLevel = "low",
        plateNumber = "SG1234", status = "arriving"
    )

    @Test
    fun `getItemCount returns 0 initially`() {
        val adapter = BusCardAdapter()
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `getItemCount with initial data`() {
        val adapter = BusCardAdapter(listOf(makeBusInfo("b1"), makeBusInfo("b2")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `updateBusList changes item count`() {
        val adapter = BusCardAdapter()
        adapter.updateBusList(listOf(makeBusInfo("b1"), makeBusInfo("b2"), makeBusInfo("b3")))
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `updateBusList with empty list clears items`() {
        val adapter = BusCardAdapter(listOf(makeBusInfo()))
        adapter.updateBusList(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `adapter handles different statuses`() {
        val buses = listOf(
            makeBusInfo("b1").copy(status = "arriving"),
            makeBusInfo("b2").copy(status = "coming"),
            makeBusInfo("b3").copy(status = "delayed")
        )
        val adapter = BusCardAdapter(buses)
        assertEquals(3, adapter.itemCount)
    }
}
