package com.ecogo.ui.fragments

import android.view.View
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
class ChallengesFragmentTest {

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<ChallengesFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `key views are present`() {
        val scenario = launchFragmentInContainer<ChallengesFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.btn_back))
            assertNotNull(view.findViewById<TabLayout>(R.id.tab_layout))
            assertNotNull(view.findViewById<RecyclerView>(R.id.recycler_challenges))
        }
    }

    @Test
    fun `tab layout has three tabs`() {
        val scenario = launchFragmentInContainer<ChallengesFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val tabLayout = fragment.requireView().findViewById<TabLayout>(R.id.tab_layout)
            assertEquals(3, tabLayout.tabCount)
        }
    }

    @Test
    fun `recyclerView has adapter`() {
        val scenario = launchFragmentInContainer<ChallengesFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_challenges)
            assertNotNull(recycler.adapter)
            assertNotNull(recycler.layoutManager)
        }
    }

    @Test
    fun `empty state view is present`() {
        val scenario = launchFragmentInContainer<ChallengesFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val empty = fragment.requireView().findViewById<View>(R.id.empty_state)
            assertNotNull(empty)
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<ChallengesFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
