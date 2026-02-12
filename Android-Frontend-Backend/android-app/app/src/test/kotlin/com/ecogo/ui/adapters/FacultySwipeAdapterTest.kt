package com.ecogo.ui.adapters

import com.ecogo.data.FacultyData
import com.ecogo.data.Outfit
import org.junit.Assert.*
import org.junit.Test

class FacultySwipeAdapterTest {

    private fun makeFaculty(id: String = "soc") = FacultyData(
        id = id, name = "Computing", color = "#3B82F6", slogan = "Code the future", outfit = Outfit()
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = FacultySwipeAdapter(listOf(makeFaculty("soc"), makeFaculty("biz"))) {}
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = FacultySwipeAdapter(emptyList()) {}
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `click callback is set`() {
        var selected: FacultyData? = null
        val adapter = FacultySwipeAdapter(listOf(makeFaculty())) { selected = it }
        assertNull(selected)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles faculty with outfit items`() {
        val faculty = FacultyData(
            id = "eng", name = "Engineering", color = "#F59E0B",
            slogan = "Build", outfit = Outfit(head = "hat_helmet", body = "body_plaid")
        )
        val adapter = FacultySwipeAdapter(listOf(faculty)) {}
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles multiple faculties`() {
        val faculties = (1..5).map { makeFaculty("f$it") }
        val adapter = FacultySwipeAdapter(faculties) {}
        assertEquals(5, adapter.itemCount)
    }
}
