package com.ecogo.ui.fragments

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.google.android.material.chip.Chip
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ShopFragmentTest {

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
}
