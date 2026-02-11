package com.ecogo.mapengine.ui.map

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.ecogo.R
import com.ecogo.mapengine.data.model.GeoPoint
import com.ecogo.mapengine.data.model.RouteStep
import com.ecogo.mapengine.data.model.TransitDetails
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RouteStepAdapterTest {

    private lateinit var parent: RecyclerView

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        parent = RecyclerView(context).apply {
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun makeStep(instruction: String = "Walk north") = RouteStep(
        instruction = instruction, distance = 200.0, duration = 180,
        travel_mode = "WALKING"
    )

    private fun makeTransitStep() = RouteStep(
        instruction = "Take Bus 95", distance = 3500.0, duration = 720,
        travel_mode = "TRANSIT",
        transit_details = TransitDetails(
            line_name = "Bus 95", line_short_name = "95",
            departure_stop = "COM2", arrival_stop = "UTown",
            num_stops = 4, vehicle_type = "BUS", headsign = "Clementi"
        )
    )

    // ==================== Item Count Tests ====================

    @Test
    fun `getItemCount returns 0 initially`() {
        val adapter = RouteStepAdapter()
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `setSteps updates item count`() {
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(makeStep("Step 1"), makeStep("Step 2")))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `setSteps with empty list clears items`() {
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(makeStep()))
        assertEquals(1, adapter.itemCount)
        adapter.setSteps(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `adapter handles transit step with details`() {
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(makeTransitStep()))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles mixed walking and transit steps`() {
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(makeStep("Walk to stop"), makeTransitStep(), makeStep("Walk to dest")))
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `adapter handles step with polyline points`() {
        val step = makeStep().copy(
            polyline_points = listOf(GeoPoint(lng = 103.77, lat = 1.29), GeoPoint(lng = 103.78, lat = 1.30))
        )
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(step))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles step with null transit details`() {
        val step = makeStep().copy(transit_details = null)
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(step))
        assertEquals(1, adapter.itemCount)
    }

    // ==================== View Binding Tests ====================

    @Test
    fun `onCreateViewHolder creates valid ViewHolder`() {
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(makeStep()))
        val holder = adapter.onCreateViewHolder(parent, 0)
        assertNotNull(holder)
        assertNotNull(holder.itemView)
    }

    @Test
    fun `onBindViewHolder sets instruction text`() {
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(makeStep("步行至公交站")))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvInstruction = holder.itemView.findViewById<TextView>(R.id.tvStepInstruction)
        assertEquals("步行至公交站", tvInstruction.text.toString())
    }

    @Test
    fun `onBindViewHolder formats distance less than 1000m in meters`() {
        val adapter = RouteStepAdapter()
        val step = makeStep().copy(distance = 250.0, duration = 180)
        adapter.setSteps(listOf(step))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvDistance = holder.itemView.findViewById<TextView>(R.id.tvStepDistance)
        assertEquals("250 米 · 3 分钟", tvDistance.text.toString())
    }

    @Test
    fun `onBindViewHolder formats distance at least 1000m in kilometers`() {
        val adapter = RouteStepAdapter()
        val step = makeStep().copy(distance = 3500.0, duration = 720)
        adapter.setSteps(listOf(step))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvDistance = holder.itemView.findViewById<TextView>(R.id.tvStepDistance)
        assertEquals("3.5 公里 · 12 分钟", tvDistance.text.toString())
    }

    @Test
    fun `onBindViewHolder formats duration less than 60s in seconds`() {
        val adapter = RouteStepAdapter()
        val step = makeStep().copy(distance = 50.0, duration = 30)
        adapter.setSteps(listOf(step))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvDistance = holder.itemView.findViewById<TextView>(R.id.tvStepDistance)
        assertEquals("50 米 · 30 秒", tvDistance.text.toString())
    }

    @Test
    fun `onBindViewHolder formats duration at least 60s in minutes`() {
        val adapter = RouteStepAdapter()
        val step = makeStep().copy(distance = 500.0, duration = 300)
        adapter.setSteps(listOf(step))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvDistance = holder.itemView.findViewById<TextView>(R.id.tvStepDistance)
        assertEquals("500 米 · 5 分钟", tvDistance.text.toString())
    }

    @Test
    fun `onBindViewHolder formats distance exactly 1000m`() {
        val adapter = RouteStepAdapter()
        val step = makeStep().copy(distance = 1000.0, duration = 600)
        adapter.setSteps(listOf(step))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvDistance = holder.itemView.findViewById<TextView>(R.id.tvStepDistance)
        assertEquals("1.0 公里 · 10 分钟", tvDistance.text.toString())
    }

    @Test
    fun `onBindViewHolder formats duration exactly 60s`() {
        val adapter = RouteStepAdapter()
        val step = makeStep().copy(distance = 100.0, duration = 60)
        adapter.setSteps(listOf(step))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvDistance = holder.itemView.findViewById<TextView>(R.id.tvStepDistance)
        assertEquals("100 米 · 1 分钟", tvDistance.text.toString())
    }

    // ==================== Transit Details Tests ====================

    @Test
    fun `onBindViewHolder shows transit details for TRANSIT mode`() {
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(makeTransitStep()))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val layoutTransit = holder.itemView.findViewById<View>(R.id.layoutTransitDetails)
        assertEquals(View.VISIBLE, layoutTransit.visibility)
    }

    @Test
    fun `onBindViewHolder hides transit details for WALKING mode`() {
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(makeStep()))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val layoutTransit = holder.itemView.findViewById<View>(R.id.layoutTransitDetails)
        assertEquals(View.GONE, layoutTransit.visibility)
    }

    @Test
    fun `onBindViewHolder uses line_short_name when available`() {
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(makeTransitStep()))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvLineName = holder.itemView.findViewById<TextView>(R.id.tvLineName)
        assertEquals("95", tvLineName.text.toString())
    }

    @Test
    fun `onBindViewHolder uses line_name when short_name is null`() {
        val adapter = RouteStepAdapter()
        val step = makeTransitStep().copy(
            transit_details = TransitDetails(
                line_name = "地铁1号线", line_short_name = null,
                departure_stop = "天安门", arrival_stop = "王府井",
                num_stops = 2, vehicle_type = "SUBWAY"
            )
        )
        adapter.setSteps(listOf(step))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvLineName = holder.itemView.findViewById<TextView>(R.id.tvLineName)
        assertEquals("地铁1号线", tvLineName.text.toString())
    }

    @Test
    fun `onBindViewHolder sets departure stop text`() {
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(makeTransitStep()))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvDeparture = holder.itemView.findViewById<TextView>(R.id.tvDepartureStop)
        assertEquals("上车: COM2", tvDeparture.text.toString())
    }

    @Test
    fun `onBindViewHolder sets arrival stop with num_stops when positive`() {
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(makeTransitStep()))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvArrival = holder.itemView.findViewById<TextView>(R.id.tvArrivalStop)
        assertEquals("下车: UTown (经过 4 站)", tvArrival.text.toString())
    }

    @Test
    fun `onBindViewHolder sets arrival stop without num_stops when zero`() {
        val adapter = RouteStepAdapter()
        val step = makeTransitStep().copy(
            transit_details = TransitDetails(
                line_name = "Bus X", line_short_name = "X",
                departure_stop = "A", arrival_stop = "B",
                num_stops = 0, vehicle_type = "BUS"
            )
        )
        adapter.setSteps(listOf(step))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvArrival = holder.itemView.findViewById<TextView>(R.id.tvArrivalStop)
        assertEquals("下车: B", tvArrival.text.toString())
    }

    @Test
    fun `onBindViewHolder shows headsign when present`() {
        val adapter = RouteStepAdapter()
        adapter.setSteps(listOf(makeTransitStep()))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvHeadsign = holder.itemView.findViewById<TextView>(R.id.tvHeadsign)
        assertEquals(View.VISIBLE, tvHeadsign.visibility)
        assertEquals("往Clementi方向", tvHeadsign.text.toString())
    }

    @Test
    fun `onBindViewHolder hides headsign when null`() {
        val adapter = RouteStepAdapter()
        val step = makeTransitStep().copy(
            transit_details = TransitDetails(
                line_name = "Bus Y", line_short_name = "Y",
                departure_stop = "C", arrival_stop = "D",
                num_stops = 3, vehicle_type = "BUS", headsign = null
            )
        )
        adapter.setSteps(listOf(step))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvHeadsign = holder.itemView.findViewById<TextView>(R.id.tvHeadsign)
        assertEquals(View.GONE, tvHeadsign.visibility)
    }

    // ==================== Travel Mode Icon Tests ====================

    @Test
    fun `onBindViewHolder DRIVING step hides transit details`() {
        val adapter = RouteStepAdapter()
        val step = RouteStep(
            instruction = "Drive along highway", distance = 5000.0,
            duration = 300, travel_mode = "DRIVING"
        )
        adapter.setSteps(listOf(step))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val layoutTransit = holder.itemView.findViewById<View>(R.id.layoutTransitDetails)
        assertEquals(View.GONE, layoutTransit.visibility)
    }

    @Test
    fun `onBindViewHolder BICYCLING step hides transit details`() {
        val adapter = RouteStepAdapter()
        val step = RouteStep(
            instruction = "Cycle along path", distance = 1500.0,
            duration = 360, travel_mode = "BICYCLING"
        )
        adapter.setSteps(listOf(step))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val layoutTransit = holder.itemView.findViewById<View>(R.id.layoutTransitDetails)
        assertEquals(View.GONE, layoutTransit.visibility)
    }

    @Test
    fun `onBindViewHolder unknown travel mode hides transit details`() {
        val adapter = RouteStepAdapter()
        val step = RouteStep(
            instruction = "Unknown mode", distance = 100.0,
            duration = 60, travel_mode = "FLYING"
        )
        adapter.setSteps(listOf(step))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val layoutTransit = holder.itemView.findViewById<View>(R.id.layoutTransitDetails)
        assertEquals(View.GONE, layoutTransit.visibility)
    }

    @Test
    fun `onBindViewHolder TRANSIT without transit_details hides details`() {
        val adapter = RouteStepAdapter()
        val step = RouteStep(
            instruction = "Transit", distance = 2000.0,
            duration = 600, travel_mode = "TRANSIT", transit_details = null
        )
        adapter.setSteps(listOf(step))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val layoutTransit = holder.itemView.findViewById<View>(R.id.layoutTransitDetails)
        assertEquals(View.GONE, layoutTransit.visibility)
    }

    // ==================== Multiple Steps Binding ====================

    @Test
    fun `multiple steps bind correctly at different positions`() {
        val adapter = RouteStepAdapter()
        val steps = listOf(
            makeStep("Walk to station"),
            makeTransitStep(),
            makeStep("Walk to destination")
        )
        adapter.setSteps(steps)

        val holder0 = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder0, 0)
        assertEquals("Walk to station",
            holder0.itemView.findViewById<TextView>(R.id.tvStepInstruction).text.toString())
        assertEquals(View.GONE,
            holder0.itemView.findViewById<View>(R.id.layoutTransitDetails).visibility)

        val holder1 = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder1, 1)
        assertEquals("Take Bus 95",
            holder1.itemView.findViewById<TextView>(R.id.tvStepInstruction).text.toString())
        assertEquals(View.VISIBLE,
            holder1.itemView.findViewById<View>(R.id.layoutTransitDetails).visibility)

        val holder2 = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder2, 2)
        assertEquals("Walk to destination",
            holder2.itemView.findViewById<TextView>(R.id.tvStepInstruction).text.toString())
        assertEquals(View.GONE,
            holder2.itemView.findViewById<View>(R.id.layoutTransitDetails).visibility)
    }

    @Test
    fun `onBindViewHolder WALKING step sets instruction and distance`() {
        val adapter = RouteStepAdapter()
        val step = RouteStep(
            instruction = "Walk north on Main St", distance = 450.0,
            duration = 300, travel_mode = "WALKING"
        )
        adapter.setSteps(listOf(step))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        assertEquals("Walk north on Main St",
            holder.itemView.findViewById<TextView>(R.id.tvStepInstruction).text.toString())
        assertEquals("450 米 · 5 分钟",
            holder.itemView.findViewById<TextView>(R.id.tvStepDistance).text.toString())
    }
}
