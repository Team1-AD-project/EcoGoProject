package com.ecogo.ui.adapters

import com.ecogo.data.ShopItem
import org.junit.Assert.*
import org.junit.Test

class ShopItemAdapterTest {

    private fun makeShopItem(id: String = "hat_grad", owned: Boolean = false, equipped: Boolean = false) =
        ShopItem(id = id, name = "Grad Cap", type = "head", cost = 50, owned = owned, equipped = equipped)

    @Test
    fun `getItemCount returns correct size`() {
        val items = listOf(
            ShopListItem.Header("Headwear"),
            ShopListItem.Item(makeShopItem("hat_grad")),
            ShopListItem.Item(makeShopItem("hat_cap"))
        )
        val adapter = ShopItemAdapter(items) {}
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = ShopItemAdapter(emptyList()) {}
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `getItemViewType returns HEADER for header items`() {
        val items = listOf(
            ShopListItem.Header("Head"),
            ShopListItem.Item(makeShopItem())
        )
        val adapter = ShopItemAdapter(items) {}
        assertEquals(0, adapter.getItemViewType(0)) // VIEW_TYPE_HEADER
        assertEquals(1, adapter.getItemViewType(1)) // VIEW_TYPE_ITEM
    }

    @Test
    fun `isHeader returns true for header position`() {
        val items = listOf(
            ShopListItem.Header("Head"),
            ShopListItem.Item(makeShopItem())
        )
        val adapter = ShopItemAdapter(items) {}
        assertTrue(adapter.isHeader(0))
        assertFalse(adapter.isHeader(1))
    }

    @Test
    fun `isHeader returns false for out of bounds`() {
        val adapter = ShopItemAdapter(emptyList()) {}
        assertFalse(adapter.isHeader(99))
    }

    @Test
    fun `updateItems changes item count`() {
        val adapter = ShopItemAdapter(listOf(ShopListItem.Header("Old"))) {}
        adapter.updateItems(listOf(
            ShopListItem.Header("New"),
            ShopListItem.Item(makeShopItem("i1")),
            ShopListItem.Item(makeShopItem("i2"))
        ))
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `click callback is set`() {
        var clicked: ShopItem? = null
        val adapter = ShopItemAdapter(listOf(ShopListItem.Item(makeShopItem()))) { clicked = it }
        assertNull(clicked)
        assertEquals(1, adapter.itemCount)
    }
}
