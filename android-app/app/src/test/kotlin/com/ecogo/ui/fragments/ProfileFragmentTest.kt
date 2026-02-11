package com.ecogo.ui.fragments

import android.view.View
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.ecogo.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProfileFragmentTest {

    /**
     * Helper: launch ProfileFragment with TestNavHostController
     */
    private fun launchWithNav(): Pair<androidx.fragment.app.testing.FragmentScenario<ProfileFragment>, TestNavHostController> {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.setGraph(R.navigation.nav_graph)
        navController.setCurrentDestination(R.id.profileFragment)

        val scenario = launchFragmentInContainer<ProfileFragment>(
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
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.view)
        }
    }

    @Test
    fun `fragment view is not null after onViewCreated`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.text_name))
            assertNotNull(view.findViewById<View>(R.id.text_faculty))
            assertNotNull(view.findViewById<View>(R.id.text_points))
        }
    }

    // ==================== Initial UI Views ====================

    @Test
    fun `mascot card is present`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.card_mascot))
            assertNotNull(view.findViewById<View>(R.id.mascot_lion))
        }
    }

    @Test
    fun `points card is present`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.card_points))
            assertNotNull(view.findViewById<View>(R.id.text_points))
            assertNotNull(view.findViewById<View>(R.id.button_redeem))
        }
    }

    @Test
    fun `settings button is present`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.button_settings))
        }
    }

    @Test
    fun `trip history button is present`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.button_trip_history))
        }
    }

    // ==================== Tabs Setup ====================

    @Test
    fun `closet card is visible after setup`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val card = fragment.requireView().findViewById<View>(R.id.card_closet)
            assertEquals(View.VISIBLE, card.visibility)
        }
    }

    @Test
    fun `badges card is visible after setup`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val card = fragment.requireView().findViewById<View>(R.id.card_badges)
            assertEquals(View.VISIBLE, card.visibility)
        }
    }

    @Test
    fun `closet card has preview mascot and description`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.mascot_closet_preview))
            assertNotNull(view.findViewById<View>(R.id.text_closet_desc))
        }
    }

    @Test
    fun `badge card has preview text and count`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.text_badge_preview))
            assertNotNull(view.findViewById<View>(R.id.text_badge_count))
        }
    }

    // ==================== User Info Display ====================

    @Test
    fun `name text view has default or loaded text`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val nameText = fragment.requireView().findViewById<TextView>(R.id.text_name)
            assertNotNull(nameText.text)
        }
    }

    @Test
    fun `faculty text view has default or loaded text`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val facultyText = fragment.requireView().findViewById<TextView>(R.id.text_faculty)
            assertNotNull(facultyText.text)
        }
    }

    @Test
    fun `points text view has default or loaded text`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val pointsText = fragment.requireView().findViewById<TextView>(R.id.text_points)
            assertNotNull(pointsText.text)
        }
    }

    // ==================== Navigation Clicks ====================

    @Test
    fun `button_settings click navigates to settingsFragment`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.button_settings).performClick()
        }
        assertEquals(R.id.settingsFragment, navController.currentDestination?.id)
    }

    @Test
    fun `button_redeem click navigates to voucherFragment`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.button_redeem).performClick()
        }
        assertEquals(R.id.voucherFragment, navController.currentDestination?.id)
    }

    @Test
    fun `button_trip_history click navigates to tripHistoryFragment`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.button_trip_history).performClick()
        }
        assertEquals(R.id.tripHistoryFragment, navController.currentDestination?.id)
    }

    // ==================== Closet & Badge Card Clickable ====================

    @Test
    fun `closet card is clickable`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val card = fragment.requireView().findViewById<View>(R.id.card_closet)
            assertTrue(card.isClickable)
        }
    }

    @Test
    fun `badges card is clickable`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val card = fragment.requireView().findViewById<View>(R.id.card_badges)
            assertTrue(card.isClickable)
        }
    }

    // ==================== Fragment Destroy ====================

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<ProfileFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
