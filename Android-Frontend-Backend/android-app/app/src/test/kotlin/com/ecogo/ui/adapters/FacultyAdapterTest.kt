package com.ecogo.ui.adapters

import com.ecogo.data.FacultyData
import com.ecogo.data.Outfit
import org.junit.Assert.*
import org.junit.Test

class FacultyAdapterTest {

    private fun makeFaculty(id: String = "soc", name: String = "Computing") = FacultyData(
        id = id, name = name, color = "#3B82F6", slogan = "Code the future", outfit = Outfit()
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = FacultyAdapter(listOf(makeFaculty("soc"), makeFaculty("biz", "Business"))) {}
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = FacultyAdapter(emptyList()) {}
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `click callback is set`() {
        var selected: FacultyData? = null
        val adapter = FacultyAdapter(listOf(makeFaculty())) { selected = it }
        assertNull(selected)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles faculty with custom outfit`() {
        val faculty = FacultyData(
            id = "eng", name = "Engineering", color = "#F59E0B",
            slogan = "Build the future", outfit = Outfit(head = "hat_helmet", body = "body_plaid")
        )
        val adapter = FacultyAdapter(listOf(faculty)) {}
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles multiple faculties`() {
        val faculties = (1..7).map { makeFaculty("f$it", "Faculty $it") }
        val adapter = FacultyAdapter(faculties) {}
        assertEquals(7, adapter.itemCount)
    }
}
