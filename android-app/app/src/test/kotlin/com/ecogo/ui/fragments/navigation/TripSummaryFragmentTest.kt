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
class TripSummaryFragmentTest {

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<TripSummaryFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `key views are present`() {
        val scenario = launchFragmentInContainer<TripSummaryFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.btn_close))
            assertNotNull(view.findViewById<View>(R.id.text_eco_rating))
        }
    }

    @Test
    fun `stat cards are present`() {
        val scenario = launchFragmentInContainer<TripSummaryFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.card_distance))
            assertNotNull(view.findViewById<View>(R.id.card_duration))
            assertNotNull(view.findViewById<View>(R.id.card_carbon))
            assertNotNull(view.findViewById<View>(R.id.card_points))
        }
    }

    @Test
    fun `stat text views are present`() {
        val scenario = launchFragmentInContainer<TripSummaryFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.text_distance))
            assertNotNull(view.findViewById<View>(R.id.text_duration))
            assertNotNull(view.findViewById<View>(R.id.text_carbon_saved))
            assertNotNull(view.findViewById<View>(R.id.text_points))
        }
    }

    @Test
    fun `action buttons are present`() {
        val scenario = launchFragmentInContainer<TripSummaryFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<MaterialButton>(R.id.btn_share))
            assertNotNull(view.findViewById<MaterialButton>(R.id.btn_view_leaderboard))
            assertNotNull(view.findViewById<MaterialButton>(R.id.btn_redeem))
            assertNotNull(view.findViewById<MaterialButton>(R.id.btn_again))
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<TripSummaryFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
