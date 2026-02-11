package com.ecogo.ui.fragments

import android.view.View
import android.widget.EditText
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EditProfileFragmentTest {

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `input fields are present`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<EditText>(R.id.edit_nickname))
            assertNotNull(view.findViewById<View>(R.id.edit_faculty))
            assertNotNull(view.findViewById<EditText>(R.id.edit_dormitory))
            assertNotNull(view.findViewById<EditText>(R.id.edit_teaching_building))
            assertNotNull(view.findViewById<EditText>(R.id.edit_study_spot))
        }
    }

    @Test
    fun `notification switches are present`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<SwitchMaterial>(R.id.switch_new_challenges))
            assertNotNull(view.findViewById<SwitchMaterial>(R.id.switch_activity_reminders))
            assertNotNull(view.findViewById<SwitchMaterial>(R.id.switch_friend_activity))
        }
    }

    @Test
    fun `transport modes recycler is present`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<RecyclerView>(R.id.recycler_transport_modes))
        }
    }

    @Test
    fun `save button is present`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val btn = fragment.requireView().findViewById<MaterialButton>(R.id.btn_save_bottom)
            assertNotNull(btn)
            assertTrue(btn.isClickable)
        }
    }

    @Test
    fun `back button is present`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.btn_back))
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
