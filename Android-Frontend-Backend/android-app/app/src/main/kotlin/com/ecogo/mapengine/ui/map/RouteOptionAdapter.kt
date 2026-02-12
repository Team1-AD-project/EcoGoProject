package com.ecogo.mapengine.ui.map

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.ecogo.mapengine.data.model.RouteAlternative
import com.ecogo.databinding.ItemMapengineRouteOptionBinding

/**
 * Route selection adapter
 */
class RouteOptionAdapter(
    private val onRouteSelected: (RouteAlternative) -> Unit
) : RecyclerView.Adapter<RouteOptionAdapter.RouteOptionViewHolder>() {

    private var routes: List<RouteAlternative> = emptyList()
    private var selectedIndex: Int = 0

    fun setRoutes(newRoutes: List<RouteAlternative>) {
        routes = newRoutes
        selectedIndex = 0
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteOptionViewHolder {
        val binding = ItemMapengineRouteOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RouteOptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteOptionViewHolder, position: Int) {
        holder.bind(routes[position], position == selectedIndex)
    }

    override fun getItemCount(): Int = routes.size

    inner class RouteOptionViewHolder(
        private val binding: ItemMapengineRouteOptionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(route: RouteAlternative, isSelected: Boolean) {
            // Route summary
            binding.tvRouteSummary.text = route.summary

            // Duration
            binding.tvRouteTime.text = "${route.estimated_duration} min"

            // Distance
            val distanceText = if (route.total_distance >= 1.0) {
                String.format("%.1f km", route.total_distance)
            } else {
                String.format("%.0f m", route.total_distance * 1000)
            }
            binding.tvRouteDistance.text = distanceText

            // Carbon emissions
            binding.tvRouteCarbon.text = String.format("%.2f kg", route.total_carbon)

            // Selected state
            if (isSelected) {
                binding.cardRouteOption.strokeWidth = 6
                binding.cardRouteOption.strokeColor = ContextCompat.getColor(
                    binding.root.context,
                    R.color.green_primary
                )
            } else {
                binding.cardRouteOption.strokeWidth = 0
            }

            // Click event
            binding.root.setOnClickListener {
                val oldSelectedIndex = selectedIndex
                selectedIndex = adapterPosition
                notifyItemChanged(oldSelectedIndex)
                notifyItemChanged(selectedIndex)
                onRouteSelected(route)
            }
        }
    }
}
