package com.ecogo.ui.fragments

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VoucherGoodsFragmentTest {

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<VoucherGoodsFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `recycler view is present`() {
        val scenario = launchFragmentInContainer<VoucherGoodsFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<RecyclerView>(R.id.recycler_goods))
        }
    }

    @Test
    fun `category chips are present`() {
        val scenario = launchFragmentInContainer<VoucherGoodsFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.chip_group_categories))
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<VoucherGoodsFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
