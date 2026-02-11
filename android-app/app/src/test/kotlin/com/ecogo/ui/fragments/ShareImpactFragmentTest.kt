package com.ecogo.ui.fragments

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import com.ecogo.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ShareImpactFragmentTest {

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `key views are present`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.btn_back))
            assertNotNull(view.findViewById<View>(R.id.card_preview))
            assertNotNull(view.findViewById<View>(R.id.card_stats))
        }
    }

    @Test
    fun `period chips are present`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<Chip>(R.id.chip_today))
            assertNotNull(view.findViewById<Chip>(R.id.chip_week))
            assertNotNull(view.findViewById<Chip>(R.id.chip_month))
        }
    }

    @Test
    fun `stats text views are present`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.text_trips))
            assertNotNull(view.findViewById<View>(R.id.text_distance))
            assertNotNull(view.findViewById<View>(R.id.text_carbon_saved))
            assertNotNull(view.findViewById<View>(R.id.text_points))
        }
    }

    @Test
    fun `share and save buttons are present`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<MaterialButton>(R.id.btn_share))
            assertNotNull(view.findViewById<MaterialButton>(R.id.btn_save))
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
