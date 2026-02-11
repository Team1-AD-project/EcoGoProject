package com.ecogo.ui.adapters

import com.ecogo.data.FacultyData
import com.ecogo.data.Outfit
import org.junit.Assert.*
import org.junit.Test

class FacultyOutfitGridAdapterTest {

    private fun makeFaculty(id: String = "soc") = FacultyData(
        id = id, name = "Computing", color = "#3B82F6", slogan = "Code the future", outfit = Outfit()
    )

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = FacultyOutfitGridAdapter(
            listOf(makeFaculty("soc"), makeFaculty("biz")),
            onFacultyClick = {}
        )
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = FacultyOutfitGridAdapter(emptyList(), onFacultyClick = {})
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `click callback is set`() {
        var clicked: FacultyData? = null
        val adapter = FacultyOutfitGridAdapter(
            listOf(makeFaculty()), onFacultyClick = { clicked = it }
        )
        assertNull(clicked)
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `updateEquipped does not crash`() {
        val adapter = FacultyOutfitGridAdapter(listOf(makeFaculty()), onFacultyClick = {})
        adapter.updateEquipped("soc")
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `updateOwned does not crash`() {
        val adapter = FacultyOutfitGridAdapter(listOf(makeFaculty()), onFacultyClick = {})
        adapter.updateOwned(setOf("soc", "biz"))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles costCalculator`() {
        val adapter = FacultyOutfitGridAdapter(
            listOf(makeFaculty()),
            costCalculator = { 500 },
            onFacultyClick = {}
        )
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles all params`() {
        val adapter = FacultyOutfitGridAdapter(
            faculties = listOf(makeFaculty("soc"), makeFaculty("biz")),
            equippedFacultyId = "soc",
            ownedFacultyIds = setOf("soc"),
            userFacultyId = "soc",
            costCalculator = { 300 },
            onFacultyClick = {}
        )
        assertEquals(2, adapter.itemCount)
    }
}
