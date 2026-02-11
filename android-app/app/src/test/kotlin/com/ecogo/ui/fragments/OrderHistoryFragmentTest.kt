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
class OrderHistoryFragmentTest {

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<OrderHistoryFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `toolbar is present`() {
        val scenario = launchFragmentInContainer<OrderHistoryFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.toolbar))
        }
    }

    @Test
    fun `recycler view is present`() {
        val scenario = launchFragmentInContainer<OrderHistoryFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<RecyclerView>(R.id.recycler_history))
        }
    }

    @Test
    fun `empty state layout exists`() {
        val scenario = launchFragmentInContainer<OrderHistoryFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.layout_empty))
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<OrderHistoryFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
