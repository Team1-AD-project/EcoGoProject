package com.ecogo.ui.adapters

import com.ecogo.data.FriendActivity
import org.junit.Assert.*
import org.junit.Test

class FriendActivityAdapterTest {

    private fun makeActivity(action: String = "joined_activity") = FriendActivity(
        friendId = "f1", friendName = "Alice", action = action,
        timestamp = "2h ago", details = "Joined Beach Cleanup"
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = FriendActivityAdapter(listOf(makeActivity(), makeActivity("earned_badge")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = FriendActivityAdapter(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `adapter handles joined_activity action`() {
        val adapter = FriendActivityAdapter(listOf(makeActivity("joined_activity")))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles earned_badge action`() {
        val adapter = FriendActivityAdapter(listOf(makeActivity("earned_badge")))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles completed_goal action`() {
        val adapter = FriendActivityAdapter(listOf(makeActivity("completed_goal")))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles unknown action`() {
        val adapter = FriendActivityAdapter(listOf(makeActivity("some_new_action")))
        assertEquals(1, adapter.itemCount)
    }
}
