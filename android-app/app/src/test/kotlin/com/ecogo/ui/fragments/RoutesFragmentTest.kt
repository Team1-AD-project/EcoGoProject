package com.ecogo.ui.fragments

import android.view.View
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.recyclerview.widget.LinearLayoutManager
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
class RoutesFragmentTest {

    /**
     * Helper: launch RoutesFragment with TestNavHostController
     */
    private fun launchWithNav(): Pair<androidx.fragment.app.testing.FragmentScenario<RoutesFragment>, TestNavHostController> {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.setGraph(R.navigation.nav_graph)
        navController.setCurrentDestination(R.id.routesFragment)

        val scenario = launchFragmentInContainer<RoutesFragment>(
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
        val scenario = launchFragmentInContainer<RoutesFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.view)
        }
    }

    @Test
    fun `fragment view is not null after onViewCreated`() {
        val scenario = launchFragmentInContainer<RoutesFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.recycler_routes))
            assertNotNull(view.findViewById<View>(R.id.spinner_bus_stop))
        }
    }

    // ==================== Views Present ====================

    @Test
    fun `title text view is present`() {
        val scenario = launchFragmentInContainer<RoutesFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val title = fragment.requireView().findViewById<TextView>(R.id.text_title)
            assertNotNull(title)
            assertTrue(title.text.isNotEmpty())
        }
    }

    @Test
    fun `bus stop spinner is present`() {
        val scenario = launchFragmentInContainer<RoutesFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val spinner = fragment.requireView().findViewById<Spinner>(R.id.spinner_bus_stop)
            assertNotNull(spinner)
        }
    }

    @Test
    fun `recycler view is present`() {
        val scenario = launchFragmentInContainer<RoutesFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<RecyclerView>(R.id.recycler_routes))
        }
    }

    // ==================== RecyclerView Setup ====================

    @Test
    fun `recyclerView has adapter after setup`() {
        val scenario = launchFragmentInContainer<RoutesFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_routes)
            assertNotNull(recycler.adapter)
        }
    }

    @Test
    fun `recyclerView has LinearLayoutManager`() {
        val scenario = launchFragmentInContainer<RoutesFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_routes)
            assertNotNull(recycler.layoutManager)
            assertTrue(recycler.layoutManager is LinearLayoutManager)
        }
    }

    // ==================== Spinner Setup ====================

    @Test
    fun `spinner has adapter with bus stops`() {
        val scenario = launchFragmentInContainer<RoutesFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val spinner = fragment.requireView().findViewById<Spinner>(R.id.spinner_bus_stop)
            assertNotNull(spinner.adapter)
            assertTrue("Spinner should have at least one bus stop", spinner.adapter.count > 0)
        }
    }

    @Test
    fun `spinner has default selection`() {
        val scenario = launchFragmentInContainer<RoutesFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val spinner = fragment.requireView().findViewById<Spinner>(R.id.spinner_bus_stop)
            assertTrue("Spinner should have a selected item", spinner.selectedItemPosition >= 0)
        }
    }

    // ==================== Navigation ====================

    @Test
    fun `navigation controller is attached`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment {
            assertEquals(R.id.routesFragment, navController.currentDestination?.id)
        }
    }

    // ==================== Fragment Destroy ====================

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<RoutesFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
