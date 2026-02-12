package com.ecogo.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.data.BusInfo
import com.ecogo.databinding.ItemBusCardBinding

/**
 * Bus information adapter
 */
class BusCardAdapter(
    private var busList: List<BusInfo> = emptyList()
) : RecyclerView.Adapter<BusCardAdapter.BusViewHolder>() {

    fun updateBusList(newBusList: List<BusInfo>) {
        busList = newBusList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusViewHolder {
        val binding = ItemBusCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BusViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BusViewHolder, position: Int) {
        holder.bind(busList[position])
    }

    override fun getItemCount(): Int = busList.size

    inner class BusViewHolder(
        private val binding: ItemBusCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(busInfo: BusInfo) {
            binding.apply {
                textBusId.text = busInfo.busId
                textDestination.text = "To ${busInfo.destination}"
                textEta.text = "${busInfo.etaMinutes} min"
                textPlate.text = busInfo.plateNumber

                
                // Status display
                textStatus.text = when (busInfo.status) {
                    "arriving" -> "⚡ Arriving soon"
                    "coming" -> "🚌 Approaching"
                    "delayed" -> "⚠️ Delayed"
                    else -> ""
                }
                
                // Set color based on crowding level
                // TODO: Can set different background colors based on crowding level
            }
        }
    }
}
