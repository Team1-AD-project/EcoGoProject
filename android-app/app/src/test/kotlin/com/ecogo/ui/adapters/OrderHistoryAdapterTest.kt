package com.ecogo.ui.adapters

import com.ecogo.data.OrderSummaryUi
import org.junit.Assert.*
import org.junit.Test

class OrderHistoryAdapterTest {

    private fun makeOrder(id: String = "o1") = OrderSummaryUi(
        id = id, orderNumber = "ORD-001", status = "COMPLETED",
        finalAmount = 29.90, createdAt = "2025-05-01T10:00:00",
        itemCount = 2, isRedemption = false,
        trackingNumber = "TRK123", carrier = "SingPost"
    )

    @Test
    fun `getItemCount returns 0 initially`() {
        val adapter = OrderHistoryAdapter()
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `getItemCount with initial data`() {
        val adapter = OrderHistoryAdapter(listOf(makeOrder("o1"), makeOrder("o2")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `update changes item count`() {
        val adapter = OrderHistoryAdapter()
        adapter.update(listOf(makeOrder("o1"), makeOrder("o2")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `update with empty list clears items`() {
        val adapter = OrderHistoryAdapter(listOf(makeOrder()))
        adapter.update(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `adapter handles redemption order`() {
        val order = makeOrder().copy(isRedemption = true, finalAmount = 0.0)
        val adapter = OrderHistoryAdapter(listOf(order))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles order with null optional fields`() {
        val order = OrderSummaryUi(id = "o1")
        val adapter = OrderHistoryAdapter(listOf(order))
        assertEquals(1, adapter.itemCount)
    }
}
