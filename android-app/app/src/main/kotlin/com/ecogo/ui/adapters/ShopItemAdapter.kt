package com.ecogo.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.ecogo.data.ShopItem

class ShopItemAdapter(
    private val items: List<ShopItem>,
    private val onPurchase: (ShopItem) -> Unit
) : RecyclerView.Adapter<ShopItemAdapter.ShopViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shop, parent, false)
        return ShopViewHolder(view, onPurchase)
    }
    
    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        holder.bind(items[position])
    }
    
    override fun getItemCount() = items.size
    
    class ShopViewHolder(
        itemView: View,
        private val onPurchase: (ShopItem) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val icon: TextView = itemView.findViewById(R.id.text_icon)
        private val name: TextView = itemView.findViewById(R.id.text_name)
        private val cost: TextView = itemView.findViewById(R.id.text_cost)
        private val status: TextView = itemView.findViewById(R.id.text_status)
        private val check: View = itemView.findViewById(R.id.image_check)
        
        fun bind(item: ShopItem) {
            name.text = item.name
            cost.text = "${item.cost} pts"
            
            icon.text = when (item.type) {
                "head" -> if (item.id.contains("grad")) "🎓" else "🧢"
                "face" -> if (item.id.contains("sun")) "🕶️" else "👓"
                "body" -> if (item.id.contains("hoodie")) "🧥" else "👕"
                else -> "🎁"
            }
            
            if (item.owned) {
                status.visibility = View.VISIBLE
                status.text = itemView.context.getString(R.string.profile_equipped)
                cost.visibility = View.GONE
                check.visibility = View.VISIBLE
            } else {
                status.visibility = View.GONE
                cost.visibility = View.VISIBLE
                check.visibility = View.GONE
            }
            
            itemView.setOnClickListener {
                onPurchase(item)
            }
        }
    }
}
