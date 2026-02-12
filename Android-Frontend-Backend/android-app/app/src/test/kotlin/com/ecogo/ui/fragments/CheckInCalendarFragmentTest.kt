package com.ecogo.ui.fragments

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
class CheckInCalendarFragmentTest {

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<CheckInCalendarFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `toolbar and navigation views are present`() {
        val scenario = launchFragmentInContainer<CheckInCalendarFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.toolbar))
            assertNotNull(view.findViewById<View>(R.id.button_previous_month))
            assertNotNull(view.findViewById<View>(R.id.button_next_month))
            assertNotNull(view.findViewById<View>(R.id.text_month_year))
        }
    }

    @Test
    fun `stats views are present`() {
        val scenario = launchFragmentInContainer<CheckInCalendarFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.text_consecutive_days))
            assertNotNull(view.findViewById<View>(R.id.text_total_check_ins))
            assertNotNull(view.findViewById<View>(R.id.text_month_check_ins))
        }
    }

    @Test
    fun `check in button is present`() {
        val scenario = launchFragmentInContainer<CheckInCalendarFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<MaterialButton>(R.id.button_check_in))
        }
    }

    @Test
    fun `calendar grid is present`() {
        val scenario = launchFragmentInContainer<CheckInCalendarFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.calendar_grid))
        }
    }

    @Test
    fun `month year text is not empty`() {
        val scenario = launchFragmentInContainer<CheckInCalendarFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val text = fragment.requireView().findViewById<android.widget.TextView>(R.id.text_month_year)
            assertTrue(text.text.isNotEmpty())
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<CheckInCalendarFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
