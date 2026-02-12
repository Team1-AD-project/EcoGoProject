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
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ActivityDetailFragmentTest {

    private val args = bundleOf("activityId" to "test-activity")

    private fun getField(fragment: ActivityDetailFragment, fieldName: String): Any? {
        val field = ActivityDetailFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(fragment)
    }

    private fun setField(fragment: ActivityDetailFragment, fieldName: String, value: Any?) {
        val field = ActivityDetailFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(fragment, value)
    }

    private fun invokePrivate(fragment: ActivityDetailFragment, methodName: String): Any? {
        val method = ActivityDetailFragment::class.java.getDeclaredMethod(methodName)
        method.isAccessible = true
        return method.invoke(fragment)
    }

    // ==================== Lifecycle ====================

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

    // ==================== Default field values ====================

    @Test
    fun `activityId is set from args`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("test-activity", getField(fragment, "activityId"))
        }
    }

    @Test
    fun `isJoined defaults to false`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertFalse(getField(fragment, "isJoined") as Boolean)
        }
    }

    @Test
    fun `activityStatus defaults to PUBLISHED`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("PUBLISHED", getField(fragment, "activityStatus"))
        }
    }

    // ==================== updateJoinButton ====================

    @Test
    fun `updateJoinButton when not joined shows join text`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "isJoined", false)
            invokePrivate(fragment, "updateJoinButton")
            val btn = fragment.requireView().findViewById<MaterialButton>(R.id.btn_join)
            assertTrue(btn.text.toString().contains("参加"))
            assertNull(btn.icon)
        }
    }

    @Test
    fun `updateJoinButton when joined shows leave text`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "isJoined", true)
            invokePrivate(fragment, "updateJoinButton")
            val btn = fragment.requireView().findViewById<MaterialButton>(R.id.btn_join)
            assertTrue(btn.text.toString().contains("退出"))
        }
    }

    // ==================== updateButtonStates ====================

    @Test
    fun `updateButtonStates ENDED disables join`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "activityStatus", "ENDED")
            invokePrivate(fragment, "updateButtonStates")
            val btnJoin = fragment.requireView().findViewById<MaterialButton>(R.id.btn_join)
            assertFalse(btnJoin.isEnabled)
        }
    }

    @Test
    fun `updateButtonStates ENDED disables route`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "activityStatus", "ENDED")
            invokePrivate(fragment, "updateButtonStates")
            val btnRoute = fragment.requireView().findViewById<MaterialButton>(R.id.btn_start_route)
            assertFalse(btnRoute.isEnabled)
        }
    }

    @Test
    fun `updateButtonStates ONGOING enables join`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "activityStatus", "ONGOING")
            invokePrivate(fragment, "updateButtonStates")
            assertTrue(fragment.requireView().findViewById<MaterialButton>(R.id.btn_join).isEnabled)
        }
    }

    @Test
    fun `updateButtonStates ONGOING joined enables route`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "activityStatus", "ONGOING")
            setField(fragment, "isJoined", true)
            invokePrivate(fragment, "updateButtonStates")
            assertTrue(fragment.requireView().findViewById<MaterialButton>(R.id.btn_start_route).isEnabled)
        }
    }

    @Test
    fun `updateButtonStates ONGOING not joined disables route`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "activityStatus", "ONGOING")
            setField(fragment, "isJoined", false)
            invokePrivate(fragment, "updateButtonStates")
            assertFalse(fragment.requireView().findViewById<MaterialButton>(R.id.btn_start_route).isEnabled)
        }
    }

    @Test
    fun `updateButtonStates PUBLISHED enables join`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "activityStatus", "PUBLISHED")
            invokePrivate(fragment, "updateButtonStates")
            assertTrue(fragment.requireView().findViewById<MaterialButton>(R.id.btn_join).isEnabled)
        }
    }

    // ==================== share button ====================

    @Test
    fun `share button click shows toast`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.btn_share).performClick()
            assertNotNull(ShadowToast.getTextOfLatestToast())
        }
    }

    // ==================== start route without coords ====================

    @Test
    fun `start route without coords shows toast`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "activityLat", null)
            setField(fragment, "activityLng", null)
            fragment.requireView().findViewById<MaterialButton>(R.id.btn_start_route).performClick()
            assertNotNull(ShadowToast.getTextOfLatestToast())
        }
    }

    // ==================== time views ====================

    @Test
    fun `time views are present`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.text_start_time))
            assertNotNull(view.findViewById<View>(R.id.text_end_time))
        }
    }

    // ==================== progress bar ====================

    @Test
    fun `progress participants view is present`() {
        val scenario = launchFragmentInContainer<ActivityDetailFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<ProgressBar>(R.id.progress_participants))
        }
    }
}
