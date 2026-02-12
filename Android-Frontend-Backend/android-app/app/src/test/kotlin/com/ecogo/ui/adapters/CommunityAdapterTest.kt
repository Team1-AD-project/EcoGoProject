package com.ecogo.ui.adapters

import com.ecogo.data.FacultyCarbonData
import org.junit.Assert.*
import org.junit.Test

class CommunityAdapterTest {

    private fun makeFaculty(name: String = "Computing", carbon: Double = 100.0) =
        FacultyCarbonData(faculty = name, totalCarbon = carbon)

    @Test
    fun `getItemCount returns correct size`() {
        val adapter = CommunityAdapter(listOf(makeFaculty("SOC", 200.0), makeFaculty("BIZ", 150.0)))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `getItemCount returns 0 for empty list`() {
        val adapter = CommunityAdapter(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `adapter handles single faculty`() {
        val adapter = CommunityAdapter(listOf(makeFaculty()))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles faculties with zero carbon`() {
        val faculties = listOf(makeFaculty("A", 0.0), makeFaculty("B", 0.0))
        val adapter = CommunityAdapter(faculties)
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `adapter handles large number of faculties`() {
        val faculties = (1..20).map { makeFaculty("Faculty$it", it * 10.0) }
        val adapter = CommunityAdapter(faculties)
        assertEquals(20, adapter.itemCount)
    }
}
