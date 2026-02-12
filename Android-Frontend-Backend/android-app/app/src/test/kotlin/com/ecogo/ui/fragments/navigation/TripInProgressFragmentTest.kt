package com.ecogo.ui.fragments.navigation

import android.view.View
import android.widget.ProgressBar
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
class TripInProgressFragmentTest {

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<TripInProgressFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `trip info views are present`() {
        val scenario = launchFragmentInContainer<TripInProgressFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.text_destination))
            assertNotNull(view.findViewById<ProgressBar>(R.id.progress_trip))
            assertNotNull(view.findViewById<View>(R.id.text_progress))
        }
    }

    @Test
    fun `stats views are present`() {
        val scenario = launchFragmentInContainer<TripInProgressFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.text_distance_covered))
            assertNotNull(view.findViewById<View>(R.id.text_distance_remaining))
            assertNotNull(view.findViewById<View>(R.id.text_carbon_saved))
            assertNotNull(view.findViewById<View>(R.id.text_points_realtime))
        }
    }

    @Test
    fun `action buttons are present`() {
        val scenario = launchFragmentInContainer<TripInProgressFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.btn_complete_trip))
            assertNotNull(view.findViewById<View>(R.id.btn_cancel_trip))
        }
    }

    @Test
    fun `next steps recycler is present`() {
        val scenario = launchFragmentInContainer<TripInProgressFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<RecyclerView>(R.id.recycler_next_steps))
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<TripInProgressFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
