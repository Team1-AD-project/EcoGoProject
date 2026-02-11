package com.ecogo.ui.fragments

import android.view.View
import android.widget.EditText
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.viewpager2.widget.ViewPager2
import com.ecogo.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SignupWizardFragmentTest {

    // ==================== Lifecycle ====================

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.view)
        }
    }

    @Test
    fun `fragment view is not null after onViewCreated`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.layout_personal_info))
            assertNotNull(view.findViewById<View>(R.id.layout_faculty_selection))
        }
    }

    // ==================== Step 0: Initial State ====================

    @Test
    fun `personal info layout is visible initially`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.layout_personal_info).visibility)
        }
    }

    @Test
    fun `other layouts are hidden initially`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_faculty_selection).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_transport_preference).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_common_locations).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_interests_goals).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_mascot_reveal).visibility)
        }
    }

    // ==================== Step 0: Input Fields ====================

    @Test
    fun `all personal info input fields are present`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<EditText>(R.id.input_username))
            assertNotNull(view.findViewById<EditText>(R.id.input_email))
            assertNotNull(view.findViewById<EditText>(R.id.input_nusnet))
            assertNotNull(view.findViewById<EditText>(R.id.input_password))
            assertNotNull(view.findViewById<EditText>(R.id.input_confirm_password))
        }
    }

    @Test
    fun `all input layouts are present`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<TextInputLayout>(R.id.input_layout_username))
            assertNotNull(view.findViewById<TextInputLayout>(R.id.input_layout_email))
            assertNotNull(view.findViewById<TextInputLayout>(R.id.input_layout_nusnet))
            assertNotNull(view.findViewById<TextInputLayout>(R.id.input_layout_password))
            assertNotNull(view.findViewById<TextInputLayout>(R.id.input_layout_confirm_password))
        }
    }

    @Test
    fun `next button is initially disabled`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val btn = fragment.requireView().findViewById<MaterialButton>(R.id.btn_next_to_faculty)
            assertFalse(btn.isEnabled)
            assertEquals(0.5f, btn.alpha, 0.01f)
        }
    }

    // ==================== Step 0: Validation ====================

    @Test
    fun `short username shows error`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.input_username).setText("ab")
            view.findViewById<EditText>(R.id.input_email).setText("test@nus.edu")
            view.findViewById<EditText>(R.id.input_nusnet).setText("e0123456")
            view.findViewById<EditText>(R.id.input_password).setText("password123")
            view.findViewById<EditText>(R.id.input_confirm_password).setText("password123")

            val layout = view.findViewById<TextInputLayout>(R.id.input_layout_username)
            assertNotNull(layout.error)
        }
    }

    @Test
    fun `invalid email shows error`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.input_username).setText("testuser")
            view.findViewById<EditText>(R.id.input_email).setText("invalidemail")
            view.findViewById<EditText>(R.id.input_nusnet).setText("e0123456")
            view.findViewById<EditText>(R.id.input_password).setText("password123")
            view.findViewById<EditText>(R.id.input_confirm_password).setText("password123")

            val layout = view.findViewById<TextInputLayout>(R.id.input_layout_email)
            assertNotNull(layout.error)
        }
    }

    @Test
    fun `invalid nusnet id shows error`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.input_username).setText("testuser")
            view.findViewById<EditText>(R.id.input_email).setText("test@nus.edu")
            view.findViewById<EditText>(R.id.input_nusnet).setText("x012")
            view.findViewById<EditText>(R.id.input_password).setText("password123")
            view.findViewById<EditText>(R.id.input_confirm_password).setText("password123")

            val layout = view.findViewById<TextInputLayout>(R.id.input_layout_nusnet)
            assertNotNull(layout.error)
        }
    }

    @Test
    fun `short password shows error`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.input_username).setText("testuser")
            view.findViewById<EditText>(R.id.input_email).setText("test@nus.edu")
            view.findViewById<EditText>(R.id.input_nusnet).setText("e0123456")
            view.findViewById<EditText>(R.id.input_password).setText("abc")
            view.findViewById<EditText>(R.id.input_confirm_password).setText("abc")

            val layout = view.findViewById<TextInputLayout>(R.id.input_layout_password)
            assertNotNull(layout.error)
        }
    }

    @Test
    fun `mismatched passwords show error`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.input_username).setText("testuser")
            view.findViewById<EditText>(R.id.input_email).setText("test@nus.edu")
            view.findViewById<EditText>(R.id.input_nusnet).setText("e0123456")
            view.findViewById<EditText>(R.id.input_password).setText("password123")
            view.findViewById<EditText>(R.id.input_confirm_password).setText("differentpw")

            val layout = view.findViewById<TextInputLayout>(R.id.input_layout_confirm_password)
            assertNotNull(layout.error)
        }
    }

    @Test
    fun `valid form enables next button`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.input_username).setText("testuser")
            view.findViewById<EditText>(R.id.input_email).setText("test@nus.edu")
            view.findViewById<EditText>(R.id.input_nusnet).setText("e0123456")
            view.findViewById<EditText>(R.id.input_password).setText("password123")
            view.findViewById<EditText>(R.id.input_confirm_password).setText("password123")

            val btn = view.findViewById<MaterialButton>(R.id.btn_next_to_faculty)
            assertTrue(btn.isEnabled)
            assertEquals(1f, btn.alpha, 0.01f)
        }
    }

    @Test
    fun `valid form clears all errors`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.input_username).setText("testuser")
            view.findViewById<EditText>(R.id.input_email).setText("test@nus.edu")
            view.findViewById<EditText>(R.id.input_nusnet).setText("e0123456")
            view.findViewById<EditText>(R.id.input_password).setText("password123")
            view.findViewById<EditText>(R.id.input_confirm_password).setText("password123")

            assertNull(view.findViewById<TextInputLayout>(R.id.input_layout_username).error)
            assertNull(view.findViewById<TextInputLayout>(R.id.input_layout_email).error)
            assertNull(view.findViewById<TextInputLayout>(R.id.input_layout_nusnet).error)
            assertNull(view.findViewById<TextInputLayout>(R.id.input_layout_password).error)
            assertNull(view.findViewById<TextInputLayout>(R.id.input_layout_confirm_password).error)
        }
    }

    // ==================== Step 1: Faculty Selection Views ====================

    @Test
    fun `faculty selection has viewpager`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<ViewPager2>(R.id.viewpager_faculties))
        }
    }

    @Test
    fun `faculty selection has page indicator`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.text_page_indicator))
        }
    }

    // ==================== Step 2: Transport Preference Views ====================

    @Test
    fun `transport preference has recycler view`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.recycler_transport_modes))
        }
    }

    @Test
    fun `transport preference has continue button`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<MaterialButton>(R.id.btn_continue_transport))
        }
    }

    // ==================== Step 3: Common Locations Views ====================

    @Test
    fun `common locations has input fields`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<EditText>(R.id.input_dorm))
            assertNotNull(view.findViewById<EditText>(R.id.input_building))
            assertNotNull(view.findViewById<EditText>(R.id.input_library))
        }
    }

    @Test
    fun `common locations has chips`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<Chip>(R.id.chip_gym))
            assertNotNull(view.findViewById<Chip>(R.id.chip_canteen))
            assertNotNull(view.findViewById<Chip>(R.id.chip_lab))
            assertNotNull(view.findViewById<Chip>(R.id.chip_sports))
        }
    }

    @Test
    fun `common locations has skip and continue buttons`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<MaterialButton>(R.id.btn_skip_locations))
            assertNotNull(view.findViewById<MaterialButton>(R.id.btn_continue_locations))
        }
    }

    // ==================== Step 4: Interests & Goals Views ====================

    @Test
    fun `interests goals has interest chips`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<Chip>(R.id.chip_sustainability))
            assertNotNull(view.findViewById<Chip>(R.id.chip_challenges))
            assertNotNull(view.findViewById<Chip>(R.id.chip_community))
            assertNotNull(view.findViewById<Chip>(R.id.chip_rewards))
            assertNotNull(view.findViewById<Chip>(R.id.chip_leaderboard))
        }
    }

    @Test
    fun `interests goals has weekly goal slider`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val slider = fragment.requireView().findViewById<Slider>(R.id.slider_weekly_goal)
            assertNotNull(slider)
            assertEquals(5f, slider.value, 0.01f)
        }
    }

    @Test
    fun `interests goals has notification switches`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            val switchChallenges = view.findViewById<MaterialSwitch>(R.id.switch_challenges)
            val switchReminders = view.findViewById<MaterialSwitch>(R.id.switch_reminders)
            val switchFriends = view.findViewById<MaterialSwitch>(R.id.switch_friends)

            assertNotNull(switchChallenges)
            assertNotNull(switchReminders)
            assertNotNull(switchFriends)

            // Default states from XML
            assertTrue(switchChallenges.isChecked)
            assertTrue(switchReminders.isChecked)
            assertFalse(switchFriends.isChecked)
        }
    }

    @Test
    fun `interests goals has finish button`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<MaterialButton>(R.id.btn_finish_signup))
        }
    }

    // ==================== Step 5: Mascot Reveal Views ====================

    @Test
    fun `mascot reveal layout has required views`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.text_reveal_title))
            assertNotNull(view.findViewById<View>(R.id.text_reveal_subtitle))
            assertNotNull(view.findViewById<View>(R.id.mascot_reveal))
            assertNotNull(view.findViewById<View>(R.id.text_faculty_name))
            assertNotNull(view.findViewById<View>(R.id.text_faculty_slogan))
            assertNotNull(view.findViewById<View>(R.id.view_faculty_color))
            assertNotNull(view.findViewById<View>(R.id.text_outfit_items))
            assertNotNull(view.findViewById<MaterialButton>(R.id.btn_lets_go))
        }
    }

    // ==================== Progress Indicators ====================

    @Test
    fun `step 0 has progress indicators`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.progress_step1))
            assertNotNull(view.findViewById<View>(R.id.progress_step2))
            assertNotNull(view.findViewById<View>(R.id.progress_step3))
        }
    }

    // ==================== Fragment Destroy ====================

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
