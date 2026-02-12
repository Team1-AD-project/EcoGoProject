package com.ecogo.ui.fragments

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.viewpager2.widget.ViewPager2
import com.ecogo.R
import com.google.android.material.tabs.TabLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VoucherFragmentTest {

    private fun launchWithNav(): Pair<androidx.fragment.app.testing.FragmentScenario<VoucherFragment>, TestNavHostController> {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.setGraph(R.navigation.nav_graph)
        navController.setCurrentDestination(R.id.voucherFragment)
        val scenario = launchFragmentInContainer<VoucherFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { Navigation.setViewNavController(it.requireView(), navController) }
        return scenario to navController
    }

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<VoucherFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `key views are present`() {
        val scenario = launchFragmentInContainer<VoucherFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.btn_back))
            assertNotNull(view.findViewById<View>(R.id.text_user_points))
            assertNotNull(view.findViewById<TabLayout>(R.id.tab_layout))
            assertNotNull(view.findViewById<ViewPager2>(R.id.view_pager))
        }
    }

    @Test
    fun `viewpager has adapter`() {
        val scenario = launchFragmentInContainer<VoucherFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val viewPager = fragment.requireView().findViewById<ViewPager2>(R.id.view_pager)
            assertNotNull(viewPager.adapter)
        }
    }

    @Test
    fun `history card navigates to order history`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_history).performClick()
        }
        assertEquals(R.id.orderHistoryFragment, navController.currentDestination?.id)
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<VoucherFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
