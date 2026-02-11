package com.ecogo.ui.adapters

import org.junit.Assert.*
import org.junit.Test

class ChatMessageAdapterTest {

    private fun userMsg(text: String = "Hello") =
        ChatMessageAdapter.ChatMessage(text = text, isUser = true)

    private fun aiMsg(text: String = "Hi there") =
        ChatMessageAdapter.ChatMessage(text = text, isUser = false)

    private fun suggestionsMsg() =
        ChatMessageAdapter.ChatMessage(text = "", isUser = false, suggestions = listOf("Option A", "Option B"))

    private fun bookingMsg() = ChatMessageAdapter.ChatMessage(
        text = "", isUser = false,
        bookingCard = ChatMessageAdapter.BookingCardData(
            bookingId = "b1", fromName = "NUS", toName = "Orchard",
            departAt = "10:00", passengers = 2, status = "confirmed"
        )
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = ChatMessageAdapter(mutableListOf(userMsg(), aiMsg()))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = ChatMessageAdapter(mutableListOf())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `getItemViewType returns USER for user message`() {
        val adapter = ChatMessageAdapter(mutableListOf(userMsg()))
        assertEquals(ChatMessageAdapter.VIEW_TYPE_USER, adapter.getItemViewType(0))
    }

    @Test
    fun `getItemViewType returns AI for ai message`() {
        val adapter = ChatMessageAdapter(mutableListOf(aiMsg()))
        assertEquals(ChatMessageAdapter.VIEW_TYPE_AI, adapter.getItemViewType(0))
    }

    @Test
    fun `getItemViewType returns SUGGESTIONS for suggestions message`() {
        val adapter = ChatMessageAdapter(mutableListOf(suggestionsMsg()))
        assertEquals(ChatMessageAdapter.VIEW_TYPE_SUGGESTIONS, adapter.getItemViewType(0))
    }

    @Test
    fun `getItemViewType returns BOOKING_CARD for booking message`() {
        val adapter = ChatMessageAdapter(mutableListOf(bookingMsg()))
        assertEquals(ChatMessageAdapter.VIEW_TYPE_BOOKING_CARD, adapter.getItemViewType(0))
    }

    @Test
    fun `addMessage increases item count`() {
        val adapter = ChatMessageAdapter(mutableListOf(userMsg()))
        assertEquals(1, adapter.itemCount)
        adapter.addMessage(aiMsg())
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `mixed message types have correct view types`() {
        val messages = mutableListOf(userMsg(), aiMsg(), suggestionsMsg(), bookingMsg())
        val adapter = ChatMessageAdapter(messages)
        assertEquals(4, adapter.itemCount)
        assertEquals(ChatMessageAdapter.VIEW_TYPE_USER, adapter.getItemViewType(0))
        assertEquals(ChatMessageAdapter.VIEW_TYPE_AI, adapter.getItemViewType(1))
        assertEquals(ChatMessageAdapter.VIEW_TYPE_SUGGESTIONS, adapter.getItemViewType(2))
        assertEquals(ChatMessageAdapter.VIEW_TYPE_BOOKING_CARD, adapter.getItemViewType(3))
    }
}
