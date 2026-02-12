package com.ecogo.ui.fragments

import android.view.View
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.ecogo.R
import com.ecogo.mapengine.ui.map.MapActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HomeFragmentTest {

    private val STATUS_ON_TIME = "On Time"

    /**
     * Helper: launch HomeFragment and attach a TestNavHostController (no Mockito needed)
     */
    private fun launchWithNav(): Pair<androidx.fragment.app.testing.FragmentScenario<HomeFragment>, TestNavHostController> {
        val navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.setGraph(R.navigation.nav_graph)
        navController.setCurrentDestination(R.id.homeFragment)

        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), navController)
        }
        return scenario to navController
    }

    private fun invokePrivate(fragment: HomeFragment, methodName: String, vararg args: Any?): Any? {
        val paramTypes = args.map {
            when (it) {
                is Int -> Int::class.java
                is String -> String::class.java
                else -> it?.javaClass ?: Any::class.java
            }
        }.toTypedArray()
        val method = HomeFragment::class.java.getDeclaredMethod(methodName, *paramTypes)
        method.isAccessible = true
        return method.invoke(fragment, *args)
    }

    // ==================== Lifecycle ====================

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.view)
        }
    }

    @Test
    fun `fragment view is not null after onViewCreated`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.text_welcome))
            assertNotNull(fragment.requireView().findViewById<View>(R.id.text_bus_number))
        }
    }

    // ==================== Initial UI Setup ====================

    @Test
    fun `setupUI sets bus info text`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            val busNumber = view.findViewById<TextView>(R.id.text_bus_number).text.toString()
            val busTime = view.findViewById<TextView>(R.id.text_bus_time).text.toString()
            val busRoute = view.findViewById<TextView>(R.id.text_bus_route).text.toString()
            assertTrue("Bus number should not be empty", busNumber.isNotEmpty())
            assertTrue("Bus time should not be empty", busTime.isNotEmpty())
            assertTrue("Bus route should not be empty", busRoute.isNotEmpty())
        }
    }

    @Test
    fun `setupUI sets monthly points and SoC score views`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            val monthlyPoints = view.findViewById<TextView>(R.id.text_monthly_points).text.toString()
            val socScore = view.findViewById<TextView>(R.id.text_soc_score).text.toString()
            assertTrue("Monthly points should not be empty", monthlyPoints.isNotEmpty())
            assertTrue("SoC score should not be empty", socScore.isNotEmpty())
        }
    }

    @Test
    fun `setupUI sets location text with today date`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            val locationText = view.findViewById<TextView>(R.id.text_location).text.toString()
            assertTrue(locationText.contains(java.time.LocalDate.now().year.toString()))
        }
    }

    // ==================== RecyclerView Setup ====================

    @Test
    fun `recyclerHighlights is initialized with adapter`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_highlights)
            assertNotNull(recycler.adapter)
            assertNotNull(recycler.layoutManager)
        }
    }

    @Test
    fun `recyclerActivities is initialized with adapter`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_activities)
            assertNotNull(recycler.adapter)
            assertNotNull(recycler.layoutManager)
        }
    }

    // ==================== Notification ====================

    @Test
    fun `close notification button hides notification card`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            val closeButton = view.findViewById<View>(R.id.button_close_notification)
            val card = view.findViewById<View>(R.id.card_notification)

            closeButton.performClick()
            assertEquals(View.GONE, card.visibility)
        }
    }

    // ==================== Navigation Clicks ====================

    @Test
    fun `cardNextBus click navigates to routesFragment`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_next_bus).performClick()
        }
        assertEquals(R.id.routesFragment, navController.currentDestination?.id)
    }

    @Test
    fun `cardMonthlyPoints click navigates to profileFragment`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_monthly_points).performClick()
        }
        assertEquals(R.id.profileFragment, navController.currentDestination?.id)
    }

    @Test
    fun `cardCommunityScore click navigates to communityFragment`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_community_score).performClick()
        }
        assertEquals(R.id.communityFragment, navController.currentDestination?.id)
    }

    @Test
    fun `cardCarbonFootprint click navigates to profileFragment`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_carbon_footprint).performClick()
        }
        assertEquals(R.id.profileFragment, navController.currentDestination?.id)
    }

    @Test
    fun `cardDailyGoal click navigates to profileFragment`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_daily_goal).performClick()
        }
        assertEquals(R.id.profileFragment, navController.currentDestination?.id)
    }

    @Test
    fun `cardVoucherShortcut click navigates to voucher`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_voucher_shortcut).performClick()
        }
        assertEquals(R.id.voucherFragment, navController.currentDestination?.id)
    }

    @Test
    fun `cardChallengesShortcut click navigates to challenges`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_challenges_shortcut).performClick()
        }
        assertEquals(R.id.challengesFragment, navController.currentDestination?.id)
    }

    @Test
    fun `textViewAll click navigates to monthlyHighlights`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.text_view_all).performClick()
        }
        assertEquals(R.id.monthlyHighlightsFragment, navController.currentDestination?.id)
    }

    @Test
    fun `textViewAllActivities click navigates to activities`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.text_view_all_activities).performClick()
        }
        assertEquals(R.id.activitiesFragment, navController.currentDestination?.id)
    }

    @Test
    fun `mascotAvatar click navigates to profileFragment`() {
        val (scenario, navController) = launchWithNav()
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.mascot_avatar).performClick()
        }
        assertEquals(R.id.profileFragment, navController.currentDestination?.id)
    }

    // ==================== MapActivity Intent ====================

    @Test
    fun `buttonOpenMap click starts MapActivity`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.button_open_map).performClick()

            val shadowActivity = Shadows.shadowOf(fragment.requireActivity())
            val intent = shadowActivity.nextStartedActivity
            assertNotNull(intent)
            assertEquals(MapActivity::class.java.name, intent.component?.className)
        }
    }

    @Test
    fun `cardMap click starts MapActivity`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<View>(R.id.card_map).performClick()

            val shadowActivity = Shadows.shadowOf(fragment.requireActivity())
            val intent = shadowActivity.nextStartedActivity
            assertNotNull(intent)
            assertEquals(MapActivity::class.java.name, intent.component?.className)
        }
    }

    // ==================== getFacultyAbbreviation ====================

    @Test
    fun `getFacultyAbbreviation - School of Computing returns SoC`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("SoC", invokePrivate(fragment, "getFacultyAbbreviation", "School of Computing"))
        }
    }

    @Test
    fun `getFacultyAbbreviation - Faculty of Science returns FoS`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("FoS", invokePrivate(fragment, "getFacultyAbbreviation", "Faculty of Science"))
        }
    }

    @Test
    fun `getFacultyAbbreviation - Faculty of Engineering returns FoE`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("FoE", invokePrivate(fragment, "getFacultyAbbreviation", "Faculty of Engineering"))
        }
    }

    @Test
    fun `getFacultyAbbreviation - Business School returns Biz`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("Biz", invokePrivate(fragment, "getFacultyAbbreviation", "Business School"))
        }
    }

    @Test
    fun `getFacultyAbbreviation - SDE returns SDE`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("SDE", invokePrivate(fragment, "getFacultyAbbreviation", "School of Design and Environment"))
        }
    }

    @Test
    fun `getFacultyAbbreviation - FASS returns FASS`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("FASS", invokePrivate(fragment, "getFacultyAbbreviation", "Faculty of Arts and Social Sciences"))
        }
    }

    @Test
    fun `getFacultyAbbreviation - YLLSoM returns YLLSoM`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("YLLSoM", invokePrivate(fragment, "getFacultyAbbreviation", "Yong Loo Lin School of Medicine"))
        }
    }

    @Test
    fun `getFacultyAbbreviation - SSHSPH returns SSHSPH`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("SSHSPH", invokePrivate(fragment, "getFacultyAbbreviation", "Saw Swee Hock School of Public Health"))
        }
    }

    @Test
    fun `getFacultyAbbreviation - Faculty of Law returns Law`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("Law", invokePrivate(fragment, "getFacultyAbbreviation", "Faculty of Law"))
        }
    }

    @Test
    fun `getFacultyAbbreviation - School of Music returns Music`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("Music", invokePrivate(fragment, "getFacultyAbbreviation", "School of Music"))
        }
    }

    @Test
    fun `getFacultyAbbreviation - unknown uses initials fallback`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val result = invokePrivate(fragment, "getFacultyAbbreviation", "College of Humanities and Sciences") as String
            assertEquals("CHS", result)
        }
    }

    // ==================== statusFromEta ====================

    @Test
    fun `statusFromEta - negative eta returns On Time`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(STATUS_ON_TIME, invokePrivate(fragment, "statusFromEta", -1))
        }
    }

    @Test
    fun `statusFromEta - eta 0 returns Arriving`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("Arriving", invokePrivate(fragment, "statusFromEta", 0))
        }
    }

    @Test
    fun `statusFromEta - eta 2 returns Arriving`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("Arriving", invokePrivate(fragment, "statusFromEta", 2))
        }
    }

    @Test
    fun `statusFromEta - eta 5 returns On Time`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(STATUS_ON_TIME, invokePrivate(fragment, "statusFromEta", 5))
        }
    }

    @Test
    fun `statusFromEta - eta 8 returns On Time`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(STATUS_ON_TIME, invokePrivate(fragment, "statusFromEta", 8))
        }
    }

    @Test
    fun `statusFromEta - eta 15 returns Delayed`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("Delayed", invokePrivate(fragment, "statusFromEta", 15))
        }
    }

    @Test
    fun `statusFromEta - eta 30 returns Delayed`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("Delayed", invokePrivate(fragment, "statusFromEta", 30))
        }
    }

    @Test
    fun `statusFromEta - eta 31 returns Scheduled`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("Scheduled", invokePrivate(fragment, "statusFromEta", 31))
        }
    }

    // ==================== getWeatherIcon ====================

    @Test
    fun `getWeatherIcon - rain returns rain icon`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(R.drawable.ic_weather_rain, invokePrivate(fragment, "getWeatherIcon", "Heavy Rain"))
        }
    }

    @Test
    fun `getWeatherIcon - shower returns rain icon`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(R.drawable.ic_weather_rain, invokePrivate(fragment, "getWeatherIcon", "Light Shower"))
        }
    }

    @Test
    fun `getWeatherIcon - drizzle returns rain icon`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(R.drawable.ic_weather_rain, invokePrivate(fragment, "getWeatherIcon", "Drizzle"))
        }
    }

    @Test
    fun `getWeatherIcon - thunder returns rain icon`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(R.drawable.ic_weather_rain, invokePrivate(fragment, "getWeatherIcon", "Thunderstorm"))
        }
    }

    @Test
    fun `getWeatherIcon - storm returns rain icon`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(R.drawable.ic_weather_rain, invokePrivate(fragment, "getWeatherIcon", "Tropical Storm"))
        }
    }

    @Test
    fun `getWeatherIcon - cloud returns cloudy icon`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(R.drawable.ic_weather_cloudy, invokePrivate(fragment, "getWeatherIcon", "Partly Cloudy"))
        }
    }

    @Test
    fun `getWeatherIcon - overcast returns cloudy icon`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(R.drawable.ic_weather_cloudy, invokePrivate(fragment, "getWeatherIcon", "Overcast"))
        }
    }

    @Test
    fun `getWeatherIcon - fog returns cloudy icon`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(R.drawable.ic_weather_cloudy, invokePrivate(fragment, "getWeatherIcon", "Dense Fog"))
        }
    }

    @Test
    fun `getWeatherIcon - mist returns cloudy icon`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(R.drawable.ic_weather_cloudy, invokePrivate(fragment, "getWeatherIcon", "Light Mist"))
        }
    }

    @Test
    fun `getWeatherIcon - haze returns cloudy icon`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(R.drawable.ic_weather_cloudy, invokePrivate(fragment, "getWeatherIcon", "Haze"))
        }
    }

    @Test
    fun `getWeatherIcon - sunny returns sunny icon`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(R.drawable.ic_weather_sunny, invokePrivate(fragment, "getWeatherIcon", "Sunny"))
        }
    }

    @Test
    fun `getWeatherIcon - clear returns sunny icon`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(R.drawable.ic_weather_sunny, invokePrivate(fragment, "getWeatherIcon", "Clear sky"))
        }
    }

    @Test
    fun `getWeatherIcon - unknown returns cloudy default`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals(R.drawable.ic_weather_cloudy, invokePrivate(fragment, "getWeatherIcon", "Something Unknown"))
        }
    }

    // ==================== churnToastMessage ====================

    @Test
    fun `churnToastMessage - LOW returns stable message`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val msg = invokePrivate(fragment, "churnToastMessage", "LOW") as String
            assertTrue(msg.contains("状态稳定"))
        }
    }

    @Test
    fun `churnToastMessage - MEDIUM returns challenge message`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val msg = invokePrivate(fragment, "churnToastMessage", "MEDIUM") as String
            assertTrue(msg.contains("小挑战"))
        }
    }

    @Test
    fun `churnToastMessage - HIGH returns comeback message`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val msg = invokePrivate(fragment, "churnToastMessage", "HIGH") as String
            assertTrue(msg.contains("不太活跃"))
        }
    }

    @Test
    fun `churnToastMessage - INSUFFICIENT_DATA returns info message`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val msg = invokePrivate(fragment, "churnToastMessage", "INSUFFICIENT_DATA") as String
            assertTrue(msg.contains("更精准"))
        }
    }

    @Test
    fun `churnToastMessage - null returns welcome message`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = HomeFragment::class.java.getDeclaredMethod("churnToastMessage", String::class.java)
            method.isAccessible = true
            val msg = method.invoke(fragment, null as String?) as String
            assertTrue(msg.contains("欢迎回来"))
        }
    }

    @Test
    fun `churnToastMessage - unknown returns welcome message`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val msg = invokePrivate(fragment, "churnToastMessage", "UNKNOWN") as String
            assertTrue(msg.contains("欢迎回来"))
        }
    }

    // ==================== shouldShowChurnToastToday ====================

    @Test
    fun `shouldShowChurnToastToday - returns true first time`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            // Clear the pref first
            val sp = fragment.requireContext().getSharedPreferences("ecogo_pref", android.content.Context.MODE_PRIVATE)
            sp.edit().remove("churn_toast_last_date").apply()
            val result = invokePrivate(fragment, "shouldShowChurnToastToday") as Boolean
            assertTrue(result)
        }
    }

    @Test
    fun `shouldShowChurnToastToday - returns false on second call same day`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            // Clear the pref and call once
            val sp = fragment.requireContext().getSharedPreferences("ecogo_pref", android.content.Context.MODE_PRIVATE)
            sp.edit().remove("churn_toast_last_date").apply()
            invokePrivate(fragment, "shouldShowChurnToastToday")
            // Second call should return false
            val result = invokePrivate(fragment, "shouldShowChurnToastToday") as Boolean
            assertFalse(result)
        }
    }

    // ==================== loadCarbonFootprint ====================

    @Test
    fun `loadCarbonFootprint - does nothing without crash`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            // This method is empty (deprecated), just ensure no crash
            invokePrivate(fragment, "loadCarbonFootprint")
        }
    }

    // ==================== getPreferredStopForHome ====================

    @Test
    fun `getPreferredStopForHome - returns defaults when no prefs set`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val sp = fragment.requireContext().getSharedPreferences("nextbus_pref", android.content.Context.MODE_PRIVATE)
            sp.edit().clear().apply()
            @Suppress("UNCHECKED_CAST")
            val result = invokePrivate(fragment, "getPreferredStopForHome") as Pair<String, String>
            assertEquals("UTOWN", result.first)
            assertEquals("University Town (UTown)", result.second)
        }
    }

    @Test
    fun `getPreferredStopForHome - returns saved prefs`() {
        val scenario = launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val sp = fragment.requireContext().getSharedPreferences("nextbus_pref", android.content.Context.MODE_PRIVATE)
            sp.edit().putString("stop_code", "KENT").putString("stop_label", "Kent Ridge").apply()
            @Suppress("UNCHECKED_CAST")
            val result = invokePrivate(fragment, "getPreferredStopForHome") as Pair<String, String>
            assertEquals("KENT", result.first)
            assertEquals("Kent Ridge", result.second)
        }
    }

    // ==================== Fragment Destroy ====================

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
