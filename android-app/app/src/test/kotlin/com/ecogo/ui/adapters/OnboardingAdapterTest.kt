package com.ecogo.ui.adapters

import org.junit.Assert.*
import org.junit.Test

class OnboardingAdapterTest {

    @Test
    fun `getItemCount returns 5 hardcoded pages`() {
        val adapter = OnboardingAdapter()
        assertEquals(5, adapter.itemCount)
    }

    @Test
    fun `adapter is not empty`() {
        val adapter = OnboardingAdapter()
        assertTrue(adapter.itemCount > 0)
    }

    @Test
    fun `item count is always 5`() {
        // OnboardingAdapter has hardcoded pages, so count is always 5
        val adapter1 = OnboardingAdapter()
        val adapter2 = OnboardingAdapter()
        assertEquals(adapter1.itemCount, adapter2.itemCount)
    }

    @Test
    fun `OnboardingPage data class holds correct values`() {
        val page = OnboardingAdapter.OnboardingPage(0, "Title", "Description")
        assertEquals("Title", page.title)
        assertEquals("Description", page.description)
        assertEquals(0, page.iconRes)
    }
}
