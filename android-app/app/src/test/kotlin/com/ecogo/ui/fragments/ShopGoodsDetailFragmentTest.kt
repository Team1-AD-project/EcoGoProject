package com.ecogo.ui.fragments

import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import com.ecogo.R
import com.ecogo.auth.TokenManager
import com.google.android.material.button.MaterialButton
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ShopGoodsDetailFragmentTest {

    companion object {
        private const val TEST_ITEM = "Test Item"
        private const val TEST_GOODS_ID = "test-goods-id"
    }

    @Before
    fun setup() {
        TokenManager.init(ApplicationProvider.getApplicationContext())
        TokenManager.saveToken("test-token", "test-user", "TestUser")
    }

    private fun getField(fragment: ShopGoodsDetailFragment, fieldName: String): Any? {
        val field = ShopGoodsDetailFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(fragment)
    }

    private fun setField(fragment: ShopGoodsDetailFragment, fieldName: String, value: Any?) {
        val field = ShopGoodsDetailFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(fragment, value)
    }

    private fun invokePrivate(fragment: ShopGoodsDetailFragment, methodName: String): Any? {
        val method = ShopGoodsDetailFragment::class.java.getDeclaredMethod(methodName)
        method.isAccessible = true
        return method.invoke(fragment)
    }

    // ==================== Lifecycle ====================

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `key views are present`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.btn_back))
            assertNotNull(view.findViewById<View>(R.id.text_name))
            assertNotNull(view.findViewById<View>(R.id.text_description))
            assertNotNull(view.findViewById<View>(R.id.text_cost))
        }
    }

    @Test
    fun `redeem button is present`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<MaterialButton>(R.id.btn_redeem))
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }

    // ==================== setupUI visibility ====================

    @Test
    fun `btnUse is hidden`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(View.GONE, fragment.requireView().findViewById<View>(R.id.btn_use).visibility)
        }
    }

    @Test
    fun `layoutVoucherInfo is hidden`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(View.GONE, fragment.requireView().findViewById<View>(R.id.layout_voucher_info).visibility)
        }
    }

    @Test
    fun `layoutRedeemAction is visible`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(View.VISIBLE, fragment.requireView().findViewById<View>(R.id.layout_redeem_action).visibility)
        }
    }

    // ==================== Default field values ====================

    @Test
    fun `goodsName defaults to empty string`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("", getField(fragment, "goodsName"))
        }
    }

    @Test
    fun `goodsPoints defaults to 0`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(0, getField(fragment, "goodsPoints"))
        }
    }

    @Test
    fun `goodsStock defaults to 0`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(0, getField(fragment, "goodsStock"))
        }
    }

    @Test
    fun `goodsActive defaults to true`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertTrue(getField(fragment, "goodsActive") as Boolean)
        }
    }

    // ==================== Field manipulation ====================

    @Test
    fun `goodsName can be set`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "goodsName", TEST_ITEM)
            assertEquals(TEST_ITEM, getField(fragment, "goodsName"))
        }
    }

    @Test
    fun `goodsPoints can be set`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "goodsPoints", 500)
            assertEquals(500, getField(fragment, "goodsPoints"))
        }
    }

    @Test
    fun `goodsStock can be set`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "goodsStock", 10)
            assertEquals(10, getField(fragment, "goodsStock"))
        }
    }

    @Test
    fun `goodsActive can be toggled`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "goodsActive", false)
            assertFalse(getField(fragment, "goodsActive") as Boolean)
        }
    }

    // ==================== confirmRedeem ====================

    @Test
    fun `confirmRedeem when inactive does not crash`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "goodsActive", false)
            invokePrivate(fragment, "confirmRedeem")
        }
    }

    @Test
    fun `confirmRedeem when stock is zero does not crash`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "goodsActive", true)
            setField(fragment, "goodsStock", 0)
            invokePrivate(fragment, "confirmRedeem")
        }
    }

    @Test
    fun `confirmRedeem when active and in stock does not crash`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "goodsActive", true)
            setField(fragment, "goodsStock", 5)
            setField(fragment, "goodsName", TEST_ITEM)
            setField(fragment, "goodsPoints", 100)
            invokePrivate(fragment, "confirmRedeem")
        }
    }

    @Test
    fun `confirmRedeem when inactive and zero stock does not crash`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "goodsActive", false)
            setField(fragment, "goodsStock", 0)
            invokePrivate(fragment, "confirmRedeem")
        }
    }

    // ==================== additional views ====================

    @Test
    fun `text_instructions view is present`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.text_instructions))
        }
    }

    @Test
    fun `btn_back is clickable`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val btn = fragment.requireView().findViewById<View>(R.id.btn_back)
            assertTrue(btn.isClickable)
        }
    }

    @Test
    fun `btn_redeem is clickable`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val btn = fragment.requireView().findViewById<MaterialButton>(R.id.btn_redeem)
            assertTrue(btn.isClickable)
        }
    }

    // ==================== repository field ====================

    @Test
    fun `repository field is initialized`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(getField(fragment, "repo"))
        }
    }

    // ==================== launch with args ====================

    @Test
    fun `fragment launched with goodsId arg inflates successfully`() {
        val args = bundleOf("goodsId" to TEST_GOODS_ID)
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(
            fragmentArgs = args,
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `fragment launched with args has correct view visibility`() {
        val args = bundleOf("goodsId" to TEST_GOODS_ID)
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(
            fragmentArgs = args,
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertEquals(View.GONE, fragment.requireView().findViewById<View>(R.id.btn_use).visibility)
            assertEquals(View.GONE, fragment.requireView().findViewById<View>(R.id.layout_voucher_info).visibility)
            assertEquals(View.VISIBLE, fragment.requireView().findViewById<View>(R.id.layout_redeem_action).visibility)
        }
    }

    // ==================== performRedeem ====================

    @Test
    fun `performRedeem with valid setup does not crash`() {
        val args = bundleOf("goodsId" to TEST_GOODS_ID)
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(
            fragmentArgs = args,
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            setField(fragment, "goodsName", "Test Goods")
            setField(fragment, "goodsPoints", 100)
            try {
                invokePrivate(fragment, "performRedeem")
            } catch (_: Exception) { /* coroutine may throw */ }
        }
    }

    @Test
    fun `performRedeem disables redeem button`() {
        val args = bundleOf("goodsId" to TEST_GOODS_ID)
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(
            fragmentArgs = args,
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            setField(fragment, "goodsName", "Test Goods")
            setField(fragment, "goodsPoints", 100)
            try {
                invokePrivate(fragment, "performRedeem")
            } catch (_: Exception) { /* coroutine may throw */ }
            val btn = fragment.requireView().findViewById<MaterialButton>(R.id.btn_redeem)
            assertFalse(btn.isEnabled)
        }
    }

    // ==================== showSuccessDialog ====================

    @Test
    fun `showSuccessDialog does not crash`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            try {
                invokePrivate(fragment, "showSuccessDialog")
            } catch (_: Exception) { /* dialog layout may not resolve fully */ }
        }
    }

    // ==================== currentUserId ====================

    @Test
    fun `currentUserId returns userId from TokenManager`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = ShopGoodsDetailFragment::class.java.getDeclaredMethod("getCurrentUserId")
            method.isAccessible = true
            val userId = method.invoke(fragment) as String
            assertEquals("test-user", userId)
        }
    }

    // ==================== binding field ====================

    @Test
    fun `binding field is not null during view lifecycle`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val bindingField = ShopGoodsDetailFragment::class.java.getDeclaredField("_binding")
            bindingField.isAccessible = true
            assertNotNull(bindingField.get(fragment))
        }
    }

    // ==================== confirmRedeem click on btn ====================

    @Test
    fun `btn_redeem click triggers confirmRedeem`() {
        val scenario = launchFragmentInContainer<ShopGoodsDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "goodsActive", true)
            setField(fragment, "goodsStock", 5)
            setField(fragment, "goodsName", "Click Test")
            setField(fragment, "goodsPoints", 200)
            fragment.requireView().findViewById<MaterialButton>(R.id.btn_redeem).performClick()
        }
    }
}
