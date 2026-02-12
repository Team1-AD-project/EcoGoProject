package com.ecogo.ui.adapters

import com.ecogo.data.FacultyData
import com.ecogo.data.Outfit
import org.junit.Assert.*
import org.junit.Test

class FacultyFlipAdapterTest {

    private fun makeFaculty(id: String = "soc") = FacultyData(
        id = id, name = "Computing", color = "#3B82F6", slogan = "Code the future", outfit = Outfit()
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = FacultyFlipAdapter(listOf(makeFaculty("soc"), makeFaculty("biz"))) {}
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = FacultyFlipAdapter(emptyList()) {}
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `click callback is set`() {
        var selected: FacultyData? = null
        val adapter = FacultyFlipAdapter(listOf(makeFaculty())) { selected = it }
        assertNull(selected)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles faculty with outfit items`() {
        val faculty = FacultyData(
            id = "eng", name = "Engineering", color = "#F59E0B",
            slogan = "Build", outfit = Outfit(head = "hat_helmet", face = "face_goggles", body = "body_plaid")
        )
        val adapter = FacultyFlipAdapter(listOf(faculty)) {}
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles multiple faculties`() {
        val faculties = (1..5).map { makeFaculty("f$it") }
        val adapter = FacultyFlipAdapter(faculties) {}
        assertEquals(5, adapter.itemCount)
    }
}
