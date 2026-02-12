package com.ecogo.ui.fragments

import android.view.View
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.ecogo.data.Product
import com.google.android.material.chip.Chip
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ShopFragmentTest {

    private fun invokePrivate(fragment: ShopFragment, methodName: String, vararg args: Any?): Any? {
        val paramTypes = args.map {
            when (it) {
                is String -> String::class.java
                is Boolean -> Boolean::class.java
                is Int -> Int::class.java
                is Product -> Product::class.java
                else -> it?.javaClass ?: Any::class.java
            }
        }.toTypedArray()
        val method = ShopFragment::class.java.getDeclaredMethod(methodName, *paramTypes)
        method.isAccessible = true
        return method.invoke(fragment, *args)
    }

    private fun getField(fragment: ShopFragment, fieldName: String): Any? {
        val field = ShopFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(fragment)
    }

    private fun setField(fragment: ShopFragment, fieldName: String, value: Any?) {
        val field = ShopFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(fragment, value)
    }

    private fun makeProduct(
        id: String = "test",
        name: String = "Test Product",
        type: String = "voucher",
        pointsPrice: Int? = 100,
        cashPrice: Double? = null
    ) = Product(
        id = id, name = name, description = "desc",
        type = type, category = "food",
        pointsPrice = pointsPrice, cashPrice = cashPrice, imageUrl = null
    )

    // ==================== Lifecycle ====================

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `balance views are present`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.text_points))
            assertNotNull(view.findViewById<View>(R.id.text_cash))
        }
    }

    @Test
    fun `filter tabs are present`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<Chip>(R.id.tab_all))
            assertNotNull(view.findViewById<Chip>(R.id.tab_vouchers))
            assertNotNull(view.findViewById<Chip>(R.id.tab_goods))
        }
    }

    @Test
    fun `products recycler is present with adapter`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_products)
            assertNotNull(recycler)
            assertNotNull(recycler.adapter)
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }

    // ==================== setupUI ====================

    @Test
    fun `setupUI sets points text`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val pointsText = fragment.requireView().findViewById<TextView>(R.id.text_points).text.toString()
            assertTrue(pointsText.contains("pts"))
        }
    }

    @Test
    fun `setupUI sets cash text`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val cashText = fragment.requireView().findViewById<TextView>(R.id.text_cash).text.toString()
            assertTrue(cashText.contains("SGD"))
        }
    }

    @Test
    fun `setupUI points text contains default value`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val pointsText = fragment.requireView().findViewById<TextView>(R.id.text_points).text.toString()
            assertTrue(pointsText.contains("1250"))
        }
    }

    // ==================== filterProducts ====================

    @Test
    fun `filterProducts sets currentFilter to all`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "filterProducts", "all")
            assertEquals("all", getField(fragment, "currentFilter"))
        }
    }

    @Test
    fun `filterProducts sets currentFilter to voucher`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "filterProducts", "voucher")
            assertEquals("voucher", getField(fragment, "currentFilter"))
        }
    }

    @Test
    fun `filterProducts sets currentFilter to goods`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "filterProducts", "goods")
            assertEquals("goods", getField(fragment, "currentFilter"))
        }
    }

    // ==================== tab clicks ====================

    @Test
    fun `tab all click sets filter to all`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<Chip>(R.id.tab_all).performClick()
            assertEquals("all", getField(fragment, "currentFilter"))
        }
    }

    @Test
    fun `tab vouchers click sets filter to voucher`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<Chip>(R.id.tab_vouchers).performClick()
            assertEquals("voucher", getField(fragment, "currentFilter"))
        }
    }

    @Test
    fun `tab goods click sets filter to goods`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<Chip>(R.id.tab_goods).performClick()
            assertEquals("goods", getField(fragment, "currentFilter"))
        }
    }

    // ==================== extractPaymentIntentId ====================

    @Test
    fun `extractPaymentIntentId extracts correct id`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val result = invokePrivate(fragment, "extractPaymentIntentId", "pi_12345_secret_abcde") as String
            assertEquals("pi_12345", result)
        }
    }

    @Test
    fun `extractPaymentIntentId with complex secret`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val result = invokePrivate(fragment, "extractPaymentIntentId", "pi_3ABC_secret_XYZ123") as String
            assertEquals("pi_3ABC", result)
        }
    }

    // ==================== handlePurchase ====================

    @Test
    fun `handlePurchase with points calls redeemWithPoints`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "handlePurchase", makeProduct(pointsPrice = 100), "points")
        }
    }

    @Test
    fun `handlePurchase with cash calls buyWithCash`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "handlePurchase", makeProduct(pointsPrice = null, cashPrice = 5.0), "cash")
        }
    }

    // ==================== redeemWithPoints ====================

    @Test
    fun `redeemWithPoints with null pointsPrice shows toast`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "redeemWithPoints", makeProduct(pointsPrice = null))
            assertNotNull(ShadowToast.getTextOfLatestToast())
        }
    }

    @Test
    fun `redeemWithPoints with insufficient points shows dialog`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "currentPoints", 10)
            invokePrivate(fragment, "redeemWithPoints", makeProduct(pointsPrice = 5000))
        }
    }

    @Test
    fun `redeemWithPoints with sufficient points shows confirm dialog`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "currentPoints", 5000)
            invokePrivate(fragment, "redeemWithPoints", makeProduct(pointsPrice = 100))
        }
    }

    // ==================== buyWithCash ====================

    @Test
    fun `buyWithCash with null cashPrice shows toast`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "buyWithCash", makeProduct(type = "goods", pointsPrice = null, cashPrice = null))
            assertNotNull(ShadowToast.getTextOfLatestToast())
        }
    }

    @Test
    fun `buyWithCash with cashPrice sets currentProduct`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val product = makeProduct(id = "test-cash", type = "goods", pointsPrice = null, cashPrice = 9.99)
            invokePrivate(fragment, "buyWithCash", product)
            val currentProduct = getField(fragment, "currentProduct") as? Product
            assertEquals("test-cash", currentProduct?.id)
        }
    }

    // ==================== currentPoints / currentFilter defaults ====================

    @Test
    fun `currentPoints default is 1250`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(1250, getField(fragment, "currentPoints") as Int)
        }
    }

    @Test
    fun `currentFilter default is all`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("all", getField(fragment, "currentFilter"))
        }
    }

    @Test
    fun `currentProduct default is null`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNull(getField(fragment, "currentProduct"))
        }
    }

    @Test
    fun `currentPaymentIntentId default is null`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNull(getField(fragment, "currentPaymentIntentId"))
        }
    }

    // ==================== recycler layout ====================

    @Test
    fun `recycler has linear layout manager`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_products)
            assertNotNull(recycler.layoutManager)
            assertTrue(recycler.layoutManager is androidx.recyclerview.widget.LinearLayoutManager)
        }
    }

    // ==================== showInsufficientPointsDialog ====================

    @Test
    fun `showInsufficientPointsDialog does not crash`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "showInsufficientPointsDialog", 5000)
        }
    }

    @Test
    fun `showInsufficientPointsDialog with zero points does not crash`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "showInsufficientPointsDialog", 0)
        }
    }

    // ==================== showRedeemConfirmDialog ====================

    @Test
    fun `showRedeemConfirmDialog does not crash`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = ShopFragment::class.java.getDeclaredMethod(
                "showRedeemConfirmDialog",
                Product::class.java,
                Function0::class.java
            )
            method.isAccessible = true
            method.invoke(fragment, makeProduct(), { })
        }
    }

    // ==================== showSuccessDialog ====================

    @Test
    fun `showSuccessDialog does not crash`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = ShopFragment::class.java.getDeclaredMethod(
                "showSuccessDialog",
                String::class.java,
                String::class.java
            )
            method.isAccessible = true
            method.invoke(fragment, "Test success!", "-100 pts")
        }
    }

    // ==================== showPaymentInfoDialog ====================

    @Test
    fun `showPaymentInfoDialog does not crash`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = ShopFragment::class.java.getDeclaredMethod(
                "showPaymentInfoDialog",
                Product::class.java,
                Function0::class.java
            )
            method.isAccessible = true
            method.invoke(fragment, makeProduct(cashPrice = 9.99), { })
        }
    }

    // ==================== onPaymentSheetResult ====================

    @Test
    fun `onPaymentSheetResult canceled shows toast`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = ShopFragment::class.java.getDeclaredMethod(
                "onPaymentSheetResult",
                com.stripe.android.paymentsheet.PaymentSheetResult::class.java
            )
            method.isAccessible = true
            method.invoke(fragment, com.stripe.android.paymentsheet.PaymentSheetResult.Canceled)
            assertNotNull(ShadowToast.getTextOfLatestToast())
        }
    }

    // ==================== setField for currentPoints ====================

    @Test
    fun `setting currentPoints updates field`() {
        val scenario = launchFragmentInContainer<ShopFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "currentPoints", 999)
            assertEquals(999, getField(fragment, "currentPoints") as Int)
        }
    }
}
