package com.ecogo.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.ecogo.databinding.ItemTransportModeBinding

class TransportModeAdapter(
    private val modes: List<String>,
    private val selectedModes: MutableSet<String>,
    private val onSelectionChanged: (Set<String>) -> Unit
) : RecyclerView.Adapter<TransportModeAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemTransportModeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransportModeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mode = modes[position]
        val context = holder.itemView.context
        val isSelected = selectedModes.contains(mode)

        holder.binding.run {
            // 设置文本和图标
            textTitle.text = getModeDisplayName(mode)
            textSubtitle.text = getModeDescription(mode)
            
            // 设置图标
            val iconRes = getModeIcon(mode)
            textIcon.visibility = View.GONE
            imgIcon.visibility = View.VISIBLE
            imgIcon.setImageResource(iconRes)

            // 设置选中状态样式
            if (isSelected) {
                cardTransportMode.strokeColor = ContextCompat.getColor(context, R.color.primary)
                checkIndicator.visibility = View.VISIBLE
                imgIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary))
            } else {
                cardTransportMode.strokeColor = ContextCompat.getColor(context, R.color.border)
                checkIndicator.visibility = View.GONE
                imgIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
            }

            // 点击事件
            root.setOnClickListener {
                if (selectedModes.contains(mode)) {
                    selectedModes.remove(mode)
                } else {
                    selectedModes.add(mode)
                }
                notifyItemChanged(position)
                onSelectionChanged(selectedModes)
            }
        }
    }

    override fun getItemCount(): Int = modes.size

    private fun getModeDisplayName(mode: String): String {
        return when (mode.lowercase()) {
            "walk" -> "Walking"
            "bike" -> "Cycling"
            "bus" -> "Bus"
            "subway" -> "MRT / Subway"
            "car" -> "Car"
            "electric_bike" -> "E-Bike"
            else -> mode.replaceFirstChar { it.uppercase() }
        }
    }

    private fun getModeDescription(mode: String): String {
        return when (mode.lowercase()) {
            "walk" -> "On foot around campus"
            "bike" -> "Bicycle or shared bike"
            "bus" -> "Campus shuttle & public buses"
            "subway" -> "Mass Rapid Transit"
            "car" -> "Private car or ride-hailing"
            "electric_bike" -> "Electric bicycle or scooter"
            else -> "Other transport mode"
        }
    }
    
    private fun getModeIcon(mode: String): Int {
        return when (mode.lowercase()) {
            "walk" -> R.drawable.ic_walking
            "bike" -> R.drawable.ic_bicycling
            "bus" -> R.drawable.ic_bus
            "subway" -> R.drawable.ic_transit // Assuming exists, or fallback
            "car" -> R.drawable.ic_driving
            "electric_bike" -> R.drawable.ic_electric_bike
            else -> R.drawable.ic_route // Fallback
        }
    }
}
