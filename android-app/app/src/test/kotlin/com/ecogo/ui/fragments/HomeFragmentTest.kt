package com.ecogo.ui.fragments

import android.view.View
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.ecogo.mapengine.ui.map.MapActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HomeFragmentTest {

    /**
     * Helper: launch HomeFragment and attach a mock NavController
     */
    private fun launchWithNav(): Pair<androidx.fragment.app.testing.FragmentScenario<HomeFragment>, NavController> {
        val mockNav = mock<NavController>()
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), mockNav)
        }
        return scenario to mockNav
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
        val (scenario, mockNav) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_next_bus).performClick()
        }
        verify(mockNav).navigate(R.id.routesFragment)
    }

    @Test
    fun `cardMonthlyPoints click navigates to profileFragment`() {
        val (scenario, mockNav) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_monthly_points).performClick()
        }
        verify(mockNav).navigate(R.id.profileFragment)
    }

    @Test
    fun `cardCommunityScore click navigates to communityFragment`() {
        val (scenario, mockNav) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_community_score).performClick()
        }
        verify(mockNav).navigate(R.id.communityFragment)
    }

    @Test
    fun `cardCarbonFootprint click navigates to profileFragment`() {
        val (scenario, mockNav) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_carbon_footprint).performClick()
        }
        verify(mockNav).navigate(R.id.profileFragment)
    }

    @Test
    fun `cardDailyGoal click navigates to profileFragment`() {
        val (scenario, mockNav) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_daily_goal).performClick()
        }
        verify(mockNav).navigate(R.id.profileFragment)
    }

    @Test
    fun `cardVoucherShortcut click navigates to voucher`() {
        val (scenario, mockNav) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_voucher_shortcut).performClick()
        }
        verify(mockNav).navigate(R.id.action_home_to_voucher)
    }

    @Test
    fun `cardChallengesShortcut click navigates to challenges`() {
        val (scenario, mockNav) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_challenges_shortcut).performClick()
        }
        verify(mockNav).navigate(R.id.action_home_to_challenges)
    }

    @Test
    fun `textViewAll click navigates to monthlyHighlights`() {
        val (scenario, mockNav) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.text_view_all).performClick()
        }
        verify(mockNav).navigate(R.id.action_home_to_monthlyHighlights)
    }

    @Test
    fun `textViewAllActivities click navigates to activities`() {
        val (scenario, mockNav) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.text_view_all_activities).performClick()
        }
        verify(mockNav).navigate(R.id.action_home_to_activities)
    }

    @Test
    fun `mascotAvatar click navigates to profileFragment`() {
        val (scenario, mockNav) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.mascot_avatar).performClick()
        }
        verify(mockNav).navigate(R.id.profileFragment)
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
