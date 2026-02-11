package com.ecogo.ui.fragments

import android.graphics.Bitmap
import android.view.View
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import com.ecogo.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ShareImpactFragmentTest {

    private fun invokePrivate(fragment: ShareImpactFragment, methodName: String, vararg args: Any?): Any? {
        val paramTypes = args.map {
            when (it) {
                is String -> String::class.java
                else -> it?.javaClass ?: Any::class.java
            }
        }.toTypedArray()
        val method = ShareImpactFragment::class.java.getDeclaredMethod(methodName, *paramTypes)
        method.isAccessible = true
        return method.invoke(fragment, *args)
    }

    private fun getField(fragment: ShareImpactFragment, fieldName: String): Any? {
        val field = ShareImpactFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(fragment)
    }

    // ==================== Lifecycle ====================

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `key views are present`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.btn_back))
            assertNotNull(view.findViewById<View>(R.id.card_preview))
            assertNotNull(view.findViewById<View>(R.id.card_stats))
        }
    }

    @Test
    fun `period chips are present`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<Chip>(R.id.chip_today))
            assertNotNull(view.findViewById<Chip>(R.id.chip_week))
            assertNotNull(view.findViewById<Chip>(R.id.chip_month))
        }
    }

    @Test
    fun `stats text views are present`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.text_trips))
            assertNotNull(view.findViewById<View>(R.id.text_distance))
            assertNotNull(view.findViewById<View>(R.id.text_carbon_saved))
            assertNotNull(view.findViewById<View>(R.id.text_points))
        }
    }

    @Test
    fun `share and save buttons are present`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<MaterialButton>(R.id.btn_share))
            assertNotNull(view.findViewById<MaterialButton>(R.id.btn_save))
        }
    }

    // ==================== selectPeriod ====================

    @Test
    fun `selectPeriod - today sets selectedPeriod and chip state`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "selectPeriod", "today")
            assertEquals("today", getField(fragment, "selectedPeriod"))
            val view = fragment.requireView()
            assertTrue(view.findViewById<Chip>(R.id.chip_today).isChecked)
            assertFalse(view.findViewById<Chip>(R.id.chip_week).isChecked)
            assertFalse(view.findViewById<Chip>(R.id.chip_month).isChecked)
        }
    }

    @Test
    fun `selectPeriod - week sets selectedPeriod and chip state`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "selectPeriod", "week")
            assertEquals("week", getField(fragment, "selectedPeriod"))
            val view = fragment.requireView()
            assertFalse(view.findViewById<Chip>(R.id.chip_today).isChecked)
            assertTrue(view.findViewById<Chip>(R.id.chip_week).isChecked)
            assertFalse(view.findViewById<Chip>(R.id.chip_month).isChecked)
        }
    }

    @Test
    fun `selectPeriod - month sets selectedPeriod and chip state`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "selectPeriod", "month")
            assertEquals("month", getField(fragment, "selectedPeriod"))
            val view = fragment.requireView()
            assertFalse(view.findViewById<Chip>(R.id.chip_today).isChecked)
            assertFalse(view.findViewById<Chip>(R.id.chip_week).isChecked)
            assertTrue(view.findViewById<Chip>(R.id.chip_month).isChecked)
        }
    }

    // ==================== loadStatistics ====================

    @Test
    fun `loadStatistics - today shows today stats`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "selectPeriod", "today")
            val view = fragment.requireView()
            assertEquals("3", view.findViewById<TextView>(R.id.text_trips).text.toString())
            assertEquals("5.2", view.findViewById<TextView>(R.id.text_distance).text.toString())
            assertEquals("580", view.findViewById<TextView>(R.id.text_carbon_saved).text.toString())
            assertEquals("290", view.findViewById<TextView>(R.id.text_points).text.toString())
            assertTrue(view.findViewById<TextView>(R.id.text_period).text.toString().contains("今日"))
        }
    }

    @Test
    fun `loadStatistics - week shows week stats`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "selectPeriod", "week")
            val view = fragment.requireView()
            assertEquals("15", view.findViewById<TextView>(R.id.text_trips).text.toString())
            assertEquals("24.5", view.findViewById<TextView>(R.id.text_distance).text.toString())
            assertEquals("2,750", view.findViewById<TextView>(R.id.text_carbon_saved).text.toString())
            assertEquals("1,375", view.findViewById<TextView>(R.id.text_points).text.toString())
            assertTrue(view.findViewById<TextView>(R.id.text_period).text.toString().contains("本周"))
        }
    }

    @Test
    fun `loadStatistics - month shows month stats`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "selectPeriod", "month")
            val view = fragment.requireView()
            assertEquals("52", view.findViewById<TextView>(R.id.text_trips).text.toString())
            assertEquals("98.3", view.findViewById<TextView>(R.id.text_distance).text.toString())
            assertEquals("11,200", view.findViewById<TextView>(R.id.text_carbon_saved).text.toString())
            assertEquals("5,600", view.findViewById<TextView>(R.id.text_points).text.toString())
            assertTrue(view.findViewById<TextView>(R.id.text_period).text.toString().contains("本月"))
        }
    }

    // ==================== Chip Clicks ====================

    @Test
    fun `chip today click selects today period`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<Chip>(R.id.chip_today).performClick()
            assertEquals("today", getField(fragment, "selectedPeriod"))
        }
    }

    @Test
    fun `chip week click selects week period`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<Chip>(R.id.chip_week).performClick()
            assertEquals("week", getField(fragment, "selectedPeriod"))
        }
    }

    @Test
    fun `chip month click selects month period`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            fragment.requireView().findViewById<Chip>(R.id.chip_month).performClick()
            assertEquals("month", getField(fragment, "selectedPeriod"))
        }
    }

    // ==================== generateShareCard ====================

    @Test
    fun `generateShareCard - returns non-null bitmap`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val bitmap = invokePrivate(fragment, "generateShareCard") as Bitmap
            assertNotNull(bitmap)
            assertEquals(800, bitmap.width)
            assertEquals(600, bitmap.height)
        }
    }

    @Test
    fun `generateShareCard - bitmap after week period`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "selectPeriod", "week")
            val bitmap = invokePrivate(fragment, "generateShareCard") as Bitmap
            assertNotNull(bitmap)
            assertEquals(800, bitmap.width)
        }
    }

    @Test
    fun `generateShareCard - bitmap after month period`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "selectPeriod", "month")
            val bitmap = invokePrivate(fragment, "generateShareCard") as Bitmap
            assertNotNull(bitmap)
        }
    }

    // ==================== saveToGallery ====================

    @Test
    fun `saveToGallery - does not crash`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            invokePrivate(fragment, "saveToGallery")
            // Just verify no crash; shows a Toast
        }
    }

    // ==================== Initial State ====================

    @Test
    fun `initial selectedPeriod is today`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            assertEquals("today", getField(fragment, "selectedPeriod"))
        }
    }

    @Test
    fun `initial loadStatistics shows today data`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertEquals("3", view.findViewById<TextView>(R.id.text_trips).text.toString())
        }
    }

    // ==================== Destroy ====================

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<ShareImpactFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
