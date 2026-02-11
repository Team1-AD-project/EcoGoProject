package com.ecogo.ui.fragments

import android.view.View
import androidx.core.os.bundleOf
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
class ActivityDetailFragmentTest {

    private val args = bundleOf("activityId" to "test-activity")

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `key views are present`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.btn_back))
            assertNotNull(view.findViewById<View>(R.id.text_title))
            assertNotNull(view.findViewById<View>(R.id.text_description))
            assertNotNull(view.findViewById<View>(R.id.card_info))
            assertNotNull(view.findViewById<View>(R.id.card_details))
        }
    }

    @Test
    fun `action buttons are present`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<MaterialButton>(R.id.btn_join))
            assertNotNull(view.findViewById<MaterialButton>(R.id.btn_start_route))
        }
    }

    @Test
    fun `info text views are present`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.text_type))
            assertNotNull(view.findViewById<View>(R.id.text_status))
            assertNotNull(view.findViewById<View>(R.id.text_reward))
            assertNotNull(view.findViewById<View>(R.id.text_participants))
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
