package com.ecogo.ui.fragments

import android.view.View
import android.widget.EditText
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LoginFragmentTest {

    private fun launchWithNav(): Pair<androidx.fragment.app.testing.FragmentScenario<LoginFragment>, TestNavHostController> {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.setGraph(R.navigation.nav_graph)
        navController.setCurrentDestination(R.id.loginFragment)
        val scenario = launchFragmentInContainer<LoginFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { Navigation.setViewNavController(it.requireView(), navController) }
        return scenario to navController
    }

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

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<LoginFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
