package com.ecogo.ui.adapters

import com.ecogo.data.Challenge
import com.ecogo.data.UserChallengeProgress
import org.junit.Assert.*
import org.junit.Test

class MonthlyChallengeAdapterTest {

    private fun makeChallenge(id: String = "c1") = Challenge(
        id = id, title = "Walk Challenge", description = "Walk 10km",
        type = "GREEN_TRIPS_DISTANCE", target = 10.0, reward = 200, icon = "🏆"
    )

    private fun makeProgress(challengeId: String = "c1", current: Double = 5.0) = UserChallengeProgress(
        id = "p1", challengeId = challengeId, userId = "u1", status = "IN_PROGRESS",
        current = current, target = 10.0, progressPercent = current / 10.0 * 100,
        joinedAt = "2026-02-01T00:00:00"
    )

    private fun makeItem(id: String = "c1", withProgress: Boolean = false) = ChallengeWithProgress(
        challenge = makeChallenge(id),
        progress = if (withProgress) makeProgress(id) else null
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = MonthlyChallengeAdapter(listOf(makeItem("c1"), makeItem("c2"))) {}
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = MonthlyChallengeAdapter(emptyList()) {}
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `updateData changes item count`() {
        val adapter = MonthlyChallengeAdapter(listOf(makeItem())) {}
        adapter.updateData(listOf(makeItem("c1"), makeItem("c2"), makeItem("c3")))
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `updateData with empty list clears items`() {
        val adapter = MonthlyChallengeAdapter(listOf(makeItem())) {}
        adapter.updateData(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `click callback is set`() {
        var clicked: Challenge? = null
        val adapter = MonthlyChallengeAdapter(listOf(makeItem())) { clicked = it }
        assertNull(clicked)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles items with and without progress`() {
        val items = listOf(makeItem("c1", withProgress = true), makeItem("c2", withProgress = false))
        val adapter = MonthlyChallengeAdapter(items) {}
        assertEquals(2, adapter.itemCount)
    }
}
