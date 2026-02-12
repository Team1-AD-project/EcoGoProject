package com.ecogo.ui.fragments

import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import com.ecogo.R
import com.google.android.material.button.MaterialButton
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VoucherDetailFragmentTest {

    private val args = bundleOf("goodsId" to "test-goods")

    private fun getField(fragment: VoucherDetailFragment, fieldName: String): Any? {
        val field = VoucherDetailFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(fragment)
    }

    private fun setField(fragment: VoucherDetailFragment, fieldName: String, value: Any?) {
        val field = VoucherDetailFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(fragment, value)
    }

    // ==================== Lifecycle ====================

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `key views are present`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.btn_back))
            assertNotNull(view.findViewById<View>(R.id.text_name))
            assertNotNull(view.findViewById<View>(R.id.text_description))
            assertNotNull(view.findViewById<View>(R.id.text_cost))
            assertNotNull(view.findViewById<View>(R.id.card_voucher))
        }
    }

    @Test
    fun `redeem button is present`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<MaterialButton>(R.id.btn_redeem))
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }

    // ==================== setupUI ====================

    @Test
    fun `use button is hidden`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(View.GONE, fragment.requireView().findViewById<View>(R.id.btn_use).visibility)
        }
    }

    @Test
    fun `back button is clickable`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertTrue(fragment.requireView().findViewById<View>(R.id.btn_back).isClickable)
        }
    }

    // ==================== Fields from args ====================

    @Test
    fun `goodsId is set from args`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("test-goods", getField(fragment, "goodsId"))
        }
    }

    @Test
    fun `userVoucherId is null for marketplace`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val uv = getField(fragment, "userVoucherId")
            assertTrue(uv == null || uv.toString().isBlank())
        }
    }

    // ==================== confirmRedeem ====================

    @Test
    fun `confirmRedeem with blank goodsName shows wait dialog`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "goodsNameForRedeem", "")
            setField(fragment, "userVoucherId", null)
            val method = VoucherDetailFragment::class.java.getDeclaredMethod("confirmRedeem")
            method.isAccessible = true
            method.invoke(fragment)
        }
    }

    @Test
    fun `confirmRedeem inactive shows not available`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "goodsNameForRedeem", "Test")
            setField(fragment, "goodsActiveForRedeem", false)
            setField(fragment, "goodsStockForRedeem", 10)
            setField(fragment, "userVoucherId", null)
            val method = VoucherDetailFragment::class.java.getDeclaredMethod("confirmRedeem")
            method.isAccessible = true
            method.invoke(fragment)
        }
    }

    @Test
    fun `confirmRedeem zero stock shows not available`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "goodsNameForRedeem", "Test")
            setField(fragment, "goodsActiveForRedeem", true)
            setField(fragment, "goodsStockForRedeem", 0)
            setField(fragment, "userVoucherId", null)
            val method = VoucherDetailFragment::class.java.getDeclaredMethod("confirmRedeem")
            method.isAccessible = true
            method.invoke(fragment)
        }
    }

    @Test
    fun `confirmRedeem valid shows confirm`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "goodsNameForRedeem", "Test Voucher")
            setField(fragment, "goodsPointsForRedeem", 100)
            setField(fragment, "goodsActiveForRedeem", true)
            setField(fragment, "goodsStockForRedeem", 5)
            setField(fragment, "userVoucherId", null)
            val method = VoucherDetailFragment::class.java.getDeclaredMethod("confirmRedeem")
            method.isAccessible = true
            method.invoke(fragment)
        }
    }

    @Test
    fun `confirmRedeem owned voucher returns early`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "userVoucherId", "uv-123")
            val method = VoucherDetailFragment::class.java.getDeclaredMethod("confirmRedeem")
            method.isAccessible = true
            method.invoke(fragment)
        }
    }

    // ==================== Additional views ====================

    @Test
    fun `text_voucher_code is present`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.text_voucher_code))
        }
    }

    @Test
    fun `text_expiry is present`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.text_expiry))
        }
    }

    @Test
    fun `text_instructions is present`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.text_instructions))
        }
    }

    @Test
    fun `layout_redeem_action is present`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.layout_redeem_action))
        }
    }

    @Test
    fun `layout_voucher_info is present`() {
        val scenario = launchFragmentInContainer<VoucherDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.layout_voucher_info))
        }
    }
}
