package com.ecogo.ui.fragments

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.ecogo.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CommunityFragmentTest {

    private fun launchWithNav(): Pair<androidx.fragment.app.testing.FragmentScenario<CommunityFragment>, TestNavHostController> {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.setGraph(R.navigation.nav_graph)
        navController.setCurrentDestination(R.id.communityFragment)
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { Navigation.setViewNavController(it.requireView(), navController) }
        return scenario to navController
    }

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `key views are present`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.text_title))
            assertNotNull(view.findViewById<TabLayout>(R.id.tab_layout))
            assertNotNull(view.findViewById<RecyclerView>(R.id.recycler_community))
        }
    }

    @Test
    fun `tab layout has two tabs`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val tabLayout = fragment.requireView().findViewById<TabLayout>(R.id.tab_layout)
            assertEquals(2, tabLayout.tabCount)
        }
    }

    @Test
    fun `challenges button navigates to challenges`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.btn_challenges).performClick()
        }
        assertEquals(R.id.challengesFragment, navController.currentDestination?.id)
    }

    @Test
    fun `activities button navigates to activities`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.btn_activities).performClick()
        }
        assertEquals(R.id.activitiesFragment, navController.currentDestination?.id)
    }

    @Test
    fun `individual leaderboard views exist`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<RecyclerView>(R.id.recycler_individual))
            assertNotNull(view.findViewById<View>(R.id.btn_daily))
            assertNotNull(view.findViewById<View>(R.id.btn_monthly))
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
