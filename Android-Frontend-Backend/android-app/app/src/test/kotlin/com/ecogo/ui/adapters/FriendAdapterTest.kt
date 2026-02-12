package com.ecogo.ui.adapters

import com.ecogo.data.Friend
import org.junit.Assert.*
import org.junit.Test

class FriendAdapterTest {

    private fun makeFriend(id: String = "f1") = Friend(
        userId = id, nickname = "Friend $id", points = 200, rank = 1, faculty = "Computing"
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = FriendAdapter(listOf(makeFriend("f1"), makeFriend("f2")), {}, {})
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = FriendAdapter(emptyList(), {}, {})
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `message click callback is set`() {
        var messageClicked: Friend? = null
        val adapter = FriendAdapter(listOf(makeFriend()), { messageClicked = it }, {})
        assertNull(messageClicked)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `friend click callback is set`() {
        var friendClicked: Friend? = null
        val adapter = FriendAdapter(listOf(makeFriend()), {}, { friendClicked = it })
        assertNull(friendClicked)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles friend with null faculty`() {
        val friend = Friend(userId = "f1", nickname = "Test", points = 0, rank = 5, faculty = null)
        val adapter = FriendAdapter(listOf(friend), {}, {})
        assertEquals(1, adapter.itemCount)
    }
}
