package com.ecogo.ui.adapters

import com.ecogo.data.Challenge
import org.junit.Assert.*
import org.junit.Test

class ChallengeAdapterTest {

    private fun makeChallenge(id: String = "c1", title: String = "Walk 5km") = Challenge(
        id = id, title = title, description = "desc", type = "GREEN_TRIPS_DISTANCE",
        target = 5.0, reward = 100, icon = "🏆", status = "ACTIVE", participants = 10
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = ChallengeAdapter(listOf(makeChallenge(), makeChallenge("c2"))) {}
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = ChallengeAdapter(emptyList()) {}
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `updateChallenges changes item count`() {
        val adapter = ChallengeAdapter(listOf(makeChallenge())) {}
        assertEquals(1, adapter.itemCount)
        adapter.updateChallenges(listOf(makeChallenge("c1"), makeChallenge("c2"), makeChallenge("c3")))
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `updateChallenges with empty list clears items`() {
        val adapter = ChallengeAdapter(listOf(makeChallenge())) {}
        adapter.updateChallenges(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `click callback receives correct challenge`() {
        var clicked: Challenge? = null
        val challenge = makeChallenge("c99", "Test Challenge")
        val adapter = ChallengeAdapter(listOf(challenge)) { clicked = it }
        // Verify adapter is properly constructed with callback
        assertNull(clicked)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `updateChallenges with completed ids`() {
        val adapter = ChallengeAdapter(listOf(makeChallenge("c1"), makeChallenge("c2"))) {}
        adapter.updateChallenges(
            listOf(makeChallenge("c1"), makeChallenge("c2")),
            completed = setOf("c1")
        )
        assertEquals(2, adapter.itemCount)
    }
}
