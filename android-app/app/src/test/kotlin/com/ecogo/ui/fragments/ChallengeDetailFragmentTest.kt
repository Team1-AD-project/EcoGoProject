package com.ecogo.ui.fragments

import android.view.View
import android.widget.ProgressBar
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
class ChallengeDetailFragmentTest {

    private val args = bundleOf("challengeId" to "test-challenge")

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `key views are present`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.btn_back))
            assertNotNull(view.findViewById<View>(R.id.text_title))
            assertNotNull(view.findViewById<View>(R.id.text_description))
            assertNotNull(view.findViewById<View>(R.id.card_info))
            assertNotNull(view.findViewById<View>(R.id.card_progress))
        }
    }

    @Test
    fun `accept button is present`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<MaterialButton>(R.id.btn_accept))
        }
    }

    @Test
    fun `progress views are present`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<ProgressBar>(R.id.progress_challenge))
            assertNotNull(view.findViewById<View>(R.id.text_progress))
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
