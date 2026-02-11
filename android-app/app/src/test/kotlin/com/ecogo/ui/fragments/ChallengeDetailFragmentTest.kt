package com.ecogo.ui.fragments

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import com.ecogo.R
import com.ecogo.data.Challenge
import com.ecogo.data.UserChallengeProgress
import com.google.android.material.button.MaterialButton
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ChallengeDetailFragmentTest {

    private val args = bundleOf("challengeId" to "test-challenge")

    private fun getField(fragment: ChallengeDetailFragment, fieldName: String): Any? {
        val field = ChallengeDetailFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(fragment)
    }

    private fun setField(fragment: ChallengeDetailFragment, fieldName: String, value: Any?) {
        val field = ChallengeDetailFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(fragment, value)
    }

    private fun invokeDisplayChallengeDetail(fragment: ChallengeDetailFragment, challenge: Challenge) {
        val method = ChallengeDetailFragment::class.java.getDeclaredMethod("displayChallengeDetail", Challenge::class.java)
        method.isAccessible = true
        method.invoke(fragment, challenge)
    }

    private fun invokeUpdateButtonState(fragment: ChallengeDetailFragment, status: String) {
        val method = ChallengeDetailFragment::class.java.getDeclaredMethod("updateButtonState", String::class.java)
        method.isAccessible = true
        method.invoke(fragment, status)
    }

    private fun invokeUpdateProgressUI(fragment: ChallengeDetailFragment, progress: UserChallengeProgress) {
        val method = ChallengeDetailFragment::class.java.getDeclaredMethod("updateProgressUI", UserChallengeProgress::class.java)
        method.isAccessible = true
        method.invoke(fragment, progress)
    }

    private fun invokeShowError(fragment: ChallengeDetailFragment, message: String) {
        val method = ChallengeDetailFragment::class.java.getDeclaredMethod("showError", String::class.java)
        method.isAccessible = true
        method.invoke(fragment, message)
    }

    private fun makeChallenge(
        type: String = "GREEN_TRIPS_COUNT",
        reward: Int = 50,
        badge: String? = null,
        endTime: String? = null,
        participants: Int = 5
    ) = Challenge(
        id = "c1", title = "Test Challenge", description = "Do something",
        type = type, target = 10.0, reward = reward,
        badge = badge, icon = "🎯", status = "ACTIVE",
        participants = participants, startTime = null, endTime = endTime
    )

    private fun makeProgress(
        current: Double = 5.0,
        target: Double = 10.0,
        percent: Double = 50.0,
        status: String = "IN_PROGRESS",
        rewardClaimed: Boolean = false
    ) = UserChallengeProgress(
        id = "p1", challengeId = "c1", userId = "u1",
        status = status, current = current, target = target,
        progressPercent = percent, joinedAt = "2025-01-01",
        rewardClaimed = rewardClaimed
    )

    // ==================== Lifecycle ====================

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

    // ==================== Default field values ====================

    @Test
    fun `challengeId is set from args`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("test-challenge", getField(fragment, "challengeId"))
        }
    }

    @Test
    fun `isAccepted defaults to false`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertFalse(getField(fragment, "isAccepted") as Boolean)
        }
    }

    // ==================== displayChallengeDetail ====================

    @Test
    fun `displayChallengeDetail GREEN_TRIPS_COUNT type`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokeDisplayChallengeDetail(fragment, makeChallenge(type = "GREEN_TRIPS_COUNT"))
            assertEquals("Trip Count Challenge", fragment.requireView().findViewById<TextView>(R.id.text_type).text.toString())
        }
    }

    @Test
    fun `displayChallengeDetail GREEN_TRIPS_DISTANCE type`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokeDisplayChallengeDetail(fragment, makeChallenge(type = "GREEN_TRIPS_DISTANCE"))
            assertEquals("Distance Challenge", fragment.requireView().findViewById<TextView>(R.id.text_type).text.toString())
        }
    }

    @Test
    fun `displayChallengeDetail CARBON_SAVED type`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokeDisplayChallengeDetail(fragment, makeChallenge(type = "CARBON_SAVED"))
            assertEquals("Carbon Saving Challenge", fragment.requireView().findViewById<TextView>(R.id.text_type).text.toString())
        }
    }

    @Test
    fun `displayChallengeDetail sets title and description`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokeDisplayChallengeDetail(fragment, makeChallenge())
            assertEquals("Test Challenge", fragment.requireView().findViewById<TextView>(R.id.text_title).text.toString())
            assertEquals("Do something", fragment.requireView().findViewById<TextView>(R.id.text_description).text.toString())
        }
    }

    @Test
    fun `displayChallengeDetail sets reward text`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokeDisplayChallengeDetail(fragment, makeChallenge(reward = 200))
            assertEquals("+200 points", fragment.requireView().findViewById<TextView>(R.id.text_reward).text.toString())
        }
    }

    @Test
    fun `displayChallengeDetail with badge shows badge reward`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokeDisplayChallengeDetail(fragment, makeChallenge(badge = "gold_badge"))
            assertEquals(View.VISIBLE, fragment.requireView().findViewById<TextView>(R.id.text_badge_reward).visibility)
        }
    }

    @Test
    fun `displayChallengeDetail without badge hides badge reward`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokeDisplayChallengeDetail(fragment, makeChallenge(badge = null))
            assertEquals(View.GONE, fragment.requireView().findViewById<TextView>(R.id.text_badge_reward).visibility)
        }
    }

    @Test
    fun `displayChallengeDetail with endTime formats date`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokeDisplayChallengeDetail(fragment, makeChallenge(endTime = "2025-12-31T23:59:59"))
            assertEquals("2025/12/31", fragment.requireView().findViewById<TextView>(R.id.text_end_time).text.toString())
        }
    }

    @Test
    fun `displayChallengeDetail without endTime shows no deadline`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokeDisplayChallengeDetail(fragment, makeChallenge(endTime = null))
            assertEquals("No deadline", fragment.requireView().findViewById<TextView>(R.id.text_end_time).text.toString())
        }
    }

    @Test
    fun `displayChallengeDetail sets participants`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokeDisplayChallengeDetail(fragment, makeChallenge(participants = 42))
            assertEquals("42 people", fragment.requireView().findViewById<TextView>(R.id.text_participants).text.toString())
        }
    }

    @Test
    fun `displayChallengeDetail sets progress to zero`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokeDisplayChallengeDetail(fragment, makeChallenge())
            assertEquals("0 / 10", fragment.requireView().findViewById<TextView>(R.id.text_progress).text.toString())
            assertEquals("0%", fragment.requireView().findViewById<TextView>(R.id.text_progress_percent).text.toString())
        }
    }

    // ==================== updateButtonState ====================

    @Test
    fun `updateButtonState ACTIVE not accepted`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "isAccepted", false)
            invokeUpdateButtonState(fragment, "ACTIVE")
            val btn = fragment.requireView().findViewById<MaterialButton>(R.id.btn_accept)
            assertEquals("Accept Challenge", btn.text.toString())
            assertTrue(btn.isEnabled)
        }
    }

    @Test
    fun `updateButtonState ACTIVE accepted`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "isAccepted", true)
            invokeUpdateButtonState(fragment, "ACTIVE")
            val btn = fragment.requireView().findViewById<MaterialButton>(R.id.btn_accept)
            assertEquals("Keep Going", btn.text.toString())
            assertTrue(btn.isEnabled)
        }
    }

    @Test
    fun `updateButtonState COMPLETED`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokeUpdateButtonState(fragment, "COMPLETED")
            val btn = fragment.requireView().findViewById<MaterialButton>(R.id.btn_accept)
            assertEquals("Challenge Completed", btn.text.toString())
            assertFalse(btn.isEnabled)
        }
    }

    @Test
    fun `updateButtonState EXPIRED`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokeUpdateButtonState(fragment, "EXPIRED")
            val btn = fragment.requireView().findViewById<MaterialButton>(R.id.btn_accept)
            assertEquals("Challenge Expired", btn.text.toString())
            assertFalse(btn.isEnabled)
        }
    }

    // ==================== updateProgressUI ====================

    @Test
    fun `updateProgressUI in progress`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokeUpdateProgressUI(fragment, makeProgress())
            val btn = fragment.requireView().findViewById<MaterialButton>(R.id.btn_accept)
            assertEquals("Keep Going", btn.text.toString())
            assertTrue(btn.isEnabled)
            assertEquals("5 / 10", fragment.requireView().findViewById<TextView>(R.id.text_progress).text.toString())
        }
    }

    @Test
    fun `updateProgressUI completed not claimed`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "currentChallenge", makeChallenge(reward = 100))
            invokeUpdateProgressUI(fragment, makeProgress(current = 10.0, target = 10.0, percent = 100.0, status = "COMPLETED", rewardClaimed = false))
            val btn = fragment.requireView().findViewById<MaterialButton>(R.id.btn_accept)
            assertTrue(btn.text.toString().contains("Claim Reward"))
            assertTrue(btn.isEnabled)
        }
    }

    @Test
    fun `updateProgressUI completed and claimed`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokeUpdateProgressUI(fragment, makeProgress(current = 10.0, target = 10.0, percent = 100.0, status = "COMPLETED", rewardClaimed = true))
            val btn = fragment.requireView().findViewById<MaterialButton>(R.id.btn_accept)
            assertTrue(btn.text.toString().contains("Completed"))
            assertFalse(btn.isEnabled)
        }
    }

    // ==================== showError ====================

    @Test
    fun `showError does not crash`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokeShowError(fragment, "Test error message")
        }
    }

    // ==================== share button and mascot ====================

    @Test
    fun `share button click shows toast`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.btn_share).performClick()
            assertNotNull(ShadowToast.getTextOfLatestToast())
        }
    }

    @Test
    fun `mascot cheer view is present`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.mascot_cheer))
        }
    }

    @Test
    fun `progress percent view is present`() {
        val scenario = launchFragmentInContainer<ChallengeDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.text_progress_percent))
        }
    }
}
