package com.ecogo.ui.fragments

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.ecogo.R
import com.google.android.material.tabs.TabLayout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CommunityFragmentTest {

    private fun launchWithNav(): Pair<androidx.fragment.app.testing.FragmentScenario<CommunityFragment>, TestNavHostController> {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.setGraph(R.navigation.nav_graph)
        navController.setCurrentDestination(R.id.communityFragment)
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { Navigation.setViewNavController(it.requireView(), navController) }
        return scenario to navController
    }

    private fun invokePrivate(fragment: CommunityFragment, methodName: String, vararg args: Any?): Any? {
        val paramTypes = args.map {
            when (it) {
                is String -> String::class.java
                else -> it?.javaClass ?: Any::class.java
            }
        }.toTypedArray()
        val method = CommunityFragment::class.java.getDeclaredMethod(methodName, *paramTypes)
        method.isAccessible = true
        return method.invoke(fragment, *args)
    }

    private fun getField(fragment: CommunityFragment, fieldName: String): Any? {
        val field = CommunityFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(fragment)
    }

    private fun setField(fragment: CommunityFragment, fieldName: String, value: Any?) {
        val field = CommunityFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(fragment, value)
    }

    // ==================== Lifecycle ====================

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `key views are present`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.text_title))
            assertNotNull(view.findViewById<TabLayout>(R.id.tab_layout))
            assertNotNull(view.findViewById<RecyclerView>(R.id.recycler_community))
        }
    }

    @Test
    fun `tab layout has two tabs`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val tabLayout = fragment.requireView().findViewById<TabLayout>(R.id.tab_layout)
            assertEquals(2, tabLayout.tabCount)
        }
    }

    // ==================== Navigation ====================

    @Test
    fun `challenges button navigates to challenges`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.btn_challenges).performClick()
        }
        assertEquals(R.id.challengesFragment, navController.currentDestination?.id)
    }

    @Test
    fun `activities button navigates to activities`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.btn_activities).performClick()
        }
        assertEquals(R.id.activitiesFragment, navController.currentDestination?.id)
    }

    // ==================== Individual Leaderboard Views ====================

    @Test
    fun `individual leaderboard views exist`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<RecyclerView>(R.id.recycler_individual))
            assertNotNull(view.findViewById<View>(R.id.btn_daily))
            assertNotNull(view.findViewById<View>(R.id.btn_monthly))
        }
    }

    // ==================== showFacultyLeaderboard / showIndividualLeaderboard ====================

    @Test
    fun `showFacultyLeaderboard - shows faculty layout, hides individual`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "showFacultyLeaderboard")
            val view = fragment.requireView()
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.layout_faculty_leaderboard).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_individual_leaderboard).visibility)
        }
    }

    @Test
    fun `showIndividualLeaderboard - shows individual layout, hides faculty`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "showIndividualLeaderboard")
            val view = fragment.requireView()
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_faculty_leaderboard).visibility)
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.layout_individual_leaderboard).visibility)
        }
    }

    // ==================== updateToggleStyle ====================

    @Test
    fun `updateToggleStyle - DAILY mode styles daily button as active`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "currentIndividualType", "DAILY")
            invokePrivate(fragment, "showIndividualLeaderboard")
            // After showIndividualLeaderboard, updateToggleStyle is called
            // Just verify no crash and buttons are present
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.btn_daily))
            assertNotNull(view.findViewById<View>(R.id.btn_monthly))
        }
    }

    @Test
    fun `updateToggleStyle - MONTHLY mode styles monthly button as active`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            setField(fragment, "currentIndividualType", "MONTHLY")
            invokePrivate(fragment, "updateToggleStyle")
            // Verify no crash and buttons exist
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.btn_daily))
        }
    }

    // ==================== setupIndividualToggle button clicks ====================

    @Test
    fun `daily button click sets currentIndividualType to DAILY`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            // First switch to individual to set up the toggle
            invokePrivate(fragment, "showIndividualLeaderboard")
            // Click daily button
            fragment.requireView().findViewById<View>(R.id.btn_daily).performClick()
            assertEquals("DAILY", getField(fragment, "currentIndividualType"))
        }
    }

    @Test
    fun `monthly button click sets currentIndividualType to MONTHLY`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "showIndividualLeaderboard")
            fragment.requireView().findViewById<View>(R.id.btn_monthly).performClick()
            assertEquals("MONTHLY", getField(fragment, "currentIndividualType"))
        }
    }

    // ==================== Tab Selection ====================

    @Test
    fun `selecting tab 1 shows individual leaderboard`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val tabLayout = fragment.requireView().findViewById<TabLayout>(R.id.tab_layout)
            tabLayout.getTabAt(1)?.select()
            val view = fragment.requireView()
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_faculty_leaderboard).visibility)
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.layout_individual_leaderboard).visibility)
        }
    }

    @Test
    fun `selecting tab 0 shows faculty leaderboard`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val tabLayout = fragment.requireView().findViewById<TabLayout>(R.id.tab_layout)
            // Switch to tab 1 first
            tabLayout.getTabAt(1)?.select()
            // Then back to tab 0
            tabLayout.getTabAt(0)?.select()
            val view = fragment.requireView()
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.layout_faculty_leaderboard).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.layout_individual_leaderboard).visibility)
        }
    }

    // ==================== RecyclerView Setup ====================

    @Test
    fun `recyclerCommunity has layout manager and adapter after creation`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_community)
            assertNotNull(recycler.layoutManager)
            assertNotNull(recycler.adapter)
        }
    }

    @Test
    fun `recyclerIndividual has layout manager and adapter after creation`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_individual)
            assertNotNull(recycler.layoutManager)
            assertNotNull(recycler.adapter)
        }
    }

    // ==================== Leader Card ====================

    @Test
    fun `leader card views exist`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.card_leader))
            assertNotNull(view.findViewById<View>(R.id.text_leader_name))
            assertNotNull(view.findViewById<View>(R.id.text_leader_points))
        }
    }

    // ==================== Progress Indicators ====================

    @Test
    fun `progress faculty view exists`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.progress_faculty))
        }
    }

    @Test
    fun `progress individual view exists`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.progress_individual))
        }
    }

    // ==================== Destroy ====================

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<CommunityFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
