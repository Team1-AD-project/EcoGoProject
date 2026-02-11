package com.ecogo.ui.fragments

import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.viewpager2.widget.ViewPager2
import com.ecogo.R
import com.ecogo.data.FacultyData
import com.ecogo.data.Outfit
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
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SignupWizardFragmentTest {

    // ==================== Reflection helpers ====================

    private fun getPrivateMethod(name: String, vararg paramTypes: Class<*>): Method {
        val method = SignupWizardFragment::class.java.getDeclaredMethod(name, *paramTypes)
        method.isAccessible = true
        return method
    }

    private fun getFieldValue(fragment: SignupWizardFragment, fieldName: String): Any? {
        val field = SignupWizardFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(fragment)
    }

    private fun setFieldValue(fragment: SignupWizardFragment, fieldName: String, value: Any?) {
        val field = SignupWizardFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(fragment, value)
    }

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

    // ==================== Initial field states ====================

    @Test
    fun `currentStep is 0 initially`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(0, getFieldValue(fragment, "currentStep"))
        }
    }

    @Test
    fun `selectedFaculty is null initially`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNull(getFieldValue(fragment, "selectedFaculty"))
        }
    }

    @Test
    fun `username is empty initially`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("", getFieldValue(fragment, "username"))
        }
    }

    @Test
    fun `email is empty initially`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("", getFieldValue(fragment, "email"))
        }
    }

    @Test
    fun `nusnetId is empty initially`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("", getFieldValue(fragment, "nusnetId"))
        }
    }

    @Test
    fun `password is empty initially`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("", getFieldValue(fragment, "password"))
        }
    }

    @Test
    fun `transportPrefs is empty initially`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val prefs = getFieldValue(fragment, "transportPrefs") as MutableSet<String>
            assertTrue(prefs.isEmpty())
        }
    }

    @Test
    fun `dormitory is null initially`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNull(getFieldValue(fragment, "dormitory"))
        }
    }

    @Test
    fun `interests is empty initially`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val interests = getFieldValue(fragment, "interests") as MutableSet<String>
            assertTrue(interests.isEmpty())
        }
    }

    @Test
    fun `weeklyGoal is 5 initially`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(5, getFieldValue(fragment, "weeklyGoal"))
        }
    }

    @Test
    fun `notifyChallenges is true initially`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(true, getFieldValue(fragment, "notifyChallenges"))
        }
    }

    @Test
    fun `notifyReminders is true initially`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(true, getFieldValue(fragment, "notifyReminders"))
        }
    }

    @Test
    fun `notifyFriends is false initially`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(false, getFieldValue(fragment, "notifyFriends"))
        }
    }

    @Test
    fun `otherLocations is empty initially`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val locations = getFieldValue(fragment, "otherLocations") as MutableSet<String>
            assertTrue(locations.isEmpty())
        }
    }

    @Test
    fun `animation references are null initially`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNull(getFieldValue(fragment, "buttonAnimator"))
            assertNull(getFieldValue(fragment, "mascotScaleAnimator"))
            assertNull(getFieldValue(fragment, "mascotRotateAnimator"))
        }
    }

    // ==================== getItemName ====================

    @Test
    fun `getItemName returns Safety Helmet for hat_helmet`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemName", String::class.java)
            assertEquals("Safety Helmet", method.invoke(fragment, "hat_helmet"))
        }
    }

    @Test
    fun `getItemName returns Artist Beret for hat_beret`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemName", String::class.java)
            assertEquals("Artist Beret", method.invoke(fragment, "hat_beret"))
        }
    }

    @Test
    fun `getItemName returns Grad Cap for hat_grad`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemName", String::class.java)
            assertEquals("Grad Cap", method.invoke(fragment, "hat_grad"))
        }
    }

    @Test
    fun `getItemName returns Orange Cap for hat_cap`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemName", String::class.java)
            assertEquals("Orange Cap", method.invoke(fragment, "hat_cap"))
        }
    }

    @Test
    fun `getItemName returns Safety Goggles for face_goggles`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemName", String::class.java)
            assertEquals("Safety Goggles", method.invoke(fragment, "face_goggles"))
        }
    }

    @Test
    fun `getItemName returns Shades for glasses_sun`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemName", String::class.java)
            assertEquals("Shades", method.invoke(fragment, "glasses_sun"))
        }
    }

    @Test
    fun `getItemName returns Engin Plaid for body_plaid`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemName", String::class.java)
            assertEquals("Engin Plaid", method.invoke(fragment, "body_plaid"))
        }
    }

    @Test
    fun `getItemName returns Biz Suit for body_suit`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemName", String::class.java)
            assertEquals("Biz Suit", method.invoke(fragment, "body_suit"))
        }
    }

    @Test
    fun `getItemName returns Lab Coat for body_coat`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemName", String::class.java)
            assertEquals("Lab Coat", method.invoke(fragment, "body_coat"))
        }
    }

    @Test
    fun `getItemName returns NUS Tee for shirt_nus`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemName", String::class.java)
            assertEquals("NUS Tee", method.invoke(fragment, "shirt_nus"))
        }
    }

    @Test
    fun `getItemName returns Blue Hoodie for shirt_hoodie`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemName", String::class.java)
            assertEquals("Blue Hoodie", method.invoke(fragment, "shirt_hoodie"))
        }
    }

    @Test
    fun `getItemName returns empty string for unknown item`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemName", String::class.java)
            assertEquals("", method.invoke(fragment, "unknown_item"))
        }
    }

    @Test
    fun `getItemName returns empty string for none`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("getItemName", String::class.java)
            assertEquals("", method.invoke(fragment, "none"))
        }
    }

    // ==================== validatePersonalInfo edge cases ====================

    @Test
    fun `empty fields do not show errors`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            // All fields empty - no errors should be shown (only shown when non-empty and invalid)
            assertNull(view.findViewById<TextInputLayout>(R.id.input_layout_username).error)
            assertNull(view.findViewById<TextInputLayout>(R.id.input_layout_email).error)
            assertNull(view.findViewById<TextInputLayout>(R.id.input_layout_nusnet).error)
            assertNull(view.findViewById<TextInputLayout>(R.id.input_layout_password).error)
            assertNull(view.findViewById<TextInputLayout>(R.id.input_layout_confirm_password).error)
        }
    }

    @Test
    fun `exactly 3 character username is valid`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.input_username).setText("abc")
            view.findViewById<EditText>(R.id.input_email).setText("a@b.c")
            view.findViewById<EditText>(R.id.input_nusnet).setText("e012345")
            view.findViewById<EditText>(R.id.input_password).setText("123456")
            view.findViewById<EditText>(R.id.input_confirm_password).setText("123456")

            assertNull(view.findViewById<TextInputLayout>(R.id.input_layout_username).error)
            assertTrue(view.findViewById<MaterialButton>(R.id.btn_next_to_faculty).isEnabled)
        }
    }

    @Test
    fun `nusnet starting with uppercase E is valid`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.input_username).setText("testuser")
            view.findViewById<EditText>(R.id.input_email).setText("test@nus.edu")
            view.findViewById<EditText>(R.id.input_nusnet).setText("E0123456")
            view.findViewById<EditText>(R.id.input_password).setText("password123")
            view.findViewById<EditText>(R.id.input_confirm_password).setText("password123")

            assertNull(view.findViewById<TextInputLayout>(R.id.input_layout_nusnet).error)
            assertTrue(view.findViewById<MaterialButton>(R.id.btn_next_to_faculty).isEnabled)
        }
    }

    @Test
    fun `email without dot is invalid`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.input_username).setText("testuser")
            view.findViewById<EditText>(R.id.input_email).setText("test@nus")
            view.findViewById<EditText>(R.id.input_nusnet).setText("e0123456")
            view.findViewById<EditText>(R.id.input_password).setText("password123")
            view.findViewById<EditText>(R.id.input_confirm_password).setText("password123")

            assertNotNull(view.findViewById<TextInputLayout>(R.id.input_layout_email).error)
            assertFalse(view.findViewById<MaterialButton>(R.id.btn_next_to_faculty).isEnabled)
        }
    }

    @Test
    fun `email without at sign is invalid`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.input_username).setText("testuser")
            view.findViewById<EditText>(R.id.input_email).setText("testnus.edu")
            view.findViewById<EditText>(R.id.input_nusnet).setText("e0123456")
            view.findViewById<EditText>(R.id.input_password).setText("password123")
            view.findViewById<EditText>(R.id.input_confirm_password).setText("password123")

            assertNotNull(view.findViewById<TextInputLayout>(R.id.input_layout_email).error)
        }
    }

    @Test
    fun `nusnet too short even with e prefix is invalid`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.input_username).setText("testuser")
            view.findViewById<EditText>(R.id.input_email).setText("test@nus.edu")
            view.findViewById<EditText>(R.id.input_nusnet).setText("e01234")
            view.findViewById<EditText>(R.id.input_password).setText("password123")
            view.findViewById<EditText>(R.id.input_confirm_password).setText("password123")

            assertNotNull(view.findViewById<TextInputLayout>(R.id.input_layout_nusnet).error)
        }
    }

    @Test
    fun `exactly 6 char password is valid`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.input_username).setText("testuser")
            view.findViewById<EditText>(R.id.input_email).setText("test@nus.edu")
            view.findViewById<EditText>(R.id.input_nusnet).setText("e0123456")
            view.findViewById<EditText>(R.id.input_password).setText("abcdef")
            view.findViewById<EditText>(R.id.input_confirm_password).setText("abcdef")

            assertNull(view.findViewById<TextInputLayout>(R.id.input_layout_password).error)
            assertTrue(view.findViewById<MaterialButton>(R.id.btn_next_to_faculty).isEnabled)
        }
    }

    @Test
    fun `5 char password is invalid`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.input_username).setText("testuser")
            view.findViewById<EditText>(R.id.input_email).setText("test@nus.edu")
            view.findViewById<EditText>(R.id.input_nusnet).setText("e0123456")
            view.findViewById<EditText>(R.id.input_password).setText("abcde")
            view.findViewById<EditText>(R.id.input_confirm_password).setText("abcde")

            assertNotNull(view.findViewById<TextInputLayout>(R.id.input_layout_password).error)
        }
    }

    @Test
    fun `empty confirm password does not show mismatch error`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.input_username).setText("testuser")
            view.findViewById<EditText>(R.id.input_email).setText("test@nus.edu")
            view.findViewById<EditText>(R.id.input_nusnet).setText("e0123456")
            view.findViewById<EditText>(R.id.input_password).setText("password123")
            view.findViewById<EditText>(R.id.input_confirm_password).setText("")

            // Empty confirm password should not show error text (only shows when non-empty and mismatched)
            assertNull(view.findViewById<TextInputLayout>(R.id.input_layout_confirm_password).error)
        }
    }

    // ==================== Step switching ====================

    @Test
    fun `showFacultySelection switches to step 1`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showFacultySelection")
            method.invoke(fragment)

            val view = fragment.requireView()
            assertEquals(1, getFieldValue(fragment, "currentStep"))
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_personal_info).visibility)
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.layout_faculty_selection).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_transport_preference).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_common_locations).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_interests_goals).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_mascot_reveal).visibility)
        }
    }

    @Test
    fun `showFacultySelection sets up viewpager adapter`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showFacultySelection")
            method.invoke(fragment)

            val viewPager = fragment.requireView().findViewById<ViewPager2>(R.id.viewpager_faculties)
            assertNotNull(viewPager.adapter)
        }
    }

    @Test
    fun `showFacultySelection sets page indicator text`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showFacultySelection")
            method.invoke(fragment)

            val indicator = fragment.requireView().findViewById<TextView>(R.id.text_page_indicator)
            assertTrue(indicator.text.toString().startsWith("1 /"))
        }
    }

    @Test
    fun `showTransportPreference switches to step 2`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showTransportPreference")
            method.invoke(fragment)

            val view = fragment.requireView()
            assertEquals(2, getFieldValue(fragment, "currentStep"))
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_personal_info).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_faculty_selection).visibility)
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.layout_transport_preference).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_common_locations).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_interests_goals).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_mascot_reveal).visibility)
        }
    }

    @Test
    fun `showTransportPreference disables continue button initially`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showTransportPreference")
            method.invoke(fragment)

            val btn = fragment.requireView().findViewById<MaterialButton>(R.id.btn_continue_transport)
            assertFalse(btn.isEnabled)
            assertEquals(0.5f, btn.alpha, 0.01f)
        }
    }

    @Test
    fun `showCommonLocations switches to step 3`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showCommonLocations")
            method.invoke(fragment)

            val view = fragment.requireView()
            assertEquals(3, getFieldValue(fragment, "currentStep"))
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_personal_info).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_faculty_selection).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_transport_preference).visibility)
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.layout_common_locations).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_interests_goals).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_mascot_reveal).visibility)
        }
    }

    @Test
    fun `showInterestsGoals switches to step 4`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showInterestsGoals")
            method.invoke(fragment)

            val view = fragment.requireView()
            assertEquals(4, getFieldValue(fragment, "currentStep"))
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_personal_info).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_faculty_selection).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_transport_preference).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_common_locations).visibility)
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.layout_interests_goals).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_mascot_reveal).visibility)
        }
    }

    @Test
    fun `showMascotReveal switches to step 5`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "username", "TestUser")
            val faculty = FacultyData(
                id = "soc",
                name = "School of Computing",
                color = "#003D7C",
                slogan = "Computing the Future",
                outfit = Outfit(head = "hat_cap", face = "none", body = "shirt_nus")
            )

            val method = getPrivateMethod("showMascotReveal", FacultyData::class.java)
            method.invoke(fragment, faculty)

            val view = fragment.requireView()
            assertEquals(5, getFieldValue(fragment, "currentStep"))
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_personal_info).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_faculty_selection).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_transport_preference).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_common_locations).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_interests_goals).visibility)
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.layout_mascot_reveal).visibility)
        }
    }

    @Test
    fun `showMascotReveal sets welcome title`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "username", "Alice")
            val faculty = FacultyData(
                id = "soc",
                name = "School of Computing",
                color = "#003D7C",
                slogan = "Computing the Future",
                outfit = Outfit(head = "none", face = "none", body = "none")
            )

            val method = getPrivateMethod("showMascotReveal", FacultyData::class.java)
            method.invoke(fragment, faculty)

            val title = fragment.requireView().findViewById<TextView>(R.id.text_reveal_title)
            assertEquals("Welcome, Alice!", title.text.toString())
        }
    }

    @Test
    fun `showMascotReveal sets faculty name and slogan`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "username", "Bob")
            val faculty = FacultyData(
                id = "foe",
                name = "Faculty of Engineering",
                color = "#FF6600",
                slogan = "Engineer the World",
                outfit = Outfit(head = "hat_helmet", face = "face_goggles", body = "body_plaid")
            )

            val method = getPrivateMethod("showMascotReveal", FacultyData::class.java)
            method.invoke(fragment, faculty)

            val view = fragment.requireView()
            assertEquals("Faculty of Engineering", view.findViewById<TextView>(R.id.text_faculty_name).text.toString())
            assertEquals("Engineer the World", view.findViewById<TextView>(R.id.text_faculty_slogan).text.toString())
        }
    }

    @Test
    fun `showMascotReveal displays outfit items text`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "username", "Charlie")
            val faculty = FacultyData(
                id = "foe",
                name = "Engineering",
                color = "#FF6600",
                slogan = "Slogan",
                outfit = Outfit(head = "hat_helmet", face = "face_goggles", body = "body_plaid")
            )

            val method = getPrivateMethod("showMascotReveal", FacultyData::class.java)
            method.invoke(fragment, faculty)

            val outfitText = fragment.requireView().findViewById<TextView>(R.id.text_outfit_items).text.toString()
            assertTrue(outfitText.contains("Safety Helmet"))
            assertTrue(outfitText.contains("Safety Goggles"))
            assertTrue(outfitText.contains("Engin Plaid"))
            assertTrue(outfitText.startsWith("Starter Outfit:"))
        }
    }

    @Test
    fun `showMascotReveal with all none outfit shows no items`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "username", "Dave")
            val faculty = FacultyData(
                id = "test",
                name = "Test",
                color = "#000000",
                slogan = "Test",
                outfit = Outfit(head = "none", face = "none", body = "none")
            )

            val method = getPrivateMethod("showMascotReveal", FacultyData::class.java)
            method.invoke(fragment, faculty)

            val outfitText = fragment.requireView().findViewById<TextView>(R.id.text_outfit_items).text.toString()
            assertEquals("Starter Outfit: ", outfitText)
        }
    }

    @Test
    fun `showMascotReveal starts animations`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "username", "Test")
            val faculty = FacultyData(
                id = "test",
                name = "Test",
                color = "#000000",
                slogan = "Test",
                outfit = Outfit()
            )

            val method = getPrivateMethod("showMascotReveal", FacultyData::class.java)
            method.invoke(fragment, faculty)

            assertNotNull(getFieldValue(fragment, "buttonAnimator"))
            assertNotNull(getFieldValue(fragment, "mascotScaleAnimator"))
            assertNotNull(getFieldValue(fragment, "mascotRotateAnimator"))
        }
    }

    @Test
    fun `showMascotReveal sets subtitle text`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "username", "Test")
            val faculty = FacultyData(
                id = "test",
                name = "Test",
                color = "#000000",
                slogan = "Test",
                outfit = Outfit()
            )

            val method = getPrivateMethod("showMascotReveal", FacultyData::class.java)
            method.invoke(fragment, faculty)

            val subtitle = fragment.requireView().findViewById<TextView>(R.id.text_reveal_subtitle)
            assertEquals("Meet your new buddy.", subtitle.text.toString())
        }
    }

    // ==================== saveRegistrationData ====================

    @Test
    fun `saveRegistrationData writes to SharedPreferences`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "username", "testuser")
            setFieldValue(fragment, "email", "test@nus.edu")
            setFieldValue(fragment, "nusnetId", "e0123456")
            setFieldValue(fragment, "password", "secret123")
            setFieldValue(fragment, "selectedFaculty", FacultyData(
                id = "soc", name = "Computing", color = "#003D7C",
                slogan = "Compute", outfit = Outfit()
            ))

            val method = getPrivateMethod("saveRegistrationData")
            method.invoke(fragment)

            val prefs = fragment.requireContext()
                .getSharedPreferences("EcoGoPrefs", Context.MODE_PRIVATE)
            assertEquals("testuser", prefs.getString("username", ""))
            assertEquals("test@nus.edu", prefs.getString("email", ""))
            assertEquals("e0123456", prefs.getString("nusnet_id", ""))
            assertEquals("secret123", prefs.getString("password", ""))
            assertEquals("Computing", prefs.getString("faculty", ""))
            assertTrue(prefs.getBoolean("is_registered", false))
        }
    }

    @Test
    fun `saveRegistrationData saves transport prefs`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "username", "u")
            setFieldValue(fragment, "email", "e")
            setFieldValue(fragment, "nusnetId", "n")
            setFieldValue(fragment, "password", "p")
            @Suppress("UNCHECKED_CAST")
            val prefs = getFieldValue(fragment, "transportPrefs") as MutableSet<String>
            prefs.add("bus")
            prefs.add("walk")

            val method = getPrivateMethod("saveRegistrationData")
            method.invoke(fragment)

            val saved = fragment.requireContext()
                .getSharedPreferences("EcoGoPrefs", Context.MODE_PRIVATE)
                .getStringSet("transport_prefs", emptySet())
            assertTrue(saved!!.contains("bus"))
            assertTrue(saved.contains("walk"))
        }
    }

    @Test
    fun `saveRegistrationData saves interests and goals`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "username", "u")
            setFieldValue(fragment, "email", "e")
            setFieldValue(fragment, "nusnetId", "n")
            setFieldValue(fragment, "password", "p")
            @Suppress("UNCHECKED_CAST")
            val interests = getFieldValue(fragment, "interests") as MutableSet<String>
            interests.add("sustainability")
            interests.add("rewards")
            setFieldValue(fragment, "weeklyGoal", 10)
            setFieldValue(fragment, "notifyChallenges", false)
            setFieldValue(fragment, "notifyReminders", false)
            setFieldValue(fragment, "notifyFriends", true)

            val method = getPrivateMethod("saveRegistrationData")
            method.invoke(fragment)

            val prefs = fragment.requireContext()
                .getSharedPreferences("EcoGoPrefs", Context.MODE_PRIVATE)
            val savedInterests = prefs.getStringSet("interests", emptySet())
            assertTrue(savedInterests!!.contains("sustainability"))
            assertTrue(savedInterests.contains("rewards"))
            assertEquals(10, prefs.getInt("weekly_goal", 0))
            assertFalse(prefs.getBoolean("notify_challenges", true))
            assertFalse(prefs.getBoolean("notify_reminders", true))
            assertTrue(prefs.getBoolean("notify_friends", false))
        }
    }

    @Test
    fun `saveRegistrationData saves location data`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setFieldValue(fragment, "username", "u")
            setFieldValue(fragment, "email", "e")
            setFieldValue(fragment, "nusnetId", "n")
            setFieldValue(fragment, "password", "p")
            setFieldValue(fragment, "dormitory", "PGP")
            setFieldValue(fragment, "teachingBuilding", "COM1")
            setFieldValue(fragment, "studySpot", "CLB")
            @Suppress("UNCHECKED_CAST")
            val locs = getFieldValue(fragment, "otherLocations") as MutableSet<String>
            locs.add("gym")
            locs.add("canteen")

            val method = getPrivateMethod("saveRegistrationData")
            method.invoke(fragment)

            val prefs = fragment.requireContext()
                .getSharedPreferences("EcoGoPrefs", Context.MODE_PRIVATE)
            assertEquals("PGP", prefs.getString("dormitory", ""))
            assertEquals("COM1", prefs.getString("teaching_building", ""))
            assertEquals("CLB", prefs.getString("study_spot", ""))
            val savedLocs = prefs.getStringSet("other_locations", emptySet())
            assertTrue(savedLocs!!.contains("gym"))
            assertTrue(savedLocs.contains("canteen"))
        }
    }

    // ==================== saveFirstLoginStatus ====================

    @Test
    fun `saveFirstLoginStatus sets true`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("saveFirstLoginStatus", Boolean::class.java)
            method.invoke(fragment, true)

            val prefs = fragment.requireContext()
                .getSharedPreferences("EcoGoPrefs", Context.MODE_PRIVATE)
            assertTrue(prefs.getBoolean("is_first_login", false))
        }
    }

    @Test
    fun `saveFirstLoginStatus sets false`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("saveFirstLoginStatus", Boolean::class.java)
            method.invoke(fragment, false)

            val prefs = fragment.requireContext()
                .getSharedPreferences("EcoGoPrefs", Context.MODE_PRIVATE)
            assertFalse(prefs.getBoolean("is_first_login", true))
        }
    }

    // ==================== Chip interaction in step 3 (locations) ====================

    @Test
    fun `showCommonLocations chip gym toggles otherLocations set`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showCommonLocations")
            method.invoke(fragment)

            @Suppress("UNCHECKED_CAST")
            val locations = getFieldValue(fragment, "otherLocations") as MutableSet<String>
            assertTrue(locations.isEmpty())

            val chip = fragment.requireView().findViewById<Chip>(R.id.chip_gym)
            chip.isChecked = true
            assertTrue(locations.contains("gym"))

            chip.isChecked = false
            assertFalse(locations.contains("gym"))
        }
    }

    @Test
    fun `showCommonLocations chip canteen toggles otherLocations set`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showCommonLocations")
            method.invoke(fragment)

            @Suppress("UNCHECKED_CAST")
            val locations = getFieldValue(fragment, "otherLocations") as MutableSet<String>
            val chip = fragment.requireView().findViewById<Chip>(R.id.chip_canteen)
            chip.isChecked = true
            assertTrue(locations.contains("canteen"))
        }
    }

    @Test
    fun `showCommonLocations chip lab toggles otherLocations set`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showCommonLocations")
            method.invoke(fragment)

            @Suppress("UNCHECKED_CAST")
            val locations = getFieldValue(fragment, "otherLocations") as MutableSet<String>
            val chip = fragment.requireView().findViewById<Chip>(R.id.chip_lab)
            chip.isChecked = true
            assertTrue(locations.contains("lab"))
        }
    }

    @Test
    fun `showCommonLocations chip sports toggles otherLocations set`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showCommonLocations")
            method.invoke(fragment)

            @Suppress("UNCHECKED_CAST")
            val locations = getFieldValue(fragment, "otherLocations") as MutableSet<String>
            val chip = fragment.requireView().findViewById<Chip>(R.id.chip_sports)
            chip.isChecked = true
            assertTrue(locations.contains("sports"))
        }
    }

    // ==================== Chip interaction in step 4 (interests) ====================

    @Test
    fun `showInterestsGoals sustainability chip toggles interests set`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showInterestsGoals")
            method.invoke(fragment)

            @Suppress("UNCHECKED_CAST")
            val interests = getFieldValue(fragment, "interests") as MutableSet<String>
            val chip = fragment.requireView().findViewById<Chip>(R.id.chip_sustainability)
            chip.isChecked = true
            assertTrue(interests.contains("sustainability"))

            chip.isChecked = false
            assertFalse(interests.contains("sustainability"))
        }
    }

    @Test
    fun `showInterestsGoals challenges chip toggles interests set`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showInterestsGoals")
            method.invoke(fragment)

            @Suppress("UNCHECKED_CAST")
            val interests = getFieldValue(fragment, "interests") as MutableSet<String>
            val chip = fragment.requireView().findViewById<Chip>(R.id.chip_challenges)
            chip.isChecked = true
            assertTrue(interests.contains("challenges"))
        }
    }

    @Test
    fun `showInterestsGoals community chip toggles interests set`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showInterestsGoals")
            method.invoke(fragment)

            @Suppress("UNCHECKED_CAST")
            val interests = getFieldValue(fragment, "interests") as MutableSet<String>
            val chip = fragment.requireView().findViewById<Chip>(R.id.chip_community)
            chip.isChecked = true
            assertTrue(interests.contains("community"))
        }
    }

    @Test
    fun `showInterestsGoals rewards chip toggles interests set`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showInterestsGoals")
            method.invoke(fragment)

            @Suppress("UNCHECKED_CAST")
            val interests = getFieldValue(fragment, "interests") as MutableSet<String>
            val chip = fragment.requireView().findViewById<Chip>(R.id.chip_rewards)
            chip.isChecked = true
            assertTrue(interests.contains("rewards"))
        }
    }

    @Test
    fun `showInterestsGoals leaderboard chip toggles interests set`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showInterestsGoals")
            method.invoke(fragment)

            @Suppress("UNCHECKED_CAST")
            val interests = getFieldValue(fragment, "interests") as MutableSet<String>
            val chip = fragment.requireView().findViewById<Chip>(R.id.chip_leaderboard)
            chip.isChecked = true
            assertTrue(interests.contains("leaderboard"))
        }
    }

    // ==================== Notification switches in step 4 ====================

    @Test
    fun `showInterestsGoals switch_challenges toggles notifyChallenges`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showInterestsGoals")
            method.invoke(fragment)

            val sw = fragment.requireView().findViewById<MaterialSwitch>(R.id.switch_challenges)
            sw.isChecked = false
            assertEquals(false, getFieldValue(fragment, "notifyChallenges"))
            sw.isChecked = true
            assertEquals(true, getFieldValue(fragment, "notifyChallenges"))
        }
    }

    @Test
    fun `showInterestsGoals switch_reminders toggles notifyReminders`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showInterestsGoals")
            method.invoke(fragment)

            val sw = fragment.requireView().findViewById<MaterialSwitch>(R.id.switch_reminders)
            sw.isChecked = false
            assertEquals(false, getFieldValue(fragment, "notifyReminders"))
        }
    }

    @Test
    fun `showInterestsGoals switch_friends toggles notifyFriends`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showInterestsGoals")
            method.invoke(fragment)

            val sw = fragment.requireView().findViewById<MaterialSwitch>(R.id.switch_friends)
            sw.isChecked = true
            assertEquals(true, getFieldValue(fragment, "notifyFriends"))
        }
    }

    // ==================== Weekly goal slider in step 4 ====================

    @Test
    fun `showInterestsGoals slider updates weeklyGoal`() {
        val scenario = launchFragmentInContainer<SignupWizardFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("showInterestsGoals")
            method.invoke(fragment)

            val slider = fragment.requireView().findViewById<Slider>(R.id.slider_weekly_goal)
            slider.value = 10f

            assertEquals(10, getFieldValue(fragment, "weeklyGoal"))
            val goalText = fragment.requireView().findViewById<TextView>(R.id.text_goal_value)
            assertEquals("10", goalText.text.toString())
        }
    }
}
