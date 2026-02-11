package com.ecogo.ui.fragments

import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.google.android.material.button.MaterialButton
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ChatFragmentTest {

    // ==================== Lifecycle ====================

    @Test
    fun `fragment inflates view successfully`() {
        val scenario = launchFragmentInContainer<ChatFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.view)
        }
    }

    @Test
    fun `fragment view is not null after onViewCreated`() {
        val scenario = launchFragmentInContainer<ChatFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            assertNotNull(view.findViewById<View>(R.id.recycler_chat))
            assertNotNull(view.findViewById<View>(R.id.edit_message))
            assertNotNull(view.findViewById<View>(R.id.button_send))
        }
    }

    // ==================== Views Present ====================

    @Test
    fun `title text view is present`() {
        val scenario = launchFragmentInContainer<ChatFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val title = fragment.requireView().findViewById<TextView>(R.id.text_title)
            assertNotNull(title)
        }
    }

    @Test
    fun `input container is present`() {
        val scenario = launchFragmentInContainer<ChatFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            assertNotNull(fragment.requireView().findViewById<View>(R.id.input_container))
        }
    }

    @Test
    fun `send button is present`() {
        val scenario = launchFragmentInContainer<ChatFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val btn = fragment.requireView().findViewById<MaterialButton>(R.id.button_send)
            assertNotNull(btn)
            assertTrue(btn.isClickable)
        }
    }

    @Test
    fun `message input field is present and editable`() {
        val scenario = launchFragmentInContainer<ChatFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val editText = fragment.requireView().findViewById<EditText>(R.id.edit_message)
            assertNotNull(editText)
            assertTrue(editText.isEnabled)
        }
    }

    // ==================== RecyclerView Setup ====================

    @Test
    fun `recyclerView has adapter after setup`() {
        val scenario = launchFragmentInContainer<ChatFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_chat)
            assertNotNull(recycler.adapter)
        }
    }

    @Test
    fun `recyclerView has LinearLayoutManager`() {
        val scenario = launchFragmentInContainer<ChatFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_chat)
            assertNotNull(recycler.layoutManager)
            assertTrue(recycler.layoutManager is LinearLayoutManager)
        }
    }

    @Test
    fun `recyclerView has initial welcome message`() {
        val scenario = launchFragmentInContainer<ChatFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_chat)
            assertTrue("Adapter should have at least one welcome message", recycler.adapter!!.itemCount >= 1)
        }
    }

    // ==================== Input Handling ====================

    @Test
    fun `send button click with empty text does not add message`() {
        val scenario = launchFragmentInContainer<ChatFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val view = fragment.requireView()
            val recycler = view.findViewById<RecyclerView>(R.id.recycler_chat)
            val initialCount = recycler.adapter!!.itemCount

            // Ensure input is empty
            view.findViewById<EditText>(R.id.edit_message).setText("")
            view.findViewById<View>(R.id.button_send).performClick()

            assertEquals(initialCount, recycler.adapter!!.itemCount)
        }
    }

    @Test
    fun `typing text in edit field works`() {
        val scenario = launchFragmentInContainer<ChatFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.onFragment { fragment ->
            val editText = fragment.requireView().findViewById<EditText>(R.id.edit_message)
            editText.setText("Hello EcoGo")
            assertEquals("Hello EcoGo", editText.text.toString())
        }
    }

    // ==================== Fragment Destroy ====================

    @Test
    fun `fragment handles onDestroyView without crash`() {
        val scenario = launchFragmentInContainer<ChatFragment>(
            themeResId = R.style.Theme_EcoGo
        )
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
    }
}
