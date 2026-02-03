package com.ecogo.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.ecogo.data.ShopItem
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class GoodsAdapter(
    private val onGoodsClick: (ShopItem) -> Unit = {}
) : RecyclerView.Adapter<GoodsAdapter.GoodsViewHolder>() {
    
    private var goods: List<ShopItem> = emptyList()
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoodsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goods, parent, false)
        return GoodsViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: GoodsViewHolder, position: Int) {
        val item = goods[position]
        holder.bind(item)
        holder.itemView.setOnClickListener {
            onGoodsClick(item)
        }
    }
    
    override fun getItemCount() = goods.size
    
    fun updateGoods(newGoods: List<ShopItem>) {
        goods = newGoods
        notifyDataSetChanged()
    }
    
    class GoodsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: TextView = itemView.findViewById(R.id.text_goods_icon)
        private val name: TextView = itemView.findViewById(R.id.text_goods_name)
        private val description: TextView = itemView.findViewById(R.id.text_goods_description)
        private val price: TextView = itemView.findViewById(R.id.text_goods_price)
        private val button: MaterialButton = itemView.findViewById(R.id.btn_redeem_goods)
        private val categoryChip: Chip = itemView.findViewById(R.id.chip_category)
        private val stockChip: Chip = itemView.findViewById(R.id.chip_stock)
        
        fun bind(item: ShopItem) {
            // Set product name
            name.text = item.name
            
            // Set description
            description.text = getDescription(item)
            
            // Set price
            price.text = item.cost.toString()
            
            // Set icon
            icon.text = getIcon(item)
            
            // Set category label
            categoryChip.text = getCategoryName(item.type)
            
            // Set stock status
            if (item.owned) {
                stockChip.visibility = View.VISIBLE
                stockChip.text = "Owned"
                button.isEnabled = false
                button.text = "Redeemed"
            } else {
                stockChip.visibility = View.GONE
                button.isEnabled = true
                button.text = "Redeem"
            }
            
            // Set click listener
            button.setOnClickListener {
                // Handle button click in Adapter
                if (!item.owned) {
                    // Trigger redemption
                }
            }
        }
        
        private fun getDescription(item: ShopItem): String {
            return when {
                item.name.contains("Starbucks", ignoreCase = true) -> "Valid at all Starbucks locations on campus"
                item.name.contains("Subway", ignoreCase = true) -> "Valid at campus Subway"
                item.name.contains("Canteen", ignoreCase = true) -> "Valid at campus canteen"
                item.name.contains("Tea", ignoreCase = true) -> "Valid at campus tea shops"
                item.type == "head" -> "Head accessory for LiNUS"
                item.type == "face" -> "Face accessory for LiNUS"
                item.type == "body" -> "Outfit for LiNUS"
                item.type == "badge" -> "Achievement badge"
                else -> "Available in points shop"
            }
        }
        
        private fun getIcon(item: ShopItem): String {
            return when {
                item.name.contains("Starbucks", ignoreCase = true) -> "☕"
                item.name.contains("Subway", ignoreCase = true) -> "🥪"
                item.name.contains("Canteen", ignoreCase = true) -> "🍲"
                item.name.contains("Tea", ignoreCase = true) -> "🧋"
                item.name.contains("coffee", ignoreCase = true) -> "☕"
                item.type == "head" -> "👑"
                item.type == "face" -> "😎"
                item.type == "body" -> "👕"
                item.type == "badge" -> "🏅"
                else -> "🎁"
            }
        }
        
        private fun getCategoryName(type: String): String {
            return when (type) {
                "head" -> "Headwear"
                "face" -> "Face"
                "body" -> "Outfit"
                "badge" -> "Badge"
                else -> "Item"
            }
        }
    }
}
