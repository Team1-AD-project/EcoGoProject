package com.ecogo.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecogo.R
import com.ecogo.ui.fragments.MonthStat

class MonthStatAdapter(
    private var stats: List<MonthStat>
) : RecyclerView.Adapter<MonthStatAdapter.MonthStatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthStatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_month_stat, parent, false)
        return MonthStatViewHolder(view)
    }

    override fun onBindViewHolder(holder: MonthStatViewHolder, position: Int) {
        holder.bind(stats[position])
    }

    override fun getItemCount(): Int = stats.size

    fun updateData(newStats: List<MonthStat>) {
        stats = newStats
        notifyDataSetChanged()
    }

    class MonthStatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: TextView = itemView.findViewById(R.id.text_stat_icon)
        private val value: TextView = itemView.findViewById(R.id.text_stat_value)
        private val title: TextView = itemView.findViewById(R.id.text_stat_title)
        private val subtitle: TextView = itemView.findViewById(R.id.text_stat_subtitle)

        fun bind(stat: MonthStat) {
            icon.text = stat.icon
            value.text = stat.value
            title.text = stat.title
            subtitle.text = stat.subtitle
            
            // Apply color to icon background with rounded rectangle
            try {
                val color = Color.parseColor(stat.color)
                val bgDrawable = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 14f * itemView.resources.displayMetrics.density
                    setColor(Color.argb(30, Color.red(color), Color.green(color), Color.blue(color)))
                }
                icon.background = bgDrawable
                subtitle.setTextColor(color)
            } catch (e: Exception) {
                // Fallback to default colors if parsing fails
            }
        }
    }
}
