package com.ecogo.ui.fragments

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.viewpager2.widget.ViewPager2
import com.ecogo.R
import com.google.android.material.button.MaterialButton
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OnboardingFragmentTest {

    private fun launchWithNav(): Pair<androidx.fragment.app.testing.FragmentScenario<OnboardingFragment>, TestNavHostController> {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.setGraph(R.navigation.nav_graph)
        navController.setCurrentDestination(R.id.onboardingFragment)
        val scenario = launchFragmentInContainer<OnboardingFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { Navigation.setViewNavController(it.requireView(), navController) }
        return scenario to navController
    }

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<OnboardingFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `viewpager is present with adapter`() {
        val scenario = launchFragmentInContainer<OnboardingFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val viewPager = fragment.requireView().findViewById<ViewPager2>(R.id.view_pager)
            assertNotNull(viewPager)
            assertNotNull(viewPager.adapter)
        }
    }

    @Test
    fun `next button is present`() {
        val scenario = launchFragmentInContainer<OnboardingFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<MaterialButton>(R.id.button_next))
        }
    }

    @Test
    fun `skip text is present`() {
        val scenario = launchFragmentInContainer<OnboardingFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.text_skip))
        }
    }

    @Test
    fun `progress dots are present`() {
        val scenario = launchFragmentInContainer<OnboardingFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.dot_1))
            assertNotNull(view.findViewById<View>(R.id.dot_2))
            assertNotNull(view.findViewById<View>(R.id.dot_3))
        }
    }

    @Test
    fun `skip navigates to home`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.text_skip).performClick()
        }
        assertEquals(R.id.homeFragment, navController.currentDestination?.id)
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<OnboardingFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
