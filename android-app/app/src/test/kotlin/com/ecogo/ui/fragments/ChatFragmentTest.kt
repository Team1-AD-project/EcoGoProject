package com.ecogo.ui.fragments

import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.ecogo.data.AssistantMessage
import com.ecogo.data.ChatResponse
import com.ecogo.data.Citation
import com.ecogo.data.UiAction
import com.ecogo.ui.adapters.ChatMessageAdapter
import com.google.android.material.button.MaterialButton
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ChatFragmentTest {

    companion object {
        private const val NO_SOURCES = "No sources"
        private const val GREEN_TRAVEL_ADVICE = "绿色出行建议"
        private const val ROUTE_NUS_TO_CLEMENTI = "从NUS到Clementi"
        private const val TEST_DATE_TIME = "2025-01-15T10:30:00"
    }

    // ==================== Reflection helpers ====================

    private fun getPrivateMethod(name: String, vararg paramTypes: Class<*>): Method {
        val method = ChatFragment::class.java.getDeclaredMethod(name, *paramTypes)
        method.isAccessible = true
        return method
    }

    private fun getFieldValue(fragment: ChatFragment, fieldName: String): Any? {
        val field = ChatFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(fragment)
    }

    private fun setFieldValue(fragment: ChatFragment, fieldName: String, value: Any?) {
        val field = ChatFragment::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(fragment, value)
    }

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

    // ==================== Initial state ====================

    @Test
    fun `conversationId is null initially`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val conversationId = getFieldValue(fragment, "conversationId")
            assertNull(conversationId)
        }
    }

    @Test
    fun `adapter field is initialized after onViewCreated`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val adapter = getFieldValue(fragment, "adapter")
            assertNotNull(adapter)
            assertTrue(adapter is ChatMessageAdapter)
        }
    }

    // ==================== buildDisplayText ====================

    @Test
    fun `buildDisplayText with assistant text only`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("buildDisplayText", ChatResponse::class.java)
            val response = ChatResponse(
                assistant = AssistantMessage(text = "Hello world")
            )
            val result = method.invoke(fragment, response) as String
            assertEquals("Hello world", result)
        }
    }

    @Test
    fun `buildDisplayText with legacy reply field`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("buildDisplayText", ChatResponse::class.java)
            val response = ChatResponse(reply = "Legacy reply")
            val result = method.invoke(fragment, response) as String
            assertEquals("Legacy reply", result)
        }
    }

    @Test
    fun `buildDisplayText with empty response`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("buildDisplayText", ChatResponse::class.java)
            val response = ChatResponse()
            val result = method.invoke(fragment, response) as String
            assertEquals("", result)
        }
    }

    @Test
    fun `buildDisplayText with citations appends sources`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("buildDisplayText", ChatResponse::class.java)
            val response = ChatResponse(
                assistant = AssistantMessage(
                    text = "Answer text",
                    citations = listOf(
                        Citation(title = "Source 1", source = "url1", snippet = "s1"),
                        Citation(title = "Source 2", source = "url2", snippet = "s2")
                    )
                )
            )
            val result = method.invoke(fragment, response) as String
            assertTrue(result.contains("Answer text"))
            assertTrue(result.contains("📚 Sources:"))
            assertTrue(result.contains("• Source 1"))
            assertTrue(result.contains("• Source 2"))
        }
    }

    @Test
    fun `buildDisplayText with empty citations does not append sources`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("buildDisplayText", ChatResponse::class.java)
            val response = ChatResponse(
                assistant = AssistantMessage(text = NO_SOURCES, citations = emptyList())
            )
            val result = method.invoke(fragment, response) as String
            assertEquals(NO_SOURCES, result)
            assertFalse(result.contains("📚"))
        }
    }

    @Test
    fun `buildDisplayText with null citations does not append sources`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("buildDisplayText", ChatResponse::class.java)
            val response = ChatResponse(
                assistant = AssistantMessage(text = NO_SOURCES, citations = null)
            )
            val result = method.invoke(fragment, response) as String
            assertEquals(NO_SOURCES, result)
        }
    }

    // ==================== generateSmartReply ====================

    @Test
    fun `generateSmartReply with activity keyword returns activity reply`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("generateSmartReply", String::class.java)
            val result = method.invoke(fragment, "查看activity") as String
            assertTrue(result.contains("活动") || result.contains("跳转"))
        }
    }

    @Test
    fun `generateSmartReply with 活动 keyword returns activity reply`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("generateSmartReply", String::class.java)
            val result = method.invoke(fragment, "校园活动") as String
            assertTrue(result.contains("活动") || result.contains("跳转"))
        }
    }

    @Test
    fun `generateSmartReply with event keyword returns activity reply`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("generateSmartReply", String::class.java)
            val result = method.invoke(fragment, "upcoming event") as String
            assertTrue(result.contains("活动") || result.contains("跳转"))
        }
    }

    @Test
    fun `generateSmartReply with route keyword returns route reply`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("generateSmartReply", String::class.java)
            val result = method.invoke(fragment, "plan a route") as String
            assertTrue(result.contains("路线") || result.contains("出行"))
        }
    }

    @Test
    fun `generateSmartReply with 路线 keyword returns route reply`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("generateSmartReply", String::class.java)
            val result = method.invoke(fragment, "查看路线") as String
            assertTrue(result.contains("路线") || result.contains("出行"))
        }
    }

    @Test
    fun `generateSmartReply with 导航 keyword returns route reply`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("generateSmartReply", String::class.java)
            val result = method.invoke(fragment, "导航到学校") as String
            assertTrue(result.contains("路线") || result.contains("出行"))
        }
    }

    @Test
    fun `generateSmartReply with how to get keyword returns route reply`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("generateSmartReply", String::class.java)
            val result = method.invoke(fragment, "how to get to NUS") as String
            assertTrue(result.contains("路线") || result.contains("出行"))
        }
    }

    @Test
    fun `generateSmartReply with 去哪里 keyword returns route reply`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("generateSmartReply", String::class.java)
            val result = method.invoke(fragment, "去哪里吃饭") as String
            assertTrue(result.contains("路线") || result.contains("出行"))
        }
    }

    @Test
    fun `generateSmartReply with map keyword returns map reply`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("generateSmartReply", String::class.java)
            val result = method.invoke(fragment, "show me the map") as String
            assertTrue(result.contains("地图"))
        }
    }

    @Test
    fun `generateSmartReply with 地图 keyword returns map reply`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("generateSmartReply", String::class.java)
            val result = method.invoke(fragment, "打开地图") as String
            assertTrue(result.contains("地图"))
        }
    }

    @Test
    fun `generateSmartReply with 位置 keyword returns map reply`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("generateSmartReply", String::class.java)
            val result = method.invoke(fragment, "我的位置") as String
            assertTrue(result.contains("地图"))
        }
    }

    @Test
    fun `generateSmartReply with 推荐 keyword returns recommendation reply`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("generateSmartReply", String::class.java)
            val result = method.invoke(fragment, "推荐出行方式") as String
            assertTrue(result.contains(GREEN_TRAVEL_ADVICE))
        }
    }

    @Test
    fun `generateSmartReply with 建议 keyword returns recommendation reply`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("generateSmartReply", String::class.java)
            val result = method.invoke(fragment, "给我一些建议") as String
            assertTrue(result.contains(GREEN_TRAVEL_ADVICE))
        }
    }

    @Test
    fun `generateSmartReply with 怎么去 keyword returns recommendation reply`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("generateSmartReply", String::class.java)
            val result = method.invoke(fragment, "怎么去学校") as String
            assertTrue(result.contains(GREEN_TRAVEL_ADVICE))
        }
    }

    @Test
    fun `generateSmartReply with unknown keyword returns fallback reply`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("generateSmartReply", String::class.java)
            val result = method.invoke(fragment, "random gibberish xyz") as String
            assertTrue(result.contains("抱歉") || result.contains("服务器"))
        }
    }

    @Test
    fun `generateSmartReply is case insensitive`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("generateSmartReply", String::class.java)
            val result = method.invoke(fragment, "ACTIVITY") as String
            assertTrue(result.contains("活动") || result.contains("跳转"))
        }
    }

    // ==================== formatRouteForBackend ====================

    @Test
    fun `formatRouteForBackend with empty string returns empty`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("formatRouteForBackend", String::class.java)
            val result = method.invoke(fragment, "") as String
            assertEquals("", result)
        }
    }

    @Test
    fun `formatRouteForBackend with whitespace only returns empty`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("formatRouteForBackend", String::class.java)
            val result = method.invoke(fragment, "   ") as String
            assertEquals("", result)
        }
    }

    @Test
    fun `formatRouteForBackend preserves 从到 format`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("formatRouteForBackend", String::class.java)
            val result = method.invoke(fragment, ROUTE_NUS_TO_CLEMENTI) as String
            assertEquals(ROUTE_NUS_TO_CLEMENTI, result)
        }
    }

    @Test
    fun `formatRouteForBackend converts arrow to 从到`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("formatRouteForBackend", String::class.java)
            val result = method.invoke(fragment, "NUS->Clementi") as String
            assertEquals(ROUTE_NUS_TO_CLEMENTI, result)
        }
    }

    @Test
    fun `formatRouteForBackend converts unicode arrow to 从到`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("formatRouteForBackend", String::class.java)
            val result = method.invoke(fragment, "NUS→Clementi") as String
            assertEquals(ROUTE_NUS_TO_CLEMENTI, result)
        }
    }

    @Test
    fun `formatRouteForBackend converts dash to 从到`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("formatRouteForBackend", String::class.java)
            val result = method.invoke(fragment, "NUS-Clementi") as String
            assertEquals(ROUTE_NUS_TO_CLEMENTI, result)
        }
    }

    @Test
    fun `formatRouteForBackend converts em dash to 从到`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("formatRouteForBackend", String::class.java)
            val result = method.invoke(fragment, "NUS—Clementi") as String
            assertEquals(ROUTE_NUS_TO_CLEMENTI, result)
        }
    }

    @Test
    fun `formatRouteForBackend converts to keyword to 从到`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("formatRouteForBackend", String::class.java)
            val result = method.invoke(fragment, "NUS to Clementi") as String
            assertEquals(ROUTE_NUS_TO_CLEMENTI, result)
        }
    }

    @Test
    fun `formatRouteForBackend returns raw when no separator found`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("formatRouteForBackend", String::class.java)
            val result = method.invoke(fragment, "NUS") as String
            assertEquals("NUS", result)
        }
    }

    // ==================== normalizePassengersForBackend ====================

    @Test
    fun `normalizePassengersForBackend with empty string returns empty`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("normalizePassengersForBackend", String::class.java)
            val result = method.invoke(fragment, "") as String
            assertEquals("", result)
        }
    }

    @Test
    fun `normalizePassengersForBackend extracts digit and appends 人`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("normalizePassengersForBackend", String::class.java)
            val result = method.invoke(fragment, "3") as String
            assertEquals("3人", result)
        }
    }

    @Test
    fun `normalizePassengersForBackend extracts first digit from text`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("normalizePassengersForBackend", String::class.java)
            val result = method.invoke(fragment, "我有5个人") as String
            assertEquals("5人", result)
        }
    }

    @Test
    fun `normalizePassengersForBackend with no digit uses raw text`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("normalizePassengersForBackend", String::class.java)
            val result = method.invoke(fragment, "many") as String
            assertEquals("many人", result)
        }
    }

    @Test
    fun `normalizePassengersForBackend ignores digit 9`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("normalizePassengersForBackend", String::class.java)
            val result = method.invoke(fragment, "9人") as String
            // regex [1-8] won't match 9, so raw string used
            assertEquals("9人人", result)
        }
    }

    @Test
    fun `normalizePassengersForBackend boundary value 1`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("normalizePassengersForBackend", String::class.java)
            val result = method.invoke(fragment, "1") as String
            assertEquals("1人", result)
        }
    }

    @Test
    fun `normalizePassengersForBackend boundary value 8`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("normalizePassengersForBackend", String::class.java)
            val result = method.invoke(fragment, "8") as String
            assertEquals("8人", result)
        }
    }

    // ==================== normalizeDepartAtForBackend ====================

    @Test
    fun `normalizeDepartAtForBackend with empty string returns empty`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("normalizeDepartAtForBackend", String::class.java)
            val result = method.invoke(fragment, "") as String
            assertEquals("", result)
        }
    }

    @Test
    fun `normalizeDepartAtForBackend replaces slash with dash`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("normalizeDepartAtForBackend", String::class.java)
            val result = method.invoke(fragment, "2025/01/15 10:30") as String
            assertTrue(result.contains("-"))
            assertFalse(result.contains("/"))
        }
    }

    @Test
    fun `normalizeDepartAtForBackend replaces space with T`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("normalizeDepartAtForBackend", String::class.java)
            val result = method.invoke(fragment, "2025-01-15 10:30") as String
            assertTrue(result.contains("T"))
            assertEquals(TEST_DATE_TIME, result)
        }
    }

    @Test
    fun `normalizeDepartAtForBackend appends seconds when missing`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("normalizeDepartAtForBackend", String::class.java)
            val result = method.invoke(fragment, "2025-01-15T10:30") as String
            assertEquals(TEST_DATE_TIME, result)
        }
    }

    @Test
    fun `normalizeDepartAtForBackend does not append seconds when already present`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("normalizeDepartAtForBackend", String::class.java)
            val result = method.invoke(fragment, "2025-01-15T10:30:45") as String
            assertEquals("2025-01-15T10:30:45", result)
        }
    }

    @Test
    fun `normalizeDepartAtForBackend does not replace space with T when T already present`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("normalizeDepartAtForBackend", String::class.java)
            val result = method.invoke(fragment, TEST_DATE_TIME) as String
            assertEquals(TEST_DATE_TIME, result)
        }
    }

    @Test
    fun `normalizeDepartAtForBackend full slash format conversion`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("normalizeDepartAtForBackend", String::class.java)
            val result = method.invoke(fragment, "2025/06/20 14:00") as String
            assertEquals("2025-06-20T14:00:00", result)
        }
    }

    // ==================== handleUiActions ====================

    @Test
    fun `handleUiActions with null does nothing`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleUiActions", List::class.java)
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_chat)
            val countBefore = recycler.adapter!!.itemCount
            method.invoke(fragment, null)
            assertEquals(countBefore, recycler.adapter!!.itemCount)
        }
    }

    @Test
    fun `handleUiActions with empty list does nothing`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleUiActions", List::class.java)
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_chat)
            val countBefore = recycler.adapter!!.itemCount
            method.invoke(fragment, emptyList<UiAction>())
            assertEquals(countBefore, recycler.adapter!!.itemCount)
        }
    }

    @Test
    fun `handleUiActions SUGGESTIONS adds suggestion message`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleUiActions", List::class.java)
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_chat)
            val countBefore = recycler.adapter!!.itemCount

            val actions = listOf(
                UiAction(
                    type = "SUGGESTIONS",
                    payload = mapOf("options" to listOf("Option A", "Option B"))
                )
            )
            method.invoke(fragment, actions)
            assertEquals(countBefore + 1, recycler.adapter!!.itemCount)
        }
    }

    @Test
    fun `handleUiActions SUGGESTIONS with empty options does not add message`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleUiActions", List::class.java)
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_chat)
            val countBefore = recycler.adapter!!.itemCount

            val actions = listOf(
                UiAction(type = "SUGGESTIONS", payload = mapOf("options" to emptyList<String>()))
            )
            method.invoke(fragment, actions)
            assertEquals(countBefore, recycler.adapter!!.itemCount)
        }
    }

    @Test
    fun `handleUiActions BOOKING_CARD adds booking card message`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleUiActions", List::class.java)
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_chat)
            val countBefore = recycler.adapter!!.itemCount

            val actions = listOf(
                UiAction(
                    type = "BOOKING_CARD",
                    payload = mapOf(
                        "bookingId" to "b123",
                        "fromName" to "NUS",
                        "toName" to "Clementi",
                        "passengers" to 2,
                        "status" to "confirmed"
                    )
                )
            )
            method.invoke(fragment, actions)
            assertEquals(countBefore + 1, recycler.adapter!!.itemCount)
        }
    }

    @Test
    fun `handleUiActions BOOKING_CARD with null payload does not add`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleUiActions", List::class.java)
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_chat)
            val countBefore = recycler.adapter!!.itemCount

            val actions = listOf(UiAction(type = "BOOKING_CARD", payload = null))
            method.invoke(fragment, actions)
            assertEquals(countBefore, recycler.adapter!!.itemCount)
        }
    }

    @Test
    fun `handleUiActions BOOKING_CARD missing bookingId does not add`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleUiActions", List::class.java)
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_chat)
            val countBefore = recycler.adapter!!.itemCount

            val actions = listOf(
                UiAction(type = "BOOKING_CARD", payload = mapOf("fromName" to "NUS"))
            )
            method.invoke(fragment, actions)
            assertEquals(countBefore, recycler.adapter!!.itemCount)
        }
    }

    @Test
    fun `handleUiActions DEEPLINK with booking url does not crash`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleUiActions", List::class.java)
            val actions = listOf(
                UiAction(
                    type = "DEEPLINK",
                    payload = mapOf("url" to "ecogo://booking/abc123")
                )
            )
            // Should not crash - navigation may fail but that's OK
            method.invoke(fragment, actions)
        }
    }

    @Test
    fun `handleUiActions DEEPLINK with null url does nothing`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleUiActions", List::class.java)
            val actions = listOf(
                UiAction(type = "DEEPLINK", payload = mapOf("other" to "value"))
            )
            method.invoke(fragment, actions)
        }
    }

    @Test
    fun `handleUiActions SHOW_CONFIRM with null payload does nothing`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleUiActions", List::class.java)
            val actions = listOf(UiAction(type = "SHOW_CONFIRM", payload = null))
            method.invoke(fragment, actions)
        }
    }

    @Test
    fun `handleUiActions SHOW_FORM with null payload does nothing`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleUiActions", List::class.java)
            val actions = listOf(UiAction(type = "SHOW_FORM", payload = null))
            method.invoke(fragment, actions)
        }
    }

    // ==================== handleBookingCard ====================

    @Test
    fun `handleBookingCard with valid payload adds message`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleBookingCard", Map::class.java)
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_chat)
            val countBefore = recycler.adapter!!.itemCount

            val payload = mapOf<String, Any>(
                "bookingId" to "b999",
                "fromName" to "Botanic Gardens",
                "toName" to "Marina Bay",
                "departAt" to "2025-06-20T10:00:00",
                "passengers" to 3,
                "status" to "pending",
                "tripId" to "t555"
            )
            method.invoke(fragment, payload)
            assertEquals(countBefore + 1, recycler.adapter!!.itemCount)
        }
    }

    @Test
    fun `handleBookingCard with null payload does nothing`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleBookingCard", Map::class.java)
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_chat)
            val countBefore = recycler.adapter!!.itemCount
            method.invoke(fragment, null as Map<String, Any>?)
            assertEquals(countBefore, recycler.adapter!!.itemCount)
        }
    }

    @Test
    fun `handleBookingCard without bookingId does nothing`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleBookingCard", Map::class.java)
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_chat)
            val countBefore = recycler.adapter!!.itemCount
            method.invoke(fragment, mapOf<String, Any>("fromName" to "X"))
            assertEquals(countBefore, recycler.adapter!!.itemCount)
        }
    }

    @Test
    fun `handleBookingCard with minimal payload uses defaults`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleBookingCard", Map::class.java)
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_chat)
            val countBefore = recycler.adapter!!.itemCount

            // Only bookingId provided - others should use defaults
            val payload = mapOf<String, Any>("bookingId" to "b001")
            method.invoke(fragment, payload)
            assertEquals(countBefore + 1, recycler.adapter!!.itemCount)
        }
    }

    // ==================== handleDeeplink ====================

    @Test
    fun `handleDeeplink with booking url does not crash`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleDeeplink", String::class.java)
            // Won't navigate (no nav controller set up for this test), but should not crash
            method.invoke(fragment, "ecogo://booking/book123")
        }
    }

    @Test
    fun `handleDeeplink with trip url does not crash`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleDeeplink", String::class.java)
            method.invoke(fragment, "ecogo://trip/trip456")
        }
    }

    @Test
    fun `handleDeeplink with unknown url does nothing`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleDeeplink", String::class.java)
            method.invoke(fragment, "ecogo://unknown/xyz")
        }
    }

    @Test
    fun `handleDeeplink with regular url does nothing`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleDeeplink", String::class.java)
            method.invoke(fragment, "https://example.com")
        }
    }

    // ==================== handleShowConfirm ====================

    @Test
    fun `handleShowConfirm with null payload does nothing`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleShowConfirm", Map::class.java)
            method.invoke(fragment, null as Map<String, Any>?)
        }
    }

    @Test
    fun `handleShowConfirm with valid payload shows dialog without crash`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleShowConfirm", Map::class.java)
            val payload = mapOf<String, Any>(
                "title" to "Confirm Booking",
                "body" to "Do you confirm this booking?"
            )
            method.invoke(fragment, payload)
        }
    }

    @Test
    fun `handleShowConfirm with defaults shows dialog without crash`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleShowConfirm", Map::class.java)
            // Missing title and body should use defaults
            val payload = mapOf<String, Any>("other" to "value")
            method.invoke(fragment, payload)
        }
    }

    // ==================== handleShowForm ====================

    @Test
    fun `handleShowForm with null payload does nothing`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleShowForm", Map::class.java)
            method.invoke(fragment, null as Map<String, Any>?)
        }
    }

    @Test
    fun `handleShowForm with valid fields shows dialog without crash`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleShowForm", Map::class.java)
            val payload = mapOf<String, Any>(
                "title" to "Booking Form",
                "fields" to listOf(
                    mapOf("key" to "route", "label" to "Route"),
                    mapOf("key" to "passengers", "label" to "Passengers"),
                    mapOf("key" to "departAt", "label" to "Departure Time")
                )
            )
            method.invoke(fragment, payload)
        }
    }

    @Test
    fun `handleShowForm with missing fields returns early`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleShowForm", Map::class.java)
            // No "fields" key, only title
            val payload = mapOf<String, Any>("title" to "Test")
            method.invoke(fragment, payload)
        }
    }

    @Test
    fun `handleShowForm with field missing key skips that field`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("handleShowForm", Map::class.java)
            val payload = mapOf<String, Any>(
                "title" to "Test",
                "fields" to listOf(
                    mapOf("label" to "No Key Field"),  // missing "key"
                    mapOf("key" to "valid", "label" to "Valid Field")
                )
            )
            method.invoke(fragment, payload)
        }
    }

    // ==================== sendMessage integration ====================

    @Test
    fun `sendMessage adds user message to adapter`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val recycler = fragment.requireView().findViewById<RecyclerView>(R.id.recycler_chat)
            val countBefore = recycler.adapter!!.itemCount

            val method = getPrivateMethod("sendMessage", String::class.java)
            method.invoke(fragment, "Hello")

            // At minimum the user message should be added (the bot response is async)
            assertTrue(recycler.adapter!!.itemCount > countBefore)
        }
    }

    @Test
    fun `sendMessage clears edit text`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val editText = fragment.requireView().findViewById<EditText>(R.id.edit_message)
            editText.setText("test message")

            val method = getPrivateMethod("sendMessage", String::class.java)
            method.invoke(fragment, "test message")

            assertEquals("", editText.text.toString())
        }
    }

    // ==================== navigateToRoutePlanner ====================

    @Test
    fun `navigateToRoutePlanner does not crash even without nav controller`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("navigateToRoutePlanner", ChatMessageAdapter.BookingCardData::class.java)
            val card = ChatMessageAdapter.BookingCardData(
                bookingId = "b100",
                tripId = "t200",
                fromName = "A",
                toName = "B",
                departAt = "2025-01-01T10:00:00",
                passengers = 1,
                status = "confirmed"
            )
            // Should catch the exception internally
            method.invoke(fragment, card)
        }
    }

    @Test
    fun `navigateToRoutePlanner with null optional fields does not crash`() {
        val scenario = launchFragmentInContainer<ChatFragment>(themeResId = R.style.Theme_EcoGo)
        scenario.onFragment { fragment ->
            val method = getPrivateMethod("navigateToRoutePlanner", ChatMessageAdapter.BookingCardData::class.java)
            val card = ChatMessageAdapter.BookingCardData(
                bookingId = "b100",
                tripId = null,
                fromName = "A",
                toName = "B",
                departAt = null,
                passengers = 2,
                status = "pending"
            )
            method.invoke(fragment, card)
        }
    }
}
