package com.ecogo.ui.adapters

import com.ecogo.data.FacultyData
import com.ecogo.data.Outfit
import org.junit.Assert.*
import org.junit.Test

class FacultyOutfitAdapterTest {

    private fun makeFaculty(id: String = "soc") = FacultyData(
        id = id, name = "Computing", color = "#3B82F6", slogan = "Code the future", outfit = Outfit()
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = FacultyOutfitAdapter(listOf(makeFaculty("soc"), makeFaculty("biz"))) {}
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = FacultyOutfitAdapter(emptyList()) {}
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `click callback is set`() {
        var clicked: FacultyData? = null
        val adapter = FacultyOutfitAdapter(listOf(makeFaculty())) { clicked = it }
        assertNull(clicked)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles multiple faculties`() {
        val faculties = (1..7).map { makeFaculty("f$it") }
        val adapter = FacultyOutfitAdapter(faculties) {}
        assertEquals(7, adapter.itemCount)
    }
}
