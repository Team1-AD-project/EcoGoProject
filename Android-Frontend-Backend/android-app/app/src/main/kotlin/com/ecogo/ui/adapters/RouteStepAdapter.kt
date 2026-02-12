package com.ecogo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.data.RouteStep
import com.ecogo.databinding.ItemRouteStepBinding
import com.ecogo.utils.MapUtils

/**
 * Route step adapter
 */
class RouteStepAdapter(
    private var steps: List<RouteStep> = emptyList()
) : RecyclerView.Adapter<RouteStepAdapter.StepViewHolder>() {

    fun updateSteps(newSteps: List<RouteStep>) {
        steps = newSteps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StepViewHolder {
        val binding = ItemRouteStepBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StepViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StepViewHolder, position: Int) {
        holder.bind(steps[position], position + 1)
    }

    override fun getItemCount(): Int = steps.size

    inner class StepViewHolder(
        private val binding: ItemRouteStepBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(step: RouteStep, stepNumber: Int) {
            binding.apply {
                textStepNumber.text = stepNumber.toString()
                
                // Transport mode icon
                textIcon.text = when (step.mode) {
                    com.ecogo.data.TransportMode.WALK -> "🚶"
                    com.ecogo.data.TransportMode.CYCLE -> "🚲"
                    com.ecogo.data.TransportMode.BUS -> "🚌"
                    com.ecogo.data.TransportMode.MIXED -> "📍"
                }
                
                textInstruction.text = step.instruction
                
                // Details
                val distanceStr = MapUtils.formatDistance(step.distance)
                val durationStr = if (step.duration >= 60) {
                    "${step.duration / 60} min"
                } else {
                    "${step.duration} sec"
                }
                textDetails.text = "$distanceStr • $durationStr"
            }
        }
    }
}
