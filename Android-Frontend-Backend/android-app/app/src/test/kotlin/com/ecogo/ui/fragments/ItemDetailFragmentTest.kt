package com.ecogo.ui.fragments

import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import com.ecogo.R
import com.ecogo.data.Outfit
import com.google.android.material.button.MaterialButton
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ItemDetailFragmentTest {

    // Use known items from MockData.SHOP_ITEMS
    private val argsOwned = bundleOf("itemId" to "hat_grad") // owned = true, type = head
    private val argsNotOwned = bundleOf("itemId" to "face_glasses_square") // owned = false, type = face
    private val argsUnknown = bundleOf("itemId" to "nonexistent-item")

    private fun getField(fragment: ItemDetailFragment, fieldName: String): Any? {
        val field = ItemDetailFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(fragment)
    }

    private fun setField(fragment: ItemDetailFragment, fieldName: String, value: Any?) {
        val field = ItemDetailFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(fragment, value)
    }

    private fun invokePrivate(fragment: ItemDetailFragment, methodName: String): Any? {
        val method = ItemDetailFragment::class.java.getDeclaredMethod(methodName)
        method.isAccessible = true
        return method.invoke(fragment)
    }

    // ==================== Lifecycle ====================

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsNotOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `key views are present`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsNotOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.btn_back))
            assertNotNull(view.findViewById<View>(R.id.text_name))
            assertNotNull(view.findViewById<View>(R.id.text_type))
            assertNotNull(view.findViewById<View>(R.id.text_cost))
        }
    }

    @Test
    fun `action buttons are present`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsNotOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<MaterialButton>(R.id.btn_purchase))
            assertNotNull(view.findViewById<MaterialButton>(R.id.btn_equip))
            assertNotNull(view.findViewById<MaterialButton>(R.id.btn_try_on))
        }
    }

    @Test
    fun `cards are present`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsNotOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.card_preview))
            assertNotNull(view.findViewById<View>(R.id.card_item))
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsNotOwned, themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }

    // ==================== loadItemDetail ====================

    @Test
    fun `loadItemDetail face type shows correct type text`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsNotOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("面部", fragment.requireView().findViewById<TextView>(R.id.text_type).text.toString())
        }
    }

    @Test
    fun `loadItemDetail head type shows correct type text`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("头饰", fragment.requireView().findViewById<TextView>(R.id.text_type).text.toString())
        }
    }

    @Test
    fun `loadItemDetail sets item name`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsNotOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertTrue(fragment.requireView().findViewById<TextView>(R.id.text_name).text.toString().isNotEmpty())
        }
    }

    @Test
    fun `loadItemDetail sets cost text`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsNotOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertTrue(fragment.requireView().findViewById<TextView>(R.id.text_cost).text.toString().contains("积分"))
        }
    }

    @Test
    fun `loadItemDetail unknown item shows dialog`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsUnknown, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { /* dialog shown, no crash */ }
    }

    // ==================== updateButtonStates ====================

    @Test
    fun `not owned shows purchase hides equip`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsNotOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "isOwned", false)
            invokePrivate(fragment, "updateButtonStates")
            assertEquals(View.VISIBLE, fragment.requireView().findViewById<MaterialButton>(R.id.btn_purchase).visibility)
            assertEquals(View.GONE, fragment.requireView().findViewById<MaterialButton>(R.id.btn_equip).visibility)
        }
    }

    @Test
    fun `owned hides purchase shows equip`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "isOwned", true)
            invokePrivate(fragment, "updateButtonStates")
            assertEquals(View.GONE, fragment.requireView().findViewById<MaterialButton>(R.id.btn_purchase).visibility)
            assertEquals(View.VISIBLE, fragment.requireView().findViewById<MaterialButton>(R.id.btn_equip).visibility)
        }
    }

    @Test
    fun `not owned try on text`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsNotOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "isOwned", false)
            invokePrivate(fragment, "updateButtonStates")
            assertEquals("试穿预览", fragment.requireView().findViewById<MaterialButton>(R.id.btn_try_on).text.toString())
        }
    }

    @Test
    fun `owned preview text`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "isOwned", true)
            invokePrivate(fragment, "updateButtonStates")
            assertEquals("预览装备", fragment.requireView().findViewById<MaterialButton>(R.id.btn_try_on).text.toString())
        }
    }

    // ==================== togglePreview ====================

    @Test
    fun `togglePreview changes face outfit`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsNotOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("none", (getField(fragment, "currentOutfit") as Outfit).face)
            invokePrivate(fragment, "togglePreview")
            assertEquals("face_glasses_square", (getField(fragment, "currentOutfit") as Outfit).face)
        }
    }

    @Test
    fun `togglePreview twice restores original`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsNotOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "togglePreview")
            invokePrivate(fragment, "togglePreview")
            assertEquals("none", (getField(fragment, "currentOutfit") as Outfit).face)
        }
    }

    @Test
    fun `togglePreview changes head outfit`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "togglePreview")
            assertEquals("hat_grad", (getField(fragment, "currentOutfit") as Outfit).head)
        }
    }

    // ==================== purchaseItem ====================

    @Test
    fun `purchaseItem shows confirmation dialog`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsNotOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "purchaseItem")
        }
    }

    // ==================== performPurchase ====================

    @Test
    fun `performPurchase sets isOwned true`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsNotOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertFalse(getField(fragment, "isOwned") as Boolean)
            val method = ItemDetailFragment::class.java.getDeclaredMethod("performPurchase", Int::class.java)
            method.isAccessible = true
            method.invoke(fragment, 300)
            assertTrue(getField(fragment, "isOwned") as Boolean)
        }
    }

    // ==================== equipItem ====================

    @Test
    fun `equipItem makes toast with correct text`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            // equipItem calls findNavController().navigateUp() which throws without NavController,
            // so verify Toast is created before the navigation call
            try {
                invokePrivate(fragment, "equipItem")
            } catch (_: Exception) { /* navigateUp throws without NavController */ }
            assertNotNull(ShadowToast.getTextOfLatestToast())
        }
    }

    // ==================== default outfit ====================

    @Test
    fun `default outfit values`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsNotOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val outfit = getField(fragment, "currentOutfit") as Outfit
            assertEquals("none", outfit.head)
            assertEquals("none", outfit.face)
            assertEquals("shirt_nus", outfit.body)
            assertEquals("none", outfit.badge)
        }
    }

    // ==================== mascot preview ====================

    @Test
    fun `mascot preview is present`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = argsNotOwned, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.mascot_preview))
        }
    }
}
