package com.ecogo.ui.fragments

import android.view.View
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.ecogo.R
import com.ecogo.mapengine.ui.map.MapActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HomeFragmentTest {

    /**
     * Helper: launch HomeFragment and attach a TestNavHostController (no Mockito needed)
     */
    private fun launchWithNav(): Pair<androidx.fragment.app.testing.FragmentScenario<HomeFragment>, TestNavHostController> {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.setGraph(R.navigation.nav_graph)
        navController.setCurrentDestination(R.id.homeFragment)

        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), navController)
        }
        return scenario to navController
    }

    // ==================== Lifecycle ====================

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.view)
        }
    }

    @Test
    fun `fragment view is not null after onViewCreated`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.text_welcome))
            assertNotNull(fragment.requireView().findViewById<View>(R.id.text_bus_number))
        }
    }

    // ==================== Initial UI Setup ====================

    @Test
    fun `setupUI sets bus info text`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            // Bus info is set by setupUI() then may be overwritten by loadBusInfo() coroutine
            val busNumber = view.findViewById<TextView>(R.id.text_bus_number).text.toString()
            val busTime = view.findViewById<TextView>(R.id.text_bus_time).text.toString()
            val busRoute = view.findViewById<TextView>(R.id.text_bus_route).text.toString()
            assertTrue("Bus number should not be empty", busNumber.isNotEmpty())
            assertTrue("Bus time should not be empty", busTime.isNotEmpty())
            assertTrue("Bus route should not be empty", busRoute.isNotEmpty())
        }
    }

    @Test
    fun `setupUI sets monthly points and SoC score views`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            // Values may be overwritten by loadMonthlyPoints() / loadSocScore() coroutines
            val monthlyPoints = view.findViewById<TextView>(R.id.text_monthly_points).text.toString()
            val socScore = view.findViewById<TextView>(R.id.text_soc_score).text.toString()
            assertTrue("Monthly points should not be empty", monthlyPoints.isNotEmpty())
            assertTrue("SoC score should not be empty", socScore.isNotEmpty())
        }
    }

    @Test
    fun `setupUI sets location text with today date`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            val locationText = view.findViewById<TextView>(R.id.text_location).text.toString()
            // Should contain current year
            assertTrue(locationText.contains(java.time.LocalDate.now().year.toString()))
        }
    }

    // ==================== RecyclerView Setup ====================

    @Test
    fun `recyclerHighlights is initialized with adapter`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_highlights)
            assertNotNull(recycler.adapter)
            assertNotNull(recycler.layoutManager)
        }
    }

    @Test
    fun `recyclerActivities is initialized with adapter`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_activities)
            assertNotNull(recycler.adapter)
            assertNotNull(recycler.layoutManager)
        }
    }

    // ==================== Notification ====================

    @Test
    fun `close notification button hides notification card`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            val closeButton = view.findViewById<View>(R.id.button_close_notification)
            val card = view.findViewById<View>(R.id.card_notification)

            closeButton.performClick()
            assertEquals(View.GONE, card.visibility)
        }
    }

    // ==================== Navigation Clicks ====================

    @Test
    fun `cardNextBus click navigates to routesFragment`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_next_bus).performClick()
        }
        assertEquals(R.id.routesFragment, navController.currentDestination?.id)
    }

    @Test
    fun `cardMonthlyPoints click navigates to profileFragment`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_monthly_points).performClick()
        }
        assertEquals(R.id.profileFragment, navController.currentDestination?.id)
    }

    @Test
    fun `cardCommunityScore click navigates to communityFragment`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_community_score).performClick()
        }
        assertEquals(R.id.communityFragment, navController.currentDestination?.id)
    }

    @Test
    fun `cardCarbonFootprint click navigates to profileFragment`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_carbon_footprint).performClick()
        }
        assertEquals(R.id.profileFragment, navController.currentDestination?.id)
    }

    @Test
    fun `cardDailyGoal click navigates to profileFragment`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_daily_goal).performClick()
        }
        assertEquals(R.id.profileFragment, navController.currentDestination?.id)
    }

    @Test
    fun `cardVoucherShortcut click navigates to voucher`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_voucher_shortcut).performClick()
        }
        assertEquals(R.id.voucherFragment, navController.currentDestination?.id)
    }

    @Test
    fun `cardChallengesShortcut click navigates to challenges`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_challenges_shortcut).performClick()
        }
        assertEquals(R.id.challengesFragment, navController.currentDestination?.id)
    }

    @Test
    fun `textViewAll click navigates to monthlyHighlights`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.text_view_all).performClick()
        }
        assertEquals(R.id.monthlyHighlightsFragment, navController.currentDestination?.id)
    }

    @Test
    fun `textViewAllActivities click navigates to activities`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.text_view_all_activities).performClick()
        }
        assertEquals(R.id.activitiesFragment, navController.currentDestination?.id)
    }

    @Test
    fun `mascotAvatar click navigates to profileFragment`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.mascot_avatar).performClick()
        }
        assertEquals(R.id.profileFragment, navController.currentDestination?.id)
    }

    // ==================== MapActivity Intent ====================

    @Test
    fun `buttonOpenMap click starts MapActivity`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.button_open_map).performClick()

            val shadowActivity = Shadows.shadowOf(fragment.requireActivity())
            val intent = shadowActivity.nextStartedActivity
            assertNotNull(intent)
            assertEquals(MapActivity::class.java.name, intent.component?.className)
        }
    }

    @Test
    fun `cardMap click starts MapActivity`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_map).performClick()

            val shadowActivity = Shadows.shadowOf(fragment.requireActivity())
            val intent = shadowActivity.nextStartedActivity
            assertNotNull(intent)
            assertEquals(MapActivity::class.java.name, intent.component?.className)
        }
    }

    // ==================== Fragment Destroy ====================

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        // Moving to DESTROYED should not crash
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
