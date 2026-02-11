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
class ItemDetailFragmentTest {

    private val args = bundleOf("itemId" to "test-item")

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `key views are present`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
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
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<MaterialButton>(R.id.btn_purchase))
            assertNotNull(view.findViewById<MaterialButton>(R.id.btn_equip))
            assertNotNull(view.findViewById<MaterialButton>(R.id.btn_try_on))
        }
    }

    @Test
    fun `cards are present`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.card_preview))
            assertNotNull(view.findViewById<View>(R.id.card_item))
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<ItemDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
