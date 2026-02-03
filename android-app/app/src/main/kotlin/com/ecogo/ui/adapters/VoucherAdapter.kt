package com.ecogo.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.ecogo.data.Voucher

class VoucherAdapter(
    private var vouchers: List<Voucher>,
    private val onVoucherClick: (Voucher) -> Unit = {}
) : RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_voucher, parent, false)
        return VoucherViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: VoucherViewHolder, position: Int) {
        val voucher = vouchers[position]
        holder.bind(voucher)
        holder.itemView.setOnClickListener {
            onVoucherClick(voucher)
        }
    }
    
    override fun getItemCount() = vouchers.size
    
    fun updateVouchers(newVouchers: List<Voucher>) {
        vouchers = newVouchers
        notifyDataSetChanged()
    }
    
    class VoucherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: TextView = itemView.findViewById(R.id.text_icon)
        private val name: TextView = itemView.findViewById(R.id.text_name)
        private val description: TextView = itemView.findViewById(R.id.text_description)
        private val cost: TextView = itemView.findViewById(R.id.text_cost)
        private val button: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.button_redeem)
        private val statusChip: com.google.android.material.chip.Chip = itemView.findViewById(R.id.chip_status)
        
        fun bind(voucher: Voucher) {
            name.text = voucher.name
            description.text = voucher.description
            cost.text = voucher.cost.toString()
            button.text = if (voucher.available) "Redeem" else "Sold Out"
            button.isEnabled = voucher.available
            
            // Set icon
            icon.text = when {
                voucher.name.contains("Starbucks", ignoreCase = true) -> "☕"
                voucher.name.contains("Subway", ignoreCase = true) -> "🥪"
                voucher.name.contains("Canteen", ignoreCase = true) -> "🍲"
                voucher.name.contains("Tea", ignoreCase = true) -> "🧋"
                voucher.name.contains("咖啡", ignoreCase = true) -> "☕"
                voucher.name.contains("食堂", ignoreCase = true) -> "🍲"
                else -> "🎁"
            }
            
            // Set icon background color
            val iconColor = try {
                when {
                    voucher.name.contains("Starbucks", ignoreCase = true) -> 
                        android.graphics.Color.parseColor("#00704A")
                    voucher.name.contains("Subway", ignoreCase = true) -> 
                        android.graphics.Color.parseColor("#FFC72C")
                    voucher.name.contains("Canteen", ignoreCase = true) || 
                    voucher.name.contains("食堂", ignoreCase = true) -> 
                        android.graphics.Color.parseColor("#F97316")
                    voucher.name.contains("Tea", ignoreCase = true) -> 
                        android.graphics.Color.parseColor("#DC2626")
                    else -> android.graphics.Color.parseColor("#15803D")
                }
            } catch (e: Exception) {
                itemView.context.getColor(com.ecogo.R.color.primary)
            }
            
            // 使用cardBackgroundColor而不是直接设置background
            try {
                (itemView.findViewById<View>(R.id.card_icon) as? com.google.android.material.card.MaterialCardView)?.setCardBackgroundColor(iconColor)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Show popular badge for lower point coupons
            if (voucher.cost < 600 && voucher.available) {
                statusChip.visibility = View.VISIBLE
                statusChip.text = "Popular"
            } else {
                statusChip.visibility = View.GONE
            }
            
            itemView.alpha = if (voucher.available) 1.0f else 0.6f
        }
    }
}
