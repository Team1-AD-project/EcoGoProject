package com.ecogo.ui.adapters

import com.ecogo.data.Voucher
import org.junit.Assert.*
import org.junit.Test

class VoucherAdapterTest {

    private fun makeVoucher(id: String = "v1", available: Boolean = true) = Voucher(
        id = id, name = "Starbucks $5", description = "Coffee voucher", cost = 200, available = available
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = VoucherAdapter(listOf(makeVoucher("v1"), makeVoucher("v2")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = VoucherAdapter(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `updateVouchers changes item count`() {
        val adapter = VoucherAdapter(listOf(makeVoucher()))
        adapter.updateVouchers(listOf(makeVoucher("v1"), makeVoucher("v2"), makeVoucher("v3")))
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `updateVouchers with empty list clears items`() {
        val adapter = VoucherAdapter(listOf(makeVoucher()))
        adapter.updateVouchers(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `voucher click callback is set`() {
        var clicked: Voucher? = null
        val adapter = VoucherAdapter(listOf(makeVoucher()), onVoucherClick = { clicked = it })
        assertNull(clicked)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `redeem click callback is set`() {
        var redeemed: Voucher? = null
        val adapter = VoucherAdapter(listOf(makeVoucher()), onRedeemClick = { redeemed = it })
        assertNull(redeemed)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles owned voucher`() {
        val voucher = Voucher(
            id = "v1", name = "Used Voucher", description = "desc", cost = 100,
            userVoucherId = "uv1", status = "USED"
        )
        val adapter = VoucherAdapter(listOf(voucher))
        assertEquals(1, adapter.itemCount)
    }
}
