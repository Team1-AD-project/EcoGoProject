package com.ecogo.ui.fragments.navigation

import android.view.View
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LocationSearchFragmentTest {

    private val args = bundleOf("isSelectingOrigin" to true)

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<LocationSearchFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { assertNotNull(it.view) }
    }

    @Test
    fun `key views are present`() {
        val scenario = launchFragmentInContainer<LocationSearchFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.btn_back))
            assertNotNull(view.findViewById<EditText>(R.id.edit_search))
            assertNotNull(view.findViewById<RecyclerView>(R.id.recycler_locations))
        }
    }

    @Test
    fun `search field is editable`() {
        val scenario = launchFragmentInContainer<LocationSearchFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val editText = fragment.requireView().findViewById<EditText>(R.id.edit_search)
            assertTrue(editText.isEnabled)
            editText.setText("COM1")
            assertEquals("COM1", editText.text.toString())
        }
    }

    @Test
    fun `recycler has adapter`() {
        val scenario = launchFragmentInContainer<LocationSearchFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_locations)
            assertNotNull(recycler.adapter)
        }
    }

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<LocationSearchFragment>(fragmentArgs = args, themeResId = R.style.Theme_EcoGo)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
