package com.ecogo.ui.adapters

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.ecogo.data.BusInfo
import com.ecogo.data.FacultyData
import com.ecogo.data.OrderSummaryUi
import com.ecogo.data.Outfit
import com.ecogo.data.Product
import com.ecogo.data.TripSummaryUi
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AdapterViewHolderTestBatch5 {
    private lateinit var parent: RecyclerView

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val themedContext = ContextThemeWrapper(
            activity,
            com.google.android.material.R.style.Theme_MaterialComponents_Light
        )
        parent = RecyclerView(themedContext).apply {
            layoutManager = LinearLayoutManager(themedContext)
        }
    }

    // ---------------------------------------------------------------
    // Helper: FacultyData
    // ---------------------------------------------------------------
    private fun makeFaculty(
        id: String = "eng",
        name: String = "Engineering",
        color: String = "#FF6B35",
        slogan: String = "Building the Future",
        outfit: Outfit = Outfit(head = "hat_helmet", face = "face_goggles", body = "body_plaid")
    ) = FacultyData(id = id, name = name, color = color, slogan = slogan, outfit = outfit)

    // ===============================================================
    // 1. FacultySwipeAdapter
    // ===============================================================

    @Test
    fun `FacultySwipeAdapter onCreateViewHolder creates valid holder`() {
        val adapter = FacultySwipeAdapter(listOf(makeFaculty())) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        assertNotNull(holder)
        assertNotNull(holder.itemView)
    }

    @Test
    fun `FacultySwipeAdapter bind sets faculty name`() {
        val adapter = FacultySwipeAdapter(listOf(makeFaculty(name = "Computing"))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Computing", holder.itemView.findViewById<TextView>(R.id.text_faculty_name).text.toString())
    }

    @Test
    fun `FacultySwipeAdapter bind sets slogan`() {
        val adapter = FacultySwipeAdapter(listOf(makeFaculty(slogan = "Code the World"))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Code the World", holder.itemView.findViewById<TextView>(R.id.text_faculty_slogan).text.toString())
    }

    @Test
    fun `FacultySwipeAdapter bind sets outfit items with head face body`() {
        val outfit = Outfit(head = "hat_helmet", face = "face_goggles", body = "body_plaid")
        val adapter = FacultySwipeAdapter(listOf(makeFaculty(outfit = outfit))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        val text = holder.itemView.findViewById<TextView>(R.id.text_outfit_items).text.toString()
        assertTrue(text.contains("Safety Helmet"))
        assertTrue(text.contains("Safety Goggles"))
        assertTrue(text.contains("Engin Plaid"))
    }

    @Test
    fun `FacultySwipeAdapter bind sets outfit items with only head`() {
        val outfit = Outfit(head = "hat_beret", face = "none", body = "none")
        val adapter = FacultySwipeAdapter(listOf(makeFaculty(outfit = outfit))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        val text = holder.itemView.findViewById<TextView>(R.id.text_outfit_items).text.toString()
        assertEquals("\u2022 Artist Beret", text)
    }

    @Test
    fun `FacultySwipeAdapter bind sets empty outfit items when all none`() {
        val outfit = Outfit(head = "none", face = "none", body = "none")
        val adapter = FacultySwipeAdapter(listOf(makeFaculty(outfit = outfit))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("", holder.itemView.findViewById<TextView>(R.id.text_outfit_items).text.toString())
    }

    @Test
    fun `FacultySwipeAdapter bind hat_grad maps to Grad Cap`() {
        val outfit = Outfit(head = "hat_grad", face = "none", body = "none")
        val adapter = FacultySwipeAdapter(listOf(makeFaculty(outfit = outfit))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertTrue(holder.itemView.findViewById<TextView>(R.id.text_outfit_items).text.toString().contains("Grad Cap"))
    }

    @Test
    fun `FacultySwipeAdapter bind hat_cap maps to Orange Cap`() {
        val outfit = Outfit(head = "hat_cap", face = "none", body = "none")
        val adapter = FacultySwipeAdapter(listOf(makeFaculty(outfit = outfit))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertTrue(holder.itemView.findViewById<TextView>(R.id.text_outfit_items).text.toString().contains("Orange Cap"))
    }

    @Test
    fun `FacultySwipeAdapter bind glasses_sun maps to Shades`() {
        val outfit = Outfit(head = "none", face = "glasses_sun", body = "none")
        val adapter = FacultySwipeAdapter(listOf(makeFaculty(outfit = outfit))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertTrue(holder.itemView.findViewById<TextView>(R.id.text_outfit_items).text.toString().contains("Shades"))
    }

    @Test
    fun `FacultySwipeAdapter bind body_suit maps to Biz Suit`() {
        val outfit = Outfit(head = "none", face = "none", body = "body_suit")
        val adapter = FacultySwipeAdapter(listOf(makeFaculty(outfit = outfit))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertTrue(holder.itemView.findViewById<TextView>(R.id.text_outfit_items).text.toString().contains("Biz Suit"))
    }

    @Test
    fun `FacultySwipeAdapter bind body_coat maps to Lab Coat`() {
        val outfit = Outfit(head = "none", face = "none", body = "body_coat")
        val adapter = FacultySwipeAdapter(listOf(makeFaculty(outfit = outfit))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertTrue(holder.itemView.findViewById<TextView>(R.id.text_outfit_items).text.toString().contains("Lab Coat"))
    }

    @Test
    fun `FacultySwipeAdapter bind shirt_nus maps to NUS Tee`() {
        val outfit = Outfit(head = "none", face = "none", body = "shirt_nus")
        val adapter = FacultySwipeAdapter(listOf(makeFaculty(outfit = outfit))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertTrue(holder.itemView.findViewById<TextView>(R.id.text_outfit_items).text.toString().contains("NUS Tee"))
    }

    @Test
    fun `FacultySwipeAdapter bind shirt_hoodie maps to Blue Hoodie`() {
        val outfit = Outfit(head = "none", face = "none", body = "shirt_hoodie")
        val adapter = FacultySwipeAdapter(listOf(makeFaculty(outfit = outfit))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertTrue(holder.itemView.findViewById<TextView>(R.id.text_outfit_items).text.toString().contains("Blue Hoodie"))
    }

    @Test
    fun `FacultySwipeAdapter bind unknown item id maps to empty string`() {
        val outfit = Outfit(head = "hat_unknown", face = "none", body = "none")
        val adapter = FacultySwipeAdapter(listOf(makeFaculty(outfit = outfit))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        // Unknown maps to empty string, so the line should be "• "
        assertEquals("\u2022 ", holder.itemView.findViewById<TextView>(R.id.text_outfit_items).text.toString())
    }

    @Test
    fun `FacultySwipeAdapter click triggers callback`() {
        var clicked: FacultyData? = null
        val faculty = makeFaculty(name = "Science")
        val adapter = FacultySwipeAdapter(listOf(faculty)) { clicked = it }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        holder.itemView.findViewById<MaterialCardView>(R.id.card_faculty).performClick()
        assertNotNull(clicked)
        assertEquals("Science", clicked!!.name)
    }

    @Test
    fun `FacultySwipeAdapter getItemCount returns faculty list size`() {
        val adapter = FacultySwipeAdapter(listOf(makeFaculty("a"), makeFaculty("b"), makeFaculty("c"))) {}
        assertEquals(3, adapter.itemCount)
    }

    // ===============================================================
    // 2. FacultyAdapter
    // ===============================================================

    @Test
    fun `FacultyAdapter onCreateViewHolder creates valid holder`() {
        val adapter = FacultyAdapter(listOf(makeFaculty())) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        assertNotNull(holder)
        assertNotNull(holder.itemView)
    }

    @Test
    fun `FacultyAdapter bind sets faculty name`() {
        val adapter = FacultyAdapter(listOf(makeFaculty(name = "Law"))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Law", holder.itemView.findViewById<TextView>(R.id.text_faculty_name).text.toString())
    }

    @Test
    fun `FacultyAdapter bind sets slogan`() {
        val adapter = FacultyAdapter(listOf(makeFaculty(slogan = "Justice for All"))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Justice for All", holder.itemView.findViewById<TextView>(R.id.text_faculty_slogan).text.toString())
    }

    @Test
    fun `FacultyAdapter unselected state has strokeWidth 2`() {
        val adapter = FacultyAdapter(listOf(makeFaculty())) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        val card = holder.itemView as MaterialCardView
        assertEquals(2, card.strokeWidth)
    }

    @Test
    fun `FacultyAdapter click triggers callback`() {
        var clicked: FacultyData? = null
        val adapter = FacultyAdapter(listOf(makeFaculty(name = "FASS"))) { clicked = it }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        holder.itemView.performClick()
        assertNotNull(clicked)
        assertEquals("FASS", clicked!!.name)
    }

    @Test
    fun `FacultyAdapter getItemCount returns list size`() {
        val adapter = FacultyAdapter(listOf(makeFaculty("a"), makeFaculty("b"))) {}
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `FacultyAdapter bind multiple items first not selected`() {
        val adapter = FacultyAdapter(listOf(makeFaculty(name = "Eng"), makeFaculty(name = "Sci"))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        // Default selectedPosition is -1, so position 0 is not selected
        val card = holder.itemView as MaterialCardView
        assertEquals(2, card.strokeWidth)
    }

    @Test
    fun `FacultyAdapter bind different faculty names at different positions`() {
        val adapter = FacultyAdapter(listOf(makeFaculty(name = "Alpha"), makeFaculty(name = "Beta"))) {}
        val holder1 = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder1, 0)
        assertEquals("Alpha", holder1.itemView.findViewById<TextView>(R.id.text_faculty_name).text.toString())

        val holder2 = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder2, 1)
        assertEquals("Beta", holder2.itemView.findViewById<TextView>(R.id.text_faculty_name).text.toString())
    }

    // ===============================================================
    // 3. FacultyFlipAdapter
    // ===============================================================

    @Test
    fun `FacultyFlipAdapter onCreateViewHolder creates valid holder`() {
        val adapter = FacultyFlipAdapter(listOf(makeFaculty())) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        assertNotNull(holder)
        assertNotNull(holder.itemView)
    }

    @Test
    fun `FacultyFlipAdapter bind sets front name`() {
        val adapter = FacultyFlipAdapter(listOf(makeFaculty(name = "Medicine"))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Medicine", holder.itemView.findViewById<TextView>(R.id.text_front_name).text.toString())
    }

    @Test
    fun `FacultyFlipAdapter bind sets back name`() {
        val adapter = FacultyFlipAdapter(listOf(makeFaculty(name = "Medicine"))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Medicine", holder.itemView.findViewById<TextView>(R.id.text_back_name).text.toString())
    }

    @Test
    fun `FacultyFlipAdapter bind sets back slogan`() {
        val adapter = FacultyFlipAdapter(listOf(makeFaculty(slogan = "Heal the World"))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Heal the World", holder.itemView.findViewById<TextView>(R.id.text_back_slogan).text.toString())
    }

    @Test
    fun `FacultyFlipAdapter initial state front visible back gone`() {
        val adapter = FacultyFlipAdapter(listOf(makeFaculty())) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(View.VISIBLE, holder.itemView.findViewById<View>(R.id.card_front).visibility)
        assertEquals(View.GONE, holder.itemView.findViewById<View>(R.id.card_back).visibility)
    }

    @Test
    fun `FacultyFlipAdapter selected indicator initially gone`() {
        val adapter = FacultyFlipAdapter(listOf(makeFaculty())) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(View.GONE, holder.itemView.findViewById<View>(R.id.view_selected_indicator).visibility)
    }

    @Test
    fun `FacultyFlipAdapter bind sets back outfit text`() {
        val outfit = Outfit(head = "hat_helmet", face = "face_goggles", body = "body_plaid")
        val adapter = FacultyFlipAdapter(listOf(makeFaculty(outfit = outfit))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        val text = holder.itemView.findViewById<TextView>(R.id.text_back_outfit).text.toString()
        assertTrue(text.contains("Safety Helmet"))
        assertTrue(text.contains("Safety Goggles"))
        assertTrue(text.contains("Engin Plaid"))
    }

    @Test
    fun `FacultyFlipAdapter bind outfit with only body`() {
        val outfit = Outfit(head = "none", face = "none", body = "body_suit")
        val adapter = FacultyFlipAdapter(listOf(makeFaculty(outfit = outfit))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Biz Suit", holder.itemView.findViewById<TextView>(R.id.text_back_outfit).text.toString())
    }

    @Test
    fun `FacultyFlipAdapter bind outfit with none shows empty`() {
        val outfit = Outfit(head = "none", face = "none", body = "none")
        val adapter = FacultyFlipAdapter(listOf(makeFaculty(outfit = outfit))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("", holder.itemView.findViewById<TextView>(R.id.text_back_outfit).text.toString())
    }

    @Test
    fun `FacultyFlipAdapter getItemCount returns correct size`() {
        val adapter = FacultyFlipAdapter(listOf(makeFaculty("a"), makeFaculty("b"), makeFaculty("c"), makeFaculty("d"))) {}
        assertEquals(4, adapter.itemCount)
    }

    @Test
    fun `FacultyFlipAdapter bind hat_beret maps to Artist Beret on back`() {
        val outfit = Outfit(head = "hat_beret", face = "none", body = "none")
        val adapter = FacultyFlipAdapter(listOf(makeFaculty(outfit = outfit))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertTrue(holder.itemView.findViewById<TextView>(R.id.text_back_outfit).text.toString().contains("Artist Beret"))
    }

    @Test
    fun `FacultyFlipAdapter front rotationY is 0 initially`() {
        val adapter = FacultyFlipAdapter(listOf(makeFaculty())) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(0f, holder.itemView.findViewById<View>(R.id.card_front).rotationY, 0.01f)
    }

    @Test
    fun `FacultyFlipAdapter back rotationY is 180 initially`() {
        val adapter = FacultyFlipAdapter(listOf(makeFaculty())) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(180f, holder.itemView.findViewById<View>(R.id.card_back).rotationY, 0.01f)
    }

    // ===============================================================
    // 4. BusCardAdapter
    // ===============================================================

    private fun makeBusInfo(
        busId: String = "D1",
        routeName: String = "Route D1",
        destination: String = "UTown",
        currentLat: Double = 1.305,
        currentLng: Double = 103.772,
        etaMinutes: Int = 5,
        stopsAway: Int = 2,
        crowdLevel: String = "low",
        plateNumber: String = "SBS1234A",
        status: String = "arriving",
        color: String = "#DB2777"
    ) = BusInfo(
        busId = busId, routeName = routeName, destination = destination,
        currentLat = currentLat, currentLng = currentLng,
        etaMinutes = etaMinutes, stopsAway = stopsAway,
        crowdLevel = crowdLevel, plateNumber = plateNumber,
        status = status, color = color
    )

    @Test
    fun `BusCardAdapter onCreateViewHolder creates valid holder`() {
        val adapter = BusCardAdapter(listOf(makeBusInfo()))
        val holder = adapter.onCreateViewHolder(parent, 0)
        assertNotNull(holder)
        assertNotNull(holder.itemView)
    }

    @Test
    fun `BusCardAdapter bind sets busId`() {
        val adapter = BusCardAdapter(listOf(makeBusInfo(busId = "A2")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("A2", holder.itemView.findViewById<TextView>(R.id.text_bus_id).text.toString())
    }

    @Test
    fun `BusCardAdapter bind sets destination with prefix`() {
        val adapter = BusCardAdapter(listOf(makeBusInfo(destination = "Science Park")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("\u524D\u5F80 Science Park", holder.itemView.findViewById<TextView>(R.id.text_destination).text.toString())
    }

    @Test
    fun `BusCardAdapter bind sets eta in minutes`() {
        val adapter = BusCardAdapter(listOf(makeBusInfo(etaMinutes = 8)))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("8\u5206\u949F", holder.itemView.findViewById<TextView>(R.id.text_eta).text.toString())
    }

    @Test
    fun `BusCardAdapter bind sets plate number`() {
        val adapter = BusCardAdapter(listOf(makeBusInfo(plateNumber = "SG9876Z")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("SG9876Z", holder.itemView.findViewById<TextView>(R.id.text_plate).text.toString())
    }

    @Test
    fun `BusCardAdapter bind status arriving`() {
        val adapter = BusCardAdapter(listOf(makeBusInfo(status = "arriving")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("\u26A1 \u5373\u5C06\u5230\u8FBE", holder.itemView.findViewById<TextView>(R.id.text_status).text.toString())
    }

    @Test
    fun `BusCardAdapter bind status coming`() {
        val adapter = BusCardAdapter(listOf(makeBusInfo(status = "coming")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("\uD83D\uDE8C \u5373\u5C06\u5230\u7AD9", holder.itemView.findViewById<TextView>(R.id.text_status).text.toString())
    }

    @Test
    fun `BusCardAdapter bind status delayed`() {
        val adapter = BusCardAdapter(listOf(makeBusInfo(status = "delayed")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("\u26A0\uFE0F \u5EF6\u8BEF", holder.itemView.findViewById<TextView>(R.id.text_status).text.toString())
    }

    @Test
    fun `BusCardAdapter bind unknown status shows empty`() {
        val adapter = BusCardAdapter(listOf(makeBusInfo(status = "cancelled")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("", holder.itemView.findViewById<TextView>(R.id.text_status).text.toString())
    }

    @Test
    fun `BusCardAdapter getItemCount returns list size`() {
        val adapter = BusCardAdapter(listOf(makeBusInfo("a"), makeBusInfo("b")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `BusCardAdapter empty list has zero count`() {
        val adapter = BusCardAdapter()
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `BusCardAdapter updateBusList changes items`() {
        val adapter = BusCardAdapter()
        adapter.updateBusList(listOf(makeBusInfo("x"), makeBusInfo("y"), makeBusInfo("z")))
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `BusCardAdapter bind zero eta`() {
        val adapter = BusCardAdapter(listOf(makeBusInfo(etaMinutes = 0)))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("0\u5206\u949F", holder.itemView.findViewById<TextView>(R.id.text_eta).text.toString())
    }

    // ===============================================================
    // 5. FacultyOutfitGridAdapter
    // ===============================================================

    @Test
    fun `FacultyOutfitGridAdapter onCreateViewHolder creates valid holder`() {
        val adapter = FacultyOutfitGridAdapter(listOf(makeFaculty())) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        assertNotNull(holder)
        assertNotNull(holder.itemView)
    }

    @Test
    fun `FacultyOutfitGridAdapter bind sets faculty name`() {
        val adapter = FacultyOutfitGridAdapter(listOf(makeFaculty(name = "Design"))) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Design", holder.itemView.findViewById<TextView>(R.id.text_faculty_name).text.toString())
    }

    @Test
    fun `FacultyOutfitGridAdapter equipped shows check and Equipped text`() {
        val faculty = makeFaculty(id = "eng")
        val adapter = FacultyOutfitGridAdapter(
            faculties = listOf(faculty),
            equippedFacultyId = "eng",
            ownedFacultyIds = setOf("eng")
        ) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(View.VISIBLE, holder.itemView.findViewById<ImageView>(R.id.image_equipped).visibility)
        assertTrue(holder.itemView.findViewById<TextView>(R.id.text_outfit_items).text.toString().contains("Equipped"))
    }

    @Test
    fun `FacultyOutfitGridAdapter equipped card strokeWidth is 4`() {
        val faculty = makeFaculty(id = "eng")
        val adapter = FacultyOutfitGridAdapter(
            faculties = listOf(faculty),
            equippedFacultyId = "eng",
            ownedFacultyIds = setOf("eng")
        ) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        val card = holder.itemView.findViewById<MaterialCardView>(R.id.card_faculty_outfit)
        assertEquals(4, card.strokeWidth)
    }

    @Test
    fun `FacultyOutfitGridAdapter not equipped hides check and strokeWidth is 0`() {
        val faculty = makeFaculty(id = "sci")
        val adapter = FacultyOutfitGridAdapter(
            faculties = listOf(faculty),
            equippedFacultyId = "eng",
            ownedFacultyIds = setOf("sci")
        ) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(View.GONE, holder.itemView.findViewById<ImageView>(R.id.image_equipped).visibility)
        assertEquals(0, holder.itemView.findViewById<MaterialCardView>(R.id.card_faculty_outfit).strokeWidth)
    }

    @Test
    fun `FacultyOutfitGridAdapter owned and userFaculty shows Your Faculty`() {
        val faculty = makeFaculty(id = "eng")
        val adapter = FacultyOutfitGridAdapter(
            faculties = listOf(faculty),
            equippedFacultyId = null,
            ownedFacultyIds = setOf("eng"),
            userFacultyId = "eng"
        ) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertTrue(holder.itemView.findViewById<TextView>(R.id.text_outfit_items).text.toString().contains("Your Faculty"))
    }

    @Test
    fun `FacultyOutfitGridAdapter owned but not userFaculty shows Owned`() {
        val faculty = makeFaculty(id = "eng")
        val adapter = FacultyOutfitGridAdapter(
            faculties = listOf(faculty),
            equippedFacultyId = null,
            ownedFacultyIds = setOf("eng"),
            userFacultyId = "sci"
        ) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Owned", holder.itemView.findViewById<TextView>(R.id.text_outfit_items).text.toString())
    }

    @Test
    fun `FacultyOutfitGridAdapter not owned shows cost`() {
        val faculty = makeFaculty(id = "eng")
        val adapter = FacultyOutfitGridAdapter(
            faculties = listOf(faculty),
            equippedFacultyId = null,
            ownedFacultyIds = emptySet(),
            costCalculator = { 500 }
        ) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertTrue(holder.itemView.findViewById<TextView>(R.id.text_outfit_items).text.toString().contains("500 pts"))
    }

    @Test
    fun `FacultyOutfitGridAdapter owned alpha is 1`() {
        val faculty = makeFaculty(id = "eng")
        val adapter = FacultyOutfitGridAdapter(
            faculties = listOf(faculty),
            ownedFacultyIds = setOf("eng")
        ) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(1f, holder.itemView.alpha, 0.01f)
    }

    @Test
    fun `FacultyOutfitGridAdapter not owned alpha is 0_7`() {
        val faculty = makeFaculty(id = "eng")
        val adapter = FacultyOutfitGridAdapter(
            faculties = listOf(faculty),
            ownedFacultyIds = emptySet()
        ) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(0.7f, holder.itemView.alpha, 0.01f)
    }

    @Test
    fun `FacultyOutfitGridAdapter click triggers callback`() {
        var clicked: FacultyData? = null
        val faculty = makeFaculty(id = "biz", name = "Business")
        val adapter = FacultyOutfitGridAdapter(listOf(faculty)) { clicked = it }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        holder.itemView.performClick()
        assertNotNull(clicked)
        assertEquals("Business", clicked!!.name)
    }

    @Test
    fun `FacultyOutfitGridAdapter updateEquipped changes state`() {
        val faculty = makeFaculty(id = "eng")
        val adapter = FacultyOutfitGridAdapter(
            faculties = listOf(faculty),
            equippedFacultyId = null,
            ownedFacultyIds = setOf("eng")
        ) {}
        adapter.updateEquipped("eng")
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(View.VISIBLE, holder.itemView.findViewById<ImageView>(R.id.image_equipped).visibility)
    }

    @Test
    fun `FacultyOutfitGridAdapter updateOwned changes state`() {
        val faculty = makeFaculty(id = "eng")
        val adapter = FacultyOutfitGridAdapter(
            faculties = listOf(faculty),
            ownedFacultyIds = emptySet()
        ) {}
        adapter.updateOwned(setOf("eng"))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(1f, holder.itemView.alpha, 0.01f)
    }

    // ===============================================================
    // 6. ActivityAdapter
    // ===============================================================

    private fun makeActivity(
        id: String = "a1",
        title: String = "Campus Cleanup",
        description: String = "Clean the campus",
        type: String = "OFFLINE",
        status: String = "PUBLISHED",
        rewardCredits: Int = 50,
        currentParticipants: Int = 10,
        startTime: String? = "2026-03-15T09:00:00"
    ) = com.ecogo.data.Activity(
        id = id, title = title, description = description, type = type,
        status = status, rewardCredits = rewardCredits,
        currentParticipants = currentParticipants, startTime = startTime
    )

    @Test
    fun `ActivityAdapter onCreateViewHolder creates valid holder`() {
        val adapter = ActivityAdapter(listOf(makeActivity()))
        val holder = adapter.onCreateViewHolder(parent, 0)
        assertNotNull(holder)
        assertNotNull(holder.itemView)
    }

    @Test
    fun `ActivityAdapter bind sets title`() {
        val adapter = ActivityAdapter(listOf(makeActivity(title = "Green Walk")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Green Walk", holder.itemView.findViewById<TextView>(R.id.text_title).text.toString())
    }

    @Test
    fun `ActivityAdapter bind formats date from ISO as yyyy slash MM slash dd`() {
        val adapter = ActivityAdapter(listOf(makeActivity(startTime = "2026-03-15T09:00:00")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("2026/03/15", holder.itemView.findViewById<TextView>(R.id.text_date).text.toString())
    }

    @Test
    fun `ActivityAdapter bind null startTime shows TBD`() {
        val adapter = ActivityAdapter(listOf(makeActivity(startTime = null)))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("TBD", holder.itemView.findViewById<TextView>(R.id.text_date).text.toString())
    }

    @Test
    fun `ActivityAdapter bind sets location from description`() {
        val adapter = ActivityAdapter(listOf(makeActivity(description = "UTown Plaza")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("UTown Plaza", holder.itemView.findViewById<TextView>(R.id.text_location).text.toString())
    }

    @Test
    fun `ActivityAdapter bind empty description ONLINE shows Online Event`() {
        val adapter = ActivityAdapter(listOf(makeActivity(description = "", type = "ONLINE")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Online Event", holder.itemView.findViewById<TextView>(R.id.text_location).text.toString())
    }

    @Test
    fun `ActivityAdapter bind empty description OFFLINE shows On-Campus`() {
        val adapter = ActivityAdapter(listOf(makeActivity(description = "", type = "OFFLINE")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("On-Campus", holder.itemView.findViewById<TextView>(R.id.text_location).text.toString())
    }

    @Test
    fun `ActivityAdapter bind empty description unknown type shows type`() {
        val adapter = ActivityAdapter(listOf(makeActivity(description = "", type = "HYBRID")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("HYBRID", holder.itemView.findViewById<TextView>(R.id.text_location).text.toString())
    }

    @Test
    fun `ActivityAdapter bind category ONLINE shows Campaign`() {
        val adapter = ActivityAdapter(listOf(makeActivity(type = "ONLINE")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Campaign", holder.itemView.findViewById<TextView>(R.id.text_category).text.toString())
    }

    @Test
    fun `ActivityAdapter bind category OFFLINE shows Campus`() {
        val adapter = ActivityAdapter(listOf(makeActivity(type = "OFFLINE")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Campus", holder.itemView.findViewById<TextView>(R.id.text_category).text.toString())
    }

    @Test
    fun `ActivityAdapter bind category unknown shows Event`() {
        val adapter = ActivityAdapter(listOf(makeActivity(type = "SPECIAL")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Event", holder.itemView.findViewById<TextView>(R.id.text_category).text.toString())
    }

    @Test
    fun `ActivityAdapter bind icon for Clean title`() {
        val adapter = ActivityAdapter(listOf(makeActivity(title = "Campus Cleanup")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("\uD83E\uDDF9", holder.itemView.findViewById<TextView>(R.id.text_icon).text.toString())
    }

    @Test
    fun `ActivityAdapter bind icon for Workshop title`() {
        val adapter = ActivityAdapter(listOf(makeActivity(title = "Eco Workshop")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("\uD83E\uDD57", holder.itemView.findViewById<TextView>(R.id.text_icon).text.toString())
    }

    @Test
    fun `ActivityAdapter bind icon for Run title`() {
        val adapter = ActivityAdapter(listOf(makeActivity(title = "Morning Run")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("\uD83C\uDFC3", holder.itemView.findViewById<TextView>(R.id.text_icon).text.toString())
    }

    @Test
    fun `ActivityAdapter bind icon for Recycl title`() {
        val adapter = ActivityAdapter(listOf(makeActivity(title = "Recycling Drive")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("\u267B\uFE0F", holder.itemView.findViewById<TextView>(R.id.text_icon).text.toString())
    }

    @Test
    fun `ActivityAdapter bind icon default is seedling`() {
        val adapter = ActivityAdapter(listOf(makeActivity(title = "Mystery Event")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("\uD83C\uDF31", holder.itemView.findViewById<TextView>(R.id.text_icon).text.toString())
    }

    @Test
    fun `ActivityAdapter click triggers callback`() {
        var clicked: com.ecogo.data.Activity? = null
        val adapter = ActivityAdapter(listOf(makeActivity(title = "Click Me"))) { clicked = it }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        holder.itemView.performClick()
        assertNotNull(clicked)
        assertEquals("Click Me", clicked!!.title)
    }

    @Test
    fun `ActivityAdapter updateActivities changes items`() {
        val adapter = ActivityAdapter(listOf(makeActivity()))
        adapter.updateActivities(listOf(makeActivity("a1"), makeActivity("a2"), makeActivity("a3")))
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `ActivityAdapter updateActivities with empty list`() {
        val adapter = ActivityAdapter(listOf(makeActivity()))
        adapter.updateActivities(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    // ===============================================================
    // 7. TransportModeAdapter
    // ===============================================================

    @Test
    fun `TransportModeAdapter onCreateViewHolder creates valid holder`() {
        val adapter = TransportModeAdapter(listOf("walk"), mutableSetOf()) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        assertNotNull(holder)
        assertNotNull(holder.itemView)
    }

    @Test
    fun `TransportModeAdapter bind walk displays Walking`() {
        val adapter = TransportModeAdapter(listOf("walk"), mutableSetOf()) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Walking", holder.itemView.findViewById<TextView>(R.id.text_title).text.toString())
    }

    @Test
    fun `TransportModeAdapter bind bike displays Cycling`() {
        val adapter = TransportModeAdapter(listOf("bike"), mutableSetOf()) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Cycling", holder.itemView.findViewById<TextView>(R.id.text_title).text.toString())
    }

    @Test
    fun `TransportModeAdapter bind bus displays Bus`() {
        val adapter = TransportModeAdapter(listOf("bus"), mutableSetOf()) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Bus", holder.itemView.findViewById<TextView>(R.id.text_title).text.toString())
    }

    @Test
    fun `TransportModeAdapter bind subway displays MRT Subway`() {
        val adapter = TransportModeAdapter(listOf("subway"), mutableSetOf()) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("MRT / Subway", holder.itemView.findViewById<TextView>(R.id.text_title).text.toString())
    }

    @Test
    fun `TransportModeAdapter bind car displays Car`() {
        val adapter = TransportModeAdapter(listOf("car"), mutableSetOf()) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Car", holder.itemView.findViewById<TextView>(R.id.text_title).text.toString())
    }

    @Test
    fun `TransportModeAdapter bind electric_bike displays E-Bike`() {
        val adapter = TransportModeAdapter(listOf("electric_bike"), mutableSetOf()) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("E-Bike", holder.itemView.findViewById<TextView>(R.id.text_title).text.toString())
    }

    @Test
    fun `TransportModeAdapter bind unknown mode capitalizes first char`() {
        val adapter = TransportModeAdapter(listOf("skateboard"), mutableSetOf()) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Skateboard", holder.itemView.findViewById<TextView>(R.id.text_title).text.toString())
    }

    @Test
    fun `TransportModeAdapter bind walk description`() {
        val adapter = TransportModeAdapter(listOf("walk"), mutableSetOf()) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("On foot around campus", holder.itemView.findViewById<TextView>(R.id.text_subtitle).text.toString())
    }

    @Test
    fun `TransportModeAdapter bind bike description`() {
        val adapter = TransportModeAdapter(listOf("bike"), mutableSetOf()) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Bicycle or shared bike", holder.itemView.findViewById<TextView>(R.id.text_subtitle).text.toString())
    }

    @Test
    fun `TransportModeAdapter bind bus description`() {
        val adapter = TransportModeAdapter(listOf("bus"), mutableSetOf()) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Campus shuttle & public buses", holder.itemView.findViewById<TextView>(R.id.text_subtitle).text.toString())
    }

    @Test
    fun `TransportModeAdapter bind subway description`() {
        val adapter = TransportModeAdapter(listOf("subway"), mutableSetOf()) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Mass Rapid Transit", holder.itemView.findViewById<TextView>(R.id.text_subtitle).text.toString())
    }

    @Test
    fun `TransportModeAdapter bind car description`() {
        val adapter = TransportModeAdapter(listOf("car"), mutableSetOf()) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Private car or ride-hailing", holder.itemView.findViewById<TextView>(R.id.text_subtitle).text.toString())
    }

    @Test
    fun `TransportModeAdapter bind electric_bike description`() {
        val adapter = TransportModeAdapter(listOf("electric_bike"), mutableSetOf()) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Electric bicycle or scooter", holder.itemView.findViewById<TextView>(R.id.text_subtitle).text.toString())
    }

    @Test
    fun `TransportModeAdapter bind unknown mode description`() {
        val adapter = TransportModeAdapter(listOf("hovercraft"), mutableSetOf()) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Other transport mode", holder.itemView.findViewById<TextView>(R.id.text_subtitle).text.toString())
    }

    @Test
    fun `TransportModeAdapter selected mode shows checkIndicator visible`() {
        val adapter = TransportModeAdapter(listOf("walk"), mutableSetOf("walk")) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(View.VISIBLE, holder.itemView.findViewById<View>(R.id.check_indicator).visibility)
    }

    @Test
    fun `TransportModeAdapter unselected mode hides checkIndicator`() {
        val adapter = TransportModeAdapter(listOf("walk"), mutableSetOf()) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(View.GONE, holder.itemView.findViewById<View>(R.id.check_indicator).visibility)
    }

    @Test
    fun `TransportModeAdapter click toggles selection`() {
        val selected = mutableSetOf<String>()
        var lastSelection: Set<String> = emptySet()
        val adapter = TransportModeAdapter(listOf("bus"), selected) { lastSelection = it }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        holder.itemView.performClick()
        assertTrue(selected.contains("bus"))
        assertTrue(lastSelection.contains("bus"))
    }

    @Test
    fun `TransportModeAdapter click deselects if already selected`() {
        val selected = mutableSetOf("walk")
        var lastSelection: Set<String> = emptySet()
        val adapter = TransportModeAdapter(listOf("walk"), selected) { lastSelection = it }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        holder.itemView.performClick()
        assertFalse(selected.contains("walk"))
        assertFalse(lastSelection.contains("walk"))
    }

    @Test
    fun `TransportModeAdapter getItemCount returns modes size`() {
        val adapter = TransportModeAdapter(listOf("walk", "bike", "bus"), mutableSetOf()) {}
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `TransportModeAdapter bind uses imgIcon visible and textIcon gone`() {
        val adapter = TransportModeAdapter(listOf("walk"), mutableSetOf()) {}
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(View.GONE, holder.itemView.findViewById<TextView>(R.id.text_icon).visibility)
        assertEquals(View.VISIBLE, holder.itemView.findViewById<ImageView>(R.id.img_icon).visibility)
    }

    // ===============================================================
    // 8. TripHistoryAdapter
    // ===============================================================

    private fun makeTripSummary(
        id: String = "t1",
        routeText: String = "NUS -> Orchard",
        timeText: String = "2026-02-10 14:30",
        primaryText: String = "3.2 km",
        metaText: String = "Walking - 25 min",
        statusText: String = "COMPLETED"
    ) = TripSummaryUi(
        id = id, routeText = routeText, timeText = timeText,
        primaryText = primaryText, metaText = metaText, statusText = statusText
    )

    @Test
    fun `TripHistoryAdapter onCreateViewHolder creates valid holder`() {
        val adapter = TripHistoryAdapter(listOf(makeTripSummary()))
        val holder = adapter.onCreateViewHolder(parent, 0)
        assertNotNull(holder)
        assertNotNull(holder.itemView)
    }

    @Test
    fun `TripHistoryAdapter bind sets route text`() {
        val adapter = TripHistoryAdapter(listOf(makeTripSummary(routeText = "Home -> Campus")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Home -> Campus", holder.itemView.findViewById<TextView>(R.id.text_trip_route).text.toString())
    }

    @Test
    fun `TripHistoryAdapter bind sets time text`() {
        val adapter = TripHistoryAdapter(listOf(makeTripSummary(timeText = "2026-01-20 08:15")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("2026-01-20 08:15", holder.itemView.findViewById<TextView>(R.id.text_time).text.toString())
    }

    @Test
    fun `TripHistoryAdapter bind sets primary text`() {
        val adapter = TripHistoryAdapter(listOf(makeTripSummary(primaryText = "5.0 km")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("5.0 km", holder.itemView.findViewById<TextView>(R.id.text_primary).text.toString())
    }

    @Test
    fun `TripHistoryAdapter bind sets meta text`() {
        val adapter = TripHistoryAdapter(listOf(makeTripSummary(metaText = "Bus - 15 min")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Bus - 15 min", holder.itemView.findViewById<TextView>(R.id.text_meta).text.toString())
    }

    @Test
    fun `TripHistoryAdapter bind sets status chip text`() {
        val adapter = TripHistoryAdapter(listOf(makeTripSummary(statusText = "COMPLETED")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("COMPLETED", holder.itemView.findViewById<Chip>(R.id.chip_status).text.toString())
    }

    @Test
    fun `TripHistoryAdapter empty list has zero count`() {
        val adapter = TripHistoryAdapter()
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `TripHistoryAdapter update changes items`() {
        val adapter = TripHistoryAdapter()
        adapter.update(listOf(makeTripSummary("t1"), makeTripSummary("t2")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `TripHistoryAdapter bind different statuses`() {
        val adapter = TripHistoryAdapter(listOf(makeTripSummary(statusText = "CANCELLED")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("CANCELLED", holder.itemView.findViewById<Chip>(R.id.chip_status).text.toString())
    }

    // ===============================================================
    // 9. OrderHistoryAdapter
    // ===============================================================

    private fun makeOrder(
        id: String = "o1",
        orderNumber: String? = "ORDER-2026-0001",
        status: String? = "COMPLETED",
        finalAmount: Double? = 12.50,
        createdAt: String? = null,
        itemCount: Int? = 2,
        isRedemption: Boolean? = false,
        trackingNumber: String? = null,
        carrier: String? = null
    ) = OrderSummaryUi(
        id = id, orderNumber = orderNumber, status = status,
        finalAmount = finalAmount, createdAt = createdAt,
        itemCount = itemCount, isRedemption = isRedemption,
        trackingNumber = trackingNumber, carrier = carrier
    )

    @Test
    fun `OrderHistoryAdapter onCreateViewHolder creates valid holder`() {
        val adapter = OrderHistoryAdapter(listOf(makeOrder()))
        val holder = adapter.onCreateViewHolder(parent, 0)
        assertNotNull(holder)
        assertNotNull(holder.itemView)
    }

    @Test
    fun `OrderHistoryAdapter bind sets order number`() {
        val adapter = OrderHistoryAdapter(listOf(makeOrder(orderNumber = "ORD-999")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("ORD-999", holder.itemView.findViewById<TextView>(R.id.text_order_number).text.toString())
    }

    @Test
    fun `OrderHistoryAdapter bind null order number falls back to id`() {
        val adapter = OrderHistoryAdapter(listOf(makeOrder(id = "fallback_id", orderNumber = null)))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("fallback_id", holder.itemView.findViewById<TextView>(R.id.text_order_number).text.toString())
    }

    @Test
    fun `OrderHistoryAdapter bind formats amount`() {
        val adapter = OrderHistoryAdapter(listOf(makeOrder(finalAmount = 25.50)))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("$25.50", holder.itemView.findViewById<TextView>(R.id.text_amount).text.toString())
    }

    @Test
    fun `OrderHistoryAdapter bind null amount shows zero`() {
        val adapter = OrderHistoryAdapter(listOf(makeOrder(finalAmount = null)))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("$0.00", holder.itemView.findViewById<TextView>(R.id.text_amount).text.toString())
    }

    @Test
    fun `OrderHistoryAdapter bind sets meta text with itemCount and Purchase`() {
        val adapter = OrderHistoryAdapter(listOf(makeOrder(itemCount = 3, isRedemption = false)))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("3 items \u00B7 Purchase", holder.itemView.findViewById<TextView>(R.id.text_meta).text.toString())
    }

    @Test
    fun `OrderHistoryAdapter bind sets meta text with Redemption`() {
        val adapter = OrderHistoryAdapter(listOf(makeOrder(itemCount = 1, isRedemption = true)))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("1 items \u00B7 Redemption", holder.itemView.findViewById<TextView>(R.id.text_meta).text.toString())
    }

    @Test
    fun `OrderHistoryAdapter bind null itemCount shows 0`() {
        val adapter = OrderHistoryAdapter(listOf(makeOrder(itemCount = null)))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertTrue(holder.itemView.findViewById<TextView>(R.id.text_meta).text.toString().startsWith("0 items"))
    }

    @Test
    fun `OrderHistoryAdapter bind sets status chip text uppercase`() {
        val adapter = OrderHistoryAdapter(listOf(makeOrder(status = "pending")))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("PENDING", holder.itemView.findViewById<Chip>(R.id.chip_status).text.toString())
    }

    @Test
    fun `OrderHistoryAdapter bind null status shows UNKNOWN`() {
        val adapter = OrderHistoryAdapter(listOf(makeOrder(status = null)))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("UNKNOWN", holder.itemView.findViewById<Chip>(R.id.chip_status).text.toString())
    }

    @Test
    fun `OrderHistoryAdapter bind null createdAt shows dash`() {
        val adapter = OrderHistoryAdapter(listOf(makeOrder(createdAt = null)))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("-", holder.itemView.findViewById<TextView>(R.id.text_date).text.toString())
    }

    @Test
    fun `OrderHistoryAdapter empty list has zero count`() {
        val adapter = OrderHistoryAdapter()
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `OrderHistoryAdapter update changes items`() {
        val adapter = OrderHistoryAdapter()
        adapter.update(listOf(makeOrder("o1"), makeOrder("o2"), makeOrder("o3")))
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `OrderHistoryAdapter bind null isRedemption treated as Purchase`() {
        val adapter = OrderHistoryAdapter(listOf(makeOrder(isRedemption = null)))
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertTrue(holder.itemView.findViewById<TextView>(R.id.text_meta).text.toString().contains("Purchase"))
    }

    // ===============================================================
    // 10. ProductAdapter (additional tests for uncovered paths)
    // ===============================================================

    private fun makeProduct(
        id: String = "p1",
        name: String = "Starbucks $5 Off",
        description: String = "Valid at all locations",
        type: String = "voucher",
        category: String = "food",
        pointsPrice: Int? = 500,
        cashPrice: Double? = 3.00,
        available: Boolean = true,
        stock: Int? = null
    ) = Product(
        id = id, name = name, description = description, type = type,
        category = category, pointsPrice = pointsPrice, cashPrice = cashPrice,
        available = available, stock = stock
    )

    @Test
    fun `ProductAdapter onCreateViewHolder creates valid holder`() {
        val adapter = ProductAdapter(listOf(makeProduct())) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        assertNotNull(holder)
        assertNotNull(holder.itemView)
    }

    @Test
    fun `ProductAdapter bind sets name`() {
        val adapter = ProductAdapter(listOf(makeProduct(name = "Grab $10"))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Grab $10", holder.itemView.findViewById<TextView>(R.id.text_name).text.toString())
    }

    @Test
    fun `ProductAdapter bind sets description`() {
        val adapter = ProductAdapter(listOf(makeProduct(description = "Use anywhere"))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("Use anywhere", holder.itemView.findViewById<TextView>(R.id.text_description).text.toString())
    }

    @Test
    fun `ProductAdapter bind sets category`() {
        val adapter = ProductAdapter(listOf(makeProduct(category = "transport"))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("transport", holder.itemView.findViewById<TextView>(R.id.text_category).text.toString())
    }

    @Test
    fun `ProductAdapter bind unavailable product sets alpha 0_5 and disables buttons`() {
        val adapter = ProductAdapter(listOf(makeProduct(available = false))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(0.5f, holder.itemView.alpha, 0.01f)
        assertFalse(holder.itemView.findViewById<MaterialButton>(R.id.button_redeem).isEnabled)
        assertFalse(holder.itemView.findViewById<MaterialButton>(R.id.button_buy).isEnabled)
    }

    @Test
    fun `ProductAdapter bind available product sets alpha 1 and enables buttons`() {
        val adapter = ProductAdapter(listOf(makeProduct(available = true))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(1.0f, holder.itemView.alpha, 0.01f)
        assertTrue(holder.itemView.findViewById<MaterialButton>(R.id.button_redeem).isEnabled)
        assertTrue(holder.itemView.findViewById<MaterialButton>(R.id.button_buy).isEnabled)
    }

    @Test
    fun `ProductAdapter bind stock zero disables buttons`() {
        val adapter = ProductAdapter(listOf(makeProduct(available = true, stock = 0))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(0.5f, holder.itemView.alpha, 0.01f)
        assertFalse(holder.itemView.findViewById<MaterialButton>(R.id.button_redeem).isEnabled)
    }

    @Test
    fun `ProductAdapter bind null pointsPrice hides points chip and redeem button`() {
        val adapter = ProductAdapter(listOf(makeProduct(pointsPrice = null))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(View.GONE, holder.itemView.findViewById<Chip>(R.id.chip_points).visibility)
        assertEquals(View.GONE, holder.itemView.findViewById<MaterialButton>(R.id.button_redeem).visibility)
    }

    @Test
    fun `ProductAdapter bind non-null pointsPrice shows points chip`() {
        val adapter = ProductAdapter(listOf(makeProduct(pointsPrice = 300))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(View.VISIBLE, holder.itemView.findViewById<Chip>(R.id.chip_points).visibility)
        assertEquals("300 pts", holder.itemView.findViewById<Chip>(R.id.chip_points).text.toString())
    }

    @Test
    fun `ProductAdapter bind null cashPrice hides cash chip and buy button`() {
        val adapter = ProductAdapter(listOf(makeProduct(cashPrice = null))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(View.GONE, holder.itemView.findViewById<Chip>(R.id.chip_cash).visibility)
        assertEquals(View.GONE, holder.itemView.findViewById<MaterialButton>(R.id.button_buy).visibility)
    }

    @Test
    fun `ProductAdapter bind non-null cashPrice shows cash chip`() {
        val adapter = ProductAdapter(listOf(makeProduct(cashPrice = 5.99))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(View.VISIBLE, holder.itemView.findViewById<Chip>(R.id.chip_cash).visibility)
        assertEquals("$5.99 SGD", holder.itemView.findViewById<Chip>(R.id.chip_cash).text.toString())
    }

    @Test
    fun `ProductAdapter bind Starbucks icon`() {
        val adapter = ProductAdapter(listOf(makeProduct(name = "Starbucks Voucher"))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("\u2615", holder.itemView.findViewById<TextView>(R.id.text_icon).text.toString())
    }

    @Test
    fun `ProductAdapter bind Grab icon`() {
        val adapter = ProductAdapter(listOf(makeProduct(name = "Grab Ride $5"))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("\uD83D\uDE97", holder.itemView.findViewById<TextView>(R.id.text_icon).text.toString())
    }

    @Test
    fun `ProductAdapter bind Foodpanda icon`() {
        val adapter = ProductAdapter(listOf(makeProduct(name = "Foodpanda Discount"))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("\uD83C\uDF55", holder.itemView.findViewById<TextView>(R.id.text_icon).text.toString())
    }

    @Test
    fun `ProductAdapter bind Bottle icon`() {
        val adapter = ProductAdapter(listOf(makeProduct(name = "Eco Bottle"))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("\uD83C\uDF31", holder.itemView.findViewById<TextView>(R.id.text_icon).text.toString())
    }

    @Test
    fun `ProductAdapter bind T-Shirt icon`() {
        val adapter = ProductAdapter(listOf(makeProduct(name = "NUS T-Shirt"))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("\uD83D\uDC55", holder.itemView.findViewById<TextView>(R.id.text_icon).text.toString())
    }

    @Test
    fun `ProductAdapter bind Tote icon`() {
        val adapter = ProductAdapter(listOf(makeProduct(name = "Green Tote Bag"))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("\uD83D\uDC55", holder.itemView.findViewById<TextView>(R.id.text_icon).text.toString())
    }

    @Test
    fun `ProductAdapter bind Tree icon`() {
        val adapter = ProductAdapter(listOf(makeProduct(name = "Plant a Tree"))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("\uD83C\uDF33", holder.itemView.findViewById<TextView>(R.id.text_icon).text.toString())
    }

    @Test
    fun `ProductAdapter bind Book icon`() {
        val adapter = ProductAdapter(listOf(makeProduct(name = "Eco Book"))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("\uD83D\uDCDA", holder.itemView.findViewById<TextView>(R.id.text_icon).text.toString())
    }

    @Test
    fun `ProductAdapter bind default icon`() {
        val adapter = ProductAdapter(listOf(makeProduct(name = "Mystery Gift"))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals("\uD83C\uDF81", holder.itemView.findViewById<TextView>(R.id.text_icon).text.toString())
    }

    @Test
    fun `ProductAdapter redeem button click triggers callback with points`() {
        var result: Pair<Product, String>? = null
        val product = makeProduct(name = "Test")
        val adapter = ProductAdapter(listOf(product)) { p, m -> result = Pair(p, m) }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        holder.itemView.findViewById<MaterialButton>(R.id.button_redeem).performClick()
        assertNotNull(result)
        assertEquals("points", result!!.second)
    }

    @Test
    fun `ProductAdapter buy button click triggers callback with cash`() {
        var result: Pair<Product, String>? = null
        val product = makeProduct(name = "Test")
        val adapter = ProductAdapter(listOf(product)) { p, m -> result = Pair(p, m) }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        holder.itemView.findViewById<MaterialButton>(R.id.button_buy).performClick()
        assertNotNull(result)
        assertEquals("cash", result!!.second)
    }

    @Test
    fun `ProductAdapter updateProducts changes count`() {
        val adapter = ProductAdapter(listOf(makeProduct())) { _, _ -> }
        adapter.updateProducts(listOf(makeProduct("a"), makeProduct("b"), makeProduct("c")))
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `ProductAdapter bind available with positive stock enables buttons`() {
        val adapter = ProductAdapter(listOf(makeProduct(available = true, stock = 10))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(1.0f, holder.itemView.alpha, 0.01f)
        assertTrue(holder.itemView.findViewById<MaterialButton>(R.id.button_redeem).isEnabled)
    }

    @Test
    fun `ProductAdapter bind null stock with available true enables buttons`() {
        val adapter = ProductAdapter(listOf(makeProduct(available = true, stock = null))) { _, _ -> }
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        assertEquals(1.0f, holder.itemView.alpha, 0.01f)
        assertTrue(holder.itemView.findViewById<MaterialButton>(R.id.button_redeem).isEnabled)
    }
}
