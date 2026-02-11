package com.ecogo.ui.fragments.navigation

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import com.ecogo.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TripDetailFragmentTest {

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<TripDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `key views are present`() {
        val scenario = launchFragmentInContainer<TripDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.btn_back))
            assertNotNull(view.findViewById<View>(R.id.text_from))
            assertNotNull(view.findViewById<View>(R.id.text_to))
            assertNotNull(view.findViewById<View>(R.id.text_depart_at))
            assertNotNull(view.findViewById<View>(R.id.text_passengers))
            assertNotNull(view.findViewById<View>(R.id.text_booking_id))
            assertNotNull(view.findViewById<View>(R.id.text_status))
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<TripDetailFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
