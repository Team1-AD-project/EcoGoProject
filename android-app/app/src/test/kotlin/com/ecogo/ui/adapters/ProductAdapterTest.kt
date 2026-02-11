package com.ecogo.ui.adapters

import com.ecogo.data.Product
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProductAdapterTest {

    private fun makeProduct(id: String = "p1", available: Boolean = true) = Product(
        id = id, name = "Starbucks Gift Card", description = "A nice gift card",
        type = "voucher", category = "food", pointsPrice = 500, cashPrice = 5.0,
        available = available, stock = 10
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = ProductAdapter(listOf(makeProduct("p1"), makeProduct("p2"))) { _, _ -> }
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = ProductAdapter(emptyList()) { _, _ -> }
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `updateProducts changes item count`() {
        val adapter = ProductAdapter(listOf(makeProduct())) { _, _ -> }
        adapter.updateProducts(listOf(makeProduct("p1"), makeProduct("p2")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `updateProducts with empty list clears items`() {
        val adapter = ProductAdapter(listOf(makeProduct())) { _, _ -> }
        adapter.updateProducts(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `purchase callback is set`() {
        var purchasedProduct: Product? = null
        var paymentMethod: String? = null
        val adapter = ProductAdapter(listOf(makeProduct())) { p, m ->
            purchasedProduct = p; paymentMethod = m
        }
        assertNull(purchasedProduct)
        assertNull(paymentMethod)
    }

    @Test
    fun `adapter handles product with null prices`() {
        val product = Product(
            id = "p1", name = "Free Item", description = "desc",
            type = "goods", category = "other", pointsPrice = null, cashPrice = null
        )
        val adapter = ProductAdapter(listOf(product)) { _, _ -> }
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles unavailable product`() {
        val adapter = ProductAdapter(listOf(makeProduct(available = false))) { _, _ -> }
        assertEquals(1, adapter.itemCount)
    }
}
