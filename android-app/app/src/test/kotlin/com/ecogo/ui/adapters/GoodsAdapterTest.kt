package com.ecogo.ui.adapters

import com.ecogo.data.ShopItem
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GoodsAdapterTest {

    private fun makeShopItem(id: String = "item1", owned: Boolean = false) = ShopItem(
        id = id, name = "Crown", type = "head", cost = 100, owned = owned
    )

    @Test
    fun `getItemCount returns 0 initially`() {
        val adapter = GoodsAdapter()
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `updateGoods sets correct item count`() {
        val adapter = GoodsAdapter()
        adapter.updateGoods(listOf(makeShopItem("i1"), makeShopItem("i2")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `updateGoods with empty list clears items`() {
        val adapter = GoodsAdapter()
        adapter.updateGoods(listOf(makeShopItem()))
        assertEquals(1, adapter.itemCount)
        adapter.updateGoods(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `click callback is set`() {
        var clicked: ShopItem? = null
        val adapter = GoodsAdapter { clicked = it }
        assertNull(clicked)
    }

    @Test
    fun `adapter handles owned items`() {
        val adapter = GoodsAdapter()
        adapter.updateGoods(listOf(makeShopItem("i1", owned = true), makeShopItem("i2", owned = false)))
        assertEquals(2, adapter.itemCount)
    }
}
