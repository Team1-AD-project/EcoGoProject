package com.ecogo.ui.fragments

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.ecogo.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MonthlyHighlightsFragmentTest {

    private fun launchWithNav(): Pair<androidx.fragment.app.testing.FragmentScenario<MonthlyHighlightsFragment>, TestNavHostController> {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.setGraph(R.navigation.nav_graph)
        navController.setCurrentDestination(R.id.monthlyHighlightsFragment)
        val scenario = launchFragmentInContainer<MonthlyHighlightsFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { Navigation.setViewNavController(it.requireView(), navController) }
        return scenario to navController
    }

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<MonthlyHighlightsFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `key views are present`() {
        val scenario = launchFragmentInContainer<MonthlyHighlightsFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.btn_back))
            assertNotNull(view.findViewById<View>(R.id.text_current_month))
            assertNotNull(view.findViewById<View>(R.id.card_header))
        }
    }

    @Test
    fun `recycler views are present`() {
        val scenario = launchFragmentInContainer<MonthlyHighlightsFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<RecyclerView>(R.id.recycler_month_stats))
            assertNotNull(view.findViewById<RecyclerView>(R.id.recycler_featured_activities))
            assertNotNull(view.findViewById<RecyclerView>(R.id.recycler_challenges))
        }
    }

    @Test
    fun `view all challenges navigates to challenges`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.btn_view_all_challenges).performClick()
        }
        assertEquals(R.id.challengesFragment, navController.currentDestination?.id)
    }

    @Test
    fun `leaderboard views are present`() {
        val scenario = launchFragmentInContainer<MonthlyHighlightsFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.text_rank1_name))
            assertNotNull(view.findViewById<View>(R.id.text_rank2_name))
            assertNotNull(view.findViewById<View>(R.id.text_rank3_name))
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<MonthlyHighlightsFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
