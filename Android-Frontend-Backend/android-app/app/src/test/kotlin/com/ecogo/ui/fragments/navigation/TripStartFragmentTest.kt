package com.ecogo.ui.fragments.navigation

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import com.ecogo.R
import com.google.android.material.button.MaterialButton
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TripStartFragmentTest {

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<TripStartFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `key views are present`() {
        val scenario = launchFragmentInContainer<TripStartFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.btn_back))
            assertNotNull(view.findViewById<View>(R.id.card_route_info))
            assertNotNull(view.findViewById<View>(R.id.card_estimates))
        }
    }

    @Test
    fun `route info views are present`() {
        val scenario = launchFragmentInContainer<TripStartFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.text_route_name))
            assertNotNull(view.findViewById<View>(R.id.text_origin))
            assertNotNull(view.findViewById<View>(R.id.text_destination))
        }
    }

    @Test
    fun `estimate views are present`() {
        val scenario = launchFragmentInContainer<TripStartFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.text_estimated_time))
            assertNotNull(view.findViewById<View>(R.id.text_estimated_distance))
            assertNotNull(view.findViewById<View>(R.id.text_carbon_saved))
            assertNotNull(view.findViewById<View>(R.id.text_points_earn))
        }
    }

    @Test
    fun `start trip button is present`() {
        val scenario = launchFragmentInContainer<TripStartFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val btn = fragment.requireView().findViewById<MaterialButton>(R.id.btn_start_trip)
            assertNotNull(btn)
            assertTrue(btn.isClickable)
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<TripStartFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
