package com.ecogo.ui.adapters

import com.ecogo.api.GoodsDto
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ShopGoodsAdapterV2Test {

    private fun makeGoods(id: String = "g1") = GoodsDto(
        id = id, name = "Eco Bottle", description = "Reusable bottle",
        price = 15.0, stock = 50, category = "merchandise",
        brand = "EcoGo", imageUrl = null, redemptionPoints = 200
    )

    @Test
    fun `getItemCount returns 0 initially`() {
        val adapter = ShopGoodsAdapterV2(onItemClick = {})
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `getItemCount with initial data`() {
        val adapter = ShopGoodsAdapterV2(listOf(makeGoods("g1"), makeGoods("g2"))) {}
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `update changes item count`() {
        val adapter = ShopGoodsAdapterV2(onItemClick = {})
        adapter.update(listOf(makeGoods("g1"), makeGoods("g2")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `update with empty list clears items`() {
        val adapter = ShopGoodsAdapterV2(listOf(makeGoods())) {}
        adapter.update(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `click callback is set`() {
        var clicked: GoodsDto? = null
        val adapter = ShopGoodsAdapterV2(listOf(makeGoods())) { clicked = it }
        assertNull(clicked)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles VIP required goods`() {
        val goods = makeGoods().copy(vipLevelRequired = 2)
        val adapter = ShopGoodsAdapterV2(listOf(goods)) {}
        assertEquals(1, adapter.itemCount)
    }
}
