package com.ecogo.mapengine.ui.map

import android.app.Activity
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.ecogo.mapengine.data.model.GeoPoint
import com.ecogo.mapengine.data.model.RouteAlternative
import com.ecogo.mapengine.data.model.RouteStep
import com.google.android.material.card.MaterialCardView
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RouteOptionAdapterTest {

    private lateinit var parent: RecyclerView

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(Activity::class.java).create().get()
        val themedContext = ContextThemeWrapper(activity, com.google.android.material.R.style.Theme_MaterialComponents_Light)
        parent = RecyclerView(themedContext).apply {
            layoutManager = LinearLayoutManager(themedContext)
        }
    }

    private fun makeRoute(index: Int = 0) = RouteAlternative(
        index = index, total_distance = 2.5, estimated_duration = 18,
        total_carbon = 0.15, route_points = listOf(GeoPoint(lng = 103.77, lat = 1.29)),
        route_steps = listOf(
            RouteStep(
                instruction = "Walk to bus stop", distance = 200.0,
                duration = 180, travel_mode = "WALKING"
            )
        ),
        summary = "Bus 95 → Walk"
    )

    // ==================== Item Count Tests ====================

    @Test
    fun `getItemCount returns 0 initially`() {
        val adapter = RouteOptionAdapter(onRouteSelected = {})
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `setRoutes updates item count`() {
        val adapter = RouteOptionAdapter(onRouteSelected = {})
        adapter.setRoutes(listOf(makeRoute(0), makeRoute(1)))
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `setRoutes with empty list clears items`() {
        val adapter = RouteOptionAdapter(onRouteSelected = {})
        adapter.setRoutes(listOf(makeRoute()))
        assertEquals(1, adapter.itemCount)
        adapter.setRoutes(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `setRoutes resets selected index`() {
        val adapter = RouteOptionAdapter(onRouteSelected = {})
        adapter.setRoutes(listOf(makeRoute(0), makeRoute(1), makeRoute(2)))
        assertEquals(3, adapter.itemCount)
        adapter.setRoutes(listOf(makeRoute(0)))
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `callback is set`() {
        var selected: RouteAlternative? = null
        val adapter = RouteOptionAdapter { selected = it }
        adapter.setRoutes(listOf(makeRoute()))
        assertNull(selected)
        assertEquals(1, adapter.itemCount)
    }

    // ==================== View Binding Tests ====================

    @Test
    fun `onCreateViewHolder creates valid ViewHolder`() {
        val adapter = RouteOptionAdapter {}
        adapter.setRoutes(listOf(makeRoute()))
        val holder = adapter.onCreateViewHolder(parent, 0)
        assertNotNull(holder)
        assertNotNull(holder.itemView)
    }

    @Test
    fun `onBindViewHolder sets summary text`() {
        val adapter = RouteOptionAdapter {}
        val route = makeRoute().copy(summary = "地铁1号线 → 公交46路")
        adapter.setRoutes(listOf(route))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvSummary = holder.itemView.findViewById<TextView>(R.id.tvRouteSummary)
        assertEquals("地铁1号线 → 公交46路", tvSummary.text.toString())
    }

    @Test
    fun `onBindViewHolder sets time text`() {
        val adapter = RouteOptionAdapter {}
        adapter.setRoutes(listOf(makeRoute()))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvTime = holder.itemView.findViewById<TextView>(R.id.tvRouteTime)
        assertEquals("18 分钟", tvTime.text.toString())
    }

    @Test
    fun `onBindViewHolder formats distance at least 1km in kilometers`() {
        val adapter = RouteOptionAdapter {}
        val route = makeRoute().copy(total_distance = 2.5)
        adapter.setRoutes(listOf(route))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvDistance = holder.itemView.findViewById<TextView>(R.id.tvRouteDistance)
        assertEquals("2.5 公里", tvDistance.text.toString())
    }

    @Test
    fun `onBindViewHolder formats distance less than 1km in meters`() {
        val adapter = RouteOptionAdapter {}
        val route = makeRoute().copy(total_distance = 0.3)
        adapter.setRoutes(listOf(route))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvDistance = holder.itemView.findViewById<TextView>(R.id.tvRouteDistance)
        assertEquals("300 米", tvDistance.text.toString())
    }

    @Test
    fun `onBindViewHolder formats distance exactly 1km`() {
        val adapter = RouteOptionAdapter {}
        val route = makeRoute().copy(total_distance = 1.0)
        adapter.setRoutes(listOf(route))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvDistance = holder.itemView.findViewById<TextView>(R.id.tvRouteDistance)
        assertEquals("1.0 公里", tvDistance.text.toString())
    }

    @Test
    fun `onBindViewHolder sets carbon text`() {
        val adapter = RouteOptionAdapter {}
        val route = makeRoute().copy(total_carbon = 0.26)
        adapter.setRoutes(listOf(route))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvCarbon = holder.itemView.findViewById<TextView>(R.id.tvRouteCarbon)
        assertEquals("0.26 kg", tvCarbon.text.toString())
    }

    @Test
    fun `onBindViewHolder first item is selected with stroke`() {
        val adapter = RouteOptionAdapter {}
        adapter.setRoutes(listOf(makeRoute(0), makeRoute(1)))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val card = holder.itemView.findViewById<MaterialCardView>(R.id.cardRouteOption)
        assertEquals(6, card.strokeWidth)
    }

    @Test
    fun `onBindViewHolder second item is not selected with no stroke`() {
        val adapter = RouteOptionAdapter {}
        adapter.setRoutes(listOf(makeRoute(0), makeRoute(1)))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 1)

        val card = holder.itemView.findViewById<MaterialCardView>(R.id.cardRouteOption)
        assertEquals(0, card.strokeWidth)
    }

    @Test
    fun `click on route triggers callback with correct route`() {
        var selected: RouteAlternative? = null
        val adapter = RouteOptionAdapter { selected = it }
        val route = makeRoute().copy(summary = "Clicked Route")
        adapter.setRoutes(listOf(route))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        holder.itemView.performClick()
        assertNotNull(selected)
        assertEquals("Clicked Route", selected!!.summary)
    }

    @Test
    fun `onBindViewHolder with large distance formats correctly`() {
        val adapter = RouteOptionAdapter {}
        val route = makeRoute().copy(total_distance = 15.8, estimated_duration = 45)
        adapter.setRoutes(listOf(route))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvDistance = holder.itemView.findViewById<TextView>(R.id.tvRouteDistance)
        assertEquals("15.8 公里", tvDistance.text.toString())
        val tvTime = holder.itemView.findViewById<TextView>(R.id.tvRouteTime)
        assertEquals("45 分钟", tvTime.text.toString())
    }

    @Test
    fun `onBindViewHolder with zero carbon`() {
        val adapter = RouteOptionAdapter {}
        val route = makeRoute().copy(total_carbon = 0.0)
        adapter.setRoutes(listOf(route))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvCarbon = holder.itemView.findViewById<TextView>(R.id.tvRouteCarbon)
        assertEquals("0.00 kg", tvCarbon.text.toString())
    }

    @Test
    fun `onBindViewHolder with very small distance`() {
        val adapter = RouteOptionAdapter {}
        val route = makeRoute().copy(total_distance = 0.05)
        adapter.setRoutes(listOf(route))

        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)

        val tvDistance = holder.itemView.findViewById<TextView>(R.id.tvRouteDistance)
        assertEquals("50 米", tvDistance.text.toString())
    }

    @Test
    fun `multiple routes bind correctly at different positions`() {
        val adapter = RouteOptionAdapter {}
        val routes = listOf(
            makeRoute(0).copy(summary = "Route A", total_distance = 1.5),
            makeRoute(1).copy(summary = "Route B", total_distance = 0.8),
            makeRoute(2).copy(summary = "Route C", total_distance = 5.0)
        )
        adapter.setRoutes(routes)

        val holder0 = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder0, 0)
        assertEquals("Route A", holder0.itemView.findViewById<TextView>(R.id.tvRouteSummary).text.toString())
        assertEquals("1.5 公里", holder0.itemView.findViewById<TextView>(R.id.tvRouteDistance).text.toString())

        val holder1 = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder1, 1)
        assertEquals("Route B", holder1.itemView.findViewById<TextView>(R.id.tvRouteSummary).text.toString())
        assertEquals("800 米", holder1.itemView.findViewById<TextView>(R.id.tvRouteDistance).text.toString())

        val holder2 = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder2, 2)
        assertEquals("Route C", holder2.itemView.findViewById<TextView>(R.id.tvRouteSummary).text.toString())
        assertEquals("5.0 公里", holder2.itemView.findViewById<TextView>(R.id.tvRouteDistance).text.toString())
    }
}
