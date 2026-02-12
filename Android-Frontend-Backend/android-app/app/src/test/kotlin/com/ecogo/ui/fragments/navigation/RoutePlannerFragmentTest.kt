package com.ecogo.ui.fragments.navigation

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RoutePlannerFragmentTest {

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<RoutePlannerFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `origin and destination views are present`() {
        val scenario = launchFragmentInContainer<RoutePlannerFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.origin_container))
            assertNotNull(view.findViewById<View>(R.id.destination_container))
            assertNotNull(view.findViewById<View>(R.id.text_origin))
            assertNotNull(view.findViewById<View>(R.id.text_destination))
        }
    }

    @Test
    fun `transport mode selectors are present`() {
        val scenario = launchFragmentInContainer<RoutePlannerFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.mode_walk))
            assertNotNull(view.findViewById<View>(R.id.mode_cycle))
            assertNotNull(view.findViewById<View>(R.id.mode_bus))
        }
    }

    @Test
    fun `routes recycler is present`() {
        val scenario = launchFragmentInContainer<RoutePlannerFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<RecyclerView>(R.id.recycler_routes))
        }
    }

    @Test
    fun `start navigation button is present`() {
        val scenario = launchFragmentInContainer<RoutePlannerFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val btn = fragment.requireView().findViewById<View>(R.id.btn_start_navigation)
            assertNotNull(btn)
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<RoutePlannerFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
