package com.ecogo.mapengine.ui.map

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.ecogo.mapengine.data.model.RouteStep

/**
 * Route Step RecyclerView Adapter
 */
class RouteStepAdapter : RecyclerView.Adapter<RouteStepAdapter.StepViewHolder>() {

    private var steps: List<RouteStep> = emptyList()

    fun setSteps(newSteps: List<RouteStep>) {
        steps = newSteps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mapengine_route_step, parent, false)
        return StepViewHolder(view)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        holder.bind(steps[position])
    }

    override fun getItemCount(): Int = steps.size

    class StepViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivStepIcon: ImageView = itemView.findViewById(R.id.ivStepIcon)
        private val tvStepInstruction: TextView = itemView.findViewById(R.id.tvStepInstruction)
        private val tvStepDistance: TextView = itemView.findViewById(R.id.tvStepDistance)

        // Transit detail views
        private val layoutTransitDetails: View = itemView.findViewById(R.id.layoutTransitDetails)
        private val tvLineName: TextView = itemView.findViewById(R.id.tvLineName)
        private val tvDepartureStop: TextView = itemView.findViewById(R.id.tvDepartureStop)
        private val tvArrivalStop: TextView = itemView.findViewById(R.id.tvArrivalStop)
        private val tvHeadsign: TextView = itemView.findViewById(R.id.tvHeadsign)

        fun bind(step: RouteStep) {
            // Set step icon
            val iconRes = when (step.travel_mode) {
                "WALKING" -> R.drawable.ic_walking
                "TRANSIT" -> R.drawable.ic_transit
                "DRIVING" -> R.drawable.ic_driving
                "BICYCLING" -> R.drawable.ic_bicycling
                else -> R.drawable.ic_walking
            }
            ivStepIcon.setImageResource(iconRes)

            // Set step instruction
            tvStepInstruction.text = step.instruction

            // Set distance and duration
            val distanceText = if (step.distance >= 1000) {
                String.format("%.1f km", step.distance / 1000.0)
            } else {
                String.format("%.0f m", step.distance)
            }
            val durationText = if (step.duration >= 60) {
                String.format("%d min", step.duration / 60)
            } else {
                String.format("%d sec", step.duration)
            }
            tvStepDistance.text = "$distanceText · $durationText"

            // Display transit details (TRANSIT mode only)
            if (step.travel_mode == "TRANSIT" && step.transit_details != null) {
                layoutTransitDetails.visibility = View.VISIBLE

                val transit = step.transit_details

                // Line name
                val lineName = transit.line_short_name ?: transit.line_name
                tvLineName.text = lineName

                // Departure stop
                tvDepartureStop.text = "Depart: ${transit.departure_stop}"

                // Arrival stop and number of stops
                val arrivalText = if (transit.num_stops > 0) {
                    "Arrive: ${transit.arrival_stop} (${transit.num_stops} stops)"
                } else {
                    "Arrive: ${transit.arrival_stop}"
                }
                tvArrivalStop.text = arrivalText

                // Headsign
                if (transit.headsign != null) {
                    tvHeadsign.visibility = View.VISIBLE
                    tvHeadsign.text = "Towards ${transit.headsign}"
                } else {
                    tvHeadsign.visibility = View.GONE
                }
            } else {
                layoutTransitDetails.visibility = View.GONE
            }
        }
    }
}
