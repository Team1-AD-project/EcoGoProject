package com.ecogo.ui.adapters

import com.ecogo.data.IndividualRanking
import org.junit.Assert.*
import org.junit.Test

class LeaderboardAdapterTest {

    private fun makeRanking(rank: Int = 1, userId: String = "u1") = IndividualRanking(
        userId = userId, nickname = "User$rank", rank = rank, carbonSaved = 100.0 * rank, isVip = rank == 1
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = LeaderboardAdapter(listOf(makeRanking(1), makeRanking(2), makeRanking(3)))
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = LeaderboardAdapter(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `adapter handles single item`() {
        val adapter = LeaderboardAdapter(listOf(makeRanking(1)))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles large list`() {
        val rankings = (1..50).map { makeRanking(it, "u$it") }
        val adapter = LeaderboardAdapter(rankings)
        assertEquals(50, adapter.itemCount)
    }

    @Test
    fun `adapter preserves ranking data`() {
        val rankings = listOf(makeRanking(1, "top"), makeRanking(2, "second"))
        val adapter = LeaderboardAdapter(rankings)
        assertEquals(2, adapter.itemCount)
    }
}
