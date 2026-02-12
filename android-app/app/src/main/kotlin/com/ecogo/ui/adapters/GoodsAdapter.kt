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

private const val TYPE_HEAD = "head"
private const val TYPE_FACE = "face"
private const val TYPE_BODY = "body"
private const val TYPE_BADGE = "badge"
private const val DEFAULT_STROKE_COLOR = "#BBF7D0"

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
            
            // Apply category-specific styling
            applyCategoryStyle(item.type)
            
            // Set stock status
            if (item.owned) {
                stockChip.visibility = View.VISIBLE
                stockChip.text = "✅ Owned"
                button.isEnabled = false
                button.text = "Owned"
                button.alpha = 0.6f
                itemView.alpha = 0.9f
            } else {
                stockChip.visibility = View.GONE
                button.isEnabled = true
                button.text = "Redeem"
                button.alpha = 1.0f
                itemView.alpha = 1.0f
            }
            
            // Set click listener
            button.setOnClickListener {
                if (!item.owned) {
                    // Trigger redemption in parent
                }
            }
        }
        
        private fun getDescription(item: ShopItem): String {
            return when (item.type) {
                TYPE_HEAD -> when {
                    item.name.contains("Crown") -> "Royal headwear for your LiNUS avatar"
                    item.name.contains("Wizard") -> "Magical hat with mystical powers"
                    item.name.contains("Chef") -> "Professional chef's headwear"
                    item.name.contains("Cowboy") -> "Wild west style hat"
                    else -> "Stylish headwear for LiNUS avatar"
                }
                TYPE_FACE -> when {
                    item.name.contains("VR") -> "Experience virtual reality in style"
                    item.name.contains("Superhero") -> "Protect your secret identity"
                    item.name.contains("Monocle") -> "Classic sophisticated look"
                    else -> "Cool accessory for LiNUS face"
                }
                TYPE_BODY -> when {
                    item.name.contains("Superhero") -> "Save the world with style"
                    item.name.contains("Ninja") -> "Stealthy and stylish outfit"
                    item.name.contains("Tuxedo") -> "Elegant formal attire"
                    item.name.contains("Kimono") -> "Traditional Japanese garment"
                    else -> "Fashionable outfit for LiNUS avatar"
                }
                TYPE_BADGE -> when {
                    item.name.contains("Legend") -> "Ultimate achievement for eco champions"
                    item.name.contains("Pioneer") -> "Early adopter exclusive badge"
                    item.name.contains("Warrior") -> "Earned by true eco heroes"
                    item.name.contains("Streak") -> "Maintain your daily activity streak"
                    else -> "Special achievement badge"
                }
                else -> "Customize your LiNUS avatar"
            }
        }
        
        private fun getIcon(item: ShopItem): String {
            // Extract emoji from name if exists
            val emojiRegex = "[\\p{So}\\p{Sk}]".toRegex()
            val foundEmoji = emojiRegex.find(item.name)
            if (foundEmoji != null) {
                return foundEmoji.value
            }
            
            // Fallback icons
            return when (item.type) {
                TYPE_HEAD -> "👑"
                TYPE_FACE -> "😎"
                TYPE_BODY -> "👕"
                TYPE_BADGE -> "🏅"
                else -> "🎁"
            }
        }
        
        private fun getCategoryName(type: String): String {
            return when (type) {
                TYPE_HEAD -> "👑 Headwear"
                TYPE_FACE -> "😎 Accessory"
                TYPE_BODY -> "👕 Outfit"
                TYPE_BADGE -> "🏅 Badge"
                else -> "🎁 Item"
            }
        }
        
        private fun applyCategoryStyle(type: String) {
            // Apply different background colors for different categories
            val iconCard = itemView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_image)
            val bgColor = try {
                when (type) {
                    TYPE_HEAD -> android.graphics.Color.parseColor("#FEF3C7")     // Yellow
                    TYPE_FACE -> android.graphics.Color.parseColor("#DBEAFE")     // Blue
                    TYPE_BODY -> android.graphics.Color.parseColor("#FCE7F3")     // Pink
                    TYPE_BADGE -> android.graphics.Color.parseColor("#D1FAE5")    // Green
                    else -> android.graphics.Color.parseColor("#F0FDF4")
                }
            } catch (e: Exception) {
                android.graphics.Color.parseColor("#F0FDF4")
            }
            iconCard?.setCardBackgroundColor(bgColor)
            
            // Apply stroke color matching the background
            val strokeColor = try {
                when (type) {
                    TYPE_HEAD -> android.graphics.Color.parseColor("#FDE68A")
                    TYPE_FACE -> android.graphics.Color.parseColor("#BFDBFE")
                    TYPE_BODY -> android.graphics.Color.parseColor("#FBCFE8")
                    TYPE_BADGE -> android.graphics.Color.parseColor(DEFAULT_STROKE_COLOR)
                    else -> android.graphics.Color.parseColor(DEFAULT_STROKE_COLOR)
                }
            } catch (e: Exception) {
                android.graphics.Color.parseColor(DEFAULT_STROKE_COLOR)
            }
            iconCard?.strokeColor = strokeColor
        }
    }
}
