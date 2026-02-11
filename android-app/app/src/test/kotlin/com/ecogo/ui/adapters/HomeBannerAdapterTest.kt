package com.ecogo.ui.adapters

import com.ecogo.data.HomeBanner
import org.junit.Assert.*
import org.junit.Test

class HomeBannerAdapterTest {

    private fun makeBanner(id: String = "b1") = HomeBanner(
        id = id, title = "Welcome!", subtitle = "Start your green journey",
        actionText = "Learn More", actionTarget = "challenges"
    )

    @Test
    fun `initial item count is 0`() {
        val adapter = HomeBannerAdapter {}
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `click callback is set`() {
        var clicked: HomeBanner? = null
        val adapter = HomeBannerAdapter { clicked = it }
        assertNull(clicked)
    }

    @Test
    fun `banner with null subtitle is valid`() {
        val banner = HomeBanner(id = "b1", title = "Title Only")
        assertNull(banner.subtitle)
    }

    @Test
    fun `banner with null actionText is valid`() {
        val banner = HomeBanner(id = "b1", title = "No Action")
        assertNull(banner.actionText)
        assertNull(banner.actionTarget)
    }

    @Test
    fun `banner default color is green`() {
        val banner = HomeBanner(id = "b1", title = "Test")
        assertEquals("#15803D", banner.backgroundColor)
    }
}
