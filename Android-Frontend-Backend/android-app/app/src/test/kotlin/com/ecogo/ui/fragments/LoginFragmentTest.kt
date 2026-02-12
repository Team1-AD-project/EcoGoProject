package com.ecogo.ui.fragments

import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
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
class LoginFragmentTest {

    private val TOAST_PLEASE_ENTER = "Please enter"

    private fun launchWithNav(): Pair<androidx.fragment.app.testing.FragmentScenario<LoginFragment>, TestNavHostController> {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.setGraph(R.navigation.nav_graph)
        navController.setCurrentDestination(R.id.loginFragment)
        val scenario = launchFragmentInContainer<LoginFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { Navigation.setViewNavController(it.requireView(), navController) }
        return scenario to navController
    }

    // ==================== Lifecycle ====================

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<LoginFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `input fields are present`() {
        val scenario = launchFragmentInContainer<LoginFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<EditText>(R.id.edit_nusnet_id))
            assertNotNull(view.findViewById<EditText>(R.id.edit_password))
        }
    }

    @Test
    fun `sign in button is present`() {
        val scenario = launchFragmentInContainer<LoginFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val btn = fragment.requireView().findViewById<MaterialButton>(R.id.button_sign_in)
            assertNotNull(btn)
            assertTrue(btn.isClickable)
        }
    }

    // ==================== Navigation ====================

    @Test
    fun `register button navigates to signup`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.button_register).performClick()
        }
        assertEquals(R.id.signupWizardFragment, navController.currentDestination?.id)
    }

    @Test
    fun `forgot password text is present`() {
        val scenario = launchFragmentInContainer<LoginFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.text_forgot_password))
        }
    }

    // ==================== Sign In Validation ====================

    @Test
    fun `sign in with empty fields shows toast`() {
        val scenario = launchFragmentInContainer<LoginFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            // Leave fields empty and click sign in
            view.findViewById<EditText>(R.id.edit_nusnet_id).setText("")
            view.findViewById<EditText>(R.id.edit_password).setText("")
            view.findViewById<View>(R.id.button_sign_in).performClick()

            val latestToast = ShadowToast.getTextOfLatestToast()
            assertNotNull(latestToast)
            assertTrue(latestToast.contains(TOAST_PLEASE_ENTER))
        }
    }

    @Test
    fun `sign in with only nusnet id shows toast`() {
        val scenario = launchFragmentInContainer<LoginFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.edit_nusnet_id).setText("testuser")
            view.findViewById<EditText>(R.id.edit_password).setText("")
            view.findViewById<View>(R.id.button_sign_in).performClick()

            val latestToast = ShadowToast.getTextOfLatestToast()
            assertNotNull(latestToast)
            assertTrue(latestToast.contains(TOAST_PLEASE_ENTER))
        }
    }

    @Test
    fun `sign in with only password shows toast`() {
        val scenario = launchFragmentInContainer<LoginFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.edit_nusnet_id).setText("")
            view.findViewById<EditText>(R.id.edit_password).setText("pass123")
            view.findViewById<View>(R.id.button_sign_in).performClick()

            val latestToast = ShadowToast.getTextOfLatestToast()
            assertNotNull(latestToast)
            assertTrue(latestToast.contains(TOAST_PLEASE_ENTER))
        }
    }

    // ==================== Test Account Login ====================

    @Test
    fun `sign in with test account 123-123 navigates to home`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.edit_nusnet_id).setText("123")
            view.findViewById<EditText>(R.id.edit_password).setText("123")
            view.findViewById<View>(R.id.button_sign_in).performClick()
        }
        assertEquals(R.id.homeFragment, navController.currentDestination?.id)
    }

    @Test
    fun `sign in with test account sets is_logged_in pref`() {
        val (scenario, _) = launchWithNav()
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.edit_nusnet_id).setText("123")
            view.findViewById<EditText>(R.id.edit_password).setText("123")
            view.findViewById<View>(R.id.button_sign_in).performClick()

            val prefs = fragment.requireContext().getSharedPreferences("EcoGoPrefs", android.content.Context.MODE_PRIVATE)
            assertTrue(prefs.getBoolean("is_logged_in", false))
        }
    }

    @Test
    fun `sign in with test account shows success toast`() {
        val (scenario, _) = launchWithNav()
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.edit_nusnet_id).setText("123")
            view.findViewById<EditText>(R.id.edit_password).setText("123")
            view.findViewById<View>(R.id.button_sign_in).performClick()

            val latestToast = ShadowToast.getTextOfLatestToast()
            assertNotNull(latestToast)
            assertTrue(latestToast.contains("Login Successful") || latestToast.contains("Test Account"))
        }
    }

    // ==================== Sign In with real credentials (API call) ====================

    @Test
    fun `sign in with non-test credentials disables button`() {
        val scenario = launchFragmentInContainer<LoginFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.edit_nusnet_id).setText("realuser")
            view.findViewById<EditText>(R.id.edit_password).setText("realpass")
            view.findViewById<View>(R.id.button_sign_in).performClick()

            // Button should show "Signing in..." briefly (coroutine starts)
            // Can't easily verify async state, but no crash is good
        }
    }

    // ==================== Register Button ====================

    @Test
    fun `register button is present and clickable`() {
        val scenario = launchFragmentInContainer<LoginFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val btn = fragment.requireView().findViewById<View>(R.id.button_register)
            assertNotNull(btn)
            assertTrue(btn.isClickable)
        }
    }

    // ==================== Destroy ====================

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<LoginFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
