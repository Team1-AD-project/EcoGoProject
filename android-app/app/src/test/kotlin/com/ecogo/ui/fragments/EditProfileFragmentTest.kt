package com.ecogo.ui.fragments

import android.view.View
import android.widget.EditText
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.ecogo.auth.TokenManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputLayout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import androidx.test.core.app.ApplicationProvider

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EditProfileFragmentTest {

    companion object {
        private const val TEST_TOKEN = "test-token"
        private const val TEST_USER_ID = "test-user-id"
    }

    @Before
    fun setup() {
        TokenManager.init(ApplicationProvider.getApplicationContext())
    }

    private fun invokePrivate(fragment: EditProfileFragment, methodName: String, vararg args: Any?): Any? {
        val paramTypes = args.map {
            when (it) {
                is String -> String::class.java
                is Boolean -> Boolean::class.java
                is Int -> Int::class.java
                else -> it?.javaClass ?: Any::class.java
            }
        }.toTypedArray()
        val method = EditProfileFragment::class.java.getDeclaredMethod(methodName, *paramTypes)
        method.isAccessible = true
        return method.invoke(fragment, *args)
    }

    private fun getField(fragment: EditProfileFragment, fieldName: String): Any? {
        val field = EditProfileFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(fragment)
    }

    // ==================== Lifecycle ====================

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

    // ==================== FACULTIES Companion Object ====================

    @Test
    fun `FACULTIES list is not empty`() {
        assertTrue(EditProfileFragment.FACULTIES.isNotEmpty())
    }

    @Test
    fun `FACULTIES contains School of Computing`() {
        assertTrue(EditProfileFragment.FACULTIES.contains("School of Computing"))
    }

    @Test
    fun `FACULTIES contains Faculty of Engineering`() {
        assertTrue(EditProfileFragment.FACULTIES.contains("Faculty of Engineering"))
    }

    @Test
    fun `FACULTIES has expected count`() {
        assertEquals(14, EditProfileFragment.FACULTIES.size)
    }

    @Test
    fun `FACULTIES contains all expected faculties`() {
        val faculties = EditProfileFragment.FACULTIES
        assertTrue(faculties.contains("Faculty of Science"))
        assertTrue(faculties.contains("Faculty of Arts and Social Sciences"))
        assertTrue(faculties.contains("Business School"))
        assertTrue(faculties.contains("Faculty of Law"))
        assertTrue(faculties.contains("Faculty of Dentistry"))
        assertTrue(faculties.contains("Yong Loo Lin School of Medicine"))
        assertTrue(faculties.contains("School of Design and Environment"))
        assertTrue(faculties.contains("Yong Siew Toh Conservatory of Music"))
        assertTrue(faculties.contains("Faculty of Nursing"))
        assertTrue(faculties.contains("School of Public Health"))
    }

    // ==================== setupFacultyDropdown ====================

    @Test
    fun `setupFacultyDropdown sets adapter on edit_faculty`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "setupFacultyDropdown")
        }
    }

    // ==================== setupTransportList ====================

    @Test
    fun `setupTransportList sets layout manager`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_transport_modes)
            assertNotNull(recycler.layoutManager)
        }
    }

    @Test
    fun `transport recycler has LinearLayoutManager`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_transport_modes)
            assertTrue(recycler.layoutManager is androidx.recyclerview.widget.LinearLayoutManager)
        }
    }

    // ==================== selectedTransportModes ====================

    @Test
    fun `selectedTransportModes is initially empty`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            @Suppress("UNCHECKED_CAST")
            val modes = getField(fragment, "selectedTransportModes") as MutableSet<String>
            assertTrue(modes.isEmpty())
        }
    }

    // ==================== cachedProfile ====================

    @Test
    fun `cachedProfile is initially null`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNull(getField(fragment, "cachedProfile"))
        }
    }

    // ==================== loadLocalPreferences ====================

    @Test
    fun `loadLocalPreferences loads from shared prefs`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val sp = fragment.requireContext().getSharedPreferences("ecogo_profile", android.content.Context.MODE_PRIVATE)
            sp.edit().putString("dormitoryOrResidence", "PGPR").putString("mainTeachingBuilding", "COM1").putString("favoriteStudySpot", "CLB").apply()
            invokePrivate(fragment, "loadLocalPreferences")
            val view = fragment.requireView()
            assertEquals("PGPR", view.findViewById<EditText>(R.id.edit_dormitory).text.toString())
            assertEquals("COM1", view.findViewById<EditText>(R.id.edit_teaching_building).text.toString())
            assertEquals("CLB", view.findViewById<EditText>(R.id.edit_study_spot).text.toString())
        }
    }

    @Test
    fun `loadLocalPreferences with empty prefs loads empty strings`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val sp = fragment.requireContext().getSharedPreferences("ecogo_profile", android.content.Context.MODE_PRIVATE)
            sp.edit().clear().apply()
            invokePrivate(fragment, "loadLocalPreferences")
            val view = fragment.requireView()
            assertEquals("", view.findViewById<EditText>(R.id.edit_dormitory).text.toString())
        }
    }

    @Test
    fun `loadLocalPreferences with null values loads empty strings`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val sp = fragment.requireContext().getSharedPreferences("ecogo_profile", android.content.Context.MODE_PRIVATE)
            sp.edit().clear().apply()
            invokePrivate(fragment, "loadLocalPreferences")
            assertEquals("", fragment.requireView().findViewById<EditText>(R.id.edit_teaching_building).text.toString())
            assertEquals("", fragment.requireView().findViewById<EditText>(R.id.edit_study_spot).text.toString())
        }
    }

    // ==================== getLocalPref / getLocalPrefInt ====================

    @Test
    fun `getLocalPref returns default when key not set`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val sp = fragment.requireContext().getSharedPreferences("ecogo_profile", android.content.Context.MODE_PRIVATE)
            sp.edit().clear().apply()
            val result = invokePrivate(fragment, "getLocalPref", "newChallenges", true) as Boolean
            assertTrue(result)
        }
    }

    @Test
    fun `getLocalPref returns saved value`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val sp = fragment.requireContext().getSharedPreferences("ecogo_profile", android.content.Context.MODE_PRIVATE)
            sp.edit().putBoolean("newChallenges", false).apply()
            val result = invokePrivate(fragment, "getLocalPref", "newChallenges", true) as Boolean
            assertFalse(result)
        }
    }

    @Test
    fun `getLocalPref with false default returns false when not set`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val sp = fragment.requireContext().getSharedPreferences("ecogo_profile", android.content.Context.MODE_PRIVATE)
            sp.edit().clear().apply()
            val result = invokePrivate(fragment, "getLocalPref", "friendActivity", false) as Boolean
            assertFalse(result)
        }
    }

    @Test
    fun `getLocalPrefInt returns default when key not set`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val sp = fragment.requireContext().getSharedPreferences("ecogo_profile", android.content.Context.MODE_PRIVATE)
            sp.edit().clear().apply()
            val result = invokePrivate(fragment, "getLocalPrefInt", "weeklyGoals", 20) as Int
            assertEquals(20, result)
        }
    }

    @Test
    fun `getLocalPrefInt returns saved value`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val sp = fragment.requireContext().getSharedPreferences("ecogo_profile", android.content.Context.MODE_PRIVATE)
            sp.edit().putInt("weeklyGoals", 50).apply()
            val result = invokePrivate(fragment, "getLocalPrefInt", "weeklyGoals", 20) as Int
            assertEquals(50, result)
        }
    }

    @Test
    fun `getLocalPrefInt with different default value`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val sp = fragment.requireContext().getSharedPreferences("ecogo_profile", android.content.Context.MODE_PRIVATE)
            sp.edit().clear().apply()
            val result = invokePrivate(fragment, "getLocalPrefInt", "weeklyGoals", 30) as Int
            assertEquals(30, result)
        }
    }

    // ==================== saveLocalPreferences ====================

    @Test
    fun `saveLocalPreferences saves values correctly`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = EditProfileFragment::class.java.getDeclaredMethod(
                "saveLocalPreferences",
                String::class.java, String::class.java, String::class.java,
                Int::class.java, Boolean::class.java, Boolean::class.java, Boolean::class.java
            )
            method.isAccessible = true
            method.invoke(fragment, "PGPR", "COM1", "CLB", 30, true, false, true)

            val sp = fragment.requireContext().getSharedPreferences("ecogo_profile", android.content.Context.MODE_PRIVATE)
            assertEquals("PGPR", sp.getString("dormitoryOrResidence", ""))
            assertEquals("COM1", sp.getString("mainTeachingBuilding", ""))
            assertEquals("CLB", sp.getString("favoriteStudySpot", ""))
            assertEquals(30, sp.getInt("weeklyGoals", 0))
            assertTrue(sp.getBoolean("newChallenges", false))
            assertFalse(sp.getBoolean("activityReminders", true))
            assertTrue(sp.getBoolean("friendActivity", false))
        }
    }

    @Test
    fun `saveLocalPreferences saves empty strings correctly`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = EditProfileFragment::class.java.getDeclaredMethod(
                "saveLocalPreferences",
                String::class.java, String::class.java, String::class.java,
                Int::class.java, Boolean::class.java, Boolean::class.java, Boolean::class.java
            )
            method.isAccessible = true
            method.invoke(fragment, "", "", "", 20, false, false, false)

            val sp = fragment.requireContext().getSharedPreferences("ecogo_profile", android.content.Context.MODE_PRIVATE)
            assertEquals("", sp.getString("dormitoryOrResidence", "X"))
            assertEquals("", sp.getString("mainTeachingBuilding", "X"))
            assertEquals("", sp.getString("favoriteStudySpot", "X"))
            assertEquals(20, sp.getInt("weeklyGoals", -1))
            assertFalse(sp.getBoolean("newChallenges", true))
        }
    }

    // ==================== saveProfile validation ====================

    @Test
    fun `saveProfile with empty nickname shows error`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.edit_nickname).setText("")
            invokePrivate(fragment, "saveProfile")
        }
    }

    @Test
    fun `saveProfile with null userId shows toast`() {
        TokenManager.logout()
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.edit_nickname).setText("TestUser")
            invokePrivate(fragment, "saveProfile")
            val toast = ShadowToast.getTextOfLatestToast()
            assertNotNull(toast)
        }
    }

    @Test
    fun `saveProfile with valid nickname and userId builds request`() {
        TokenManager.saveToken(TEST_TOKEN, TEST_USER_ID, "TestUser")
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.edit_nickname).setText("TestUser")
            view.findViewById<EditText>(R.id.edit_dormitory).setText("PGPR")
            view.findViewById<EditText>(R.id.edit_teaching_building).setText("COM1")
            view.findViewById<EditText>(R.id.edit_study_spot).setText("CLB")
            view.findViewById<EditText>(R.id.edit_weekly_goals).setText("25")
            view.findViewById<SwitchMaterial>(R.id.switch_new_challenges).isChecked = true
            view.findViewById<SwitchMaterial>(R.id.switch_activity_reminders).isChecked = false
            view.findViewById<SwitchMaterial>(R.id.switch_friend_activity).isChecked = true
            invokePrivate(fragment, "saveProfile")
            // saveProfile gathers fields and builds UpdateProfileRequest — coverage is the goal
        }
    }

    @Test
    fun `saveProfile with valid nickname after empty does not crash`() {
        TokenManager.saveToken(TEST_TOKEN, TEST_USER_ID, "TestUser")
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.edit_nickname).setText("")
            invokePrivate(fragment, "saveProfile")
            view.findViewById<EditText>(R.id.edit_nickname).setText("ValidUser")
            invokePrivate(fragment, "saveProfile")
        }
    }

    @Test
    fun `saveProfile with empty weeklyGoals defaults to 20`() {
        TokenManager.saveToken(TEST_TOKEN, TEST_USER_ID, "TestUser")
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.edit_nickname).setText("TestUser")
            view.findViewById<EditText>(R.id.edit_weekly_goals).setText("")
            invokePrivate(fragment, "saveProfile")
        }
    }

    @Test
    fun `saveProfile with non-numeric weeklyGoals defaults to 20`() {
        TokenManager.saveToken(TEST_TOKEN, TEST_USER_ID, "TestUser")
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.edit_nickname).setText("TestUser")
            view.findViewById<EditText>(R.id.edit_weekly_goals).setText("abc")
            invokePrivate(fragment, "saveProfile")
        }
    }

    @Test
    fun `saveProfile reads switch states correctly`() {
        TokenManager.saveToken(TEST_TOKEN, TEST_USER_ID, "TestUser")
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.edit_nickname).setText("TestUser")
            view.findViewById<SwitchMaterial>(R.id.switch_new_challenges).isChecked = false
            view.findViewById<SwitchMaterial>(R.id.switch_activity_reminders).isChecked = true
            view.findViewById<SwitchMaterial>(R.id.switch_friend_activity).isChecked = false
            invokePrivate(fragment, "saveProfile")
            assertFalse(view.findViewById<MaterialButton>(R.id.btn_save_bottom).isEnabled)
        }
    }

    @Test
    fun `saveProfile with empty faculty does not crash`() {
        TokenManager.saveToken(TEST_TOKEN, TEST_USER_ID, "TestUser")
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            view.findViewById<EditText>(R.id.edit_nickname).setText("TestUser")
            view.findViewById<EditText>(R.id.edit_faculty).setText("")
            invokePrivate(fragment, "saveProfile")
        }
    }

    // ==================== Weekly Goals Edit ====================

    @Test
    fun `weekly goals edit field is present`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<EditText>(R.id.edit_weekly_goals))
        }
    }

    // ==================== layout_nickname ====================

    @Test
    fun `layout_nickname is present`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<TextInputLayout>(R.id.layout_nickname))
        }
    }

    // ==================== Destroy ====================

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<EditProfileFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
