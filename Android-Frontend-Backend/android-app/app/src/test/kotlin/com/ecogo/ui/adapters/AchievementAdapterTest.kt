package com.ecogo.ui.adapters

import com.ecogo.data.Achievement
import org.junit.Assert.*
import org.junit.Test

class AchievementAdapterTest {

    private fun makeAchievement(id: String = "a1", unlocked: Boolean = true) = Achievement(
        id = id, name = "Badge $id", description = "Desc", unlocked = unlocked
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = AchievementAdapter(listOf(makeAchievement("a1"), makeAchievement("a2")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = AchievementAdapter(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `adapter accepts equippedBadgeId`() {
        val adapter = AchievementAdapter(
            listOf(makeAchievement("a1")),
            equippedBadgeId = "a1"
        )
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter accepts onBadgeClick callback`() {
        var clickedId: String? = null
        val adapter = AchievementAdapter(
            listOf(makeAchievement("a1")),
            onBadgeClick = { clickedId = it }
        )
        assertNull(clickedId)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles mix of locked and unlocked`() {
        val adapter = AchievementAdapter(
            listOf(makeAchievement("a1", true), makeAchievement("a2", false), makeAchievement("a3", true))
        )
        assertEquals(3, adapter.itemCount)
    }
}
