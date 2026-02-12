package com.ecogo.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.ecogo.data.NavRoute
import com.ecogo.databinding.ItemRouteOptionBinding
import com.ecogo.utils.CarbonCalculator
import com.ecogo.utils.MapUtils

/**
 * Route option adapter
 */
class RouteOptionAdapter(
    private var routes: List<NavRoute> = emptyList(),
    private val onRouteClick: (NavRoute) -> Unit
) : RecyclerView.Adapter<RouteOptionAdapter.RouteViewHolder>() {

    fun updateRoutes(newRoutes: List<NavRoute>) {
        routes = newRoutes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val binding = ItemRouteOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RouteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(routes[position])
    }

    override fun getItemCount(): Int = routes.size

    inner class RouteViewHolder(
        private val binding: ItemRouteOptionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(route: NavRoute) {
            binding.apply {
                // Show badge (if any)
                if (route.badge.isNotEmpty()) {
                    textBadge.text = route.badge
                    textBadge.visibility = View.VISIBLE
                } else {
                    textBadge.visibility = View.GONE
                }
                
                // Transport mode icon
                val modeIcon = when (route.mode) {
                    com.ecogo.data.TransportMode.WALK -> "🚶 Walking"
                    com.ecogo.data.TransportMode.CYCLE -> "🚲 Cycling"
                    com.ecogo.data.TransportMode.BUS -> "🚌 Bus"
                    com.ecogo.data.TransportMode.MIXED -> "🚶🚌 Mixed"
                }
                textMode.text = modeIcon
                
                // Time and distance
                textDuration.text = MapUtils.formatDuration(route.duration)
                textDistance.text = String.format("%.1f km", route.distance)
                
                // Eco data
                textCarbon.text = CarbonCalculator.formatCarbon(route.carbonEmission)
                textPoints.text = "+${route.points} pts"
                textSavings.text = "Save $${String.format("%.1f", CarbonCalculator.calculateMoneySaved(route.distance))}"
                
                // Highlight recommended route
                if (route.isRecommended) {
                    root.setCardBackgroundColor(
                        root.context.getColor(R.color.background)
                    )
                } else {
                    root.setCardBackgroundColor(
                        root.context.getColor(android.R.color.white)
                    )
                }
                
                root.setOnClickListener {
                    onRouteClick(route)
                }
            }
        }
    }
}
