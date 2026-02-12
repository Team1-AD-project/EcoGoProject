package com.ecogo.ui.fragments

import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.google.android.material.tabs.TabLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ActivitiesFragmentTest {

    private val args = bundleOf("showJoinedOnly" to false)

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<ActivitiesFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `key views are present`() {
        val scenario = launchFragmentInContainer<ActivitiesFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.btn_back))
            assertNotNull(view.findViewById<TabLayout>(R.id.tab_layout))
            assertNotNull(view.findViewById<RecyclerView>(R.id.recycler_activities))
        }
    }

    @Test
    fun `tab layout has two tabs`() {
        val scenario = launchFragmentInContainer<ActivitiesFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val tabLayout = fragment.requireView().findViewById<TabLayout>(R.id.tab_layout)
            assertEquals(2, tabLayout.tabCount)
        }
    }

    @Test
    fun `recyclerView has adapter`() {
        val scenario = launchFragmentInContainer<ActivitiesFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_activities)
            assertNotNull(recycler.adapter)
            assertNotNull(recycler.layoutManager)
        }
    }

    @Test
    fun `empty state is hidden initially`() {
        val scenario = launchFragmentInContainer<ActivitiesFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val empty = fragment.requireView().findViewById<View>(R.id.empty_state)
            assertNotNull(empty)
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<ActivitiesFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
